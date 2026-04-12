package com.erp.model;

import java.time.LocalDateTime;

public class MovimentoCaixa {
    private int id;
    private int sessaoId;
    private String tipo;
    private double valor;
    private String descricao;
    private int usuarioId;
    private String usuarioNome;
    private LocalDateTime dataHora;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessaoId() { return sessaoId; }
    public void setSessaoId(int sessaoId) { this.sessaoId = sessaoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
