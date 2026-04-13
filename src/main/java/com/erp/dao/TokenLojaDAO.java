package com.erp.dao;

import com.erp.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TokenLojaDAO {

    private static final Logger log = LoggerFactory.getLogger(TokenLojaDAO.class);

    /** Verifica se o token existe e ainda é válido (não usado OU ilimitado). */
    public boolean tokenValido(String token) {
        String sql = "SELECT id FROM tokens_loja WHERE token = ? AND (usado = FALSE OR ilimitado = TRUE)";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Erro ao validar token", e);
            return false;
        }
    }

    /** Marca o token como usado, associando à loja criada. Tokens ilimitados não são marcados. */
    public boolean marcarUsado(String token, int lojaId) {
        String sql = "UPDATE tokens_loja SET usado=TRUE, loja_id=?, usado_em=NOW() WHERE token=? AND ilimitado=FALSE AND usado=FALSE";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            ps.setString(2, token.toUpperCase().trim());
            ps.executeUpdate(); // ilimitado não é afetado, retorna 0 rows — OK
            return true;
        } catch (SQLException e) {
            log.error("Erro ao marcar token como usado", e);
            return false;
        }
    }

    /** Gera N novos tokens e retorna a lista gerada. */
    public List<String> gerarTokens(int quantidade, String descricaoBase) {
        List<String> gerados = new ArrayList<>();
        String sql = "INSERT INTO tokens_loja (token, descricao) VALUES (upper(substring(md5(random()::text),1,8)||'-'||substring(md5(random()::text),1,4)||'-'||substring(md5(random()::text),1,4)), ?) RETURNING token";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= quantidade; i++) {
                ps.setString(1, descricaoBase + " " + i);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) gerados.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao gerar tokens", e);
        }
        return gerados;
    }

    public record InfoToken(String token, String descricao, boolean usado, String lojaId, String criadoEm) {}

    /** Lista todos os tokens (para painel admin). */
    public List<InfoToken> listarTodos() {
        List<InfoToken> lista = new ArrayList<>();
        String sql = """
            SELECT t.token, t.descricao, t.usado,
                   COALESCE(l.nome, '-') AS loja_nome,
                   TO_CHAR(t.criado_em AT TIME ZONE 'America/Sao_Paulo', 'DD/MM/YYYY HH24:MI') AS criado
            FROM tokens_loja t
            LEFT JOIN lojas l ON l.id = t.loja_id
            ORDER BY t.criado_em DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new InfoToken(
                    rs.getString("token"),
                    rs.getString("descricao"),
                    rs.getBoolean("usado"),
                    rs.getString("loja_nome"),
                    rs.getString("criado")
                ));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar tokens", e);
        }
        return lista;
    }
}
