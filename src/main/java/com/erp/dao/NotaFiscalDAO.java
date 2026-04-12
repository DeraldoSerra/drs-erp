package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.NotaFiscal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotaFiscalDAO {

    private static final Logger log = LoggerFactory.getLogger(NotaFiscalDAO.class);

    public boolean salvar(NotaFiscal nf) {
        String sql = """
            INSERT INTO notas_fiscais (venda_id, chave_acesso, numero, serie, protocolo,
                data_emissao, data_autorizacao, xml_nfe, xml_proc_nfe, status, motivo, ambiente, loja_id)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
            if (nf.getVendaId() > 0) ps.setInt(1, nf.getVendaId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, nf.getChaveAcesso());
            ps.setInt(3, nf.getNumero());
            ps.setString(4, nf.getSerie());
            ps.setString(5, nf.getProtocolo());
            ps.setTimestamp(6, nf.getDataEmissao() != null ? Timestamp.valueOf(nf.getDataEmissao()) : new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(7, nf.getDataAutorizacao() != null ? Timestamp.valueOf(nf.getDataAutorizacao()) : null);
            ps.setString(8, nf.getXmlNfe());
            ps.setString(9, nf.getXmlProcNfe());
            ps.setString(10, nf.getStatus());
            ps.setString(11, nf.getMotivo());
            ps.setString(12, nf.getAmbiente());
            ps.setInt(13, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) nf.setId(rs.getInt(1));
            }
            log.info("NotaFiscal salva: chave={}", nf.getChaveAcesso());
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar NotaFiscal", e);
            return false;
        }
    }

    public boolean atualizar(NotaFiscal nf) {
        String sql = """
            UPDATE notas_fiscais SET protocolo=?, data_autorizacao=?, xml_proc_nfe=?,
                status=?, motivo=?
            WHERE id=?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nf.getProtocolo());
            ps.setTimestamp(2, nf.getDataAutorizacao() != null ? Timestamp.valueOf(nf.getDataAutorizacao()) : null);
            ps.setString(3, nf.getXmlProcNfe());
            ps.setString(4, nf.getStatus());
            ps.setString(5, nf.getMotivo());
            ps.setInt(6, nf.getId());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Erro ao atualizar NotaFiscal", e);
            return false;
        }
    }

    public List<NotaFiscal> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        List<NotaFiscal> lista = new ArrayList<>();
        String sql = """
            SELECT nf.*, v.total AS total_venda, c.nome AS cliente_nome
            FROM notas_fiscais nf
            LEFT JOIN vendas v ON v.id = nf.venda_id
            LEFT JOIN clientes c ON c.id = v.cliente_id
            WHERE DATE(nf.data_emissao) BETWEEN ? AND ? AND nf.loja_id = ?
            ORDER BY nf.data_emissao DESC
            """;
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fim));
            ps.setInt(3, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar notas fiscais", e);
        }
        return lista;
    }

    public Optional<NotaFiscal> buscarPorVendaId(int vendaId) {
        String sql = "SELECT * FROM notas_fiscais WHERE venda_id=? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar NF por venda", e);
        }
        return Optional.empty();
    }

    public Optional<NotaFiscal> buscarPorId(int id) {
        String sql = """
            SELECT nf.*, v.total AS total_venda, c.nome AS cliente_nome
            FROM notas_fiscais nf
            LEFT JOIN vendas v ON v.id = nf.venda_id
            LEFT JOIN clientes c ON c.id = v.cliente_id
            WHERE nf.id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar NF por id", e);
        }
        return Optional.empty();
    }

    private NotaFiscal mapear(ResultSet rs) throws SQLException {
        NotaFiscal nf = new NotaFiscal();
        nf.setId(rs.getInt("id"));
        nf.setVendaId(rs.getInt("venda_id"));
        nf.setChaveAcesso(rs.getString("chave_acesso"));
        nf.setNumero(rs.getInt("numero"));
        nf.setSerie(rs.getString("serie"));
        nf.setProtocolo(rs.getString("protocolo"));
        Timestamp dtEmissao = rs.getTimestamp("data_emissao");
        if (dtEmissao != null) nf.setDataEmissao(dtEmissao.toLocalDateTime());
        Timestamp dtAut = rs.getTimestamp("data_autorizacao");
        if (dtAut != null) nf.setDataAutorizacao(dtAut.toLocalDateTime());
        nf.setXmlNfe(rs.getString("xml_nfe"));
        nf.setXmlProcNfe(rs.getString("xml_proc_nfe"));
        nf.setStatus(rs.getString("status"));
        nf.setMotivo(rs.getString("motivo"));
        nf.setAmbiente(rs.getString("ambiente"));
        // campos extras (podem não existir dependendo da query)
        try { nf.setTotalVenda(rs.getDouble("total_venda")); } catch (SQLException ignored) {}
        try { nf.setClienteNome(rs.getString("cliente_nome")); } catch (SQLException ignored) {}
        return nf;
    }
}
