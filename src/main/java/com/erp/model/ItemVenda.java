package com.erp.model;

public class ItemVenda {
    private int id;
    private int vendaId;
    private int produtoId;
    private String produtoCodigo;
    private String produtoNome;
    private double quantidade;
    private double precoUnit;
    private double desconto;
    private double subtotal;

    public ItemVenda() {}

    public ItemVenda(Produto produto, double quantidade) {
        this.produtoId    = produto.getId();
        this.produtoCodigo = produto.getCodigo();
        this.produtoNome  = produto.getNome();
        this.precoUnit    = produto.getPrecoVenda();
        this.quantidade   = quantidade;
        calcularSubtotal();
    }

    public void calcularSubtotal() {
        this.subtotal = (precoUnit * quantidade) - desconto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVendaId() { return vendaId; }
    public void setVendaId(int vendaId) { this.vendaId = vendaId; }

    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }

    public String getProdutoCodigo() { return produtoCodigo; }
    public void setProdutoCodigo(String produtoCodigo) { this.produtoCodigo = produtoCodigo; }

    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

    public double getQuantidade() { return quantidade; }
    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
        calcularSubtotal();
    }

    public double getPrecoUnit() { return precoUnit; }
    public void setPrecoUnit(double precoUnit) {
        this.precoUnit = precoUnit;
        calcularSubtotal();
    }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) {
        this.desconto = desconto;
        calcularSubtotal();
    }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
