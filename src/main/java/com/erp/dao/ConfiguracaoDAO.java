package com.erp.dao;

import com.erp.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Armazena configurações gerais do sistema como pares chave=valor.
 * Tabela: configuracoes (chave TEXT PK, valor TEXT)
 */
public class ConfiguracaoDAO {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracaoDAO.class);

    public void criarTabelaSeNecessario() {
        String sql = """
            CREATE TABLE IF NOT EXISTS configuracoes (
                chave TEXT PRIMARY KEY,
                valor TEXT
            )
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            log.error("Erro ao criar tabela configuracoes", e);
        }
    }

    /** Retorna o valor da chave, ou null se não encontrada. */
    public String get(String chave) {
        String sql = "SELECT valor FROM configuracoes WHERE chave = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("valor") : null;
            }
        } catch (SQLException e) {
            log.error("Erro ao ler configuracao [{}]", chave, e);
            return null;
        }
    }

    /** Retorna o valor ou o padrão se não encontrado. */
    public String get(String chave, String padrao) {
        String v = get(chave);
        return v != null ? v : padrao;
    }

    /** Insere ou atualiza o valor de uma chave. */
    public void set(String chave, String valor) {
        String sql = """
            INSERT INTO configuracoes (chave, valor) VALUES (?, ?)
            ON CONFLICT (chave) DO UPDATE SET valor = EXCLUDED.valor
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chave);
            ps.setString(2, valor);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao salvar configuracao [{}]", chave, e);
        }
    }

    /** Remove uma chave. */
    public void remove(String chave) {
        String sql = "DELETE FROM configuracoes WHERE chave = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chave);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao remover configuracao [{}]", chave, e);
        }
    }
}
