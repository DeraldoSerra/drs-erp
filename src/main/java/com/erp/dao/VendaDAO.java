package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.ItemVenda;
import com.erp.model.Venda;
import com.erp.util.Sessao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendaDAO {

    private static final Logger log = LoggerFactory.getLogger(VendaDAO.class);
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    public VendaDAO() {
        migrarColunas();
    }

    /** Garante que a coluna reembolsado exista na tabela vendas (compatibilidade com instâncias antigas). */
    private void migrarColunas() {
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE vendas ADD COLUMN IF NOT EXISTS reembolsado BOOLEAN DEFAULT FALSE");
        } catch (Exception e) {
            log.warn("Migração de colunas: {}", e.getMessage());
        }
    }

    public boolean salvar(Venda v) {
        String sqlVenda = """
            INSERT INTO vendas (numero, cliente_id, usuario_id, data_venda, subtotal, desconto,
            acrescimo, total, forma_pagamento, valor_pago, troco, status, observacoes, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        String sqlItem = """
            INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unit, desconto, subtotal)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao()) {
            conn.setAutoCommit(false);
            try {
                // Gerar número da venda
                v.setNumero(gerarNumero(conn));

                try (PreparedStatement ps = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, v.getNumero());
                    if (v.getClienteId() > 0) ps.setInt(2, v.getClienteId()); else ps.setNull(2, Types.INTEGER);
                    ps.setInt(3, v.getUsuarioId());
                    ps.setTimestamp(4, Timestamp.valueOf(v.getDataVenda()));
                    ps.setDouble(5, v.getSubtotal());
                    ps.setDouble(6, v.getDesconto());
                    ps.setDouble(7, v.getAcrescimo());
                    ps.setDouble(8, v.getTotal());
                    ps.setString(9, v.getFormaPagamento());
                    ps.setDouble(10, v.getValorPago());
                    ps.setDouble(11, v.getTroco());
                    ps.setString(12, v.getStatus());
                    ps.setString(13, v.getObservacoes());
                    ps.setInt(14, Sessao.getInstance().getLojaId());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) v.setId(rs.getInt(1));
                    }
                }

                // Itens + baixa de estoque
                for (ItemVenda item : v.getItens()) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                        ps.setInt(1, v.getId());
                        ps.setInt(2, item.getProdutoId());
                        ps.setDouble(3, item.getQuantidade());
                        ps.setDouble(4, item.getPrecoUnit());
                        ps.setDouble(5, item.getDesconto());
                        ps.setDouble(6, item.getSubtotal());
                        ps.executeUpdate();
                    }
                    // Baixar estoque
                    baixarEstoque(conn, item.getProdutoId(), item.getQuantidade(), v.getId(), v.getUsuarioId());
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Erro ao salvar venda", e);
            return false;
        }
    }

    public boolean cancelar(int vendaId, int usuarioId, String motivo) {
        int lojaId = Sessao.getInstance().getLojaId();
        String sql = "UPDATE vendas SET status='CANCELADA', observacoes=? WHERE id=? AND loja_id=?";
        try (Connection conn = DatabaseConfig.getConexao()) {
            conn.setAutoCommit(false);
            try {
                List<ItemVenda> itens = listarItens(vendaId);
                for (ItemVenda item : itens) {
                    estornarEstoque(conn, item.getProdutoId(), item.getQuantidade(), vendaId, usuarioId);
                }
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, motivo != null ? motivo : "");
                    ps.setInt(2, vendaId);
                    ps.setInt(3, lojaId);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Erro ao cancelar venda", e);
            return false;
        }
    }

    /** Compatibilidade retroativa sem motivo. */
    public boolean cancelar(int vendaId, int usuarioId) {
        return cancelar(vendaId, usuarioId, "");
    }

    public List<Venda> listarCanceladas(LocalDate inicio, LocalDate fim) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<Venda> lista = new ArrayList<>();
        String sql = """
            SELECT v.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM vendas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            LEFT JOIN usuarios u ON u.id = v.usuario_id
            WHERE v.status = 'CANCELADA'
              AND v.data_venda >= ? AND v.data_venda < ? AND v.loja_id = ?
            ORDER BY v.data_venda DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
            ps.setInt(3, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar vendas canceladas", e);
        }
        return lista;
    }

    public List<Venda> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<Venda> lista = new ArrayList<>();
        // Usa range de TIMESTAMP para evitar problemas de fuso horário com setDate/JDBC
        String sql = """
            SELECT v.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM vendas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            LEFT JOIN usuarios u ON u.id = v.usuario_id
            WHERE v.data_venda >= ? AND v.data_venda < ? AND v.loja_id = ?
            ORDER BY v.data_venda DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
            ps.setInt(3, lojaId);
            log.info("Buscando vendas: {} a {} para loja {}", inicio, fim, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            log.info("Vendas encontradas: {}", lista.size());
        } catch (SQLException e) {
            log.error("Erro ao listar vendas", e);
        }
        return lista;
    }

    public Optional<Venda> buscarPorId(int id) {
        int lojaId = Sessao.getInstance().getLojaId();
        String sql = """
            SELECT v.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM vendas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            LEFT JOIN usuarios u ON u.id = v.usuario_id
            WHERE v.id = ? AND v.loja_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venda v = mapear(rs);
                    v.setItens(listarItens(id));
                    return Optional.of(v);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar venda", e);
        }
        return Optional.empty();
    }

    public List<ItemVenda> listarItens(int vendaId) {
        List<ItemVenda> lista = new ArrayList<>();
        String sql = """
            SELECT iv.*, p.nome AS produto_nome, p.codigo AS produto_codigo
            FROM itens_venda iv
            JOIN produtos p ON p.id = iv.produto_id
            WHERE iv.venda_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemVenda item = new ItemVenda();
                    item.setId(rs.getInt("id"));
                    item.setVendaId(vendaId);
                    item.setProdutoId(rs.getInt("produto_id"));
                    item.setProdutoCodigo(rs.getString("produto_codigo"));
                    item.setProdutoNome(rs.getString("produto_nome"));
                    item.setQuantidade(rs.getDouble("quantidade"));
                    item.setPrecoUnit(rs.getDouble("preco_unit"));
                    item.setDesconto(rs.getDouble("desconto"));
                    item.setSubtotal(rs.getDouble("subtotal"));
                    lista.add(item);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao listar itens da venda", e);
        }
        return lista;
    }

    public double totalVendasHoje() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT COALESCE(SUM(total),0) FROM vendas WHERE DATE(data_venda)=CURRENT_DATE AND status='FINALIZADA' AND loja_id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { log.error("Erro", e); }
        return 0;
    }

    public long quantidadeVendasHoje() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT COUNT(*) FROM vendas WHERE DATE(data_venda)=CURRENT_DATE AND status='FINALIZADA' AND loja_id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { log.error("Erro", e); }
        return 0;
    }

    private void baixarEstoque(Connection conn, int produtoId, double qtd, int vendaId, int usuarioId) throws SQLException {
        String sel = "SELECT estoque_atual FROM produtos WHERE id=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setInt(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double anterior = rs.getDouble(1);
                    double novo = anterior - qtd;
                    produtoDAO.atualizarEstoque(produtoId, novo, conn);
                    registrarMovimento(conn, produtoId, "SAIDA", qtd, anterior, novo, "Venda #" + vendaId, vendaId, usuarioId);
                }
            }
        }
    }

    private void estornarEstoque(Connection conn, int produtoId, double qtd, int vendaId, int usuarioId) throws SQLException {
        String sel = "SELECT estoque_atual FROM produtos WHERE id=? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setInt(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double anterior = rs.getDouble(1);
                    double novo = anterior + qtd;
                    produtoDAO.atualizarEstoque(produtoId, novo, conn);
                    registrarMovimento(conn, produtoId, "ENTRADA", qtd, anterior, novo, "Estorno Venda #" + vendaId, vendaId, usuarioId);
                }
            }
        }
    }

    private void registrarMovimento(Connection conn, int produtoId, String tipo, double qtd,
                                     double anterior, double novo, String motivo,
                                     Integer vendaId, int usuarioId) throws SQLException {
        String sql = """
            INSERT INTO movimentacoes_estoque (produto_id, tipo, quantidade, estoque_anterior, estoque_novo, motivo, venda_id, usuario_id)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            ps.setString(2, tipo);
            ps.setDouble(3, qtd);
            ps.setDouble(4, anterior);
            ps.setDouble(5, novo);
            ps.setString(6, motivo);
            if (vendaId != null) ps.setInt(7, vendaId); else ps.setNull(7, Types.INTEGER);
            ps.setInt(8, usuarioId);
            ps.executeUpdate();
        }
    }

    private String gerarNumero(Connection conn) throws SQLException {
        int lojaId = Sessao.getInstance().getLojaId();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(MAX(CAST(numero AS BIGINT)), 0) + 1 FROM vendas WHERE loja_id = ?")) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return String.format("%08d", rs.getLong(1));
            }
        }
        return String.format("%08d", System.currentTimeMillis() % 100000000);
    }

    // ===== ANALYTICS =====

    public List<Object[]> vendasUltimos7Dias() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT TO_CHAR(DATE(data_venda), 'DD/MM') AS dia, COALESCE(SUM(total), 0) AS total
            FROM vendas
            WHERE DATE(data_venda) >= CURRENT_DATE - INTERVAL '6 days'
              AND status = 'FINALIZADA' AND loja_id = ?
            GROUP BY DATE(data_venda), TO_CHAR(DATE(data_venda), 'DD/MM')
            ORDER BY DATE(data_venda)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Object[]{rs.getString("dia"), rs.getDouble("total")});
            }
        } catch (SQLException e) { log.error("Erro vendasUltimos7Dias", e); }
        return lista;
    }

    public List<Object[]> vendasPorFormaMes() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT forma_pagamento, COUNT(*) AS qtd
            FROM vendas
            WHERE EXTRACT(MONTH FROM data_venda) = EXTRACT(MONTH FROM CURRENT_DATE)
              AND EXTRACT(YEAR FROM data_venda) = EXTRACT(YEAR FROM CURRENT_DATE)
              AND status = 'FINALIZADA' AND loja_id = ?
            GROUP BY forma_pagamento ORDER BY qtd DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Object[]{rs.getString("forma_pagamento"), rs.getLong("qtd")});
            }
        } catch (SQLException e) { log.error("Erro vendasPorFormaMes", e); }
        return lista;
    }

    public List<Object[]> topProdutosMes() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT p.nome, SUM(iv.quantidade) AS qtd_total
            FROM itens_venda iv
            JOIN produtos p ON p.id = iv.produto_id
            JOIN vendas v ON v.id = iv.venda_id
            WHERE EXTRACT(MONTH FROM v.data_venda) = EXTRACT(MONTH FROM CURRENT_DATE)
              AND EXTRACT(YEAR FROM v.data_venda) = EXTRACT(YEAR FROM CURRENT_DATE)
              AND v.status = 'FINALIZADA' AND v.loja_id = ?
            GROUP BY p.nome ORDER BY qtd_total DESC LIMIT 5
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Object[]{rs.getString("nome"), rs.getDouble("qtd_total")});
            }
        } catch (SQLException e) { log.error("Erro topProdutosMes", e); }
        return lista;
    }

    public double totalVendasMes() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            SELECT COALESCE(SUM(total), 0) FROM vendas
            WHERE EXTRACT(MONTH FROM data_venda) = EXTRACT(MONTH FROM CURRENT_DATE)
              AND EXTRACT(YEAR FROM data_venda) = EXTRACT(YEAR FROM CURRENT_DATE)
              AND status = 'FINALIZADA' AND loja_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { log.error("Erro totalVendasMes", e); }
        return 0;
    }

    public long clientesAtendidosMes() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            SELECT COUNT(DISTINCT cliente_id) FROM vendas
            WHERE EXTRACT(MONTH FROM data_venda) = EXTRACT(MONTH FROM CURRENT_DATE)
              AND EXTRACT(YEAR FROM data_venda) = EXTRACT(YEAR FROM CURRENT_DATE)
              AND status = 'FINALIZADA' AND cliente_id IS NOT NULL AND loja_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { log.error("Erro clientesAtendidosMes", e); }
        return 0;
    }

    public List<Venda> listarVendasDaSessao(int sessaoId) {
        int lojaId = Sessao.getInstance().getLojaId();
        List<Venda> lista = new ArrayList<>();
        // Busca todas as vendas cujo data_venda está dentro do período da sessão
        String sql = """
            SELECT v.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM vendas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            LEFT JOIN usuarios u ON u.id = v.usuario_id
            WHERE v.loja_id = ?
              AND v.data_venda >= (SELECT abertura FROM sessoes_caixa WHERE id = ?)
              AND (v.data_venda <= (SELECT COALESCE(fechamento, NOW()) FROM sessoes_caixa WHERE id = ?))
            ORDER BY v.data_venda ASC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            ps.setInt(2, sessaoId);
            ps.setInt(3, sessaoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Venda v = mapear(rs);
                    v.setItens(listarItens(v.getId()));
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao listar vendas da sessão", e);
        }
        return lista;
    }

    public Optional<Venda> buscarPorNumero(String numero) {
        int lojaId = Sessao.getInstance().getLojaId();
        String sql = """
            SELECT v.*, c.nome AS cliente_nome, u.nome AS usuario_nome
            FROM vendas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            LEFT JOIN usuarios u ON u.id = v.usuario_id
            WHERE v.numero = ? AND v.loja_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setInt(2, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venda v = mapear(rs);
                    v.setItens(listarItens(v.getId()));
                    return Optional.of(v);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar venda por número", e);
        }
        return Optional.empty();
    }

    private Venda mapear(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.setId(rs.getInt("id"));
        v.setNumero(rs.getString("numero"));
        v.setClienteId(rs.getInt("cliente_id"));
        v.setClienteNome(rs.getString("cliente_nome"));
        v.setUsuarioId(rs.getInt("usuario_id"));
        v.setUsuarioNome(rs.getString("usuario_nome"));
        Timestamp dt = rs.getTimestamp("data_venda");
        if (dt != null) v.setDataVenda(dt.toLocalDateTime());
        v.setFormaPagamento(rs.getString("forma_pagamento"));
        v.setStatus(rs.getString("status"));
        v.setObservacoes(rs.getString("observacoes"));

        // Setters abaixo chamam recalcular() internamente — defini-los primeiro;
        // depois sobrescrevemos subtotal/total/troco com os valores reais do banco.
        v.setDesconto(rs.getDouble("desconto"));
        v.setAcrescimo(rs.getDouble("acrescimo"));
        v.setValorPago(rs.getDouble("valor_pago"));

        // Restaura os valores corretos do banco (recalcular() sobrescreve com itens vazios).
        v.setSubtotal(rs.getDouble("subtotal"));
        v.setTotal(rs.getDouble("total"));
        v.setTroco(rs.getDouble("troco"));

        try { v.setReembolsado(rs.getBoolean("reembolsado")); } catch (SQLException ignored) {}
        return v;
    }

    /** Marca uma venda como reembolsada no banco. */
    public boolean marcarReembolsado(int vendaId) {
        String sql = "UPDATE vendas SET reembolsado=TRUE WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Erro ao marcar venda como reembolsada", e);
            return false;
        }
    }
}
