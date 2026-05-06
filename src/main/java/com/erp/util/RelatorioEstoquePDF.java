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
import java.util.Optional;

/**
 * Gera relatório PDF de produtos com estoque abaixo do mínimo.
 */
public class RelatorioEstoquePDF {

    private static final Logger log = LoggerFactory.getLogger(RelatorioEstoquePDF.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font F_EMPRESA   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_SECAO     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(40, 60, 120));
    private static final Font F_NORMAL    = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_NEGRITO   = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_HEADER_TB = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_ALERTA    = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(180, 80, 0));
    private static final Font F_CRITICO   = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   new BaseColor(200, 30, 30));

    private static final BaseColor COR_HEADER    = new BaseColor(40, 60, 120);
    private static final BaseColor COR_LINHA_PAR = new BaseColor(240, 244, 255);
    private static final BaseColor COR_CRITICO   = new BaseColor(255, 235, 235);
    private static final BaseColor COR_ALERTA    = new BaseColor(255, 248, 235);

    private RelatorioEstoquePDF() {}

    /**
     * Gera PDF do relatório de estoque baixo.
     *
     * @param produtos Lista de produtos com estoque abaixo do mínimo
     * @return caminho do arquivo PDF gerado, ou null em caso de erro
     */
    public static String gerar(List<Produto> produtos) {
        try {
            Optional<Empresa> optEmpresa = new EmpresaDAO().carregar();
            Empresa empresa = optEmpresa.orElse(null);

            String nomeArquivo = System.getProperty("java.io.tmpdir")
                    + File.separator + "relatorio_estoque_baixo_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

            Document doc = new Document(PageSize.A4, 28f, 28f, 28f, 28f);
            PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
            doc.open();

            // ── CABEÇALHO DA EMPRESA ─────────────────────────────────────────────
            adicionarCabecalhoEmpresa(doc, empresa);
            linhaHR(doc);

            Paragraph titulo = new Paragraph("RELATÓRIO DE ESTOQUE BAIXO", F_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(4);
            titulo.setSpacingAfter(2);
            doc.add(titulo);

            doc.add(centrado("Gerado em: " + LocalDateTime.now().format(FMT), F_SUBTITULO));
            linhaHR(doc);

            // ── ALERTA ──────────────────────────────────────────────────────────
            long criticos = produtos.stream()
                    .filter(p -> p.getEstoqueAtual() <= 0)
                    .count();

            PdfPTable tAlert = new PdfPTable(2);
            tAlert.setWidthPercentage(60);
            tAlert.setWidths(new float[]{1.5f, 1f});
            tAlert.setSpacingBefore(4);
            tAlert.setSpacingAfter(10);
            tAlert.setHorizontalAlignment(Element.ALIGN_LEFT);

            addAlertaCell(tAlert, "⚠ Produtos abaixo do mínimo:", String.valueOf(produtos.size()), new BaseColor(255, 243, 205));
            addAlertaCell(tAlert, "🔴 Produtos zerados/negativos:", String.valueOf(criticos), COR_CRITICO.equals(BaseColor.WHITE) ? new BaseColor(255, 235, 235) : new BaseColor(255, 235, 235));
            doc.add(tAlert);

            // ── TABELA PRINCIPAL ─────────────────────────────────────────────────
            tituloSecao(doc, "PRODUTOS COM REPOSIÇÃO NECESSÁRIA");

            PdfPTable tProd = new PdfPTable(7);
            tProd.setWidthPercentage(100);
            tProd.setWidths(new float[]{0.8f, 2.5f, 1.2f, 0.6f, 0.9f, 0.9f, 0.9f});
            tProd.setSpacingAfter(10);

            addHeaderCell(tProd, "Código");
            addHeaderCell(tProd, "Produto");
            addHeaderCell(tProd, "Categoria");
            addHeaderCell(tProd, "UN");
            addHeaderCell(tProd, "Atual");
            addHeaderCell(tProd, "Mínimo");
            addHeaderCell(tProd, "Venda");

            boolean par = false;
            for (Produto p : produtos) {
                boolean ehCritico = p.getEstoqueAtual() <= 0;
                BaseColor bg = ehCritico ? COR_CRITICO : (par ? COR_LINHA_PAR : COR_ALERTA);
                Font fLinha = ehCritico ? F_CRITICO : F_NORMAL;

                addDataCell(tProd, nvl(p.getCodigo(), "-"),                                bg, Element.ALIGN_CENTER, fLinha);
                addDataCell(tProd, nvl(p.getNome(), "-"),                                  bg, Element.ALIGN_LEFT,   fLinha);
                addDataCell(tProd, nvl(p.getCategoriaNome(), "-"),                         bg, Element.ALIGN_LEFT,   fLinha);
                addDataCell(tProd, nvl(p.getUnidade(), "UN"),                              bg, Element.ALIGN_CENTER, fLinha);
                addDataCell(tProd, Formatador.formatarQuantidade(p.getEstoqueAtual()),     bg, Element.ALIGN_CENTER, fLinha);
                addDataCell(tProd, Formatador.formatarQuantidade(p.getEstoqueMinimo()),    bg, Element.ALIGN_CENTER, fLinha);
                addDataCell(tProd, Formatador.formatarMoeda(p.getPrecoVenda()),            bg, Element.ALIGN_RIGHT,  F_NORMAL);
                par = !par;
            }
            doc.add(tProd);

            // Legenda
            PdfPTable tLeg = new PdfPTable(2);
            tLeg.setWidthPercentage(40);
            tLeg.setHorizontalAlignment(Element.ALIGN_LEFT);
            tLeg.setSpacingAfter(10);

            PdfPCell cL1 = new PdfPCell(new Phrase("  ", F_NORMAL));
            cL1.setBackgroundColor(COR_CRITICO);
            cL1.setPadding(4);
            cL1.setBorder(Rectangle.NO_BORDER);
            tLeg.addCell(cL1);
            PdfPCell cL1t = new PdfPCell(new Phrase("Estoque zerado ou negativo", F_CRITICO));
            cL1t.setPadding(4);
            cL1t.setBorder(Rectangle.NO_BORDER);
            tLeg.addCell(cL1t);

            PdfPCell cL2 = new PdfPCell(new Phrase("  ", F_NORMAL));
            cL2.setBackgroundColor(COR_ALERTA);
            cL2.setPadding(4);
            cL2.setBorder(Rectangle.NO_BORDER);
            tLeg.addCell(cL2);
            PdfPCell cL2t = new PdfPCell(new Phrase("Estoque abaixo do mínimo", F_ALERTA));
            cL2t.setPadding(4);
            cL2t.setBorder(Rectangle.NO_BORDER);
            tLeg.addCell(cL2t);
            doc.add(tLeg);

            // ── RODAPÉ ───────────────────────────────────────────────────────────
            linhaHR(doc);
            doc.add(centrado("Relatório gerado pelo DRS ERP em "
                    + LocalDateTime.now().format(FMT), F_SUBTITULO));

            doc.close();
            return nomeArquivo;

        } catch (Exception e) {
            log.error("Erro ao gerar RelatorioEstoquePDF", e);
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

    private static void addDataCell(PdfPTable t, String texto, BaseColor bg, int align, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setPadding(3);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private static void addAlertaCell(PdfPTable t, String label, String valor, BaseColor bg) {
        Font fL = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.DARK_GRAY);
        Font fV = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(40, 60, 120));
        PdfPCell cL = new PdfPCell(new Phrase(label, fL));
        cL.setBackgroundColor(bg); cL.setPadding(6); cL.setBorderColor(BaseColor.LIGHT_GRAY);
        PdfPCell cV = new PdfPCell(new Phrase(valor, fV));
        cV.setBackgroundColor(bg); cV.setPadding(6); cV.setBorderColor(BaseColor.LIGHT_GRAY);
        cV.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cL);
        t.addCell(cV);
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
