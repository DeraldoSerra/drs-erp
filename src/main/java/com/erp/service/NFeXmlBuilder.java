package com.erp.service;

import com.erp.model.Cliente;
import com.erp.model.ItemVenda;
import com.erp.model.NFeConfig;
import com.erp.model.Venda;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Constrói o XML de NF-e 4.0 como String, sem dependências externas além do Java padrão.
 */
public class NFeXmlBuilder {

    /**
     * Constrói o XML da NF-e sem assinatura.
     *
     * @return array onde [0]=xmlString, [1]=chaveAcesso
     */
    public String[] buildXml(NFeConfig config, Venda venda, List<ItemVenda> itens, Cliente cliente, int numero) {
        String cnpj = config.getCnpjNumerico();
        String cuf = "29"; // BA
        String aamm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String mod = "55";
        String serie = String.format("%03d", config.getSerie());
        String nNF = String.format("%09d", numero);
        String tpEmis = "1";
        String cNF = gerarCNF();
        String chave43 = cuf + aamm + cnpj + mod + serie + nNF + tpEmis + cNF;
        String cDV = calcularCDV(chave43);
        String chaveAcesso = chave43 + cDV;

        String tpAmb = config.isProducao() ? "1" : "2";
        LocalDateTime agora = LocalDateTime.now();
        String dhEmi = agora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "-03:00";

        String cfopPadrao = "5102"; // Saída interna - Simples Nacional

        double vProd = 0;
        for (ItemVenda item : itens) {
            vProd += item.getSubtotal();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
        sb.append("<infNFe Id=\"NFe").append(chaveAcesso).append("\" versao=\"4.00\">");

        // ===================== IDE =====================
        sb.append("<ide>");
        sb.append("<cUF>").append(cuf).append("</cUF>");
        sb.append("<cNF>").append(cNF).append("</cNF>");
        sb.append("<natOp>VENDA DE MERCADORIA</natOp>");
        sb.append("<mod>55</mod>");
        sb.append("<serie>").append(config.getSerie()).append("</serie>");
        sb.append("<nNF>").append(numero).append("</nNF>");
        sb.append("<dhEmi>").append(dhEmi).append("</dhEmi>");
        sb.append("<tpNF>1</tpNF>");
        sb.append("<idDest>1</idDest>");
        String codMun = nvl(config.getCodMunicipio(), "2927408");
        sb.append("<cMunFG>").append(codMun).append("</cMunFG>");
        sb.append("<tpImp>1</tpImp>");
        sb.append("<tpEmis>1</tpEmis>");
        sb.append("<cDV>").append(cDV).append("</cDV>");
        sb.append("<tpAmb>").append(tpAmb).append("</tpAmb>");
        sb.append("<finNFe>1</finNFe>");
        sb.append("<indFinal>1</indFinal>");
        sb.append("<indPres>1</indPres>");
        sb.append("<procEmi>0</procEmi>");
        sb.append("<verProc>1.0.0</verProc>");
        sb.append("</ide>");

        // ===================== EMITENTE =====================
        sb.append("<emit>");
        sb.append("<CNPJ>").append(cnpj).append("</CNPJ>");
        sb.append("<xNome>").append(escapeXml(config.getRazaoSocial())).append("</xNome>");
        if (config.getNomeFantasia() != null && !config.getNomeFantasia().isBlank()) {
            sb.append("<xFant>").append(escapeXml(config.getNomeFantasia())).append("</xFant>");
        }
        sb.append("<enderEmit>");
        sb.append("<xLgr>").append(escapeXml(nvl(config.getLogradouro(), "SEM ENDERECO"))).append("</xLgr>");
        sb.append("<nro>").append(escapeXml(nvl(config.getNumeroEnd(), "S/N"))).append("</nro>");
        sb.append("<xBairro>").append(escapeXml(nvl(config.getBairro(), "SEM BAIRRO"))).append("</xBairro>");
        sb.append("<cMun>").append(codMun).append("</cMun>");
        sb.append("<xMun>").append(escapeXml(nvl(config.getMunicipio(), "Salvador"))).append("</xMun>");
        sb.append("<UF>").append(nvl(config.getUf(), "BA")).append("</UF>");
        sb.append("<CEP>").append(nvl(config.getCepNumerico(), "00000000")).append("</CEP>");
        sb.append("<cPais>1058</cPais>");
        sb.append("<xPais>BRASIL</xPais>");
        String fone = config.getTelefoneNumerico();
        if (fone != null && !fone.isBlank()) {
            sb.append("<fone>").append(fone).append("</fone>");
        }
        sb.append("</enderEmit>");
        sb.append("<CRT>").append(config.getRegimeTributario()).append("</CRT>");
        sb.append("</emit>");

        // ===================== DESTINATÁRIO =====================
        sb.append("<dest>");
        boolean destPJ = cliente != null && "J".equals(cliente.getTipoPessoa());
        String destDoc = cliente != null && cliente.getCpfCnpj() != null
                ? cliente.getCpfCnpj().replaceAll("[^0-9]", "") : "";

        if (!destDoc.isBlank()) {
            if (destPJ) {
                sb.append("<CNPJ>").append(destDoc).append("</CNPJ>");
            } else {
                sb.append("<CPF>").append(destDoc).append("</CPF>");
            }
        }
        String destNome = cliente != null ? nvl(cliente.getNome(), "CONSUMIDOR FINAL") : "CONSUMIDOR FINAL";
        sb.append("<xNome>").append(escapeXml(destNome)).append("</xNome>");

        if (cliente != null && cliente.getLogradouro() != null && !cliente.getLogradouro().isBlank()) {
            sb.append("<enderDest>");
            sb.append("<xLgr>").append(escapeXml(cliente.getLogradouro())).append("</xLgr>");
            sb.append("<nro>").append(escapeXml(nvl(cliente.getNumero(), "S/N"))).append("</nro>");
            sb.append("<xBairro>").append(escapeXml(nvl(cliente.getBairro(), "SEM BAIRRO"))).append("</xBairro>");
            sb.append("<cMun>").append(codMun).append("</cMun>");
            sb.append("<xMun>").append(escapeXml(nvl(cliente.getCidade(), "Salvador"))).append("</xMun>");
            sb.append("<UF>").append(nvl(cliente.getEstado(), "BA")).append("</UF>");
            String cepDest = cliente.getCep() != null ? cliente.getCep().replaceAll("[^0-9]", "") : "";
            if (!cepDest.isBlank()) sb.append("<CEP>").append(cepDest).append("</CEP>");
            sb.append("<cPais>1058</cPais>");
            sb.append("<xPais>BRASIL</xPais>");
            sb.append("</enderDest>");
        }
        sb.append("<indIEDest>9</indIEDest>");
        sb.append("</dest>");

        // ===================== ITENS =====================
        int nItem = 0;
        for (ItemVenda item : itens) {
            nItem++;
            sb.append("<det nItem=\"").append(nItem).append("\">");
            sb.append("<prod>");
            sb.append("<cProd>").append(escapeXml(nvl(item.getProdutoCodigo(), String.valueOf(item.getProdutoId())))).append("</cProd>");
            sb.append("<cEAN>SEM GTIN</cEAN>");
            sb.append("<xProd>").append(escapeXml(item.getProdutoNome())).append("</xProd>");
            sb.append("<NCM>00000000</NCM>");
            sb.append("<CFOP>").append(cfopPadrao).append("</CFOP>");
            sb.append("<uCom>UN</uCom>");
            sb.append("<qCom>").append(fmt4dec(item.getQuantidade())).append("</qCom>");
            sb.append("<vUnCom>").append(fmt10dec(item.getPrecoUnit())).append("</vUnCom>");
            sb.append("<vProd>").append(fmt2dec(item.getSubtotal())).append("</vProd>");
            sb.append("<cEANTrib>SEM GTIN</cEANTrib>");
            sb.append("<uTrib>UN</uTrib>");
            sb.append("<qTrib>").append(fmt4dec(item.getQuantidade())).append("</qTrib>");
            sb.append("<vUnTrib>").append(fmt10dec(item.getPrecoUnit())).append("</vUnTrib>");
            if (item.getDesconto() > 0) {
                sb.append("<vDesc>").append(fmt2dec(item.getDesconto())).append("</vDesc>");
            }
            sb.append("<indTot>1</indTot>");
            sb.append("</prod>");

            // ICMS Simples Nacional CSOSN 400
            sb.append("<imposto>");
            sb.append("<vTotTrib>0.00</vTotTrib>");
            sb.append("<ICMS><ICMSSN400><orig>0</orig><CSOSN>400</CSOSN></ICMSSN400></ICMS>");
            sb.append("<PIS><PISNT><CST>07</CST></PISNT></PIS>");
            sb.append("<COFINS><COFINSNT><CST>07</CST></COFINSNT></COFINS>");
            sb.append("</imposto>");
            sb.append("</det>");
        }

        // ===================== TOTAL =====================
        sb.append("<total><ICMSTot>");
        sb.append("<vBC>0.00</vBC>");
        sb.append("<vICMS>0.00</vICMS>");
        sb.append("<vICMSDeson>0.00</vICMSDeson>");
        sb.append("<vFCPUFDest>0.00</vFCPUFDest>");
        sb.append("<vICMSUFDest>0.00</vICMSUFDest>");
        sb.append("<vICMSUFRemet>0.00</vICMSUFRemet>");
        sb.append("<vFCP>0.00</vFCP>");
        sb.append("<vBCST>0.00</vBCST>");
        sb.append("<vST>0.00</vST>");
        sb.append("<vFCPST>0.00</vFCPST>");
        sb.append("<vFCPSTRet>0.00</vFCPSTRet>");
        sb.append("<vProd>").append(fmt2dec(vProd)).append("</vProd>");
        sb.append("<vFrete>0.00</vFrete>");
        sb.append("<vSeg>0.00</vSeg>");
        sb.append("<vDesc>").append(fmt2dec(venda.getDesconto())).append("</vDesc>");
        sb.append("<vII>0.00</vII>");
        sb.append("<vIPI>0.00</vIPI>");
        sb.append("<vIPIDevol>0.00</vIPIDevol>");
        sb.append("<vPIS>0.00</vPIS>");
        sb.append("<vCOFINS>0.00</vCOFINS>");
        sb.append("<vOutro>0.00</vOutro>");
        sb.append("<vNF>").append(fmt2dec(venda.getTotal())).append("</vNF>");
        sb.append("<vTotTrib>0.00</vTotTrib>");
        sb.append("</ICMSTot></total>");

        // ===================== TRANSPORTE =====================
        sb.append("<transp><modFrete>9</modFrete></transp>");

        // ===================== PAGAMENTO =====================
        sb.append("<pag><detPag>");
        sb.append("<tPag>").append(mapearTipoPagamento(venda.getFormaPagamento())).append("</tPag>");
        sb.append("<vPag>").append(fmt2dec(venda.getTotal())).append("</vPag>");
        sb.append("</detPag></pag>");

        // ===================== INFO ADICIONAIS =====================
        sb.append("<infAdic>");
        if (!config.isProducao()) {
            sb.append("<infCpl>NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL</infCpl>");
        }
        sb.append("</infAdic>");

        sb.append("</infNFe></NFe>");

        return new String[]{sb.toString(), chaveAcesso};
    }

    private String gerarCNF() {
        return String.format("%08d", new Random().nextInt(100000000));
    }

    /** Dígito verificador módulo 11 sobre 43 dígitos da chave */
    public static String calcularCDV(String chave43) {
        int[] pesos = {2, 3, 4, 5, 6, 7, 8, 9};
        int soma = 0;
        int pi = 0;
        for (int i = chave43.length() - 1; i >= 0; i--) {
            soma += Character.getNumericValue(chave43.charAt(i)) * pesos[pi % 8];
            pi++;
        }
        int resto = soma % 11;
        return String.valueOf(resto < 2 ? 0 : 11 - resto);
    }

    private String mapearTipoPagamento(String forma) {
        if (forma == null) return "99";
        return switch (forma.toUpperCase()) {
            case "DINHEIRO"                      -> "01";
            case "CHEQUE"                        -> "02";
            case "CARTAO_CREDITO", "CREDITO"     -> "03";
            case "CARTAO_DEBITO", "DEBITO"       -> "04";
            case "PIX"                           -> "17";
            case "BOLETO"                        -> "15";
            default                              -> "99";
        };
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String nvl(String val, String def) {
        return (val == null || val.isBlank()) ? def : val;
    }

    private String fmt2dec(double v) {
        return String.format("%.2f", v).replace(",", ".");
    }

    private String fmt4dec(double v) {
        return String.format("%.4f", v).replace(",", ".");
    }

    private String fmt10dec(double v) {
        return String.format("%.10f", v).replace(",", ".");
    }
}
