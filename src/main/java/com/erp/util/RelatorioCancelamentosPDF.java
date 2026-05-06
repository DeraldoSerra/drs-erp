package com.erp.util;

import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import com.erp.model.Venda;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera relatório PDF de vendas canceladas por período.
 */
public class RelatorioCancelamentosPDF {

    private static final Logger log = LoggerFactory.getLogger(RelatorioCancelamentosPDF.class);
    private static final DateTimeFormatter FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font F_EMPRESA   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   new BaseColor(180, 30, 30));
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_NORMAL    = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_NEGRITO   = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_HEADER_TB = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_MOTIVO    = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, new BaseColor(160, 40, 40));
    private static final Font F_TOTAL     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(180, 30, 30));

    private static final BaseColor COR_HEADER    = new BaseColor(180, 30, 30);
    private static final BaseColor COR_LINHA_PAR = new BaseColor(255, 240, 240);

    private RelatorioCancelamentosPDF() {}

    public static String gerar(LocalDate inicio, LocalDate fim, List<Venda> canceladas) {
        try {
            String pasta = System.getProperty("user.home") + File.separator + "DRS-ERP-Relatorios";
            new File(pasta).mkdirs();
            String arquivo = pasta + File.separator +
                    "cancelamentos_" + inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                    "_" + fim.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            Document doc = new Document(PageSize.A4, 36, 36, 50, 36);
            PdfWriter.getInstance(doc, new FileOutputStream(arquivo));
            doc.open();

            // Cabeçalho empresa
            Empresa empresa = new EmpresaDAO().carregar().orElse(null);
            if (empresa != null && empresa.getRazaoSocial() != null && !empresa.getRazaoSocial().isBlank()) {
                Paragraph pEmp = new Paragraph(empresa.getRazaoSocial(), F_EMPRESA);
                pEmp.setAlignment(Element.ALIGN_CENTER);
                doc.add(pEmp);
                if (empresa.getCnpj() != null && !empresa.getCnpj().isBlank()) {
                    Paragraph pCnpj = new Paragraph("CNPJ: " + empresa.getCnpj(), F_SUBTITULO);
                    pCnpj.setAlignment(Element.ALIGN_CENTER);
                    doc.add(pCnpj);
                }
            }

            Paragraph pTitulo = new Paragraph("🚫 RELATÓRIO DE CANCELAMENTOS", F_TITULO);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            pTitulo.setSpacingBefore(8);
            doc.add(pTitulo);

            String periodoTxt = inicio.equals(fim)
                    ? inicio.format(FMT_DT)
                    : inicio.format(FMT_DT) + " a " + fim.format(FMT_DT);
            Paragraph pPeriodo = new Paragraph("Período: " + periodoTxt +
                    "   |   Gerado em: " + LocalDateTime.now().format(FMT), F_SUBTITULO);
            pPeriodo.setAlignment(Element.ALIGN_CENTER);
            pPeriodo.setSpacingAfter(10);
            doc.add(pPeriodo);

            // Resumo
            double totalCancelado    = canceladas.stream().mapToDouble(Venda::getTotal).sum();
            long   qtdReembolsados   = canceladas.stream().filter(Venda::isReembolsado).count();
            PdfPTable resumo = new PdfPTable(2);
            resumo.setWidthPercentage(60);
            resumo.setHorizontalAlignment(Element.ALIGN_CENTER);
            resumo.setSpacingAfter(14);
            adicionarCelulaResumo(resumo, "Total de Cancelamentos", String.valueOf(canceladas.size()));
            adicionarCelulaResumo(resumo, "Valor Total Cancelado", Formatador.formatarMoeda(totalCancelado));
            adicionarCelulaResumo(resumo, "Reembolsos Emitidos", String.valueOf(qtdReembolsados));
            doc.add(resumo);

            if (canceladas.isEmpty()) {
                Paragraph pVazio = new Paragraph("Nenhum cancelamento encontrado no período.", F_NORMAL);
                pVazio.setAlignment(Element.ALIGN_CENTER);
                doc.add(pVazio);
                doc.close();
                return arquivo;
            }

            // Tabela de cancelamentos
            PdfPTable tabela = new PdfPTable(new float[]{40, 90, 90, 60, 60, 60, 70, 180});
            tabela.setWidthPercentage(100);
            tabela.setSpacingBefore(6);

            String[] cabecalhos = {"Nº", "Data/Hora", "Operador", "Subtotal", "Desconto", "Total", "Reembolsado", "Motivo"};
            for (String cab : cabecalhos) {
                PdfPCell cell = new PdfPCell(new Phrase(cab, F_HEADER_TB));
                cell.setBackgroundColor(COR_HEADER);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(5);
                tabela.addCell(cell);
            }

            boolean par = false;
            for (Venda v : canceladas) {
                BaseColor bg = par ? COR_LINHA_PAR : BaseColor.WHITE;
                par = !par;

                adicionarCelula(tabela, v.getNumero() != null ? v.getNumero() : "—", F_NORMAL, bg, Element.ALIGN_CENTER);
                adicionarCelula(tabela, v.getDataVenda() != null ? v.getDataVenda().format(FMT) : "—", F_NORMAL, bg, Element.ALIGN_CENTER);
                adicionarCelula(tabela, v.getUsuarioNome() != null ? v.getUsuarioNome() : "—", F_NORMAL, bg, Element.ALIGN_LEFT);
                adicionarCelula(tabela, Formatador.formatarMoeda(v.getSubtotal()), F_NORMAL, bg, Element.ALIGN_RIGHT);
                adicionarCelula(tabela, v.getDesconto() > 0 ? Formatador.formatarMoeda(v.getDesconto()) : "—", F_NORMAL, bg, Element.ALIGN_RIGHT);
                adicionarCelula(tabela, Formatador.formatarMoeda(v.getTotal()), F_NEGRITO, bg, Element.ALIGN_RIGHT);
                adicionarCelula(tabela, v.isReembolsado() ? "Sim" : "Não", F_NORMAL, bg, Element.ALIGN_CENTER);
                adicionarCelula(tabela, v.getObservacoes() != null && !v.getObservacoes().isBlank() ? v.getObservacoes() : "Não informado", F_MOTIVO, bg, Element.ALIGN_LEFT);
            }
            doc.add(tabela);

            // Rodapé total
            Paragraph pTotal = new Paragraph("Total cancelado no período: " + Formatador.formatarMoeda(totalCancelado), F_TOTAL);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            pTotal.setSpacingBefore(10);
            doc.add(pTotal);

            doc.close();
            log.info("PDF cancelamentos gerado: {}", arquivo);
            return arquivo;
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de cancelamentos", e);
            return null;
        }
    }

    private static void adicionarCelulaResumo(PdfPTable t, String rotulo, String valor) {
        PdfPCell cR = new PdfPCell(new Phrase(rotulo, F_NEGRITO));
        cR.setBorder(Rectangle.BOX);
        cR.setPadding(5);
        t.addCell(cR);
        PdfPCell cV = new PdfPCell(new Phrase(valor, F_NORMAL));
        cV.setBorder(Rectangle.BOX);
        cV.setPadding(5);
        cV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cV);
    }

    private static void adicionarCelula(PdfPTable t, String texto, Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        t.addCell(cell);
    }
}
