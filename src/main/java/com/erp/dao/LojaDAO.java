package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Loja;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LojaDAO {

    private static final Logger log = LoggerFactory.getLogger(LojaDAO.class);

    public List<Loja> listarAtivas() {
        List<Loja> lista = new ArrayList<>();
        String sql = "SELECT * FROM lojas WHERE ativa = TRUE ORDER BY nome";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar lojas ativas", e);
        }
        return lista;
    }

    public List<Loja> listarTodas() {
        List<Loja> lista = new ArrayList<>();
        String sql = "SELECT * FROM lojas ORDER BY nome";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar lojas", e);
        }
        return lista;
    }

    public Optional<Loja> buscarPorId(int id) {
        String sql = "SELECT * FROM lojas WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar loja", e);
        }
        return Optional.empty();
    }

    public boolean salvar(Loja loja) throws SQLException {
        if (loja.getId() == 0) {
            String sql = "INSERT INTO lojas (nome, cnpj, endereco, ativa) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConfig.getConexao();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, loja.getNome());
                ps.setString(2, loja.getCnpj().isEmpty() ? null : loja.getCnpj());
                ps.setString(3, loja.getEndereco().isEmpty() ? null : loja.getEndereco());
                ps.setBoolean(4, loja.isAtiva());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) loja.setId(rs.getInt(1));
                }
                return true;
            } catch (SQLException e) {
                log.error("Erro ao inserir loja: {}", e.getMessage(), e);
                throw e;
            }
        } else {
            String sql = "UPDATE lojas SET nome=?, cnpj=?, endereco=?, ativa=? WHERE id=?";
            try (Connection conn = DatabaseConfig.getConexao();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, loja.getNome());
                ps.setString(2, loja.getCnpj().isEmpty() ? null : loja.getCnpj());
                ps.setString(3, loja.getEndereco().isEmpty() ? null : loja.getEndereco());
                ps.setBoolean(4, loja.isAtiva());
                ps.setInt(5, loja.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                log.error("Erro ao atualizar loja: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    public boolean ativar(int id) {
        return setAtiva(id, true);
    }

    public boolean desativar(int id) {
        return setAtiva(id, false);
    }

    private boolean setAtiva(int id, boolean ativa) {
        String sql = "UPDATE lojas SET ativa = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, ativa);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao alterar status da loja", e);
            return false;
        }
    }

    public boolean bloquear(int id, String motivo) {
        String sql = "UPDATE lojas SET bloqueada=TRUE, motivo_bloqueio=?, bloqueada_em=NOW() WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao bloquear loja", e);
            return false;
        }
    }

    public boolean desbloquear(int id) {
        String sql = "UPDATE lojas SET bloqueada=FALSE, motivo_bloqueio=NULL, bloqueada_em=NULL WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao desbloquear loja", e);
            return false;
        }
    }

    private Loja mapear(ResultSet rs) throws SQLException {
        Loja l = new Loja();
        l.setId(rs.getInt("id"));
        l.setNome(rs.getString("nome"));
        l.setCnpj(rs.getString("cnpj"));
        l.setEndereco(rs.getString("endereco"));
        l.setAtiva(rs.getBoolean("ativa"));
        try { l.setBloqueada(rs.getBoolean("bloqueada")); } catch (SQLException ignored) {}
        try { l.setMotivoBloqueio(rs.getString("motivo_bloqueio")); } catch (SQLException ignored) {}
        return l;
    }
}
