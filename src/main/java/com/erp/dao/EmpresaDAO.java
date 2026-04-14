package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Empresa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class EmpresaDAO {

    private static final Logger log = LoggerFactory.getLogger(EmpresaDAO.class);

    private String nvl(String v) { return v == null ? "" : v; }

    /** Carrega empresa de qualquer loja (uso admin, sem sessão) */
    public Optional<Empresa> carregarParaLoja(int lojaId) {
        String sql = "SELECT * FROM empresa WHERE loja_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao carregar empresa para loja {}", lojaId, e);
        }
        return Optional.empty();
    }

    /** Salva empresa de qualquer loja (uso admin, sem sessão) */
    public boolean salvarParaLoja(Empresa e, int lojaId) {
        String sql = """
            INSERT INTO empresa (razao_social, nome_fantasia, cnpj, ie, im,
                regime_tributario, email, telefone, celular, site,
                cep, logradouro, numero, complemento, bairro, cidade, estado,
                logo_path, observacoes, habilita_nfe, tipo_emissao_nfe, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (loja_id) DO UPDATE SET
                razao_social=EXCLUDED.razao_social,
                nome_fantasia=EXCLUDED.nome_fantasia,
                cnpj=EXCLUDED.cnpj,
                ie=EXCLUDED.ie,
                im=EXCLUDED.im,
                regime_tributario=EXCLUDED.regime_tributario,
                email=EXCLUDED.email,
                telefone=EXCLUDED.telefone,
                celular=EXCLUDED.celular,
                site=EXCLUDED.site,
                cep=EXCLUDED.cep,
                logradouro=EXCLUDED.logradouro,
                numero=EXCLUDED.numero,
                complemento=EXCLUDED.complemento,
                bairro=EXCLUDED.bairro,
                cidade=EXCLUDED.cidade,
                estado=EXCLUDED.estado,
                logo_path=EXCLUDED.logo_path,
                observacoes=EXCLUDED.observacoes,
                habilita_nfe=EXCLUDED.habilita_nfe,
                tipo_emissao_nfe=EXCLUDED.tipo_emissao_nfe,
                atualizado_em=NOW()
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPs(ps, e);
            ps.setInt(22, lojaId);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            log.error("Erro ao salvar empresa para loja {}", lojaId, ex);
            return false;
        }
    }

    /** Carrega a empresa cadastrada para a loja da sessão */
    public Optional<Empresa> carregar() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = "SELECT * FROM empresa WHERE loja_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao carregar empresa", e);
        }
        return Optional.empty();
    }

    /** Salva ou atualiza os dados da empresa usando upsert por loja_id */
    public boolean salvar(Empresa e) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            INSERT INTO empresa (razao_social, nome_fantasia, cnpj, ie, im,
                regime_tributario, email, telefone, celular, site,
                cep, logradouro, numero, complemento, bairro, cidade, estado,
                logo_path, observacoes, habilita_nfe, tipo_emissao_nfe, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (loja_id) DO UPDATE SET
                razao_social=EXCLUDED.razao_social,
                nome_fantasia=EXCLUDED.nome_fantasia,
                cnpj=EXCLUDED.cnpj,
                ie=EXCLUDED.ie,
                im=EXCLUDED.im,
                regime_tributario=EXCLUDED.regime_tributario,
                email=EXCLUDED.email,
                telefone=EXCLUDED.telefone,
                celular=EXCLUDED.celular,
                site=EXCLUDED.site,
                cep=EXCLUDED.cep,
                logradouro=EXCLUDED.logradouro,
                numero=EXCLUDED.numero,
                complemento=EXCLUDED.complemento,
                bairro=EXCLUDED.bairro,
                cidade=EXCLUDED.cidade,
                estado=EXCLUDED.estado,
                logo_path=EXCLUDED.logo_path,
                observacoes=EXCLUDED.observacoes,
                habilita_nfe=EXCLUDED.habilita_nfe,
                tipo_emissao_nfe=EXCLUDED.tipo_emissao_nfe,
                atualizado_em=NOW()
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPs(ps, e);
            ps.setInt(22, lojaId);
            ps.executeUpdate();
            log.info("Empresa salva (upsert): {}", e.getRazaoSocial());
            return true;
        } catch (SQLException ex) {
            log.error("Erro ao salvar empresa", ex);
            return false;
        }
    }

    private void preencherPs(PreparedStatement ps, Empresa e) throws SQLException {
        ps.setString(1,  nvl(e.getRazaoSocial()));
        ps.setString(2,  nvl(e.getNomeFantasia()));
        ps.setString(3,  nvl(e.getCnpj()));
        ps.setString(4,  nvl(e.getIe()));
        ps.setString(5,  nvl(e.getIm()));
        ps.setString(6,  e.getRegimeTributario() != null ? e.getRegimeTributario() : "SIMPLES_NACIONAL");
        ps.setString(7,  nvl(e.getEmail()));
        ps.setString(8,  nvl(e.getTelefone()));
        ps.setString(9,  nvl(e.getCelular()));
        ps.setString(10, nvl(e.getSite()));
        ps.setString(11, nvl(e.getCep()));
        ps.setString(12, nvl(e.getLogradouro()));
        ps.setString(13, nvl(e.getNumero()));
        ps.setString(14, nvl(e.getComplemento()));
        ps.setString(15, nvl(e.getBairro()));
        ps.setString(16, nvl(e.getCidade()));
        // estado CHAR/VARCHAR(2) — nunca nulo, manda string vazia se não selecionado
        String est = e.getEstado();
        ps.setString(17, est != null && est.length() == 2 ? est : "");
        ps.setString(18, nvl(e.getLogoPath()));
        ps.setString(19, nvl(e.getObservacoes()));
        ps.setBoolean(20, e.isHabilitaNFe());
        ps.setString(21, e.getTipoEmissaoNFe() != null ? e.getTipoEmissaoNFe() : "SEFAZ");
    }

    private Empresa mapear(ResultSet rs) throws SQLException {
        Empresa e = new Empresa();
        e.setId(rs.getInt("id"));
        e.setRazaoSocial(rs.getString("razao_social"));
        e.setNomeFantasia(rs.getString("nome_fantasia"));
        e.setCnpj(rs.getString("cnpj"));
        e.setIe(rs.getString("ie"));
        e.setIm(rs.getString("im"));
        e.setRegimeTributario(rs.getString("regime_tributario"));
        e.setEmail(rs.getString("email"));
        e.setTelefone(rs.getString("telefone"));
        e.setCelular(rs.getString("celular"));
        e.setSite(rs.getString("site"));
        e.setCep(rs.getString("cep"));
        e.setLogradouro(rs.getString("logradouro"));
        e.setNumero(rs.getString("numero"));
        e.setComplemento(rs.getString("complemento"));
        e.setBairro(rs.getString("bairro"));
        e.setCidade(rs.getString("cidade"));
        e.setEstado(rs.getString("estado"));
        e.setLogoPath(rs.getString("logo_path"));
        e.setObservacoes(rs.getString("observacoes"));
        try { e.setHabilitaNFe(rs.getBoolean("habilita_nfe")); } catch (SQLException ignored) {}
        try {
            String tipe = rs.getString("tipo_emissao_nfe");
            e.setTipoEmissaoNFe(tipe != null ? tipe : "SEFAZ");
        } catch (SQLException ignored) {}
        Timestamp ts = rs.getTimestamp("atualizado_em");
        if (ts != null) e.setAtualizadoEm(ts.toLocalDateTime());
        return e;
    }
}
