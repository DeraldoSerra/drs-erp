package com.erp.dao;

import com.erp.config.DatabaseConfig;
import com.erp.model.MovimentoCaixa;
import com.erp.model.SessaoCaixa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CaixaDAO {

    private static final Logger log = LoggerFactory.getLogger(CaixaDAO.class);

    public Optional<SessaoCaixa> getSessaoAberta(int lojaId) {
        String sql = """
            SELECT sc.*, u.nome AS usuario_nome
            FROM sessoes_caixa sc
            LEFT JOIN usuarios u ON u.id = sc.usuario_id
            WHERE sc.loja_id = ? AND sc.status = 'ABERTO'
            ORDER BY sc.abertura DESC LIMIT 1
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar sessão aberta", e);
        }
        return Optional.empty();
    }

    public SessaoCaixa abrirCaixa(double valorAbertura, int usuarioId, int lojaId) {
        String sql = """
            INSERT INTO sessoes_caixa (caixa_id, loja_id, usuario_id, valor_abertura, status)
            VALUES (1, ?, ?, ?, 'ABERTO')
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lojaId);
            ps.setInt(2, usuarioId);
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(valorAbertura));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    registrarMovimento(id, "ABERTURA", valorAbertura, "Abertura de caixa", usuarioId);
                    return getSessaoAberta(lojaId).orElse(null);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao abrir caixa", e);
        }
        return null;
    }

    public boolean fecharCaixa(int sessaoId, double valorFechamento, String obs) {
        String sql = """
            UPDATE sessoes_caixa
            SET status = 'FECHADO', fechamento = NOW(),
                valor_fechamento = ?, observacoes = ?
            WHERE id = ? AND status = 'ABERTO'
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, java.math.BigDecimal.valueOf(valorFechamento));
            ps.setString(2, obs);
            ps.setInt(3, sessaoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Erro ao fechar caixa", e);
            return false;
        }
    }

    public boolean registrarMovimento(int sessaoId, String tipo, double valor, String descricao, int usuarioId) {
        String sql = """
            INSERT INTO movimentos_caixa (sessao_id, tipo, valor, descricao, usuario_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        // Update aggregates in sessoes_caixa
        String updateSql = switch (tipo) {
            case "SANGRIA"    -> "UPDATE sessoes_caixa SET total_sangrias = total_sangrias + ? WHERE id = ?";
            case "SUPRIMENTO" -> "UPDATE sessoes_caixa SET total_suprimentos = total_suprimentos + ? WHERE id = ?";
            case "VENDA_DINHEIRO" -> "UPDATE sessoes_caixa SET total_dinheiro = total_dinheiro + ?, total_vendas = total_vendas + ?, qtd_vendas = qtd_vendas + 1 WHERE id = ?";
            case "VENDA_PIX"      -> "UPDATE sessoes_caixa SET total_pix = total_pix + ?, total_vendas = total_vendas + ?, qtd_vendas = qtd_vendas + 1 WHERE id = ?";
            case "VENDA_DEBITO"   -> "UPDATE sessoes_caixa SET total_debito = total_debito + ?, total_vendas = total_vendas + ?, qtd_vendas = qtd_vendas + 1 WHERE id = ?";
            case "VENDA_CREDITO"  -> "UPDATE sessoes_caixa SET total_credito = total_credito + ?, total_vendas = total_vendas + ?, qtd_vendas = qtd_vendas + 1 WHERE id = ?";
            default -> null;
        };

        try (Connection conn = DatabaseConfig.getConexao()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, sessaoId);
                    ps.setString(2, tipo);
                    ps.setBigDecimal(3, java.math.BigDecimal.valueOf(valor));
                    ps.setString(4, descricao);
                    ps.setInt(5, usuarioId);
                    ps.executeUpdate();
                }
                if (updateSql != null) {
                    boolean isVenda = tipo.startsWith("VENDA_");
                    try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                        ps2.setBigDecimal(1, java.math.BigDecimal.valueOf(valor));
                        if (isVenda) {
                            ps2.setBigDecimal(2, java.math.BigDecimal.valueOf(valor));
                            ps2.setInt(3, sessaoId);
                        } else {
                            ps2.setInt(2, sessaoId);
                        }
                        ps2.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Erro ao registrar movimento", e);
            return false;
        }
    }

    public List<MovimentoCaixa> listarMovimentos(int sessaoId) {
        List<MovimentoCaixa> lista = new ArrayList<>();
        String sql = """
            SELECT mc.*, u.nome AS usuario_nome
            FROM movimentos_caixa mc
            LEFT JOIN usuarios u ON u.id = mc.usuario_id
            WHERE mc.sessao_id = ?
            ORDER BY mc.data_hora DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessaoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimentoCaixa m = new MovimentoCaixa();
                    m.setId(rs.getInt("id"));
                    m.setSessaoId(sessaoId);
                    m.setTipo(rs.getString("tipo"));
                    m.setValor(rs.getDouble("valor"));
                    m.setDescricao(rs.getString("descricao"));
                    m.setUsuarioId(rs.getInt("usuario_id"));
                    m.setUsuarioNome(rs.getString("usuario_nome"));
                    Timestamp ts = rs.getTimestamp("data_hora");
                    if (ts != null) m.setDataHora(ts.toLocalDateTime());
                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao listar movimentos", e);
        }
        return lista;
    }

    public SessaoCaixa calcularTotais(int sessaoId) {
        String sql = """
            SELECT sc.*, u.nome AS usuario_nome
            FROM sessoes_caixa sc
            LEFT JOIN usuarios u ON u.id = sc.usuario_id
            WHERE sc.id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessaoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            log.error("Erro ao calcular totais", e);
        }
        return null;
    }

    public List<SessaoCaixa> listarSessoes(int lojaId, LocalDate inicio, LocalDate fim) {
        List<SessaoCaixa> lista = new ArrayList<>();
        String sql = """
            SELECT sc.*, u.nome AS usuario_nome
            FROM sessoes_caixa sc
            LEFT JOIN usuarios u ON u.id = sc.usuario_id
            WHERE sc.loja_id = ? AND DATE(sc.abertura) BETWEEN ? AND ?
            ORDER BY sc.abertura DESC
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lojaId);
            ps.setDate(2, Date.valueOf(inicio));
            ps.setDate(3, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar sessões", e);
        }
        return lista;
    }

    private SessaoCaixa mapear(ResultSet rs) throws SQLException {
        SessaoCaixa s = new SessaoCaixa();
        s.setId(rs.getInt("id"));
        s.setCaixaId(rs.getInt("caixa_id"));
        s.setLojaId(rs.getInt("loja_id"));
        s.setUsuarioId(rs.getInt("usuario_id"));
        s.setUsuarioNome(rs.getString("usuario_nome"));
        Timestamp ab = rs.getTimestamp("abertura");
        if (ab != null) s.setAbertura(ab.toLocalDateTime());
        Timestamp fe = rs.getTimestamp("fechamento");
        if (fe != null) s.setFechamento(fe.toLocalDateTime());
        s.setValorAbertura(rs.getDouble("valor_abertura"));
        s.setValorFechamento(rs.getDouble("valor_fechamento"));
        s.setTotalDinheiro(rs.getDouble("total_dinheiro"));
        s.setTotalPix(rs.getDouble("total_pix"));
        s.setTotalDebito(rs.getDouble("total_debito"));
        s.setTotalCredito(rs.getDouble("total_credito"));
        s.setTotalVendas(rs.getDouble("total_vendas"));
        s.setQtdVendas(rs.getInt("qtd_vendas"));
        s.setTotalSangrias(rs.getDouble("total_sangrias"));
        s.setTotalSuprimentos(rs.getDouble("total_suprimentos"));
        s.setStatus(rs.getString("status"));
        s.setObservacoes(rs.getString("observacoes"));
        return s;
    }
}
