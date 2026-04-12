package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrcamentoDAO {

    private static final Logger log = LoggerFactory.getLogger(OrcamentoDAO.class);

    public boolean salvar(Orcamento o) {
        String sqlOrc = """
            INSERT INTO orcamentos (numero, cliente_id, usuario_id, data_criacao, validade,
                subtotal, desconto, total, observacoes, status, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;
        String sqlItem = """
            INSERT INTO itens_orcamento (orcamento_id, produto_id, quantidade, preco_unit, subtotal)
            VALUES (?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao()) {
            conn.setAutoCommit(false);
            try {
                o.setNumero(gerarNumero(conn));
                try (PreparedStatement ps = conn.prepareStatement(sqlOrc, Statement.RETURN_GENERATED_KEYS)) {
                    int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
                    ps.setString(1, o.getNumero());
                    if (o.getClienteId() > 0) ps.setInt(2, o.getClienteId()); else ps.setNull(2, Types.INTEGER);
                    ps.setInt(3, o.getUsuarioId());
                    ps.setTimestamp(4, Timestamp.valueOf(o.getDataCriacao()));
                    ps.setDate(5, Date.valueOf(o.getValidade()));
                    ps.setDouble(6, o.getSubtotal());
                    ps.setDouble(7, o.getDesconto());
                    ps.setDouble(8, o.getTotal());
                    ps.setString(9, o.getObservacoes());
                    ps.setString(10, o.getStatus());
                    ps.setInt(11, lojaId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) o.setId(rs.getInt(1));
                    }
                }
                for (ItemOrcamento item : o.getItens()) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                        ps.setInt(1, o.getId());
                        ps.setInt(2, item.getProdutoId());
                        ps.setDouble(3, item.getQuantidade());
                        ps.setDouble(4, item.getPrecoUnit());
                        ps.setDouble(5, item.getSubtotal());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Erro ao salvar orçamento", e);
            return false;
        }
    }

    public List<Orcamento> listarTodos() {
        List<Orcamento> lista = new ArrayList<>();
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            SELECT o.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM orcamentos o
            LEFT JOIN clientes c ON c.id = o.cliente_id
            LEFT JOIN usuarios u ON u.id = o.usuario_id
            WHERE o.loja_id = ?
            ORDER BY o.data_criacao DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) { log.error("Erro ao listar orçamentos", e); }
        return lista;
    }

    public Optional<Orcamento> buscarPorId(int id) {
        String sql = """
            SELECT o.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM orcamentos o
            LEFT JOIN clientes c ON c.id = o.cliente_id
            LEFT JOIN usuarios u ON u.id = o.usuario_id
            WHERE o.id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Orcamento o = mapear(rs);
                    o.setItens(listarItens(id));
                    return Optional.of(o);
                }
            }
        } catch (SQLException e) { log.error("Erro ao buscar orçamento", e); }
        return Optional.empty();
    }

    public boolean cancelar(int orcamentoId) {
        String sql = "UPDATE orcamentos SET status='CANCELADO' WHERE id=? AND status='ABERTO'";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orcamentoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao cancelar orçamento", e);
            return false;
        }
    }

    public boolean converterEmVenda(int orcamentoId, int usuarioId) {
        Optional<Orcamento> opt = buscarPorId(orcamentoId);
        if (opt.isEmpty()) return false;
        Orcamento orc = opt.get();
        if (!"ABERTO".equals(orc.getStatus())) return false;

        Venda venda = new Venda();
        venda.setClienteId(orc.getClienteId());
        venda.setClienteNome(orc.getClienteNome());
        venda.setUsuarioId(usuarioId);
        venda.setDesconto(orc.getDesconto());
        venda.setStatus("FINALIZADA");
        venda.setFormaPagamento("DINHEIRO");
        venda.setObservacoes("Convertido do orçamento #" + orc.getNumero());

        for (ItemOrcamento item : orc.getItens()) {
            ItemVenda iv = new ItemVenda();
            iv.setProdutoId(item.getProdutoId());
            iv.setProdutoCodigo(item.getProdutoCodigo());
            iv.setProdutoNome(item.getProdutoNome());
            iv.setPrecoUnit(item.getPrecoUnit());
            iv.setQuantidade(item.getQuantidade());
            iv.calcularSubtotal();
            venda.adicionarItem(iv);
        }
        venda.setValorPago(venda.getTotal());

        VendaDAO vendaDAO = new VendaDAO();
        boolean saved = vendaDAO.salvar(venda);
        if (saved) {
            String sql = "UPDATE orcamentos SET status='APROVADO' WHERE id=?";
            try (Connection conn = DatabaseConfig.getConexao();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, orcamentoId);
                ps.executeUpdate();
            } catch (SQLException e) { log.error("Erro ao atualizar status orçamento", e); }
        }
        return saved;
    }

    private List<ItemOrcamento> listarItens(int orcamentoId) {
        List<ItemOrcamento> lista = new ArrayList<>();
        String sql = """
            SELECT io.*, p.nome AS produto_nome, p.codigo AS produto_codigo
            FROM itens_orcamento io
            JOIN produtos p ON p.id = io.produto_id
            WHERE io.orcamento_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orcamentoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemOrcamento item = new ItemOrcamento();
                    item.setId(rs.getInt("id"));
                    item.setOrcamentoId(orcamentoId);
                    item.setProdutoId(rs.getInt("produto_id"));
                    item.setProdutoCodigo(rs.getString("produto_codigo"));
                    item.setProdutoNome(rs.getString("produto_nome"));
                    item.setQuantidade(rs.getDouble("quantidade"));
                    item.setPrecoUnit(rs.getDouble("preco_unit"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    lista.add(item);
                }
            }
        } catch (SQLException e) { log.error("Erro ao listar itens do orçamento", e); }
        return lista;
    }

    private Orcamento mapear(ResultSet rs) throws SQLException {
        Orcamento o = new Orcamento();
        o.setId(rs.getInt("id"));
        o.setNumero(rs.getString("numero"));
        o.setClienteId(rs.getInt("cliente_id"));
        try { o.setClienteNome(rs.getString("cliente_nome")); } catch (Exception ignored) {}
        o.setUsuarioId(rs.getInt("usuario_id"));
        try { o.setUsuarioNome(rs.getString("usuario_nome")); } catch (Exception ignored) {}
        Timestamp dt = rs.getTimestamp("data_criacao");
        if (dt != null) o.setDataCriacao(dt.toLocalDateTime());
        Date val = rs.getDate("validade");
        if (val != null) o.setValidade(val.toLocalDate());
        o.setSubtotal(rs.getDouble("subtotal"));
        o.setDesconto(rs.getDouble("desconto"));
        o.setTotal(rs.getDouble("total"));
        o.setObservacoes(rs.getString("observacoes"));
        o.setStatus(rs.getString("status"));
        return o;
    }

    private String gerarNumero(Connection conn) throws SQLException {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT COALESCE(MAX(CAST(REGEXP_REPLACE(numero,'[^0-9]','','g') AS BIGINT)),0)+1 FROM orcamentos WHERE loja_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return "ORC-" + String.format("%06d", rs.getLong(1));
            }
        }
        return "ORC-" + String.format("%06d", System.currentTimeMillis() % 1000000);
    }
}
