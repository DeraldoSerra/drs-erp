package com.erp.util;

/**
 * Unified validation utility — digit-based checks + wrappers for API-backed lookups.
 * All API calls must be executed in background threads by the caller.
 */
public class ValidadorUtil {

    public static class ResultadoValidacao {
        public final boolean valido;
        public final String  mensagem;
        public final Object  dadosRetorno;

        public ResultadoValidacao(boolean valido, String mensagem, Object dadosRetorno) {
            this.valido       = valido;
            this.mensagem     = mensagem;
            this.dadosRetorno = dadosRetorno;
        }

        public static ResultadoValidacao ok(String mensagem) {
            return new ResultadoValidacao(true, mensagem, null);
        }
        public static ResultadoValidacao ok(String mensagem, Object dados) {
            return new ResultadoValidacao(true, mensagem, dados);
        }
        public static ResultadoValidacao erro(String mensagem) {
            return new ResultadoValidacao(false, mensagem, null);
        }
    }

    // ----------------------------------------------------------------
    // CNPJ — digit algorithm only (API call is done separately by view)
    // ----------------------------------------------------------------

    public static ResultadoValidacao validarCNPJCompleto(String cnpj) {
        if (cnpj == null || cnpj.isBlank())
            return ResultadoValidacao.erro("CNPJ não informado");
        if (!ValidadorFiscal.validarCNPJ(cnpj))
            return ResultadoValidacao.erro("CNPJ inválido — verifique os dígitos");
        return ResultadoValidacao.ok("CNPJ válido — use o botão 🔍 para consultar na Receita Federal");
    }

    // ----------------------------------------------------------------
    // CPF — digit algorithm only (no free public CPF API exists)
    // ----------------------------------------------------------------

    public static ResultadoValidacao validarCPF(String cpf) {
        if (cpf == null || cpf.isBlank())
            return ResultadoValidacao.erro("CPF não informado");
        if (!ValidadorFiscal.validarCPF(cpf))
            return ResultadoValidacao.erro("CPF inválido — verifique os dígitos");
        return ResultadoValidacao.ok("CPF válido");
    }

    // ----------------------------------------------------------------
    // CEP — ViaCEP API (caller must run in background thread)
    // ----------------------------------------------------------------

    public static ResultadoValidacao buscarCEP(String cep) {
        ConsultaViaCEP.Endereco end = ConsultaViaCEP.consultar(cep);
        if (end.mensagemErro != null)
            return ResultadoValidacao.erro(end.mensagemErro);
        if (!end.encontrado)
            return ResultadoValidacao.erro("CEP não encontrado");
        String resumo = end.logradouro + ", " + end.localidade + " - " + end.uf;
        return ResultadoValidacao.ok(resumo, end);
    }

    // ----------------------------------------------------------------
    // IE — digit algorithm per state
    // ----------------------------------------------------------------

    public static ResultadoValidacao validarIE(String ie, String uf) {
        if (ie == null || ie.isBlank())
            return ResultadoValidacao.erro("IE não informada");
        if (!ValidadorIE.validar(ie, uf))
            return ResultadoValidacao.erro(ValidadorIE.mensagemErro(ie, uf));
        return ResultadoValidacao.ok("IE válida para " + (uf != null ? uf.toUpperCase() : ""));
    }

    // ----------------------------------------------------------------
    // Email — regex + optional DNS check (DNS call in background)
    // ----------------------------------------------------------------

    public static ResultadoValidacao validarEmail(String email) {
        return validarEmail(email, false);
    }

    public static ResultadoValidacao validarEmail(String email, boolean verificarDNS) {
        if (email == null || email.isBlank())
            return ResultadoValidacao.erro("E-mail não informado");
        if (!ValidadorEmail.validarFormato(email))
            return ResultadoValidacao.erro("Formato de e-mail inválido");
        if (verificarDNS && !ValidadorEmail.verificarDNS(email))
            return ResultadoValidacao.erro("Domínio do e-mail sem registro MX (não existe)");
        return ResultadoValidacao.ok("E-mail válido");
    }
}
