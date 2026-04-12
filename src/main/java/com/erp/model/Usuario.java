package com.erp.model;

import java.time.LocalDateTime;

public class Usuario {
    private int id;
    private String nome;
    private String login;
    private String senhaHash;
    private String perfil; // ADMIN, GERENTE, OPERADOR
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Usuario() {}

    public Usuario(int id, String nome, String login, String perfil, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.login = login;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public boolean isAdmin()    { return "ADMIN".equals(perfil); }
    public boolean isGerente()  { return "GERENTE".equals(perfil) || isAdmin(); }

    @Override
    public String toString() { return nome + " (" + login + ")"; }
}
