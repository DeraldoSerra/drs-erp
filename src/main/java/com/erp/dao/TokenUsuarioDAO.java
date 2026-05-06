package com.erp.dao;

import com.erp.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia tokens de cadastro de usuário.
 * O dono gera um token; quem tiver o token pode cadastrar um usuário de qualquer perfil.
 * Cada token é de uso único (exceto ilimitado=TRUE).
 */
public class TokenUsuarioDAO {

    private static final Logger log = LoggerFactory.getLogger(TokenUsuarioDAO.class);

    /** Cria a tabela se não existir. Chamado na inicialização. */
    public void criarTabelaSeNecessario() {
        String sql = """
            CREATE TABLE IF NOT EXISTS tokens_usuario (
                id          SERIAL PRIMARY KEY,
                token       TEXT    NOT NULL UNIQUE,
                descricao   TEXT,
                usado       BOOLEAN NOT NULL DEFAULT FALSE,
                ilimitado   BOOLEAN NOT NULL DEFAULT FALSE,
                usado_em    TIMESTAMP,
                criado_em   TIMESTAMP NOT NULL DEFAULT NOW()
            )
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            log.error("Erro ao criar tabela tokens_usuario", e);
        }
    }

    /** Verifica se o token é válido (não usado ou ilimitado). */
    public boolean tokenValido(String token) {
        String sql = "SELECT id FROM tokens_usuario WHERE token = ? AND (usado = FALSE OR ilimitado = TRUE)";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Erro ao validar token usuario", e);
            return false;
        }
    }

    /** Marca o token como usado após cadastro bem-sucedido. Tokens ilimitados não são marcados. */
    public void marcarUsado(String token) {
        String sql = "UPDATE tokens_usuario SET usado=TRUE, usado_em=NOW() WHERE token=? AND ilimitado=FALSE AND usado=FALSE";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.toUpperCase().trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao marcar token usuario como usado", e);
        }
    }

    /** Gera N tokens novos e retorna a lista. */
    public List<String> gerarTokens(int quantidade, String descricao, boolean ilimitado) {
        List<String> gerados = new ArrayList<>();
        String sql = """
            INSERT INTO tokens_usuario (token, descricao, ilimitado)
            VALUES (upper(substring(md5(random()::text),1,6)||'-'||substring(md5(random()::text),1,6)||'-'||substring(md5(random()::text),1,4)), ?, ?)
            RETURNING token
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                ps.setString(1, descricao.isBlank() ? "Token " + (i + 1) : descricao);
                ps.setBoolean(2, ilimitado);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) gerados.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao gerar tokens usuario", e);
        }
        return gerados;
    }

    public record InfoTokenUsuario(String token, String descricao, boolean usado, boolean ilimitado, String criadoEm) {}

    /** Lista todos os tokens para exibição no painel admin. */
    public List<InfoTokenUsuario> listarTodos() {
        List<InfoTokenUsuario> lista = new ArrayList<>();
        String sql = """
            SELECT token, descricao, usado, ilimitado,
                   TO_CHAR(criado_em AT TIME ZONE 'America/Sao_Paulo', 'DD/MM/YYYY HH24:MI') AS criado
            FROM tokens_usuario
            ORDER BY criado_em DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new InfoTokenUsuario(
                    rs.getString("token"),
                    rs.getString("descricao"),
                    rs.getBoolean("usado"),
                    rs.getBoolean("ilimitado"),
                    rs.getString("criado")
                ));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar tokens usuario", e);
        }
        return lista;
    }

    /** Exclui tokens já usados (limpeza). */
    public int limparUsados() {
        String sql = "DELETE FROM tokens_usuario WHERE usado = TRUE AND ilimitado = FALSE";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao limpar tokens usados", e);
            return 0;
        }
    }
}
