package com.erp.service;

import com.erp.config.DatabaseConfig;
import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera Livro de Vendas em formato XML para um período.
 */
public class LivroVendasXmlService {

    private static final Logger log = LoggerFactory.getLogger(LivroVendasXmlService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public File gerarXml(LocalDate inicio, LocalDate fim) throws Exception {
        Empresa empresa = new EmpresaDAO().carregar().orElse(new Empresa());
        String nomeEmpresa = empresa.getRazaoSocial() != null ? empresa.getRazaoSocial() : "EMPRESA";
        String cnpj = empresa.getCnpj() != null ? empresa.getCnpj() : "00000000000000";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("LivroVendas");
        root.setAttribute("periodo", inicio.format(PERIODO_FMT));
        root.setAttribute("emitente", nomeEmpresa);
        root.setAttribute("cnpj", cnpj);
        root.setAttribute("dataInicio", inicio.format(DATE_FMT));
        root.setAttribute("dataFim", fim.format(DATE_FMT));
        doc.appendChild(root);

        double totalGeral = 0;
        double totalDesconto = 0;
        int qtdVendas = 0;

        List<int[]> vendas = buscarVendas(inicio, fim);
        for (int[] v : vendas) {
            int vendaId = v[0];
            VendaInfo info = buscarInfoVenda(vendaId);
            if (info == null) continue;

            Element eVenda = doc.createElement("Venda");
            eVenda.setAttribute("numero", info.numero);
            eVenda.setAttribute("data", info.data);
            eVenda.setAttribute("cliente", info.cliente);
            eVenda.setAttribute("total", String.format("%.2f", info.total));
            eVenda.setAttribute("desconto", String.format("%.2f", info.desconto));
            eVenda.setAttribute("formaPagamento", info.formaPagamento);
            eVenda.setAttribute("status", info.status);

            for (ItemInfo item : info.itens) {
                Element eItem = doc.createElement("Item");
                eItem.setAttribute("produto", item.nome);
                eItem.setAttribute("qtd", String.format("%.3f", item.qtd).replaceAll("\\.?0+$", ""));
                eItem.setAttribute("unitario", String.format("%.2f", item.unitario));
                eItem.setAttribute("subtotal", String.format("%.2f", item.subtotal));
                eVenda.appendChild(eItem);
            }

            root.appendChild(eVenda);
            totalGeral += info.total;
            totalDesconto += info.desconto;
            qtdVendas++;
        }

        Element totais = doc.createElement("Totais");
        totais.setAttribute("totalVendas", String.format("%.2f", totalGeral));
        totais.setAttribute("qtdVendas", String.valueOf(qtdVendas));
        totais.setAttribute("totalDesconto", String.format("%.2f", totalDesconto));
        root.appendChild(totais);

        File arquivo = File.createTempFile("LivroVendas_" + inicio.format(DateTimeFormatter.ofPattern("yyyyMM")) + "_", ".xml");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(doc), new StreamResult(arquivo));

        log.info("Livro de Vendas XML gerado: {} ({} vendas)", arquivo.getAbsolutePath(), qtdVendas);
        return arquivo;
    }

    private List<int[]> buscarVendas(LocalDate inicio, LocalDate fim) {
        List<int[]> ids = new ArrayList<>();
        String sql = "SELECT id FROM vendas WHERE DATE(data_venda) BETWEEN ? AND ? AND status='FINALIZADA' ORDER BY data_venda";
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(new int[]{rs.getInt(1)});
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar vendas para XML", e);
        }
        return ids;
    }

    private VendaInfo buscarInfoVenda(int id) {
        String sql = """
            SELECT v.numero, DATE(v.data_venda)::text AS data,
                   COALESCE(c.nome,'Consumidor Final') AS cliente,
                   v.total, v.desconto, v.forma_pagamento, v.status
            FROM vendas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            WHERE v.id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VendaInfo info = new VendaInfo();
                    info.numero = rs.getString("numero");
                    info.data = rs.getString("data");
                    info.cliente = rs.getString("cliente");
                    info.total = rs.getDouble("total");
                    info.desconto = rs.getDouble("desconto");
                    info.formaPagamento = rs.getString("forma_pagamento");
                    info.status = rs.getString("status");
                    info.itens = buscarItensVenda(id);
                    return info;
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar venda {}", id, e);
        }
        return null;
    }

    private List<ItemInfo> buscarItensVenda(int vendaId) {
        List<ItemInfo> lista = new ArrayList<>();
        String sql = """
            SELECT p.nome, iv.quantidade, iv.preco_unit, iv.subtotal
            FROM itens_venda iv
            JOIN produtos p ON p.id = iv.produto_id
            WHERE iv.venda_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemInfo item = new ItemInfo();
                    item.nome = rs.getString("nome");
                    item.qtd = rs.getDouble("quantidade");
                    item.unitario = rs.getDouble("preco_unit");
                    item.subtotal = rs.getDouble("subtotal");
                    lista.add(item);
                }
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar itens venda {}", vendaId, e);
        }
        return lista;
    }

    private static class VendaInfo {
        String numero, data, cliente, formaPagamento, status;
        double total, desconto;
        List<ItemInfo> itens = new ArrayList<>();
    }

    private static class ItemInfo {
        String nome;
        double qtd, unitario, subtotal;
    }
}
