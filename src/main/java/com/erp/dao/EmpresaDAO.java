package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.Empresa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class EmpresaDAO {

    private static final Logger log = LoggerFactory.getLogger(EmpresaDAO.class);

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

    /** Salva ou atualiza os dados da empresa */
    public boolean salvar(Empresa e) {
        Optional<Empresa> existente = carregar();
        if (existente.isPresent()) {
            return atualizar(e, existente.get().getId());
        } else {
            return inserir(e);
        }
    }

    private boolean inserir(Empresa e) {
        String sql = """
            INSERT INTO empresa (razao_social, nome_fantasia, cnpj, ie, im,
                regime_tributario, email, telefone, celular, site,
                cep, logradouro, numero, complemento, bairro, cidade, estado,
                logo_path, observacoes, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
            preencherPs(ps, e);
            ps.setInt(20, lojaId);
            ps.executeUpdate();
            log.info("Empresa inserida: {}", e.getRazaoSocial());
            return true;
        } catch (SQLException ex) {
            log.error("Erro ao inserir empresa", ex);
            return false;
        }
    }

    private boolean atualizar(Empresa e, int id) {
        String sql = """
            UPDATE empresa SET
                razao_social=?, nome_fantasia=?, cnpj=?, ie=?, im=?,
                regime_tributario=?, email=?, telefone=?, celular=?, site=?,
                cep=?, logradouro=?, numero=?, complemento=?, bairro=?, cidade=?, estado=?,
                logo_path=?, observacoes=?, atualizado_em=NOW()
            WHERE id=?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherPs(ps, e);
            ps.setInt(20, id);
            ps.executeUpdate();
            log.info("Empresa atualizada: {}", e.getRazaoSocial());
            return true;
        } catch (SQLException ex) {
            log.error("Erro ao atualizar empresa", ex);
            return false;
        }
    }

    private void preencherPs(PreparedStatement ps, Empresa e) throws SQLException {
        ps.setString(1,  e.getRazaoSocial());
        ps.setString(2,  e.getNomeFantasia());
        ps.setString(3,  e.getCnpj());
        ps.setString(4,  e.getIe());
        ps.setString(5,  e.getIm());
        ps.setString(6,  e.getRegimeTributario());
        ps.setString(7,  e.getEmail());
        ps.setString(8,  e.getTelefone());
        ps.setString(9,  e.getCelular());
        ps.setString(10, e.getSite());
        ps.setString(11, e.getCep());
        ps.setString(12, e.getLogradouro());
        ps.setString(13, e.getNumero());
        ps.setString(14, e.getComplemento());
        ps.setString(15, e.getBairro());
        ps.setString(16, e.getCidade());
        ps.setString(17, e.getEstado());
        ps.setString(18, e.getLogoPath());
        ps.setString(19, e.getObservacoes());
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
        Timestamp ts = rs.getTimestamp("atualizado_em");
        if (ts != null) e.setAtualizadoEm(ts.toLocalDateTime());
        return e;
    }
}
