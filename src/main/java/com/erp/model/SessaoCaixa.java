package com.erp.model;

import java.time.LocalDateTime;

public class SessaoCaixa {
    private int id;
    private int caixaId;
    private int lojaId;
    private int usuarioId;
    private String usuarioNome;
    private LocalDateTime abertura;
    private LocalDateTime fechamento;
    private double valorAbertura;
    private double valorFechamento;
    private double totalDinheiro;
    private double totalPix;
    private double totalDebito;
    private double totalCredito;
    private double totalVendas;
    private int qtdVendas;
    private double totalSangrias;
    private double totalSuprimentos;
    private String status;
    private String observacoes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCaixaId() { return caixaId; }
    public void setCaixaId(int caixaId) { this.caixaId = caixaId; }

    public int getLojaId() { return lojaId; }
    public void setLojaId(int lojaId) { this.lojaId = lojaId; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }

    public LocalDateTime getAbertura() { return abertura; }
    public void setAbertura(LocalDateTime abertura) { this.abertura = abertura; }

    public LocalDateTime getFechamento() { return fechamento; }
    public void setFechamento(LocalDateTime fechamento) { this.fechamento = fechamento; }

    public double getValorAbertura() { return valorAbertura; }
    public void setValorAbertura(double valorAbertura) { this.valorAbertura = valorAbertura; }

    public double getValorFechamento() { return valorFechamento; }
    public void setValorFechamento(double valorFechamento) { this.valorFechamento = valorFechamento; }

    public double getTotalDinheiro() { return totalDinheiro; }
    public void setTotalDinheiro(double totalDinheiro) { this.totalDinheiro = totalDinheiro; }

    public double getTotalPix() { return totalPix; }
    public void setTotalPix(double totalPix) { this.totalPix = totalPix; }

    public double getTotalDebito() { return totalDebito; }
    public void setTotalDebito(double totalDebito) { this.totalDebito = totalDebito; }

    public double getTotalCredito() { return totalCredito; }
    public void setTotalCredito(double totalCredito) { this.totalCredito = totalCredito; }

    public double getTotalVendas() { return totalVendas; }
    public void setTotalVendas(double totalVendas) { this.totalVendas = totalVendas; }

    public int getQtdVendas() { return qtdVendas; }
    public void setQtdVendas(int qtdVendas) { this.qtdVendas = qtdVendas; }

    public double getTotalSangrias() { return totalSangrias; }
    public void setTotalSangrias(double totalSangrias) { this.totalSangrias = totalSangrias; }

    public double getTotalSuprimentos() { return totalSuprimentos; }
    public void setTotalSuprimentos(double totalSuprimentos) { this.totalSuprimentos = totalSuprimentos; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    /** Saldo esperado = abertura + suprimentos - sangrias + dinheiro em vendas */
    public double getSaldoEsperado() {
        return valorAbertura + totalSuprimentos - totalSangrias + totalDinheiro;
    }
}
