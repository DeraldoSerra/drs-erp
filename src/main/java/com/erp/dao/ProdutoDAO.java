package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoDAO {

    private static final Logger log = LoggerFactory.getLogger(ProdutoDAO.class);

    public boolean salvar(Produto p) {
        String sql = """
            INSERT INTO produtos (codigo, codigo_barras, nome, descricao, categoria_id, fornecedor_id,
            unidade, preco_custo, preco_venda, margem_lucro, estoque_atual, estoque_minimo,
            estoque_maximo, ncm, cfop, icms_aliquota, ativo, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
            preencherPS(ps, p);
            ps.setInt(18, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar produto", e);
            return false;
        }
    }

    public boolean atualizar(Produto p) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            UPDATE produtos SET codigo=?, codigo_barras=?, nome=?, descricao=?, categoria_id=?, fornecedor_id=?,
            unidade=?, preco_custo=?, preco_venda=?, margem_lucro=?, estoque_atual=?, estoque_minimo=?,
            estoque_maximo=?, ncm=?, cfop=?, icms_aliquota=?, ativo=?, atualizado_em=NOW() WHERE id=? AND loja_id=?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPS(ps, p);
            ps.setInt(18, p.getId());
            ps.setInt(19, lojaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao atualizar produto", e);
            return false;
        }
    }

    public boolean excluir(int id) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "UPDATE produtos SET ativo = FALSE WHERE id = ? AND loja_id = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, lojaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao excluir produto", e);
            return false;
        }
    }

    public boolean atualizarEstoque(int produtoId, double novoEstoque, Connection conn) throws SQLException {
        String sql = "UPDATE produtos SET estoque_atual=?, atualizado_em=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, novoEstoque);
            ps.setInt(2, produtoId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Produto> listarTodos() {
        return listarPorFiltro("", true);
    }

    public List<Produto> listarPorFiltro(String filtro, boolean apenasAtivos) {
        List<Produto> lista = new ArrayList<>();
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String where = apenasAtivos ? "WHERE p.ativo = TRUE" : "WHERE 1=1";
        where += " AND p.loja_id = ?";
        if (filtro != null && !filtro.isBlank()) {
            where += " AND (unaccent(p.nome) ILIKE unaccent(?) OR p.codigo ILIKE ? OR p.codigo_barras ILIKE ?)";
        }
        String sql = """
            SELECT p.*, c.nome AS categoria_nome, f.nome_fantasia AS fornecedor_nome
            FROM produtos p
            LEFT JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN fornecedores f ON f.id = p.fornecedor_id
            """ + where + " ORDER BY p.nome LIMIT 1000";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            if (filtro != null && !filtro.isBlank()) {
                ps.setString(2, "%" + filtro + "%");
                ps.setString(3, "%" + filtro + "%");
                ps.setString(4, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar produtos", e);
        }
        return lista;
    }

    public Optional<Produto> buscarPorCodigoBarras(String codigoBarras) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            SELECT p.*, c.nome AS categoria_nome, f.nome_fantasia AS fornecedor_nome
            FROM produtos p
            LEFT JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN fornecedores f ON f.id = p.fornecedor_id
            WHERE (p.codigo_barras = ? OR p.codigo = ?) AND p.ativo = TRUE AND p.loja_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            ps.setString(2, codigoBarras);
            ps.setInt(3, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar produto por código de barras", e);
        }
        return Optional.empty();
    }

    public List<Produto> listarEstoqueBaixo() {
        List<Produto> lista = new ArrayList<>();
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            SELECT p.*, c.nome AS categoria_nome, f.nome_fantasia AS fornecedor_nome
            FROM produtos p
            LEFT JOIN categorias c ON c.id = p.categoria_id
            LEFT JOIN fornecedores f ON f.id = p.fornecedor_id
            WHERE p.estoque_atual <= p.estoque_minimo AND p.ativo = TRUE AND p.loja_id = ?
            ORDER BY p.estoque_atual ASC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar estoque baixo", e);
        }
        return lista;
    }

    public long contar() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM produtos WHERE ativo=TRUE AND loja_id=?")) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { log.error("Erro ao contar produtos", e); }
        return 0;
    }

    private void preencherPS(PreparedStatement ps, Produto p) throws SQLException {
        ps.setString(1, p.getCodigo());
        ps.setString(2, p.getCodigoBarras());
        ps.setString(3, p.getNome());
        ps.setString(4, p.getDescricao());
        if (p.getCategoriaId() > 0) ps.setInt(5, p.getCategoriaId()); else ps.setNull(5, Types.INTEGER);
        if (p.getFornecedorId() > 0) ps.setInt(6, p.getFornecedorId()); else ps.setNull(6, Types.INTEGER);
        ps.setString(7, p.getUnidade());
        ps.setDouble(8, p.getPrecoCusto());
        ps.setDouble(9, p.getPrecoVenda());
        ps.setDouble(10, p.getMargemLucro());
        ps.setDouble(11, p.getEstoqueAtual());
        ps.setDouble(12, p.getEstoqueMinimo());
        ps.setDouble(13, p.getEstoqueMaximo());
        ps.setString(14, p.getNcm());
        ps.setString(15, p.getCfop());
        ps.setDouble(16, p.getIcmsAliquota());
        ps.setBoolean(17, p.isAtivo());
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNome(rs.getString("nome"));
        p.setDescricao(rs.getString("descricao"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setCategoriaNome(rs.getString("categoria_nome"));
        p.setFornecedorId(rs.getInt("fornecedor_id"));
        p.setFornecedorNome(rs.getString("fornecedor_nome"));
        p.setUnidade(rs.getString("unidade"));
        p.setPrecoCusto(rs.getDouble("preco_custo"));
        p.setPrecoVenda(rs.getDouble("preco_venda"));
        p.setMargemLucro(rs.getDouble("margem_lucro"));
        p.setEstoqueAtual(rs.getDouble("estoque_atual"));
        p.setEstoqueMinimo(rs.getDouble("estoque_minimo"));
        p.setEstoqueMaximo(rs.getDouble("estoque_maximo"));
        p.setNcm(rs.getString("ncm"));
        p.setCfop(rs.getString("cfop"));
        p.setIcmsAliquota(rs.getDouble("icms_aliquota"));
        p.setAtivo(rs.getBoolean("ativo"));
        Timestamp cr = rs.getTimestamp("criado_em");
        if (cr != null) p.setCriadoEm(cr.toLocalDateTime());
        return p;
    }
}
