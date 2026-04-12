package com.erp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContaReceber {
    private int id;
    private String descricao;
    private int clienteId;
    private String clienteNome;
    private int vendaId;
    private double valor;
    private LocalDate dataEmissao;
    private LocalDate dataVencimento;
    private LocalDate dataRecebimento;
    private double valorRecebido;
    private String formaRecebimento;
    private String status; // ABERTA, RECEBIDA, VENCIDA, CANCELADA
    private String observacoes;
    private LocalDateTime criadoEm;

    public ContaReceber() {
        this.status = "ABERTA";
        this.dataEmissao = LocalDate.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public int getVendaId() { return vendaId; }
    public void setVendaId(int vendaId) { this.vendaId = vendaId; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public LocalDate getDataRecebimento() { return dataRecebimento; }
    public void setDataRecebimento(LocalDate dataRecebimento) { this.dataRecebimento = dataRecebimento; }

    public double getValorRecebido() { return valorRecebido; }
    public void setValorRecebido(double valorRecebido) { this.valorRecebido = valorRecebido; }

    public String getFormaRecebimento() { return formaRecebimento; }
    public void setFormaRecebimento(String formaRecebimento) { this.formaRecebimento = formaRecebimento; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public boolean isVencida() {
        return "ABERTA".equals(status) && dataVencimento != null && dataVencimento.isBefore(LocalDate.now());
    }
}
