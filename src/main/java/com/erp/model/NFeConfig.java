package com.erp.model;

public class NFeConfig {

    private int id = 1;
    private String certificadoPath;
    private String certificadoSenha;
    private String cnpj;
    private String ie;
    private String razaoSocial;
    private String nomeFantasia;
    private String logradouro;
    private String numeroEnd;
    private String bairro;
    private String codMunicipio;
    private String municipio;
    private String uf = "BA";
    private String cep;
    private String telefone;
    private int regimeTributario = 1; // 1=SN, 2=SN-Excesso, 3=Normal
    private int serie = 1;
    private int proximoNumero = 1;
    private String ambiente = "PRODUCAO"; // PRODUCAO ou HOMOLOGACAO

    public NFeConfig() {}

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCertificadoPath() { return certificadoPath; }
    public void setCertificadoPath(String certificadoPath) { this.certificadoPath = certificadoPath; }

    public String getCertificadoSenha() { return certificadoSenha; }
    public void setCertificadoSenha(String certificadoSenha) { this.certificadoSenha = certificadoSenha; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getIe() { return ie; }
    public void setIe(String ie) { this.ie = ie; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumeroEnd() { return numeroEnd; }
    public void setNumeroEnd(String numeroEnd) { this.numeroEnd = numeroEnd; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCodMunicipio() { return codMunicipio; }
    public void setCodMunicipio(String codMunicipio) { this.codMunicipio = codMunicipio; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public int getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(int regimeTributario) { this.regimeTributario = regimeTributario; }

    public int getSerie() { return serie; }
    public void setSerie(int serie) { this.serie = serie; }

    public int getProximoNumero() { return proximoNumero; }
    public void setProximoNumero(int proximoNumero) { this.proximoNumero = proximoNumero; }

    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }

    public boolean isProducao() { return "PRODUCAO".equals(ambiente); }

    /** Retorna o CNPJ somente dígitos */
    public String getCnpjNumerico() {
        return cnpj == null ? "" : cnpj.replaceAll("[^0-9]", "");
    }

    /** Retorna o CEP somente dígitos */
    public String getCepNumerico() {
        return cep == null ? "" : cep.replaceAll("[^0-9]", "");
    }

    /** Retorna telefone somente dígitos */
    public String getTelefoneNumerico() {
        return telefone == null ? "" : telefone.replaceAll("[^0-9]", "");
    }

    /** Verifica se a configuração está minimamente preenchida para emissão */
    public boolean isConfigurado() {
        return certificadoPath != null && !certificadoPath.isBlank()
            && certificadoSenha != null && !certificadoSenha.isBlank()
            && cnpj != null && !cnpj.isBlank()
            && razaoSocial != null && !razaoSocial.isBlank();
    }
}
