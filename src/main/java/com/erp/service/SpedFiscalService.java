package com.erp.service;

import com.erp.config.DatabaseConfig;
import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera arquivo SPED Fiscal EFD (Escrituração Fiscal Digital) básico.
 * Encoding: ISO-8859-1 conforme leiaute SPED.
 */
public class SpedFiscalService {

    private static final Logger log = LoggerFactory.getLogger(SpedFiscalService.class);
    private static final Charset ISO = Charset.forName("ISO-8859-1");
    private static final DateTimeFormatter DDMMAAAA = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter DDMMAAAA_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int totalLinhas = 0;

    public File gerarEFD(LocalDate inicio, LocalDate fim) throws Exception {
        Empresa empresa = new EmpresaDAO().carregar().orElse(new Empresa());
        String cnpj = limpar(empresa.getCnpj());
        String nome = empresa.getRazaoSocial() != null ? empresa.getRazaoSocial() : "EMPRESA";
        String uf = empresa.getEstado() != null ? empresa.getEstado() : "SP";
        String ie = limpar(empresa.getIe());
        String im = "";
        String suframa = "";
        String perfil = "A";
        String atividade = "0";

        File arquivo = File.createTempFile("SPED_EFD_" + inicio.format(DateTimeFormatter.ofPattern("yyyyMM")) + "_", ".txt");

        List<String> linhas = new ArrayList<>();
        totalLinhas = 0;

        // ========== BLOCO 0 ==========
        linhas.add(reg("0000", "LEIAUTE EFD", "01", "0",
                inicio.format(DDMMAAAA), fim.format(DDMMAAAA),
                nome, cnpj, "", uf, ie, im, suframa, perfil, atividade));

        linhas.add(reg("0001", "0"));

        // 0100 - Dados do contabilista (em branco)
        linhas.add(reg("0100", "", "", "", "", "", "", "", "", "", "", "", "", ""));

        // 0150 - Participantes (clientes + fornecedores)
        List<String[]> participantes = buscarParticipantes();
        for (String[] p : participantes) {
            linhas.add(reg("0150", p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12]));
        }

        // 0190 - Unidades de medida
        linhas.add(reg("0190", "UN", "Unidade"));
        linhas.add(reg("0190", "KG", "Quilograma"));
        linhas.add(reg("0190", "PC", "Peca"));

        // 0200 - Produtos
        List<String[]> produtos = buscarProdutos();
        for (String[] p : produtos) {
            linhas.add(reg("0200", p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8]));
        }

        int qtd0 = linhas.size() + 1; // +1 para o 0990
        linhas.add(reg("0990", String.valueOf(qtd0 + 1)));

        // ========== BLOCO C ==========
        linhas.add(reg("C001", "0"));

        List<String[]> notas = buscarNotasFiscais(inicio, fim);
        for (String[] nf : notas) {
            linhas.add(reg("C100",
                    nf[0], nf[1], nf[2], nf[3], nf[4], nf[5], nf[6], nf[7],
                    nf[8], nf[9], nf[10], nf[11], nf[12], nf[13], nf[14],
                    nf[15], nf[16], nf[17], nf[18], nf[19], nf[20]));
            // Itens do C100
            List<String[]> itens = buscarItensNF(nf[21]); // nf[21] = id da nota
            for (String[] item : itens) {
                linhas.add(reg("C170", item[0], item[1], item[2], item[3], item[4],
                        item[5], item[6], item[7], item[8], item[9], item[10],
                        item[11], item[12], item[13], item[14], item[15], item[16]));
            }
        }

        int qtdC = calcularLinhasBloco(linhas, "C") + 2; // C001 + C990
        linhas.add(reg("C990", String.valueOf(qtdC)));

        // ========== BLOCO H (Inventário) ==========
        linhas.add(reg("H001", "0"));

        String dtInv = fim.format(DDMMAAAA);
        List<String[]> estoque = buscarEstoque();
        for (String[] e : estoque) {
            linhas.add(reg("H010", e[0], dtInv, "01", e[1], e[2], e[3], ""));
        }

        int qtdH = calcularLinhasBloco(linhas, "H") + 2;
        linhas.add(reg("H990", String.valueOf(qtdH)));

        // ========== BLOCO 9 ==========
        linhas.add(reg("9001", "0"));
        linhas.add(reg("9900", "0000", "1"));
        linhas.add(reg("9900", "0001", "1"));
        linhas.add(reg("9900", "0100", "1"));
        linhas.add(reg("9900", "0150", String.valueOf(participantes.size())));
        linhas.add(reg("9900", "0190", "3"));
        linhas.add(reg("9900", "0200", String.valueOf(produtos.size())));
        linhas.add(reg("9900", "0990", "1"));
        linhas.add(reg("9900", "C001", "1"));
        linhas.add(reg("9900", "C100", String.valueOf(notas.size())));
        linhas.add(reg("9900", "C990", "1"));
        linhas.add(reg("9900", "H001", "1"));
        linhas.add(reg("9900", "H010", String.valueOf(estoque.size())));
        linhas.add(reg("9900", "H990", "1"));
        linhas.add(reg("9900", "9001", "1"));
        linhas.add(reg("9900", "9900", "0")); // placeholder, fixado abaixo
        linhas.add(reg("9990", String.valueOf(linhas.size() + 2)));
        // Total geral: todas as linhas + 9999
        linhas.add(reg("9999", String.valueOf(linhas.size() + 1)));

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(arquivo), ISO)) {
            for (String linha : linhas) {
                writer.write(linha);
                writer.write("\r\n");
            }
        }

        log.info("SPED EFD gerado: {} ({} registros)", arquivo.getAbsolutePath(), linhas.size());
        return arquivo;
    }

    private String reg(String... campos) {
        totalLinhas++;
        StringBuilder sb = new StringBuilder("|");
        for (String c : campos) {
            sb.append(c != null ? c : "").append("|");
        }
        return sb.toString();
    }

    private int calcularLinhasBloco(List<String> linhas, String bloco) {
        return (int) linhas.stream().filter(l -> l.startsWith("|" + bloco)).count();
    }

    private List<String[]> buscarParticipantes() {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT 'C' || id, nome, COALESCE(cpf_cnpj,''), '', '', COALESCE(endereco,''),
                   '', '', COALESCE(telefone,''), COALESCE(email,''), '', '', '', ''
            FROM clientes LIMIT 500
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String[] p = new String[13];
                for (int i = 0; i < 13; i++) p[i] = rs.getString(i + 1) != null ? rs.getString(i + 1) : "";
                lista.add(p);
            }
        } catch (Exception e) { log.warn("Sem clientes para SPED: {}", e.getMessage()); }
        return lista;
    }

    private List<String[]> buscarProdutos() {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT COALESCE(codigo,''), nome, COALESCE(unidade,'UN'), COALESCE(ncm,''), '00', '0', COALESCE(preco_venda::text,'0'), '', '0' FROM produtos LIMIT 1000";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String[] p = new String[9];
                for (int i = 0; i < 9; i++) p[i] = rs.getString(i + 1) != null ? rs.getString(i + 1) : "";
                lista.add(p);
            }
        } catch (Exception e) { log.warn("Sem produtos para SPED: {}", e.getMessage()); }
        return lista;
    }

    private List<String[]> buscarNotasFiscais(LocalDate inicio, LocalDate fim) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT '55', '1', nf.numero_nf, '', nf.serie, '',
                   TO_CHAR(nf.data_emissao, 'ddMMyyyy'), '', '1', '',
                   COALESCE(c.nome,''), COALESCE(c.cpf_cnpj,''), '',
                   nf.valor_total::text, '0', '0', '0', '0', '0', '0', '0',
                   nf.id::text
            FROM notas_fiscais nf
            LEFT JOIN clientes c ON c.id = nf.cliente_id
            WHERE DATE(nf.data_emissao) BETWEEN ? AND ?
              AND nf.status = 'AUTORIZADA'
            ORDER BY nf.data_emissao
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] row = new String[22];
                    for (int i = 0; i < 22; i++) row[i] = rs.getString(i + 1) != null ? rs.getString(i + 1) : "";
                    lista.add(row);
                }
            }
        } catch (Exception e) { log.warn("Sem NF-e para SPED: {}", e.getMessage()); }
        return lista;
    }

    private List<String[]> buscarItensNF(String nfId) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT ROW_NUMBER() OVER() ::text, COALESCE(p.codigo,''),
                   p.nome, COALESCE(p.ncm,''), '', 'UN', in2.quantidade::text,
                   in2.valor_unitario::text, '0', '0', '0', '0', '0', '0', '0', '0', '0', ''
            FROM itens_nota_fiscal in2
            JOIN produtos p ON p.id = in2.produto_id
            WHERE in2.nota_fiscal_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(nfId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] row = new String[18];
                    for (int i = 0; i < 18; i++) row[i] = rs.getString(i + 1) != null ? rs.getString(i + 1) : "";
                    lista.add(row);
                }
            }
        } catch (Exception e) { log.warn("Sem itens NF {}: {}", nfId, e.getMessage()); }
        return lista;
    }

    private List<String[]> buscarEstoque() {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT COALESCE(codigo,''), estoque_atual::text, 'UN', COALESCE(preco_custo::text,'0') FROM produtos WHERE estoque_atual > 0";
        try (Connection conn = DatabaseConfig.getConexao();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String[] row = new String[4];
                for (int i = 0; i < 4; i++) row[i] = rs.getString(i + 1) != null ? rs.getString(i + 1) : "";
                lista.add(row);
            }
        } catch (Exception e) { log.warn("Sem estoque para SPED: {}", e.getMessage()); }
        return lista;
    }

    private String limpar(String s) {
        return s != null ? s.replaceAll("[^0-9]", "") : "";
    }
}
