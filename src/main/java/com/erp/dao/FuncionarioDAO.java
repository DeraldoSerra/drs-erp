package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Funcionario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FuncionarioDAO {

    private static final Logger log = LoggerFactory.getLogger(FuncionarioDAO.class);

    public boolean salvar(Funcionario f) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            INSERT INTO funcionarios (nome, cpf, rg, data_nascimento, data_admissao, cargo_id, salario,
            email, telefone, celular, cep, logradouro, numero, complemento, bairro, cidade, estado, observacoes, ativo, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherPS(ps, f);
            ps.setInt(20, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) f.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar funcionário", e);
            return false;
        }
    }

    public boolean atualizar(Funcionario f) {
        String sql = """
            UPDATE funcionarios SET nome=?, cpf=?, rg=?, data_nascimento=?, data_admissao=?, cargo_id=?,
            salario=?, email=?, telefone=?, celular=?, cep=?, logradouro=?, numero=?, complemento=?,
            bairro=?, cidade=?, estado=?, observacoes=?, ativo=?, atualizado_em=NOW() WHERE id=?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPS(ps, f);
            ps.setInt(20, f.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao atualizar funcionário", e);
            return false;
        }
    }

    public boolean excluir(int id) {
        String sql = "UPDATE funcionarios SET ativo=FALSE WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao excluir funcionário", e);
            return false;
        }
    }

    public List<Funcionario> listarTodos() {
        return listarPorFiltro("", true);
    }

    public List<Funcionario> listarPorFiltro(String filtro, boolean apenasAtivos) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<Funcionario> lista = new ArrayList<>();
        String where = apenasAtivos ? "WHERE f.ativo=TRUE AND f.loja_id=" + lojaId
                                    : "WHERE f.loja_id=" + lojaId;
        if (filtro != null && !filtro.isBlank()) {
            where += " AND (unaccent(f.nome) ILIKE unaccent(?) OR f.cpf LIKE ?)";
        }
        String sql = """
            SELECT f.*, c.nome AS cargo_nome FROM funcionarios f
            LEFT JOIN cargos c ON c.id = f.cargo_id
            """ + where + " ORDER BY f.nome LIMIT 500";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (filtro != null && !filtro.isBlank()) {
                ps.setString(1, "%" + filtro + "%");
                ps.setString(2, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar funcionários", e);
        }
        return lista;
    }

    public Optional<Funcionario> buscarPorId(int id) {
        String sql = "SELECT f.*, c.nome AS cargo_nome FROM funcionarios f LEFT JOIN cargos c ON c.id=f.cargo_id WHERE f.id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar funcionário", e);
        }
        return Optional.empty();
    }

    public long contar() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM funcionarios WHERE ativo=TRUE AND loja_id=?")) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { log.error("Erro", e); }
        return 0;
    }

    private void preencherPS(PreparedStatement ps, Funcionario f) throws SQLException {
        ps.setString(1, f.getNome());
        ps.setString(2, f.getCpf());
        ps.setString(3, f.getRg());
        if (f.getDataNascimento() != null) ps.setDate(4, Date.valueOf(f.getDataNascimento())); else ps.setNull(4, Types.DATE);
        ps.setDate(5, Date.valueOf(f.getDataAdmissao()));
        if (f.getCargoId() > 0) ps.setInt(6, f.getCargoId()); else ps.setNull(6, Types.INTEGER);
        ps.setDouble(7, f.getSalario());
        ps.setString(8, f.getEmail());
        ps.setString(9, f.getTelefone());
        ps.setString(10, f.getCelular());
        ps.setString(11, f.getCep());
        ps.setString(12, f.getLogradouro());
        ps.setString(13, f.getNumero());
        ps.setString(14, f.getComplemento());
        ps.setString(15, f.getBairro());
        ps.setString(16, f.getCidade());
        ps.setString(17, f.getEstado());
        ps.setString(18, f.getObservacoes());
        ps.setBoolean(19, f.isAtivo());
    }

    private Funcionario mapear(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario();
        f.setId(rs.getInt("id"));
        f.setNome(rs.getString("nome"));
        f.setCpf(rs.getString("cpf"));
        f.setRg(rs.getString("rg"));
        Date dn = rs.getDate("data_nascimento");
        if (dn != null) f.setDataNascimento(dn.toLocalDate());
        Date da = rs.getDate("data_admissao");
        if (da != null) f.setDataAdmissao(da.toLocalDate());
        Date dd = rs.getDate("data_demissao");
        if (dd != null) f.setDataDemissao(dd.toLocalDate());
        f.setCargoId(rs.getInt("cargo_id"));
        f.setCargoNome(rs.getString("cargo_nome"));
        f.setSalario(rs.getDouble("salario"));
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
        f.setObservacoes(rs.getString("observacoes"));
        f.setAtivo(rs.getBoolean("ativo"));
        return f;
    }
}
