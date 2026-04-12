package com.erp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Orcamento {
    private int id;
    private String numero;
    private int clienteId;
    private String clienteNome;
    private int usuarioId;
    private String usuarioNome;
    private LocalDateTime dataCriacao;
    private LocalDate validade;
    private double subtotal;
    private double desconto;
    private double total;
    private String observacoes;
    private String status; // ABERTO, APROVADO, EXPIRADO, CANCELADO
    private List<ItemOrcamento> itens;

    public Orcamento() {
        this.itens = new ArrayList<>();
        this.status = "ABERTO";
        this.dataCriacao = LocalDateTime.now();
    }

    public void adicionarItem(ItemOrcamento item) {
        itens.add(item);
        recalcular();
    }

    public void removerItem(ItemOrcamento item) {
        itens.remove(item);
        recalcular();
    }

    public void recalcular() {
        subtotal = itens.stream().mapToDouble(ItemOrcamento::getSubtotal).sum();
        total = subtotal - desconto;
    }

    public String getStatusDisplay() {
        if ("ABERTO".equals(status) && validade != null && LocalDate.now().isAfter(validade))
            return "EXPIRADO";
        return status;
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

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDate getValidade() { return validade; }
    public void setValidade(LocalDate validade) { this.validade = validade; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; recalcular(); }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ItemOrcamento> getItens() { return itens; }
    public void setItens(List<ItemOrcamento> itens) { this.itens = itens; recalcular(); }
}
