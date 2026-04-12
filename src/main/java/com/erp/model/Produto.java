package com.erp.model;

import java.time.LocalDateTime;

public class Produto {
    private int id;
    private String codigo;
    private String codigoBarras;
    private String nome;
    private String descricao;
    private int categoriaId;
    private String categoriaNome;
    private int fornecedorId;
    private String fornecedorNome;
    private String unidade;
    private double precoCusto;
    private double precoVenda;
    private double margemLucro;
    private double estoqueAtual;
    private double estoqueMinimo;
    private double estoqueMaximo;
    private String ncm;
    private String cfop;
    private double icmsAliquota;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Produto() { this.unidade = "UN"; this.ativo = true; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }

    public int getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(int fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }

    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }

    public double getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(double precoCusto) { this.precoCusto = precoCusto; }

    public double getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(double precoVenda) { this.precoVenda = precoVenda; }

    public double getMargemLucro() { return margemLucro; }
    public void setMargemLucro(double margemLucro) { this.margemLucro = margemLucro; }

    public double getEstoqueAtual() { return estoqueAtual; }
    public void setEstoqueAtual(double estoqueAtual) { this.estoqueAtual = estoqueAtual; }

    public double getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(double estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    public double getEstoqueMaximo() { return estoqueMaximo; }
    public void setEstoqueMaximo(double estoqueMaximo) { this.estoqueMaximo = estoqueMaximo; }

    public String getNcm() { return ncm; }
    public void setNcm(String ncm) { this.ncm = ncm; }

    public String getCfop() { return cfop; }
    public void setCfop(String cfop) { this.cfop = cfop; }

    public double getIcmsAliquota() { return icmsAliquota; }
    public void setIcmsAliquota(double icmsAliquota) { this.icmsAliquota = icmsAliquota; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public boolean isEstoqueBaixo() { return estoqueAtual <= estoqueMinimo; }

    @Override
    public String toString() { return "[" + codigo + "] " + nome; }
}
