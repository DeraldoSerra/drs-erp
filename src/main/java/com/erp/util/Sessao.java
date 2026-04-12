package com.erp.util;

import com.erp.model.Usuario;

public class Sessao {
    private static Sessao instancia;
    private Usuario usuarioLogado;
    private int lojaId = 1;
    private String lojaNome = "Loja Principal";

    private Sessao() {}

    public static Sessao getInstance() {
        if (instancia == null) instancia = new Sessao();
        return instancia;
    }

    public void iniciar(Usuario usuario) { this.usuarioLogado = usuario; }
    public void encerrar()              { this.usuarioLogado = null; lojaId = 1; lojaNome = "Loja Principal"; }
    public boolean estaLogado()         { return usuarioLogado != null; }
    public Usuario getUsuario()         { return usuarioLogado; }

    public int getLojaId()              { return lojaId; }
    public void setLojaId(int lojaId)   { this.lojaId = lojaId; }

    public String getLojaNome()                 { return lojaNome; }
    public void setLojaNome(String lojaNome)    { this.lojaNome = lojaNome != null ? lojaNome : "Loja Principal"; }
}
