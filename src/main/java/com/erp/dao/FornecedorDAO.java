package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Fornecedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FornecedorDAO {

    private static final Logger log = LoggerFactory.getLogger(FornecedorDAO.class);

    public boolean salvar(Fornecedor f) {
        String sql = """
            INSERT INTO fornecedores (razao_social, nome_fantasia, cnpj, ie, email, telefone, celular,
            cep, logradouro, numero, complemento, bairro, cidade, estado, contato, observacoes, ativo, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
            preencherPS(ps, f);
            ps.setInt(18, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) f.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar fornecedor", e);
            return false;
        }
    }

    public boolean atualizar(Fornecedor f) {
        String sql = """
            UPDATE fornecedores SET razao_social=?, nome_fantasia=?, cnpj=?, ie=?, email=?, telefone=?,
            celular=?, cep=?, logradouro=?, numero=?, complemento=?, bairro=?, cidade=?, estado=?,
            contato=?, observacoes=?, ativo=?, atualizado_em=NOW() WHERE id=?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPS(ps, f);
            ps.setInt(18, f.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao atualizar fornecedor", e);
            return false;
        }
    }

    public boolean excluir(int id) {
        String sql = "UPDATE fornecedores SET ativo=FALSE WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao excluir fornecedor", e);
            return false;
        }
    }

    public List<Fornecedor> listarTodos() {
        return listarPorFiltro("", true);
    }

    public List<Fornecedor> listarPorFiltro(String filtro, boolean apenasAtivos) {
        List<Fornecedor> lista = new ArrayList<>();
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String where = apenasAtivos ? "WHERE ativo=TRUE" : "WHERE 1=1";
        where += " AND loja_id = ?";
        if (filtro != null && !filtro.isBlank()) {
            where += " AND (unaccent(razao_social) ILIKE unaccent(?) OR unaccent(nome_fantasia) ILIKE unaccent(?) OR cnpj LIKE ?)";
        }
        String sql = "SELECT * FROM fornecedores " + where + " ORDER BY razao_social LIMIT 500";
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
            log.error("Erro ao listar fornecedores", e);
        }
        return lista;
    }

    public Optional<Fornecedor> buscarPorId(int id) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT * FROM fornecedores WHERE id=? AND loja_id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar fornecedor", e);
        }
        return Optional.empty();
    }

    private void preencherPS(PreparedStatement ps, Fornecedor f) throws SQLException {
        ps.setString(1, f.getRazaoSocial());
        ps.setString(2, f.getNomeFantasia());
        ps.setString(3, f.getCnpj());
        ps.setString(4, f.getIe());
        ps.setString(5, f.getEmail());
        ps.setString(6, f.getTelefone());
        ps.setString(7, f.getCelular());
        ps.setString(8, f.getCep());
        ps.setString(9, f.getLogradouro());
        ps.setString(10, f.getNumero());
        ps.setString(11, f.getComplemento());
        ps.setString(12, f.getBairro());
        ps.setString(13, f.getCidade());
        ps.setString(14, f.getEstado());
        ps.setString(15, f.getContato());
        ps.setString(16, f.getObservacoes());
        ps.setBoolean(17, f.isAtivo());
    }

    private Fornecedor mapear(ResultSet rs) throws SQLException {
        Fornecedor f = new Fornecedor();
        f.setId(rs.getInt("id"));
        f.setRazaoSocial(rs.getString("razao_social"));
        f.setNomeFantasia(rs.getString("nome_fantasia"));
        f.setCnpj(rs.getString("cnpj"));
        f.setIe(rs.getString("ie"));
        f.setEmail(rs.getString("email"));
        f.setTelefone(rs.getString("telefone"));
        f.setCelular(rs.getString("celular"));
        f.setCep(rs.getString("cep"));
        f.setLogradouro(rs.getString("logradouro"));
        f.setNumero(rs.getString("numero"));
        f.setComplemento(rs.getString("complemento"));
        f.setBairro(rs.getString("bairro"));
        f.setCidade(rs.getString("cidade"));
        f.setEstado(rs.getString("estado"));
        f.setContato(rs.getString("contato"));
        f.setObservacoes(rs.getString("observacoes"));
        f.setAtivo(rs.getBoolean("ativo"));
        Timestamp cr = rs.getTimestamp("criado_em");
        if (cr != null) f.setCriadoEm(cr.toLocalDateTime());
        return f;
    }
}
