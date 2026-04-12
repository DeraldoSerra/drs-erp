package com.erp.service;

/**
 * Resultado de uma operação NF-e.
 */
public class NFeResultado {

    public enum StatusResultado {
        AUTORIZADA, PENDENTE, ERRO, CANCELADA
    }

    private StatusResultado status;
    private String chaveAcesso;
    private String protocolo;
    private String mensagem;
    private String xmlNfe;
    private String xmlProcNfe;
    private int notaFiscalId;

    public NFeResultado() {}

    public NFeResultado(StatusResultado status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }

    public static NFeResultado sucesso(String chave, String protocolo, String xmlProc) {
        NFeResultado r = new NFeResultado();
        r.status = StatusResultado.AUTORIZADA;
        r.chaveAcesso = chave;
        r.protocolo = protocolo;
        r.xmlProcNfe = xmlProc;
        r.mensagem = "NF-e autorizada com sucesso. Protocolo: " + protocolo;
        return r;
    }

    public static NFeResultado erro(String mensagem) {
        return new NFeResultado(StatusResultado.ERRO, mensagem);
    }

    public boolean isAutorizada() {
        return StatusResultado.AUTORIZADA.equals(status);
    }

    // Getters e Setters
    public StatusResultado getStatus() { return status; }
    public void setStatus(StatusResultado status) { this.status = status; }

    public String getChaveAcesso() { return chaveAcesso; }
    public void setChaveAcesso(String chaveAcesso) { this.chaveAcesso = chaveAcesso; }

    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getXmlNfe() { return xmlNfe; }
    public void setXmlNfe(String xmlNfe) { this.xmlNfe = xmlNfe; }

    public String getXmlProcNfe() { return xmlProcNfe; }
    public void setXmlProcNfe(String xmlProcNfe) { this.xmlProcNfe = xmlProcNfe; }

    public int getNotaFiscalId() { return notaFiscalId; }
    public void setNotaFiscalId(int notaFiscalId) { this.notaFiscalId = notaFiscalId; }
}
