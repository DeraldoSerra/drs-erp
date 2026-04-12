package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Usuario;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAO {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDAO.class);

    public Optional<Usuario> autenticar(String login, String senha) {
        String sql = "SELECT * FROM usuarios WHERE login = ? AND ativo = TRUE";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("senha_hash");
                    BCrypt.Result result = BCrypt.verifyer().verify(senha.toCharArray(), hash);
                    if (result.verified) {
                        return Optional.of(mapear(rs));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao autenticar usuário", e);
        }
        return Optional.empty();
    }

    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nome";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar usuários", e);
        }
        return lista;
    }

    public boolean salvar(Usuario u, String senhaPlana) {
        String sql = "INSERT INTO usuarios (nome, login, senha_hash, perfil, ativo) VALUES (?,?,?,?,?)";
        String hash = BCrypt.withDefaults().hashToString(12, senhaPlana.toCharArray());
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getLogin());
            ps.setString(3, hash);
            ps.setString(4, u.getPerfil());
            ps.setBoolean(5, u.isAtivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) u.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar usuário", e);
            return false;
        }
    }

    public boolean atualizar(Usuario u) {
        String sql = "UPDATE usuarios SET nome=?, perfil=?, ativo=?, atualizado_em=NOW() WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getPerfil());
            ps.setBoolean(3, u.isAtivo());
            ps.setInt(4, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao atualizar usuário", e);
            return false;
        }
    }

    public boolean alterarSenha(int id, String novaSenha) {
        String hash = BCrypt.withDefaults().hashToString(12, novaSenha.toCharArray());
        String sql = "UPDATE usuarios SET senha_hash=?, atualizado_em=NOW() WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao alterar senha", e);
            return false;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setLogin(rs.getString("login"));
        u.setSenhaHash(rs.getString("senha_hash"));
        u.setPerfil(rs.getString("perfil"));
        u.setAtivo(rs.getBoolean("ativo"));
        Timestamp cr = rs.getTimestamp("criado_em");
        if (cr != null) u.setCriadoEm(cr.toLocalDateTime());
        return u;
    }
}
