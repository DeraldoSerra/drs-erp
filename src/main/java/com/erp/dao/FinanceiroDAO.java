package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.ContaPagar;
import com.erp.model.ContaReceber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinanceiroDAO {

    private static final Logger log = LoggerFactory.getLogger(FinanceiroDAO.class);

    // ==================== CONTAS A PAGAR ====================

    public boolean salvarPagar(ContaPagar c) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            INSERT INTO contas_pagar (descricao, fornecedor_id, valor, data_emissao, data_vencimento,
            status, categoria, observacoes, loja_id) VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getDescricao());
            if (c.getFornecedorId() > 0) ps.setInt(2, c.getFornecedorId()); else ps.setNull(2, Types.INTEGER);
            ps.setDouble(3, c.getValor());
            ps.setDate(4, Date.valueOf(c.getDataEmissao()));
            ps.setDate(5, Date.valueOf(c.getDataVencimento()));
            ps.setString(6, c.getStatus());
            ps.setString(7, c.getCategoria());
            ps.setString(8, c.getObservacoes());
            ps.setInt(9, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar conta a pagar", e);
            return false;
        }
    }

    public boolean pagarConta(int id, double valorPago, String forma, LocalDate dataPag) {
        String sql = "UPDATE contas_pagar SET status='PAGA', valor_pago=?, forma_pagamento=?, data_pagamento=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, valorPago);
            ps.setString(2, forma);
            ps.setDate(3, Date.valueOf(dataPag));
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao pagar conta", e);
            return false;
        }
    }

    public List<ContaPagar> listarContasPagar(String status, LocalDate inicio, LocalDate fim) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<ContaPagar> lista = new ArrayList<>();
        String where = "WHERE cp.loja_id=" + lojaId;
        if (status != null && !status.isBlank() && !"TODAS".equals(status)) where += " AND cp.status='" + status + "'";
        if (inicio != null) where += " AND cp.data_vencimento >= '" + inicio + "'";
        if (fim != null) where += " AND cp.data_vencimento <= '" + fim + "'";
        String sql = """
            SELECT cp.*, f.nome_fantasia AS fornecedor_nome
            FROM contas_pagar cp LEFT JOIN fornecedores f ON f.id = cp.fornecedor_id
            """ + where + " ORDER BY cp.data_vencimento";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapearPagar(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar contas a pagar", e);
        }
        return lista;
    }

    public double totalPagarAberto() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COALESCE(SUM(valor),0) FROM contas_pagar WHERE status='ABERTA' AND loja_id=?")) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { log.error("Erro", e); }
        return 0;
    }

    // ==================== CONTAS A RECEBER ====================

    public boolean salvarReceber(ContaReceber c) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        String sql = """
            INSERT INTO contas_receber (descricao, cliente_id, venda_id, valor, data_emissao, data_vencimento,
            status, observacoes, loja_id) VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getDescricao());
            if (c.getClienteId() > 0) ps.setInt(2, c.getClienteId()); else ps.setNull(2, Types.INTEGER);
            if (c.getVendaId() > 0) ps.setInt(3, c.getVendaId()); else ps.setNull(3, Types.INTEGER);
            ps.setDouble(4, c.getValor());
            ps.setDate(5, Date.valueOf(c.getDataEmissao()));
            ps.setDate(6, Date.valueOf(c.getDataVencimento()));
            ps.setString(7, c.getStatus());
            ps.setString(8, c.getObservacoes());
            ps.setInt(9, lojaId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            log.error("Erro ao salvar conta a receber", e);
            return false;
        }
    }

    public boolean receberConta(int id, double valorRec, String forma, LocalDate dataRec) {
        String sql = "UPDATE contas_receber SET status='RECEBIDA', valor_recebido=?, forma_recebimento=?, data_recebimento=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, valorRec);
            ps.setString(2, forma);
            ps.setDate(3, Date.valueOf(dataRec));
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao receber conta", e);
            return false;
        }
    }

    public List<ContaReceber> listarContasReceber(String status, LocalDate inicio, LocalDate fim) {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        List<ContaReceber> lista = new ArrayList<>();
        String where = "WHERE cr.loja_id=" + lojaId;
        if (status != null && !status.isBlank() && !"TODAS".equals(status)) where += " AND cr.status='" + status + "'";
        if (inicio != null) where += " AND cr.data_vencimento >= '" + inicio + "'";
        if (fim != null) where += " AND cr.data_vencimento <= '" + fim + "'";
        String sql = """
            SELECT cr.*, c.nome AS cliente_nome
            FROM contas_receber cr LEFT JOIN clientes c ON c.id = cr.cliente_id
            """ + where + " ORDER BY cr.data_vencimento";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapearReceber(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar contas a receber", e);
        }
        return lista;
    }

    public double totalReceberAberto() {
        int lojaId = com.erp.util.Sessao.getInstance().getLojaId();
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COALESCE(SUM(valor),0) FROM contas_receber WHERE status='ABERTA' AND loja_id=?")) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { log.error("Erro", e); }
        return 0;
    }

    private ContaPagar mapearPagar(ResultSet rs) throws SQLException {
        ContaPagar c = new ContaPagar();
        c.setId(rs.getInt("id"));
        c.setDescricao(rs.getString("descricao"));
        c.setFornecedorId(rs.getInt("fornecedor_id"));
        c.setFornecedorNome(rs.getString("fornecedor_nome"));
        c.setValor(rs.getDouble("valor"));
        Date de = rs.getDate("data_emissao");
        if (de != null) c.setDataEmissao(de.toLocalDate());
        Date dv = rs.getDate("data_vencimento");
        if (dv != null) c.setDataVencimento(dv.toLocalDate());
        Date dp = rs.getDate("data_pagamento");
        if (dp != null) c.setDataPagamento(dp.toLocalDate());
        c.setValorPago(rs.getDouble("valor_pago"));
        c.setFormaPagamento(rs.getString("forma_pagamento"));
        c.setStatus(rs.getString("status"));
        c.setCategoria(rs.getString("categoria"));
        c.setObservacoes(rs.getString("observacoes"));
        return c;
    }

    private ContaReceber mapearReceber(ResultSet rs) throws SQLException {
        ContaReceber c = new ContaReceber();
        c.setId(rs.getInt("id"));
        c.setDescricao(rs.getString("descricao"));
        c.setClienteId(rs.getInt("cliente_id"));
        c.setClienteNome(rs.getString("cliente_nome"));
        c.setVendaId(rs.getInt("venda_id"));
        c.setValor(rs.getDouble("valor"));
        Date de = rs.getDate("data_emissao");
        if (de != null) c.setDataEmissao(de.toLocalDate());
        Date dv = rs.getDate("data_vencimento");
        if (dv != null) c.setDataVencimento(dv.toLocalDate());
        Date dr = rs.getDate("data_recebimento");
        if (dr != null) c.setDataRecebimento(dr.toLocalDate());
        c.setValorRecebido(rs.getDouble("valor_recebido"));
        c.setFormaRecebimento(rs.getString("forma_recebimento"));
        c.setStatus(rs.getString("status"));
        c.setObservacoes(rs.getString("observacoes"));
        return c;
    }
}
