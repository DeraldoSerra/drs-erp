package com.erp.util;

import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import com.erp.model.ItemVenda;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gera comprovante de reembolso em PDF para uma venda cancelada.
 */
public class RelatorioReembolsoPDF {

    private static final Logger log = LoggerFactory.getLogger(RelatorioReembolsoPDF.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Font F_EMPRESA  = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TITULO   = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   new BaseColor(20, 80, 160));
    private static final Font F_SUB      = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_NORMAL   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_BOLD     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_HDR      = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_MOTIVO   = new Font(Font.FontFamily.HELVETICA,  9, Font.ITALIC, new BaseColor(150, 40, 40));
    private static final Font F_TOTAL    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   new BaseColor(20, 80, 160));

    private static final BaseColor COR_HDR  = new BaseColor(20, 80, 160);
    private static final BaseColor COR_PAR  = new BaseColor(232, 240, 255);

    private RelatorioReembolsoPDF() {}

    public static String gerar(Venda venda) {
        try {
            String dir = System.getProperty("user.home") + File.separator + "DRS_ERP_Relatorios";
            new File(dir).mkdirs();
            String nome = "reembolso_" + venda.getNumero() + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String path = dir + File.separator + nome;

            Document doc = new Document(PageSize.A5);
            PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.open();

            // ── Cabeçalho empresa ──
            Empresa emp = new EmpresaDAO().carregar().orElse(null);
            if (emp != null && emp.getRazaoSocial() != null && !emp.getRazaoSocial().isBlank()) {
                String nomeExib = emp.getNomeFantasia() != null && !emp.getNomeFantasia().isBlank()
                        ? emp.getNomeFantasia() : emp.getRazaoSocial();
                Paragraph pEmp = new Paragraph(nomeExib.toUpperCase(), F_EMPRESA);
                pEmp.setAlignment(Element.ALIGN_CENTER);
                doc.add(pEmp);
                if (emp.getCnpj() != null && !emp.getCnpj().isBlank()) {
                    Paragraph pCnpj = new Paragraph("CNPJ: " + emp.getCnpj(), F_SUB);
                    pCnpj.setAlignment(Element.ALIGN_CENTER);
                    doc.add(pCnpj);
                }
                String end = "";
                if (emp.getLogradouro() != null) end += emp.getLogradouro();
                if (emp.getNumero() != null) end += ", " + emp.getNumero();
                if (emp.getBairro() != null) end += " — " + emp.getBairro();
                if (!end.isBlank()) {
                    Paragraph pEnd = new Paragraph(end.trim(), F_SUB);
                    pEnd.setAlignment(Element.ALIGN_CENTER);
                    doc.add(pEnd);
                }
                doc.add(new Paragraph(" "));
            }

            // ── Título ──
            Paragraph titulo = new Paragraph("COMPROVANTE DE REEMBOLSO", F_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            doc.add(titulo);
            doc.add(new Paragraph(" "));

            // ── Info da venda ──
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(10);
            infoTable.setWidths(new float[]{1f, 2f});
            adicionarLinhaInfo(infoTable, "Nº Venda:", venda.getNumero());
            adicionarLinhaInfo(infoTable, "Data Venda:",
                    venda.getDataVenda() != null ? venda.getDataVenda().format(FMT) : "—");
            adicionarLinhaInfo(infoTable, "Data Reembolso:",
                    LocalDateTime.now().format(FMT));
            adicionarLinhaInfo(infoTable, "Operador:",
                    venda.getUsuarioNome() != null ? venda.getUsuarioNome() : "—");
            adicionarLinhaInfo(infoTable, "Pagamento Original:",
                    venda.getFormaPagamento() != null ? venda.getFormaPagamento() : "—");
            if (venda.getClienteNome() != null && !venda.getClienteNome().isBlank())
                adicionarLinhaInfo(infoTable, "Cliente:", venda.getClienteNome());
            doc.add(infoTable);

            // ── Motivo do cancelamento ──
            if (venda.getObservacoes() != null && !venda.getObservacoes().isBlank()) {
                Paragraph pMot = new Paragraph("Motivo do cancelamento: " + venda.getObservacoes(), F_MOTIVO);
                pMot.setSpacingAfter(10);
                doc.add(pMot);
            }

            // ── Itens ──
            if (venda.getItens() != null && !venda.getItens().isEmpty()) {
                Paragraph pItens = new Paragraph("Itens Reembolsados:", F_BOLD);
                pItens.setSpacingBefore(4);
                pItens.setSpacingAfter(4);
                doc.add(pItens);

                PdfPTable tbl = new PdfPTable(4);
                tbl.setWidthPercentage(100);
                tbl.setWidths(new float[]{3.5f, 1f, 1.5f, 1.5f});
                tbl.setSpacingAfter(10);

                String[] headers = {"Produto", "Qtd", "Unit.", "Subtotal"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, F_HDR));
                    cell.setBackgroundColor(COR_HDR);
                    cell.setPadding(5);
                    cell.setBorder(Rectangle.NO_BORDER);
                    tbl.addCell(cell);
                }

                boolean par = false;
                for (ItemVenda item : venda.getItens()) {
                    BaseColor bg = par ? COR_PAR : BaseColor.WHITE;
                    addCell(tbl, item.getProdutoNome() != null ? item.getProdutoNome() : "—", F_NORMAL, bg, Element.ALIGN_LEFT);
                    addCell(tbl, Formatador.formatarQuantidade(item.getQuantidade()), F_NORMAL, bg, Element.ALIGN_CENTER);
                    addCell(tbl, Formatador.formatarMoeda(item.getPrecoUnit()), F_NORMAL, bg, Element.ALIGN_RIGHT);
                    addCell(tbl, Formatador.formatarMoeda(item.getSubtotal()), F_BOLD, bg, Element.ALIGN_RIGHT);
                    par = !par;
                }
                doc.add(tbl);
            }

            // ── Totais ──
            PdfPTable totTable = new PdfPTable(2);
            totTable.setWidthPercentage(60);
            totTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totTable.setWidths(new float[]{1.5f, 1f});
            totTable.setSpacingAfter(6);
            adicionarLinhaInfo(totTable, "Subtotal:", Formatador.formatarMoeda(venda.getSubtotal()));
            if (venda.getDesconto() > 0)
                adicionarLinhaInfo(totTable, "Desconto:", "- " + Formatador.formatarMoeda(venda.getDesconto()));
            if (venda.getAcrescimo() > 0)
                adicionarLinhaInfo(totTable, "Acréscimo:", "+ " + Formatador.formatarMoeda(venda.getAcrescimo()));
            doc.add(totTable);

            Paragraph pTotal = new Paragraph("VALOR REEMBOLSADO: " + Formatador.formatarMoeda(venda.getTotal()), F_TOTAL);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            pTotal.setSpacingBefore(4);
            pTotal.setSpacingAfter(2);
            doc.add(pTotal);
            if (venda.getValorPago() > 0) {
                Font fPago = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.DARK_GRAY);
                Paragraph pPago = new Paragraph("Valor pago originalmente: " + Formatador.formatarMoeda(venda.getValorPago())
                        + (venda.getTroco() > 0 ? "   (Troco dado: " + Formatador.formatarMoeda(venda.getTroco()) + ")" : ""), fPago);
                pPago.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pPago);
            }

            // ── Rodapé ──
            doc.add(new Paragraph(" "));
            Paragraph rodape = new Paragraph(
                    "Emitido em " + LocalDateTime.now().format(FMT) + " · DRS ERP", F_SUB);
            rodape.setAlignment(Element.ALIGN_CENTER);
            doc.add(rodape);

            doc.close();
            log.info("Reembolso PDF gerado: {}", path);
            return path;
        } catch (Exception e) {
            log.error("Erro ao gerar reembolso PDF", e);
            return null;
        }
    }

    private static void adicionarLinhaInfo(PdfPTable t, String chave, String valor) {
        PdfPCell c1 = new PdfPCell(new Phrase(chave, F_BOLD));
        c1.setBorder(Rectangle.BOTTOM);
        c1.setBorderColor(new BaseColor(200, 200, 200));
        c1.setPadding(4);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, F_NORMAL));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setBorderColor(new BaseColor(200, 200, 200));
        c2.setPadding(4);
        t.addCell(c1);
        t.addCell(c2);
    }

    private static void addCell(PdfPTable t, String txt, Font f, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(txt, f));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }
}
