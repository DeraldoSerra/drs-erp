package com.erp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * Envia mensagens SOAP para os webservices NF-e do SEFAZ BA (Produção).
 * Usa exclusivamente javax.net.ssl do Java padrão.
 */
public class NFeWebService {

    private static final Logger log = LoggerFactory.getLogger(NFeWebService.class);

    // URLs produção BA
    public static final String URL_STATUS_PRODUCAO =
            "https://nfe.sefaz.ba.gov.br/webservices/NFeStatusServico4/NFeStatusServico4.asmx";
    public static final String URL_AUTORIZACAO_PRODUCAO =
            "https://nfe.sefaz.ba.gov.br/webservices/NFeAutorizacao4/NFeAutorizacao4.asmx";
    public static final String URL_RET_AUTORIZACAO_PRODUCAO =
            "https://nfe.sefaz.ba.gov.br/webservices/NFeRetAutorizacao4/NFeRetAutorizacao4.asmx";
    public static final String URL_EVENTO_PRODUCAO =
            "https://nfe.sefaz.ba.gov.br/webservices/NFeRecepcaoEvento4/NFeRecepcaoEvento4.asmx";
    public static final String URL_CONSULTA_PRODUCAO =
            "https://nfe.sefaz.ba.gov.br/webservices/NFeConsultaProtocolo4/NFeConsultaProtocolo4.asmx";

    // URLs homologação BA
    public static final String URL_STATUS_HOMOLOGACAO =
            "https://hnfe.sefaz.ba.gov.br/webservices/NFeStatusServico4/NFeStatusServico4.asmx";
    public static final String URL_AUTORIZACAO_HOMOLOGACAO =
            "https://hnfe.sefaz.ba.gov.br/webservices/NFeAutorizacao4/NFeAutorizacao4.asmx";
    public static final String URL_RET_AUTORIZACAO_HOMOLOGACAO =
            "https://hnfe.sefaz.ba.gov.br/webservices/NFeRetAutorizacao4/NFeRetAutorizacao4.asmx";
    public static final String URL_EVENTO_HOMOLOGACAO =
            "https://hnfe.sefaz.ba.gov.br/webservices/NFeRecepcaoEvento4/NFeRecepcaoEvento4.asmx";
    public static final String URL_CONSULTA_HOMOLOGACAO =
            "https://hnfe.sefaz.ba.gov.br/webservices/NFeConsultaProtocolo4/NFeConsultaProtocolo4.asmx";

    private final String pfxPath;
    private final String pfxSenha;
    private final boolean producao;

    public NFeWebService(String pfxPath, String pfxSenha, boolean producao) {
        this.pfxPath = pfxPath;
        this.pfxSenha = pfxSenha;
        this.producao = producao;
    }

    /** Consulta status do serviço SEFAZ */
    public String consultarStatus() throws Exception {
        String url = producao ? URL_STATUS_PRODUCAO : URL_STATUS_HOMOLOGACAO;
        String tpAmb = producao ? "1" : "2";
        String soap = buildSoapConsultaStatus(tpAmb);
        return enviarSoap(url, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeStatusServico4/nfeStatusServicoNF", soap);
    }

    /** Envia NF-e para autorização */
    public String autorizarNFe(String xmlNFeAssinado) throws Exception {
        String url = producao ? URL_AUTORIZACAO_PRODUCAO : URL_AUTORIZACAO_HOMOLOGACAO;
        String tpAmb = producao ? "1" : "2";
        String soap = buildSoapAutorizacao(xmlNFeAssinado, tpAmb);
        return enviarSoap(url, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4/nfeAutorizacaoLote", soap);
    }

    /** Consulta resultado de lote assíncrono */
    public String consultarRecibo(String nRec) throws Exception {
        String url = producao ? URL_RET_AUTORIZACAO_PRODUCAO : URL_RET_AUTORIZACAO_HOMOLOGACAO;
        String tpAmb = producao ? "1" : "2";
        String soap = buildSoapConsultaRecibo(nRec, tpAmb);
        return enviarSoap(url, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRetAutorizacao4/nfeRetAutorizacaoLote", soap);
    }

    /** Envia evento (cancelamento, etc.) */
    public String enviarEvento(String xmlEvento) throws Exception {
        String url = producao ? URL_EVENTO_PRODUCAO : URL_EVENTO_HOMOLOGACAO;
        String soap = buildSoapEvento(xmlEvento);
        return enviarSoap(url, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4/nfeRecepcaoEvento", soap);
    }

    /** Consulta situação de uma NF-e pela chave */
    public String consultarChave(String chaveAcesso) throws Exception {
        String url = producao ? URL_CONSULTA_PRODUCAO : URL_CONSULTA_HOMOLOGACAO;
        String tpAmb = producao ? "1" : "2";
        String soap = buildSoapConsultaChave(chaveAcesso, tpAmb);
        return enviarSoap(url, "http://www.portalfiscal.inf.br/nfe/wsdl/NFeConsultaProtocolo4/nfeConsultaNF", soap);
    }

    // ======================== SOAP BUILDERS ========================

    private String buildSoapConsultaStatus(String tpAmb) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soap12:Envelope xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
              <soap12:Body>
                <nfeStatusServicoNF xmlns="http://www.portalfiscal.inf.br/nfe/wsdl/NFeStatusServico4">
                  <nfeDadosMsg>
                    <consStatServ xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
                      <tpAmb>""" + tpAmb + """
</tpAmb>
                      <cUF>29</cUF>
                      <xServ>STATUS</xServ>
                    </consStatServ>
                  </nfeDadosMsg>
                </nfeStatusServicoNF>
              </soap12:Body>
            </soap12:Envelope>
            """;
    }

    private String buildSoapAutorizacao(String xmlNFe, String tpAmb) {
        // Remove o cabeçalho XML se presente, pois estará dentro do SOAP
        String nfeContent = xmlNFe.replaceFirst("<\\?xml[^>]*\\?>", "").trim();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap12:Body>"
                + "<nfeAutorizacaoLote xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeAutorizacao4\">"
                + "<nfeDadosMsg>"
                + "<enviNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<idLote>" + System.currentTimeMillis() + "</idLote>"
                + "<indSinc>1</indSinc>"
                + nfeContent
                + "</enviNFe>"
                + "</nfeDadosMsg>"
                + "</nfeAutorizacaoLote>"
                + "</soap12:Body>"
                + "</soap12:Envelope>";
    }

    private String buildSoapConsultaRecibo(String nRec, String tpAmb) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap12:Body>"
                + "<nfeRetAutorizacaoLote xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeRetAutorizacao4\">"
                + "<nfeDadosMsg>"
                + "<consReciNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + tpAmb + "</tpAmb>"
                + "<nRec>" + nRec + "</nRec>"
                + "</consReciNFe>"
                + "</nfeDadosMsg>"
                + "</nfeRetAutorizacaoLote>"
                + "</soap12:Body>"
                + "</soap12:Envelope>";
    }

    private String buildSoapEvento(String xmlEvento) {
        String evtContent = xmlEvento.replaceFirst("<\\?xml[^>]*\\?>", "").trim();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap12:Body>"
                + "<nfeRecepcaoEvento xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeRecepcaoEvento4\">"
                + "<nfeDadosMsg>"
                + evtContent
                + "</nfeDadosMsg>"
                + "</nfeRecepcaoEvento>"
                + "</soap12:Body>"
                + "</soap12:Envelope>";
    }

    private String buildSoapConsultaChave(String chave, String tpAmb) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap12:Body>"
                + "<nfeConsultaNF xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/NFeConsultaProtocolo4\">"
                + "<nfeDadosMsg>"
                + "<consSitNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"
                + "<tpAmb>" + tpAmb + "</tpAmb>"
                + "<xServ>CONSULTAR</xServ>"
                + "<chNFe>" + chave + "</chNFe>"
                + "</consSitNFe>"
                + "</nfeDadosMsg>"
                + "</nfeConsultaNF>"
                + "</soap12:Body>"
                + "</soap12:Envelope>";
    }

    // ======================== HTTP SOAP ========================

    private String enviarSoap(String urlStr, String soapAction, String soapBody) throws Exception {
        SSLContext sslCtx = criarSslContext();
        HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
        conn.setSSLSocketFactory(sslCtx.getSocketFactory());
        conn.setHostnameVerifier((h, s) -> true); // aceita qualquer hostname (produção SEFAZ usa wildcard)
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + soapAction + "\"");

        byte[] body = soapBody.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();

        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) resp.append(line);
            return resp.toString();
        }
    }

    /** Cria SSLContext com o certificado do cliente para mTLS */
    private SSLContext criarSslContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var fis = new java.io.FileInputStream(pfxPath)) {
            ks.load(fis, pfxSenha.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pfxSenha.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null); // usa truststore padrão do JDK

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return ctx;
    }
}
