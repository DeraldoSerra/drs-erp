package com.erp.util;

import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import com.erp.model.Venda;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Gera relatório PDF de vendas por período.
 */
public class RelatorioVendasPDF {

    private static final Logger log = LoggerFactory.getLogger(RelatorioVendasPDF.class);
    private static final DateTimeFormatter FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Fontes
    private static final Font F_EMPRESA   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_SECAO     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(40, 60, 120));
    private static final Font F_NORMAL    = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_NEGRITO   = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TOTAL     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(20, 120, 50));
    private static final Font F_CANCEL    = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, new BaseColor(180, 30, 30));
    private static final Font F_HEADER_TB = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD,   BaseColor.WHITE);

    private static final BaseColor COR_HEADER    = new BaseColor(40, 60, 120);
    private static final BaseColor COR_LINHA_PAR = new BaseColor(240, 244, 255);
    private static final BaseColor COR_RESUMO_BG = new BaseColor(245, 248, 255);
    private static final BaseColor COR_CANCEL_BG = new BaseColor(255, 235, 235);

    private RelatorioVendasPDF() {}

    /**
     * Gera PDF do relatório de vendas por período.
     *
     * @param inicio  Data início do período
     * @param fim     Data fim do período
     * @param vendas  Lista de vendas do período
     * @return caminho do arquivo PDF gerado, ou null em caso de erro
     */
    public static String gerar(LocalDate inicio, LocalDate fim, List<Venda> vendas) {
        try {
            Optional<Empresa> optEmpresa = new EmpresaDAO().carregar();
            Empresa empresa = optEmpresa.orElse(null);

            String nomeArquivo = System.getProperty("java.io.tmpdir")
                    + File.separator + "relatorio_vendas_"
                    + inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_"
                    + fim.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            Document doc = new Document(PageSize.A4, 28f, 28f, 28f, 28f);
            PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
            doc.open();

            // ── CABEÇALHO DA EMPRESA ─────────────────────────────────────────────
            adicionarCabecalhoEmpresa(doc, empresa);
            linhaHR(doc);

            Paragraph titulo = new Paragraph("RELATÓRIO DE VENDAS", F_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(4);
            titulo.setSpacingAfter(2);
            doc.add(titulo);

            doc.add(centrado("Período: " + inicio.format(FMT_DT) + " a " + fim.format(FMT_DT), F_SUBTITULO));
            doc.add(centrado("Gerado em: " + LocalDateTime.now().format(FMT), F_SUBTITULO));
            linhaHR(doc);

            // ── RESUMO GERAL ─────────────────────────────────────────────────────
            List<Venda> finalizadas  = vendas.stream().filter(v -> "FINALIZADA".equals(v.getStatus())).collect(Collectors.toList());
            List<Venda> canceladas   = vendas.stream().filter(v -> "CANCELADA".equals(v.getStatus())).collect(Collectors.toList());
            double totalFaturamento  = finalizadas.stream().mapToDouble(Venda::getTotal).sum();
            double totalSubtotal     = finalizadas.stream().mapToDouble(Venda::getSubtotal).sum();
            double totalDesconto     = finalizadas.stream().mapToDouble(Venda::getDesconto).sum();
            double totalRecebido     = finalizadas.stream().mapToDouble(Venda::getValorPago).sum();
            double totalTroco        = finalizadas.stream().mapToDouble(Venda::getTroco).sum();
            double ticketMedio       = finalizadas.isEmpty() ? 0 : totalFaturamento / finalizadas.size();

            tituloSecao(doc, "RESUMO DO PERÍODO");

            PdfPTable tRes = new PdfPTable(4);
            tRes.setWidthPercentage(100);
            tRes.setWidths(new float[]{1f, 1f, 1f, 1f});
            tRes.setSpacingAfter(6);

            addResumoCell(tRes, "Total de Vendas",    String.valueOf(finalizadas.size()),             "#4dabf7");
            addResumoCell(tRes, "Faturamento (Total)", Formatador.formatarMoeda(totalFaturamento),    "#40c057");
            addResumoCell(tRes, "Ticket Médio",        Formatador.formatarMoeda(ticketMedio),         "#6c63ff");
            addResumoCell(tRes, "Canceladas",          String.valueOf(canceladas.size()),              "#fa5252");
            doc.add(tRes);

            // 2ª linha de resumo com detalhes financeiros
            PdfPTable tRes2 = new PdfPTable(4);
            tRes2.setWidthPercentage(100);
            tRes2.setWidths(new float[]{1f, 1f, 1f, 1f});
            tRes2.setSpacingAfter(10);

            addResumoCell(tRes2, "Subtotal Bruto",     Formatador.formatarMoeda(totalSubtotal),       "#339af0");
            addResumoCell(tRes2, "Total Descontos",    Formatador.formatarMoeda(totalDesconto),        "#f03e3e");
            addResumoCell(tRes2, "Total Recebido",     Formatador.formatarMoeda(totalRecebido),        "#2f9e44");
            addResumoCell(tRes2, "Total de Troco",     Formatador.formatarMoeda(totalTroco),           "#e67700");
            doc.add(tRes2);

            // Breakdown por forma de pagamento
            tituloSecao(doc, "FATURAMENTO POR FORMA DE PAGAMENTO");
            Map<String, Double> porPagamento = finalizadas.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getFormaPagamento() != null ? v.getFormaPagamento() : "—",
                            Collectors.summingDouble(Venda::getTotal)));

            PdfPTable tPag = new PdfPTable(3);
            tPag.setWidthPercentage(60);
            tPag.setWidths(new float[]{2f, 1f, 1.5f});
            tPag.setSpacingAfter(10);
            tPag.setHorizontalAlignment(Element.ALIGN_LEFT);
            addHeaderCell(tPag, "Forma de Pagamento");
            addHeaderCell(tPag, "Qtd");
            addHeaderCell(tPag, "Total");

            boolean par = false;
            for (Map.Entry<String, Double> entry : porPagamento.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList())) {
                long qtdForma = finalizadas.stream()
                        .filter(v -> entry.getKey().equals(v.getFormaPagamento() != null ? v.getFormaPagamento() : "—"))
                        .count();
                BaseColor bg = par ? COR_LINHA_PAR : BaseColor.WHITE;
                addDataCell(tPag, entry.getKey(), bg, Element.ALIGN_LEFT);
                addDataCell(tPag, String.valueOf(qtdForma), bg, Element.ALIGN_CENTER);
                addDataCell(tPag, Formatador.formatarMoeda(entry.getValue()), bg, Element.ALIGN_RIGHT);
                par = !par;
            }
            // Linha total
            PdfPCell cTotPag = new PdfPCell(new Phrase("TOTAL", F_NEGRITO));
            cTotPag.setPadding(4); cTotPag.setBorderColor(BaseColor.LIGHT_GRAY);
            tPag.addCell(cTotPag);
            PdfPCell cTotQtd = new PdfPCell(new Phrase(String.valueOf(finalizadas.size()), F_TOTAL));
            cTotQtd.setPadding(4); cTotQtd.setBorderColor(BaseColor.LIGHT_GRAY);
            cTotQtd.setHorizontalAlignment(Element.ALIGN_CENTER);
            tPag.addCell(cTotQtd);
            PdfPCell cTotVal = new PdfPCell(new Phrase(Formatador.formatarMoeda(totalFaturamento), F_TOTAL));
            cTotVal.setPadding(4); cTotVal.setBorderColor(BaseColor.LIGHT_GRAY);
            cTotVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tPag.addCell(cTotVal);
            doc.add(tPag);

            if (totalDesconto > 0) {
                Paragraph pDesc = new Paragraph("Total de descontos concedidos: " + Formatador.formatarMoeda(totalDesconto), F_SUBTITULO);
                pDesc.setSpacingAfter(10);
                doc.add(pDesc);
            }

            // ── LISTA COMPLETA DE VENDAS ─────────────────────────────────────────
            tituloSecao(doc, "VENDAS REALIZADAS (" + finalizadas.size() + ")");

            PdfPTable tVendas = new PdfPTable(9);
            tVendas.setWidthPercentage(100);
            tVendas.setWidths(new float[]{0.6f, 1.2f, 1.4f, 0.9f, 0.9f, 0.8f, 0.9f, 0.9f, 0.7f});
            tVendas.setSpacingAfter(10);

            addHeaderCell(tVendas, "Nº");
            addHeaderCell(tVendas, "Data/Hora");
            addHeaderCell(tVendas, "Cliente");
            addHeaderCell(tVendas, "Pagamento");
            addHeaderCell(tVendas, "Subtotal");
            addHeaderCell(tVendas, "Desconto");
            addHeaderCell(tVendas, "Total");
            addHeaderCell(tVendas, "Recebido");
            addHeaderCell(tVendas, "Troco");

            par = false;
            for (Venda v : finalizadas) {
                BaseColor bg = par ? COR_LINHA_PAR : BaseColor.WHITE;
                String dataHora = v.getDataVenda() != null ? v.getDataVenda().format(FMT) : "-";
                String cliente  = v.getClienteNome() != null && !v.getClienteNome().isBlank()
                        ? v.getClienteNome() : "Consumidor Final";
                addDataCell(tVendas, "#" + v.getNumero(),                                   bg, Element.ALIGN_CENTER);
                addDataCell(tVendas, dataHora,                                               bg, Element.ALIGN_CENTER);
                addDataCell(tVendas, cliente,                                                bg, Element.ALIGN_LEFT);
                addDataCell(tVendas, nvl(v.getFormaPagamento(), "-"),                        bg, Element.ALIGN_CENTER);
                addDataCell(tVendas, Formatador.formatarMoeda(v.getSubtotal()),              bg, Element.ALIGN_RIGHT);
                addDataCell(tVendas, v.getDesconto() > 0
                        ? Formatador.formatarMoeda(v.getDesconto()) : "-",                   bg, Element.ALIGN_RIGHT);
                addDataCell(tVendas, Formatador.formatarMoeda(v.getTotal()),                 bg, Element.ALIGN_RIGHT);
                addDataCell(tVendas, v.getValorPago() > 0
                        ? Formatador.formatarMoeda(v.getValorPago()) : "-",                  bg, Element.ALIGN_RIGHT);
                addDataCell(tVendas, v.getTroco() > 0
                        ? Formatador.formatarMoeda(v.getTroco()) : "-",                      bg, Element.ALIGN_RIGHT);
                par = !par;
            }

            // Linha de totais
            addTotaisCell(tVendas, "TOTAIS", 4);
            addTotaisValCell(tVendas, Formatador.formatarMoeda(totalSubtotal));
            addTotaisValCell(tVendas, Formatador.formatarMoeda(totalDesconto));
            addTotaisValCell(tVendas, Formatador.formatarMoeda(totalFaturamento));
            addTotaisValCell(tVendas, Formatador.formatarMoeda(totalRecebido));
            addTotaisValCell(tVendas, Formatador.formatarMoeda(totalTroco));
            doc.add(tVendas);

            // ── VENDAS CANCELADAS ────────────────────────────────────────────────
            if (!canceladas.isEmpty()) {
                tituloSecao(doc, "VENDAS CANCELADAS (" + canceladas.size() + ")");

                PdfPTable tCanc = new PdfPTable(6);
                tCanc.setWidthPercentage(100);
                tCanc.setWidths(new float[]{0.6f, 1.2f, 1.2f, 0.8f, 0.8f, 2.0f});
                tCanc.setSpacingAfter(10);

                addHeaderCell(tCanc, "Nº");
                addHeaderCell(tCanc, "Data/Hora");
                addHeaderCell(tCanc, "Cliente");
                addHeaderCell(tCanc, "Subtotal");
                addHeaderCell(tCanc, "Total");
                addHeaderCell(tCanc, "Motivo");

                for (Venda v : canceladas) {
                    String dataHora = v.getDataVenda() != null ? v.getDataVenda().format(FMT) : "-";
                    String cliente  = v.getClienteNome() != null && !v.getClienteNome().isBlank()
                            ? v.getClienteNome() : "Consumidor Final";
                    String motivo   = v.getObservacoes() != null && !v.getObservacoes().isBlank()
                            ? v.getObservacoes() : "—";
                    addCancelCell(tCanc, "#" + v.getNumero());
                    addCancelCell(tCanc, dataHora);
                    addCancelCell(tCanc, cliente);
                    addCancelCell(tCanc, Formatador.formatarMoeda(v.getSubtotal()));
                    addCancelCell(tCanc, Formatador.formatarMoeda(v.getTotal()));
                    addCancelCell(tCanc, motivo);
                }
                doc.add(tCanc);
            }

            // ── RODAPÉ ───────────────────────────────────────────────────────────
            linhaHR(doc);
            doc.add(centrado("Relatório gerado pelo DRS ERP em "
                    + LocalDateTime.now().format(FMT), F_SUBTITULO));

            doc.close();
            return nomeArquivo;

        } catch (Exception e) {
            log.error("Erro ao gerar RelatorioVendasPDF", e);
            return null;
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private static void adicionarCabecalhoEmpresa(Document doc, Empresa empresa) throws DocumentException {
        if (empresa == null) return;
        String nome = nvl(empresa.getNomeFantasia(), nvl(empresa.getRazaoSocial(), ""));
        if (!nome.isBlank()) {
            Paragraph p = new Paragraph(nome.toUpperCase(), F_EMPRESA);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
        }
        if (!nvl(empresa.getCnpj(), "").isBlank())
            doc.add(centrado("CNPJ: " + empresa.getCnpj(), F_NORMAL));
        StringBuilder end = new StringBuilder();
        if (!nvl(empresa.getLogradouro(), "").isBlank()) end.append(empresa.getLogradouro());
        if (!nvl(empresa.getNumero(), "").isBlank())     end.append(", ").append(empresa.getNumero());
        if (!nvl(empresa.getBairro(), "").isBlank())     end.append(" - ").append(empresa.getBairro());
        if (!nvl(empresa.getCidade(), "").isBlank())     end.append(" / ").append(empresa.getCidade());
        if (end.length() > 0) doc.add(centrado(end.toString(), F_NORMAL));
        if (!nvl(empresa.getTelefone(), "").isBlank())
            doc.add(centrado("Tel: " + empresa.getTelefone(), F_NORMAL));
    }

    private static void tituloSecao(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto, F_SECAO);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private static void linhaHR(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(4);
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(1);
        c.setBackgroundColor(BaseColor.LIGHT_GRAY);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
    }

    private static void addHeaderCell(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_HEADER_TB));
        c.setBackgroundColor(COR_HEADER);
        c.setPadding(4);
        c.setBorderColor(BaseColor.WHITE);
        t.addCell(c);
    }

    private static void addDataCell(PdfPTable t, String texto, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_NORMAL));
        c.setBackgroundColor(bg);
        c.setPadding(3);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private static void addCancelCell(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_CANCEL));
        c.setBackgroundColor(COR_CANCEL_BG);
        c.setPadding(3);
        c.setBorderColor(new BaseColor(220, 150, 150));
        t.addCell(c);
    }

    private static void addResumoCell(PdfPTable t, String titulo, String valor, String corHex) {
        BaseColor cor = parseHex(corHex);
        Font fVal = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, cor);
        Font fTit = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.DARK_GRAY);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COR_RESUMO_BG);
        cell.setPadding(8);
        cell.setBorderColor(new BaseColor(210, 220, 240));

        Paragraph p = new Paragraph();
        p.add(new Chunk(valor + "\n", fVal));
        p.add(new Chunk(titulo, fTit));
        cell.addElement(p);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cell);
    }

    private static void addTotaisCell(PdfPTable t, String texto, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_NEGRITO));
        c.setColspan(colspan);
        c.setBackgroundColor(new BaseColor(220, 230, 255));
        c.setPadding(4);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private static void addTotaisValCell(PdfPTable t, String texto) {
        Font fTot = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, new BaseColor(20, 80, 160));
        PdfPCell c = new PdfPCell(new Phrase(texto, fTot));
        c.setBackgroundColor(new BaseColor(220, 230, 255));
        c.setPadding(4);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private static Paragraph centrado(String texto, Font fonte) {
        Paragraph p = new Paragraph(texto, fonte);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(1);
        return p;
    }

    private static BaseColor parseHex(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return new BaseColor(r, g, b);
        } catch (Exception e) {
            return BaseColor.DARK_GRAY;
        }
    }

    private static String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }
}
