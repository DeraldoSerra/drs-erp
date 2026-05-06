package com.erp.util;

import com.erp.dao.CaixaDAO;
import com.erp.dao.EmpresaDAO;
import com.erp.dao.VendaDAO;
import com.erp.model.Empresa;
import com.erp.model.ItemVenda;
import com.erp.model.MovimentoCaixa;
import com.erp.model.SessaoCaixa;
import com.erp.model.Venda;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Gera o relatório PDF de fechamento/final do dia do caixa,
 * com detalhamento completo de todas as vendas realizadas.
 */
public class RelatorioCaixaPDF {

    private static final Logger log = LoggerFactory.getLogger(RelatorioCaixaPDF.class);
    private static final DateTimeFormatter FMT    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Largura A4 em pontos (595) para um relatório legível com tabelas
    private static final float LARGURA = 595f;
    private static final float MARGEM  = 28f;

    private static final Font F_TITULO    = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.DARK_GRAY);
    private static final Font F_SECAO     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(40, 60, 120));
    private static final Font F_NORMAL    = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_NEGRITO   = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.BLACK);
    private static final Font F_TOTAL     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(20, 120, 50));
    private static final Font F_CANCEL    = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, new BaseColor(180, 30, 30));
    private static final Font F_HEADER_TB = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   BaseColor.WHITE);
    private static final Font F_ITEM_TB   = new Font(Font.FontFamily.HELVETICA,  7, Font.NORMAL, BaseColor.BLACK);

    private static final BaseColor COR_HEADER_TB  = new BaseColor(40, 60, 120);
    private static final BaseColor COR_LINHA_PAR  = new BaseColor(240, 244, 255);
    private static final BaseColor COR_CANCEL_BG  = new BaseColor(255, 235, 235);

    private RelatorioCaixaPDF() {}

    /**
     * Gera PDF completo do relatório de fechamento do caixa.
     * Inclui: resumo financeiro, vendas detalhadas por venda (com itens),
     * vendas canceladas e movimentações de caixa.
     *
     * @param sessao SessaoCaixa com os totais calculados
     * @return caminho completo do arquivo PDF ou null em caso de erro
     */
    public static String gerar(SessaoCaixa sessao) {
        try {
            Optional<Empresa> optEmpresa = new EmpresaDAO().carregar();
            Empresa empresa = optEmpresa.orElse(null);

            String nomeArquivo = System.getProperty("java.io.tmpdir")
                    + File.separator + "relatorio_caixa_" + sessao.getId() + ".pdf";

            // Altura calculada dinamicamente — usar página A4 com rolagem
            Document doc = new Document(PageSize.A4, MARGEM, MARGEM, MARGEM, MARGEM);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(nomeArquivo));
            doc.open();

            // ── CABEÇALHO DA EMPRESA ──────────────────────────────────────────
            if (empresa != null) {
                String nome = nvl(empresa.getNomeFantasia(), nvl(empresa.getRazaoSocial(), ""));
                if (!nome.isBlank()) {
                    Paragraph pNome = new Paragraph(nome.toUpperCase(), F_TITULO);
                    pNome.setAlignment(Element.ALIGN_CENTER);
                    doc.add(pNome);
                }
                if (!nvl(empresa.getCnpj(), "").isBlank()) {
                    doc.add(centrado("CNPJ: " + empresa.getCnpj(), F_NORMAL));
                }
                StringBuilder end = new StringBuilder();
                if (!nvl(empresa.getLogradouro(), "").isBlank()) end.append(empresa.getLogradouro());
                if (!nvl(empresa.getNumero(), "").isBlank())     end.append(", ").append(empresa.getNumero());
                if (!nvl(empresa.getBairro(), "").isBlank())     end.append(" - ").append(empresa.getBairro());
                if (!nvl(empresa.getCidade(), "").isBlank())     end.append(" / ").append(empresa.getCidade());
                if (end.length() > 0) doc.add(centrado(end.toString(), F_NORMAL));
                if (!nvl(empresa.getTelefone(), "").isBlank())
                    doc.add(centrado("Tel: " + empresa.getTelefone(), F_NORMAL));
            }

            linhaHR(doc);
            Paragraph titulo = new Paragraph("RELATÓRIO DE FECHAMENTO DE CAIXA", F_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingBefore(4);
            doc.add(titulo);
            linhaHR(doc);

            // ── DADOS DA SESSÃO ───────────────────────────────────────────────
            PdfPTable tSessao = new PdfPTable(2);
            tSessao.setWidthPercentage(100);
            tSessao.setWidths(new float[]{1f, 1f});
            tSessao.setSpacingAfter(8);
            cellDado(tSessao, "Emissão:", LocalDateTime.now().format(FMT));
            cellDado(tSessao, "Operador:", nvl(sessao.getUsuarioNome(), "-"));
            cellDado(tSessao, "Abertura:", sessao.getAbertura() != null ? sessao.getAbertura().format(FMT) : "-");
            cellDado(tSessao, "Fechamento:", sessao.getFechamento() != null ? sessao.getFechamento().format(FMT) : "Em aberto");
            doc.add(tSessao);

            // ── RESUMO FINANCEIRO ─────────────────────────────────────────────
            tituloSecao(doc, "RESUMO FINANCEIRO");

            PdfPTable tResumo = new PdfPTable(2);
            tResumo.setWidthPercentage(100);
            tResumo.setWidths(new float[]{1.4f, 0.6f});
            tResumo.setSpacingAfter(8);

            headerTabela(tResumo, "Descrição", "Valor");
            rowTabela(tResumo, "Valor de Abertura",                    Formatador.formatarMoeda(sessao.getValorAbertura()), false);
            rowTabela(tResumo, "Total Vendas (" + sessao.getQtdVendas() + " vendas)", Formatador.formatarMoeda(sessao.getTotalVendas()), true);
            rowTabela(tResumo, "  ↳ Dinheiro",                        Formatador.formatarMoeda(sessao.getTotalDinheiro()), false);
            rowTabela(tResumo, "  ↳ PIX",                             Formatador.formatarMoeda(sessao.getTotalPix()), true);
            rowTabela(tResumo, "  ↳ Cartão Débito",                   Formatador.formatarMoeda(sessao.getTotalDebito()), false);
            rowTabela(tResumo, "  ↳ Cartão Crédito",                  Formatador.formatarMoeda(sessao.getTotalCredito()), true);
            rowTabela(tResumo, "Suprimentos",                          Formatador.formatarMoeda(sessao.getTotalSuprimentos()), false);
            rowTabela(tResumo, "Sangrias",                             "- " + Formatador.formatarMoeda(sessao.getTotalSangrias()), true);
            doc.add(tResumo);

            // Saldo esperado em destaque
            PdfPTable tSaldo = new PdfPTable(1);
            tSaldo.setWidthPercentage(100);
            tSaldo.setSpacingAfter(12);
            PdfPCell cSaldo = new PdfPCell(new Phrase("SALDO ESPERADO NO CAIXA: "
                    + Formatador.formatarMoeda(sessao.getSaldoEsperado()), F_TOTAL));
            cSaldo.setBackgroundColor(new BaseColor(230, 255, 235));
            cSaldo.setBorderColor(new BaseColor(20, 120, 50));
            cSaldo.setPadding(8);
            cSaldo.setHorizontalAlignment(Element.ALIGN_CENTER);
            tSaldo.addCell(cSaldo);
            doc.add(tSaldo);

            // ── VENDAS DETALHADAS ─────────────────────────────────────────────
            List<Venda> vendas = new VendaDAO().listarVendasDaSessao(sessao.getId());

            // Totalizadores calculados a partir das vendas
            int qtdItensTotal = 0;
            double totalDescontosGeral = 0;
            int qtdCanceladas = 0;

            List<Venda> vendasFinalizadas = new java.util.ArrayList<>();
            List<Venda> vendasCanceladas  = new java.util.ArrayList<>();
            for (Venda v : vendas) {
                if ("CANCELADA".equals(v.getStatus())) vendasCanceladas.add(v);
                else vendasFinalizadas.add(v);
            }

            if (!vendasFinalizadas.isEmpty()) {
                tituloSecao(doc, "VENDAS REALIZADAS (" + vendasFinalizadas.size() + ")");

                for (Venda v : vendasFinalizadas) {
                    totalDescontosGeral += v.getDesconto();
                    if (v.getItens() != null)
                        for (ItemVenda it : v.getItens()) qtdItensTotal += (int) Math.ceil(it.getQuantidade());

                    // Cabeçalho da venda
                    PdfPTable tVenda = new PdfPTable(4);
                    tVenda.setWidthPercentage(100);
                    tVenda.setWidths(new float[]{0.8f, 1.0f, 1.1f, 1.1f});
                    tVenda.setSpacingBefore(4);

                    PdfPCell cVendaHeader = new PdfPCell();
                    cVendaHeader.setColspan(4);
                    cVendaHeader.setBackgroundColor(new BaseColor(230, 235, 255));
                    cVendaHeader.setBorderColor(COR_HEADER_TB);
                    cVendaHeader.setPadding(4);
                    String clienteNome = nvl(v.getClienteNome(), "Consumidor Final");
                    String hora = v.getDataVenda() != null ? v.getDataVenda().format(FMT_HM) : "";
                    cVendaHeader.setPhrase(new Phrase(
                            "Venda #" + v.getNumero() + "  |  " + hora
                            + "  |  Cliente: " + clienteNome
                            + "  |  " + nvl(v.getFormaPagamento(), ""),
                            F_NEGRITO));
                    tVenda.addCell(cVendaHeader);

                    // Colunas dos itens
                    addHeaderCell(tVenda, "Produto");
                    addHeaderCell(tVenda, "Qtd");
                    addHeaderCell(tVenda, "Unit.");
                    addHeaderCell(tVenda, "Subtotal");

                    if (v.getItens() != null) {
                        boolean par = false;
                        for (ItemVenda item : v.getItens()) {
                            BaseColor bg = par ? COR_LINHA_PAR : BaseColor.WHITE;
                            addItemCell(tVenda, item.getProdutoNome(), bg, Element.ALIGN_LEFT);
                            addItemCell(tVenda, Formatador.formatarQuantidade(item.getQuantidade()), bg, Element.ALIGN_CENTER);
                            addItemCell(tVenda, Formatador.formatarMoeda(item.getPrecoUnit()), bg, Element.ALIGN_RIGHT);
                            addItemCell(tVenda, Formatador.formatarMoeda(item.getSubtotal()), bg, Element.ALIGN_RIGHT);
                            par = !par;
                        }
                    }

                    // Rodapé da venda
                    PdfPCell cVendaFoot = new PdfPCell();
                    cVendaFoot.setColspan(4);
                    cVendaFoot.setBackgroundColor(new BaseColor(245, 248, 255));
                    cVendaFoot.setPadding(4);
                    StringBuilder rodape = new StringBuilder();
                    rodape.append("Subtotal: ").append(Formatador.formatarMoeda(v.getSubtotal()));
                    if (v.getDesconto() > 0)
                        rodape.append("   Desconto: ").append(Formatador.formatarMoeda(v.getDesconto()));
                    if (v.getAcrescimo() > 0)
                        rodape.append("   Acréscimo: ").append(Formatador.formatarMoeda(v.getAcrescimo()));
                    rodape.append("   TOTAL: ").append(Formatador.formatarMoeda(v.getTotal()));
                    if (v.getValorPago() > 0)
                        rodape.append("   Recebido: ").append(Formatador.formatarMoeda(v.getValorPago()));
                    if ("DINHEIRO".equalsIgnoreCase(v.getFormaPagamento()) && v.getTroco() > 0)
                        rodape.append("   Troco: ").append(Formatador.formatarMoeda(v.getTroco()));
                    cVendaFoot.setPhrase(new Phrase(rodape.toString(), F_NEGRITO));
                    cVendaFoot.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    tVenda.addCell(cVendaFoot);

                    doc.add(tVenda);
                }

                // Totalizador geral das vendas
                PdfPTable tTotVendas = new PdfPTable(2);
                tTotVendas.setWidthPercentage(100);
                tTotVendas.setWidths(new float[]{1.4f, 0.6f});
                tTotVendas.setSpacingBefore(6);
                tTotVendas.setSpacingAfter(10);
                headerTabela(tTotVendas, "Totalizadores das Vendas", "");
                rowTabela(tTotVendas, "Total de Itens Vendidos (unid.)", String.valueOf(qtdItensTotal), false);
                rowTabela(tTotVendas, "Total de Descontos Concedidos",   Formatador.formatarMoeda(totalDescontosGeral), true);
                double totalRecebidoGeral = vendasFinalizadas.stream().mapToDouble(Venda::getValorPago).sum();
                double totalTrocoGeral    = vendasFinalizadas.stream().mapToDouble(Venda::getTroco).sum();
                rowTabela(tTotVendas, "Total Recebido (Valor Pago)",      Formatador.formatarMoeda(totalRecebidoGeral), false);
                rowTabela(tTotVendas, "Total de Troco",                   Formatador.formatarMoeda(totalTrocoGeral), true);
                rowTabela(tTotVendas, "Total Líquido Recebido",           Formatador.formatarMoeda(sessao.getTotalVendas()), false);
                doc.add(tTotVendas);
            }

            // ── VENDAS CANCELADAS ─────────────────────────────────────────────
            if (!vendasCanceladas.isEmpty()) {
                tituloSecao(doc, "VENDAS CANCELADAS (" + vendasCanceladas.size() + ")");

                PdfPTable tCanc = new PdfPTable(6);
                tCanc.setWidthPercentage(100);
                tCanc.setWidths(new float[]{0.7f, 0.6f, 1.1f, 0.8f, 0.8f, 2.0f});
                tCanc.setSpacingAfter(10);

                addHeaderCell(tCanc, "Nº Venda");
                addHeaderCell(tCanc, "Hora");
                addHeaderCell(tCanc, "Cliente");
                addHeaderCell(tCanc, "Pagamento");
                addHeaderCell(tCanc, "Total");
                addHeaderCell(tCanc, "Motivo");

                boolean par = false;
                for (Venda v : vendasCanceladas) {
                    BaseColor bg = COR_CANCEL_BG;
                    String hora = v.getDataVenda() != null ? v.getDataVenda().format(FMT_HM) : "-";
                    String motivo = (v.getObservacoes() != null && !v.getObservacoes().isBlank())
                            ? v.getObservacoes() : "—";
                    addCancelCell(tCanc, "#" + v.getNumero(), bg);
                    addCancelCell(tCanc, hora, bg);
                    addCancelCell(tCanc, nvl(v.getClienteNome(), "Consumidor Final"), bg);
                    addCancelCell(tCanc, nvl(v.getFormaPagamento(), "-"), bg);
                    addCancelCell(tCanc, Formatador.formatarMoeda(v.getTotal()), bg);
                    addCancelCell(tCanc, motivo, bg);
                }
                doc.add(tCanc);
            }

            // ── MOVIMENTAÇÕES DO CAIXA ────────────────────────────────────────
            List<MovimentoCaixa> movimentos = new CaixaDAO().listarMovimentos(sessao.getId());
            // Filtra somente sangrias e suprimentos (vendas já estão acima)
            List<MovimentoCaixa> movsFiltrados = new java.util.ArrayList<>();
            for (MovimentoCaixa m : movimentos) {
                String t = nvl(m.getTipo(), "");
                if (t.equals("SANGRIA") || t.equals("SUPRIMENTO") || t.equals("ABERTURA"))
                    movsFiltrados.add(m);
            }
            if (!movsFiltrados.isEmpty()) {
                tituloSecao(doc, "MOVIMENTAÇÕES DO CAIXA");
                PdfPTable tMov = new PdfPTable(4);
                tMov.setWidthPercentage(100);
                tMov.setWidths(new float[]{0.5f, 0.8f, 1.5f, 0.7f});
                tMov.setSpacingAfter(10);
                addHeaderCell(tMov, "Hora");
                addHeaderCell(tMov, "Tipo");
                addHeaderCell(tMov, "Descrição");
                addHeaderCell(tMov, "Valor");
                boolean par = false;
                for (MovimentoCaixa m : movsFiltrados) {
                    BaseColor bg = par ? COR_LINHA_PAR : BaseColor.WHITE;
                    String hora = m.getDataHora() != null ? m.getDataHora().format(FMT_HM) : "--:--";
                    addItemCell(tMov, hora, bg, Element.ALIGN_CENTER);
                    addItemCell(tMov, nvl(m.getTipo(), ""), bg, Element.ALIGN_CENTER);
                    addItemCell(tMov, nvl(m.getDescricao(), ""), bg, Element.ALIGN_LEFT);
                    addItemCell(tMov, Formatador.formatarMoeda(m.getValor()), bg, Element.ALIGN_RIGHT);
                    par = !par;
                }
                doc.add(tMov);
            }

            // ── RODAPÉ ────────────────────────────────────────────────────────
            linhaHR(doc);
            Paragraph rodape = new Paragraph("Relatório gerado em " + LocalDateTime.now().format(FMT)
                    + "  |  DRS ERP", F_NORMAL);
            rodape.setAlignment(Element.ALIGN_CENTER);
            doc.add(rodape);

            doc.close();
            return nomeArquivo;

        } catch (Exception e) {
            log.error("Erro ao gerar relatório PDF do caixa", e);
            return null;
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private static void tituloSecao(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto, F_SECAO);
        p.setSpacingBefore(8);
        p.setSpacingAfter(3);
        doc.add(p);
        PdfPTable linha = new PdfPTable(1);
        linha.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(2);
        c.setBackgroundColor(COR_HEADER_TB);
        c.setBorder(Rectangle.NO_BORDER);
        linha.addCell(c);
        doc.add(linha);
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

    private static void headerTabela(PdfPTable t, String col1, String col2) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, F_HEADER_TB));
        c1.setBackgroundColor(COR_HEADER_TB);
        c1.setPadding(5);
        c1.setBorderColor(BaseColor.WHITE);
        PdfPCell c2 = new PdfPCell(new Phrase(col2, F_HEADER_TB));
        c2.setBackgroundColor(COR_HEADER_TB);
        c2.setPadding(5);
        c2.setBorderColor(BaseColor.WHITE);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1);
        t.addCell(c2);
    }

    private static void rowTabela(PdfPTable t, String label, String valor, boolean par) {
        BaseColor bg = par ? COR_LINHA_PAR : BaseColor.WHITE;
        PdfPCell c1 = new PdfPCell(new Phrase(label, F_NORMAL));
        c1.setBackgroundColor(bg); c1.setPadding(4); c1.setBorderColor(BaseColor.LIGHT_GRAY);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, F_NEGRITO));
        c2.setBackgroundColor(bg); c2.setPadding(4); c2.setBorderColor(BaseColor.LIGHT_GRAY);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1);
        t.addCell(c2);
    }

    private static void cellDado(PdfPTable t, String label, String valor) {
        PdfPCell c = new PdfPCell();
        c.setPadding(3);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPhrase(new Phrase(label + " " + valor, F_NORMAL));
        t.addCell(c);
    }

    private static void addHeaderCell(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_HEADER_TB));
        c.setBackgroundColor(COR_HEADER_TB);
        c.setPadding(4);
        c.setBorderColor(BaseColor.WHITE);
        t.addCell(c);
    }

    private static void addItemCell(PdfPTable t, String texto, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_ITEM_TB));
        c.setBackgroundColor(bg);
        c.setPadding(3);
        c.setBorderColor(BaseColor.LIGHT_GRAY);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private static void addCancelCell(PdfPTable t, String texto, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_CANCEL));
        c.setBackgroundColor(bg);
        c.setPadding(3);
        c.setBorderColor(new BaseColor(220, 150, 150));
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