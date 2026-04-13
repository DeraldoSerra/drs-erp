package com.erp.service;

import com.erp.config.DatabaseConfig;
import com.erp.util.AppInfo;

import java.sql.*;

/**
 * Consulta a tabela `versoes` no Neon para verificar se há atualização disponível.
 */
public class AtualizacaoService {

    public record Versao(String versao, String descricao, String urlDownload, boolean obrigatoria) {}

    /** Compara versões no formato "X.Y.Z". Retorna >0 se a > b. */
    public static int comparar(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int na = i < pa.length ? Integer.parseInt(pa[i].trim()) : 0;
            int nb = i < pb.length ? Integer.parseInt(pb[i].trim()) : 0;
            if (na != nb) return na - nb;
        }
        return 0;
    }

    /**
     * Busca a versão mais recente disponível no banco.
     * @return Versao com dados da última versão, ou null se não conseguir conectar.
     */
    public static Versao buscarUltimaVersao() {
        String sql = "SELECT versao, descricao, url_download, obrigatoria " +
                     "FROM versoes ORDER BY data_lancamento DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new Versao(
                    rs.getString("versao"),
                    rs.getString("descricao"),
                    rs.getString("url_download"),
                    rs.getBoolean("obrigatoria")
                );
            }
        } catch (SQLException e) {
            // Silencioso — sem internet ou banco offline
        }
        return null;
    }

    /**
     * Verifica se existe uma versão mais nova que a atual.
     * @return a nova Versao se disponível, null caso contrário.
     */
    public static Versao verificarAtualizacao() {
        Versao ultima = buscarUltimaVersao();
        if (ultima == null) return null;
        String versaoAtual = AppInfo.getVersao();
        if (comparar(ultima.versao(), versaoAtual) > 0) {
            return ultima;
        }
        return null;
    }
}
