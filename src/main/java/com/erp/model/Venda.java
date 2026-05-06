package com.erp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venda {
    private int id;
    private String numero;
    private int clienteId;
    private String clienteNome;
    private int usuarioId;
    private String usuarioNome;
    private LocalDateTime dataVenda;
    private double subtotal;
    private double desconto;
    private double acrescimo;
    private double total;
    private String formaPagamento;
    private double valorPago;
    private double troco;
    private String status; // RASCUNHO, FINALIZADA, CANCELADA
    private String observacoes;
    private boolean reembolsado;
    private List<ItemVenda> itens;

    public Venda() {
        this.itens = new ArrayList<>();
        this.status = "RASCUNHO";
        this.formaPagamento = "DINHEIRO";
        this.dataVenda = LocalDateTime.now();
    }

    public void adicionarItem(ItemVenda item) {
        this.itens.add(item);
        recalcular();
    }

    public void removerItem(ItemVenda item) {
        this.itens.remove(item);
        recalcular();
    }

    public void recalcular() {
        this.subtotal = itens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        this.total = subtotal - desconto + acrescimo;
        this.troco = valorPago > total ? valorPago - total : 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }

    public LocalDateTime getDataVenda() { return dataVenda; }
    public void setDataVenda(LocalDateTime dataVenda) { this.dataVenda = dataVenda; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; recalcular(); }

    public double getAcrescimo() { return acrescimo; }
    public void setAcrescimo(double acrescimo) { this.acrescimo = acrescimo; recalcular(); }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public double getValorPago() { return valorPago; }
    public void setValorPago(double valorPago) { this.valorPago = valorPago; recalcular(); }

    public double getTroco() { return troco; }
    public void setTroco(double troco) { this.troco = troco; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public boolean isReembolsado() { return reembolsado; }
    public void setReembolsado(boolean reembolsado) { this.reembolsado = reembolsado; }

    public List<ItemVenda> getItens() { return itens; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; recalcular(); }
}
