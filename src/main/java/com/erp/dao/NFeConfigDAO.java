package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.NFeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class NFeConfigDAO {

    private static final Logger log = LoggerFactory.getLogger(NFeConfigDAO.class);

    public Optional<NFeConfig> carregar() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT * FROM nfe_config WHERE loja_id = ?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao carregar NFeConfig", e);
        }
        return Optional.empty();
    }

    public boolean salvar(NFeConfig cfg) {
        String sql = """
            INSERT INTO nfe_config (certificado_path, certificado_senha, cnpj, ie,
                razao_social, nome_fantasia, logradouro, numero_end, bairro,
                cod_municipio, municipio, uf, cep, telefone, regime_tributario,
                serie, proximo_numero, ambiente, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (loja_id) DO UPDATE SET
                certificado_path=EXCLUDED.certificado_path,
                certificado_senha=EXCLUDED.certificado_senha,
                cnpj=EXCLUDED.cnpj, ie=EXCLUDED.ie,
                razao_social=EXCLUDED.razao_social,
                nome_fantasia=EXCLUDED.nome_fantasia,
                logradouro=EXCLUDED.logradouro,
                numero_end=EXCLUDED.numero_end,
                bairro=EXCLUDED.bairro,
                cod_municipio=EXCLUDED.cod_municipio,
                municipio=EXCLUDED.municipio,
                uf=EXCLUDED.uf, cep=EXCLUDED.cep,
                telefone=EXCLUDED.telefone,
                regime_tributario=EXCLUDED.regime_tributario,
                serie=EXCLUDED.serie,
                proximo_numero=EXCLUDED.proximo_numero,
                ambiente=EXCLUDED.ambiente
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cfg.getCertificadoPath());
            ps.setString(2, cfg.getCertificadoSenha());
            ps.setString(3, cfg.getCnpj());
            ps.setString(4, cfg.getIe());
            ps.setString(5, cfg.getRazaoSocial());
            ps.setString(6, cfg.getNomeFantasia());
            ps.setString(7, cfg.getLogradouro());
            ps.setString(8, cfg.getNumeroEnd());
            ps.setString(9, cfg.getBairro());
            ps.setString(10, cfg.getCodMunicipio());
            ps.setString(11, cfg.getMunicipio());
            ps.setString(12, cfg.getUf());
            ps.setString(13, cfg.getCep());
            ps.setString(14, cfg.getTelefone());
            ps.setInt(15, cfg.getRegimeTributario());
            ps.setInt(16, cfg.getSerie());
            ps.setInt(17, cfg.getProximoNumero());
            ps.setString(18, cfg.getAmbiente());
            int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
            ps.setInt(19, lojaId);
            ps.executeUpdate();
            log.info("NFeConfig salvo com sucesso");
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar NFeConfig", e);
            return false;
        }
    }

    /** Incrementa o próximo número da NF-e e retorna o número usado */
    public int obterProximoNumero() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            UPDATE nfe_config SET proximo_numero = proximo_numero + 1
            WHERE loja_id = ? RETURNING proximo_numero - 1
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Erro ao obter próximo número NF-e", e);
        }
        return 1;
    }

    private NFeConfig mapear(ResultSet rs) throws SQLException {
        NFeConfig cfg = new NFeConfig();
        cfg.setId(rs.getInt("id"));
        cfg.setCertificadoPath(rs.getString("certificado_path"));
        cfg.setCertificadoSenha(rs.getString("certificado_senha"));
        cfg.setCnpj(rs.getString("cnpj"));
        cfg.setIe(rs.getString("ie"));
        cfg.setRazaoSocial(rs.getString("razao_social"));
        cfg.setNomeFantasia(rs.getString("nome_fantasia"));
        cfg.setLogradouro(rs.getString("logradouro"));
        cfg.setNumeroEnd(rs.getString("numero_end"));
        cfg.setBairro(rs.getString("bairro"));
        cfg.setCodMunicipio(rs.getString("cod_municipio"));
        cfg.setMunicipio(rs.getString("municipio"));
        cfg.setUf(rs.getString("uf"));
        cfg.setCep(rs.getString("cep"));
        cfg.setTelefone(rs.getString("telefone"));
        cfg.setRegimeTributario(rs.getInt("regime_tributario"));
        cfg.setSerie(rs.getInt("serie"));
        cfg.setProximoNumero(rs.getInt("proximo_numero"));
        cfg.setAmbiente(rs.getString("ambiente"));
        return cfg;
    }
}
