package com.erp.util;

import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import com.erp.model.Produto;
import com.itextpdf.text.BaseColor;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Gera relatório PDF de capital em estoque:
 * quantidade por produto e valor total (custo e venda).
 */
public class RelatorioCapitalPDF {

    private static final Logger log = LoggerFactory.getLogger(RelatorioCapitalPDF.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font F_EMPRESA   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_SECAO     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(40, 60, 120));
    private static final Font F_NORMAL    = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_NEGRITO   = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_HEADER_TB = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_TOTAL     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(40, 60, 120));
    private static final Font F_CARD_VAL  = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   new BaseColor(40, 60, 120));
    private static final Font F_CARD_TIT  = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.DARK_GRAY);

    private static final BaseColor COR_HEADER    = new BaseColor(40, 60, 120);
    private static final BaseColor COR_LINHA_PAR = new BaseColor(240, 244, 255);
    private static final BaseColor COR_LINHA_IMP = BaseColor.WHITE;
    private static final BaseColor COR_TOTAL_ROW = new BaseColor(220, 230, 250);
    private static final BaseColor COR_CARD      = new BaseColor(245, 247, 255);

    private RelatorioCapitalPDF() {}

    /**
     * Gera o PDF de capital em estoque.
     *
     * @param produtos lista de todos os produtos (ativos com estoque > 0 ou todos)
     * @return caminho do arquivo PDF ou null em caso de erro
     */
    public static String gerar(List<Produto> produtos) {
        try {
            Optional<Empresa> optEmpresa = new EmpresaDAO().carregar();
            Empresa empresa = optEmpresa.orElse(null);

            String nomeArquivo = System.getProperty("java.io.tmpdir")
                    + File.separator + "relatorio_capital_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

            Document doc = new Document(PageSize.A4.rotate(), 28f, 28f, 28f, 28f);
            PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
            doc.open();

            // ── CABEÇALHO ─────────────────────────────────────────────────────
            adicionarCabecalhoEmpresa(doc, empresa);
            linhaHR(doc);

            Paragraph titulo = new Paragraph("RELATÓRIO DE CAPITAL EM ESTOQUE", F_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(4);
            titulo.setSpacingAfter(2);
            doc.add(titulo);
            doc.add(centrado("Gerado em: " + LocalDateTime.now().format(FMT), F_SUBTITULO));
            linhaHR(doc);

            // ── CARDS DE RESUMO ───────────────────────────────────────────────
            long   totalSkus      = produtos.size();
            double totalQtd       = produtos.stream().mapToDouble(Produto::getEstoqueAtual).sum();
            double capitalCusto   = produtos.stream()
                    .mapToDouble(p -> p.getEstoqueAtual() * p.getPrecoCusto()).sum();
            double capitalVenda   = produtos.stream()
                    .mapToDouble(p -> p.getEstoqueAtual() * p.getPrecoVenda()).sum();
            double lucroEstimado  = capitalVenda - capitalCusto;

            PdfPTable tCards = new PdfPTable(5);
            tCards.setWidthPercentage(100);
            tCards.setWidths(new float[]{1f, 1f, 1.5f, 1.5f, 1.5f});
            tCards.setSpacingBefore(6);
            tCards.setSpacingAfter(14);

            addCard(tCards, "Total de SKUs",          String.valueOf(totalSkus));
            addCard(tCards, "Total de Itens",         Formatador.formatarQuantidade(totalQtd));
            addCard(tCards, "Capital (Custo)",        Formatador.formatarMoeda(capitalCusto));
            addCard(tCards, "Capital (Venda)",        Formatador.formatarMoeda(capitalVenda));
            addCard(tCards, "Lucro Estimado",         Formatador.formatarMoeda(lucroEstimado));
            doc.add(tCards);

            // ── TABELA POR CATEGORIA ──────────────────────────────────────────
            Map<String, List<Produto>> porCategoria = produtos.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getCategoriaNome() != null ? p.getCategoriaNome() : "Sem Categoria"));

            for (Map.Entry<String, List<Produto>> entry : porCategoria.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {

                tituloSecao(doc, "CATEGORIA: " + entry.getKey().toUpperCase());

                PdfPTable tProd = new PdfPTable(8);
                tProd.setWidthPercentage(100);
                tProd.setWidths(new float[]{0.7f, 2.5f, 0.5f, 0.9f, 1.1f, 1.3f, 1.1f, 1.3f});
                tProd.setSpacingAfter(6);

                addHeaderCell(tProd, "Código");
                addHeaderCell(tProd, "Produto");
                addHeaderCell(tProd, "UN");
                addHeaderCell(tProd, "Qtd Atual");
                addHeaderCell(tProd, "Preço Custo");
                addHeaderCell(tProd, "Capital Custo");
                addHeaderCell(tProd, "Preço Venda");
                addHeaderCell(tProd, "Capital Venda");

                double subtotQtd   = 0;
                double subtotCusto = 0;
                double subtotVenda = 0;

                boolean par = false;
                for (Produto p : entry.getValue()) {
                    BaseColor bg = par ? COR_LINHA_PAR : COR_LINHA_IMP;
                    double capCusto = p.getEstoqueAtual() * p.getPrecoCusto();
                    double capVenda = p.getEstoqueAtual() * p.getPrecoVenda();
                    subtotQtd   += p.getEstoqueAtual();
                    subtotCusto += capCusto;
                    subtotVenda += capVenda;

                    addDataCell(tProd, nvl(p.getCodigo(), "-"),                             bg, Element.ALIGN_CENTER, F_NORMAL);
                    addDataCell(tProd, nvl(p.getNome(),   "-"),                             bg, Element.ALIGN_LEFT,   F_NORMAL);
                    addDataCell(tProd, nvl(p.getUnidade(), "UN"),                           bg, Element.ALIGN_CENTER, F_NORMAL);
                    addDataCell(tProd, Formatador.formatarQuantidade(p.getEstoqueAtual()),  bg, Element.ALIGN_CENTER, F_NORMAL);
                    addDataCell(tProd, Formatador.formatarMoeda(p.getPrecoCusto()),         bg, Element.ALIGN_RIGHT,  F_NORMAL);
                    addDataCell(tProd, Formatador.formatarMoeda(capCusto),                  bg, Element.ALIGN_RIGHT,  F_NEGRITO);
                    addDataCell(tProd, Formatador.formatarMoeda(p.getPrecoVenda()),         bg, Element.ALIGN_RIGHT,  F_NORMAL);
                    addDataCell(tProd, Formatador.formatarMoeda(capVenda),                  bg, Element.ALIGN_RIGHT,  F_NEGRITO);
                    par = !par;
                }

                // Linha de subtotal por categoria
                addTotalCell(tProd, "Subtotal", 3);
                addDataCell(tProd, Formatador.formatarQuantidade(subtotQtd),  COR_TOTAL_ROW, Element.ALIGN_CENTER, F_TOTAL);
                addDataCell(tProd, "",                                         COR_TOTAL_ROW, Element.ALIGN_CENTER, F_TOTAL);
                addDataCell(tProd, Formatador.formatarMoeda(subtotCusto),     COR_TOTAL_ROW, Element.ALIGN_RIGHT,  F_TOTAL);
                addDataCell(tProd, "",                                         COR_TOTAL_ROW, Element.ALIGN_CENTER, F_TOTAL);
                addDataCell(tProd, Formatador.formatarMoeda(subtotVenda),     COR_TOTAL_ROW, Element.ALIGN_RIGHT,  F_TOTAL);

                doc.add(tProd);
            }

            // ── TOTAIS GERAIS ─────────────────────────────────────────────────
            linhaHR(doc);
            tituloSecao(doc, "TOTAIS GERAIS");

            PdfPTable tTotais = new PdfPTable(5);
            tTotais.setWidthPercentage(80);
            tTotais.setHorizontalAlignment(Element.ALIGN_LEFT);
            tTotais.setWidths(new float[]{2f, 1.5f, 2f, 2f, 2f});
            tTotais.setSpacingAfter(10);

            addHeaderCell(tTotais, "Total SKUs");
            addHeaderCell(tTotais, "Total Itens");
            addHeaderCell(tTotais, "Capital Custo");
            addHeaderCell(tTotais, "Capital Venda");
            addHeaderCell(tTotais, "Lucro Estimado");

            addDataCell(tTotais, String.valueOf(totalSkus),                COR_TOTAL_ROW, Element.ALIGN_CENTER, F_TOTAL);
            addDataCell(tTotais, Formatador.formatarQuantidade(totalQtd),  COR_TOTAL_ROW, Element.ALIGN_CENTER, F_TOTAL);
            addDataCell(tTotais, Formatador.formatarMoeda(capitalCusto),   COR_TOTAL_ROW, Element.ALIGN_RIGHT,  F_TOTAL);
            addDataCell(tTotais, Formatador.formatarMoeda(capitalVenda),   COR_TOTAL_ROW, Element.ALIGN_RIGHT,  F_TOTAL);
            addDataCell(tTotais, Formatador.formatarMoeda(lucroEstimado),  COR_TOTAL_ROW, Element.ALIGN_RIGHT,  F_TOTAL);
            doc.add(tTotais);

            // ── RODAPÉ ────────────────────────────────────────────────────────
            linhaHR(doc);
            doc.add(centrado("Relatório gerado pelo DRS ERP em "
                    + LocalDateTime.now().format(FMT), F_SUBTITULO));

            doc.close();
            return nomeArquivo;

        } catch (Exception e) {
            log.error("Erro ao gerar RelatorioCapitalPDF", e);
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

    /** Célula de total/subtotal que ocupa `colspan` colunas. */
    private static void addTotalCell(PdfPTable t, String texto, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_TOTAL));
        c.setBackgroundColor(COR_TOTAL_ROW);
        c.setColspan(colspan);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        t.addCell(c);
    }

    private static void addDataCell(PdfPTable t, String texto, BaseColor bg, int align, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setPadding(3);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private static void addCard(PdfPTable t, String titulo, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(COR_CARD);
        c.setPadding(8);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        Paragraph p = new Paragraph();
        p.add(new Phrase(titulo + "\n", F_CARD_TIT));
        p.add(new Phrase(valor,         F_CARD_VAL));
        c.addElement(p);
        t.addCell(c);
    }

    private static Paragraph centrado(String texto, Font fonte) {
        Paragraph p = new Paragraph(texto, fonte);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(1);
        return p;
    }

    private static String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }
}
