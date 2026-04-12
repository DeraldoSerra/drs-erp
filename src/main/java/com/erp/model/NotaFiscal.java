package com.erp.model;

import java.time.LocalDateTime;

public class NotaFiscal {

    private int id;
    private int vendaId;
    private String chaveAcesso;
    private int numero;
    private String serie;
    private String protocolo;
    private LocalDateTime dataEmissao;
    private LocalDateTime dataAutorizacao;
    private String xmlNfe;
    private String xmlProcNfe;
    private String status; // PENDENTE, AUTORIZADA, CANCELADA, DENEGADA, ERRO
    private String motivo;
    private String ambiente;

    // campos extras para exibição (JOIN)
    private String clienteNome;
    private double totalVenda;

    public NotaFiscal() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVendaId() { return vendaId; }
    public void setVendaId(int vendaId) { this.vendaId = vendaId; }

    public String getChaveAcesso() { return chaveAcesso; }
    public void setChaveAcesso(String chaveAcesso) { this.chaveAcesso = chaveAcesso; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }

    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }

    public LocalDateTime getDataAutorizacao() { return dataAutorizacao; }
    public void setDataAutorizacao(LocalDateTime dataAutorizacao) { this.dataAutorizacao = dataAutorizacao; }

    public String getXmlNfe() { return xmlNfe; }
    public void setXmlNfe(String xmlNfe) { this.xmlNfe = xmlNfe; }

    public String getXmlProcNfe() { return xmlProcNfe; }
    public void setXmlProcNfe(String xmlProcNfe) { this.xmlProcNfe = xmlProcNfe; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public double getTotalVenda() { return totalVenda; }
    public void setTotalVenda(double totalVenda) { this.totalVenda = totalVenda; }

    /** Retorna a chave formatada para exibição (blocos de 4) */
    public String getChaveFormatada() {
        if (chaveAcesso == null || chaveAcesso.length() != 44) return chaveAcesso;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 44; i += 4) {
            if (i > 0) sb.append(' ');
            sb.append(chaveAcesso, i, Math.min(i + 4, 44));
        }
        return sb.toString();
    }
}
