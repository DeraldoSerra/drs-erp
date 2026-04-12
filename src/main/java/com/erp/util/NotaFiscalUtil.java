package com.erp.util;

import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import com.erp.model.ItemVenda;
import com.erp.model.Venda;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Gera cupom fiscal / nota simplificada em PDF usando iText.
 */
public class NotaFiscalUtil {

    private static final Logger log = LoggerFactory.getLogger(NotaFiscalUtil.class);

    private static final float LARGURA_CUPOM = 226f; // ~80mm
    private static final float MARGEM       = 8f;

    private static final Font FONTE_TITULO  = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
    private static final Font FONTE_NORMAL  = new Font(Font.FontFamily.COURIER, 7,  Font.NORMAL);
    private static final Font FONTE_NEGRITO = new Font(Font.FontFamily.COURIER, 7,  Font.BOLD);
    private static final Font FONTE_TOTAL   = new Font(Font.FontFamily.COURIER, 9,  Font.BOLD);
    private static final Font FONTE_PEQUENA = new Font(Font.FontFamily.COURIER, 6,  Font.NORMAL);

    private NotaFiscalUtil() {}

    /**
     * Gera o PDF do cupom fiscal e retorna o caminho do arquivo.
     * O arquivo é salvo em: [pasta temp]/cupom_XXXX.pdf
     */
    public static String gerar(Venda venda) {
        try {
            Optional<Empresa> optEmpresa = new EmpresaDAO().carregar();
            Empresa empresa = optEmpresa.orElse(empresaPadrao());

            String nomeArquivo = System.getProperty("java.io.tmpdir") +
                                 File.separator + "cupom_" + venda.getNumero() + ".pdf";

            Document doc = new Document(new Rectangle(LARGURA_CUPOM, 800), MARGEM, MARGEM, MARGEM, MARGEM);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
            doc.open();

            adicionarCabecalho(doc, empresa);
            adicionarSeparador(doc);
            adicionarDadosVenda(doc, venda);
            adicionarSeparador(doc);
            adicionarItens(doc, venda);
            adicionarSeparador(doc);
            adicionarTotais(doc, venda);
            adicionarSeparador(doc);
            adicionarPagamento(doc, venda);
            adicionarSeparador(doc);
            adicionarRodape(doc, venda);

            doc.close();
            log.info("Cupom gerado: {}", nomeArquivo);
            return nomeArquivo;

        } catch (Exception e) {
            log.error("Erro ao gerar cupom fiscal", e);
            return null;
        }
    }

    private static void adicionarCabecalho(Document doc, Empresa empresa) throws DocumentException {
        String razao = empresa.getRazaoSocial() != null ? empresa.getRazaoSocial() : "MINHA EMPRESA";
        String fantasia = empresa.getNomeFantasia();
        String cnpj = empresa.getCnpj() != null ? empresa.getCnpj() : "";
        String ie   = empresa.getIe() != null ? empresa.getIe() : "";
        String end  = empresa.getEnderecoCompleto();
        String tel  = empresa.getTelefone() != null ? empresa.getTelefone() : "";
        String regime = empresa.getRegimeTributarioLabel();

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(razao + "\n", FONTE_TITULO));
        if (fantasia != null && !fantasia.isBlank())
            p.add(new Chunk(fantasia + "\n", FONTE_NORMAL));
        if (!cnpj.isBlank())
            p.add(new Chunk("CNPJ: " + cnpj + "\n", FONTE_NORMAL));
        if (!ie.isBlank())
            p.add(new Chunk("IE: " + ie + "\n", FONTE_NORMAL));
        if (!end.isBlank())
            p.add(new Chunk(end + "\n", FONTE_PEQUENA));
        if (!tel.isBlank())
            p.add(new Chunk("Tel: " + tel + "\n", FONTE_NORMAL));
        if (!regime.isBlank())
            p.add(new Chunk(regime + "\n", FONTE_PEQUENA));
        doc.add(p);
    }

    private static void adicionarDadosVenda(Document doc, Venda venda) throws DocumentException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("CUPOM FISCAL\n", FONTE_TITULO));
        p.add(new Chunk("Nº " + venda.getNumero() + "\n", FONTE_NEGRITO));
        p.add(new Chunk(fmt.format(venda.getDataVenda()) + "\n", FONTE_NORMAL));
        if (venda.getClienteNome() != null && !venda.getClienteNome().isBlank())
            p.add(new Chunk("Cliente: " + venda.getClienteNome() + "\n", FONTE_NORMAL));
        p.add(new Chunk("Operador: " + (venda.getUsuarioNome() != null ? venda.getUsuarioNome() : "-") + "\n", FONTE_NORMAL));
        doc.add(p);
    }

    private static void adicionarItens(Document doc, Venda venda) throws DocumentException {
        // Cabeçalho da tabela
        PdfPTable tabela = new PdfPTable(new float[]{40f, 10f, 15f, 15f});
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(0);

        adicionarCelulaTabela(tabela, "PRODUTO", FONTE_NEGRITO, Element.ALIGN_LEFT,  true);
        adicionarCelulaTabela(tabela, "QTD",     FONTE_NEGRITO, Element.ALIGN_CENTER, true);
        adicionarCelulaTabela(tabela, "UNIT",    FONTE_NEGRITO, Element.ALIGN_RIGHT,  true);
        adicionarCelulaTabela(tabela, "TOTAL",   FONTE_NEGRITO, Element.ALIGN_RIGHT,  true);

        for (ItemVenda item : venda.getItens()) {
            String nome = item.getProdutoNome();
            if (nome != null && nome.length() > 22) nome = nome.substring(0, 22);

            adicionarCelulaTabela(tabela, nome,                                                    FONTE_NORMAL, Element.ALIGN_LEFT,   false);
            adicionarCelulaTabela(tabela, Formatador.formatarQuantidade(item.getQuantidade()),     FONTE_NORMAL, Element.ALIGN_CENTER, false);
            adicionarCelulaTabela(tabela, Formatador.formatarMoeda(item.getPrecoUnit()),           FONTE_NORMAL, Element.ALIGN_RIGHT,  false);
            adicionarCelulaTabela(tabela, Formatador.formatarMoeda(item.getSubtotal()),            FONTE_NORMAL, Element.ALIGN_RIGHT,  false);
        }

        doc.add(tabela);
    }

    private static void adicionarCelulaTabela(PdfPTable tabela, String texto, Font fonte,
                                               int alinhamento, boolean cabecalho) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setHorizontalAlignment(alinhamento);
        cell.setBorder(cabecalho ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        cell.setPaddingBottom(2);
        cell.setPaddingTop(1);
        tabela.addCell(cell);
    }

    private static void adicionarTotais(Document doc, Venda venda) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_RIGHT);

        if (venda.getDesconto() > 0)
            p.add(new Chunk("Subtotal: " + Formatador.formatarMoeda(venda.getSubtotal()) + "\n", FONTE_NORMAL));
        if (venda.getDesconto() > 0)
            p.add(new Chunk("Desconto: -" + Formatador.formatarMoeda(venda.getDesconto()) + "\n", FONTE_NORMAL));
        if (venda.getAcrescimo() > 0)
            p.add(new Chunk("Acréscimo: " + Formatador.formatarMoeda(venda.getAcrescimo()) + "\n", FONTE_NORMAL));

        p.add(new Chunk("TOTAL: " + Formatador.formatarMoeda(venda.getTotal()) + "\n", FONTE_TOTAL));
        doc.add(p);
    }

    private static void adicionarPagamento(Document doc, Venda venda) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_LEFT);
        p.add(new Chunk("Forma de pagamento: " + venda.getFormaPagamento() + "\n", FONTE_NORMAL));
        if (venda.getValorPago() > 0)
            p.add(new Chunk("Valor pago: " + Formatador.formatarMoeda(venda.getValorPago()) + "\n", FONTE_NORMAL));
        if (venda.getTroco() > 0)
            p.add(new Chunk("Troco: " + Formatador.formatarMoeda(venda.getTroco()) + "\n", FONTE_NEGRITO));
        doc.add(p);
    }

    private static void adicionarRodape(Document doc, Venda venda) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("Obrigado pela preferência!\n", FONTE_NORMAL));
        p.add(new Chunk("Este documento não tem valor fiscal.\n", FONTE_PEQUENA));
        doc.add(p);
    }

    private static void adicionarSeparador(Document doc) throws DocumentException {
        String linha = "-".repeat(38);
        Paragraph p = new Paragraph(linha + "\n", FONTE_NORMAL);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        doc.add(p);
    }

    private static Empresa empresaPadrao() {
        Empresa e = new Empresa();
        e.setRazaoSocial("MINHA EMPRESA LTDA");
        e.setRegimeTributario("SIMPLES_NACIONAL");
        return e;
    }
}
