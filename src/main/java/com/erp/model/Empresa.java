package com.erp.model;

import java.time.LocalDateTime;

public class Empresa {

    private int id;
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String ie;          // Inscrição Estadual
    private String im;          // Inscrição Municipal
    private String regimeTributario; // SIMPLES_NACIONAL, LUCRO_PRESUMIDO, LUCRO_REAL, MEI
    private String email;
    private String telefone;
    private String celular;
    private String site;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String logoPath;    // caminho do arquivo de logo
    private String observacoes;
    private boolean habilitaNFe   = false;
    private String tipoEmissaoNFe = "SEFAZ"; // SEFAZ ou CONTINGENCIA
    private LocalDateTime atualizadoEm;

    public Empresa() {}

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getIe() { return ie; }
    public void setIe(String ie) { this.ie = ie; }

    public String getIm() { return im; }
    public void setIm(String im) { this.im = im; }

    public String getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(String regimeTributario) { this.regimeTributario = regimeTributario; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public boolean isHabilitaNFe() { return habilitaNFe; }
    public void setHabilitaNFe(boolean habilitaNFe) { this.habilitaNFe = habilitaNFe; }

    public String getTipoEmissaoNFe() { return tipoEmissaoNFe; }
    public void setTipoEmissaoNFe(String tipoEmissaoNFe) { this.tipoEmissaoNFe = tipoEmissaoNFe; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    /** Retorna endereço formatado para exibição */
    public String getEnderecoCompleto() {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null && !logradouro.isBlank()) {
            sb.append(logradouro);
            if (numero != null && !numero.isBlank()) sb.append(", ").append(numero);
            if (complemento != null && !complemento.isBlank()) sb.append(" - ").append(complemento);
            if (bairro != null && !bairro.isBlank()) sb.append(", ").append(bairro);
            if (cidade != null && !cidade.isBlank()) sb.append(" - ").append(cidade);
            if (estado != null && !estado.isBlank()) sb.append("/").append(estado);
            if (cep != null && !cep.isBlank()) sb.append(" CEP: ").append(cep);
        }
        return sb.toString();
    }

    /** Label amigável do regime tributário */
    public String getRegimeTributarioLabel() {
        if (regimeTributario == null) return "";
        return switch (regimeTributario) {
            case "SIMPLES_NACIONAL" -> "Simples Nacional";
            case "LUCRO_PRESUMIDO"  -> "Lucro Presumido";
            case "LUCRO_REAL"       -> "Lucro Real";
            case "MEI"              -> "MEI - Microempreendedor Individual";
            default -> regimeTributario;
        };
    }
}
