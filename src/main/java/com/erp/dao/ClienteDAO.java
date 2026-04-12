package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO {

    private static final Logger log = LoggerFactory.getLogger(ClienteDAO.class);

    public boolean salvar(Cliente c) {
        String sql = """
            INSERT INTO clientes (nome, cpf_cnpj, tipo_pessoa, rg_ie, email, telefone, celular,
            cep, logradouro, numero, complemento, bairro, cidade, estado, limite_credito, observacoes, ativo, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
            preencherPS(ps, c);
            ps.setInt(18, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar cliente", e);
            return false;
        }
    }

    public boolean atualizar(Cliente c) {
        String sql = """
            UPDATE clientes SET nome=?, cpf_cnpj=?, tipo_pessoa=?, rg_ie=?, email=?, telefone=?, celular=?,
            cep=?, logradouro=?, numero=?, complemento=?, bairro=?, cidade=?, estado=?, limite_credito=?,
            observacoes=?, ativo=?, atualizado_em=NOW() WHERE id=?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPS(ps, c);
            ps.setInt(18, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao atualizar cliente", e);
            return false;
        }
    }

    public boolean excluir(int id) {
        String sql = "UPDATE clientes SET ativo = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao excluir cliente", e);
            return false;
        }
    }

    public List<Cliente> listarTodos() {
        return listarPorFiltro("", true);
    }

    public List<Cliente> listarPorFiltro(String filtro, boolean apenasAtivos) {
        List<Cliente> lista = new ArrayList<>();
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String where = apenasAtivos ? "WHERE ativo = TRUE" : "WHERE 1=1";
        where += " AND clientes.loja_id = ?";
        if (filtro != null && !filtro.isBlank()) {
            where += " AND (unaccent(nome) ILIKE unaccent(?) OR cpf_cnpj LIKE ?)";
        }
        String sql = "SELECT * FROM clientes " + where + " ORDER BY nome LIMIT 500";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            if (filtro != null && !filtro.isBlank()) {
                ps.setString(2, "%" + filtro + "%");
                ps.setString(3, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar clientes", e);
        }
        return lista;
    }

    public Optional<Cliente> buscarPorId(int id) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT * FROM clientes WHERE id = ? AND loja_id = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar cliente por id", e);
        }
        return Optional.empty();
    }

    public long contar() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes WHERE ativo=TRUE AND loja_id=?")) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { log.error("Erro ao contar clientes", e); }
        return 0;
    }

    private void preencherPS(PreparedStatement ps, Cliente c) throws SQLException {
        ps.setString(1, c.getNome());
        ps.setString(2, c.getCpfCnpj());
        ps.setString(3, c.getTipoPessoa());
        ps.setString(4, c.getRgIe());
        ps.setString(5, c.getEmail());
        ps.setString(6, c.getTelefone());
        ps.setString(7, c.getCelular());
        ps.setString(8, c.getCep());
        ps.setString(9, c.getLogradouro());
        ps.setString(10, c.getNumero());
        ps.setString(11, c.getComplemento());
        ps.setString(12, c.getBairro());
        ps.setString(13, c.getCidade());
        ps.setString(14, c.getEstado());
        ps.setDouble(15, c.getLimiteCredito());
        ps.setString(16, c.getObservacoes());
        ps.setBoolean(17, c.isAtivo());
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setCpfCnpj(rs.getString("cpf_cnpj"));
        c.setTipoPessoa(rs.getString("tipo_pessoa"));
        c.setRgIe(rs.getString("rg_ie"));
        c.setEmail(rs.getString("email"));
        c.setTelefone(rs.getString("telefone"));
        c.setCelular(rs.getString("celular"));
        c.setCep(rs.getString("cep"));
        c.setLogradouro(rs.getString("logradouro"));
        c.setNumero(rs.getString("numero"));
        c.setComplemento(rs.getString("complemento"));
        c.setBairro(rs.getString("bairro"));
        c.setCidade(rs.getString("cidade"));
        c.setEstado(rs.getString("estado"));
        c.setLimiteCredito(rs.getDouble("limite_credito"));
        c.setObservacoes(rs.getString("observacoes"));
        c.setAtivo(rs.getBoolean("ativo"));
        Timestamp cr = rs.getTimestamp("criado_em");
        if (cr != null) c.setCriadoEm(cr.toLocalDateTime());
        return c;
    }
}
