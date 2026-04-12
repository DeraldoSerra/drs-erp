package com.erp.util;

/**
 * Validações fiscais brasileiras: CPF, CNPJ, formatação e máscara.
 */
public class ValidadorFiscal {

    private ValidadorFiscal() {}

    // =========================================================
    // CPF
    // =========================================================

    /** Valida CPF (aceita com ou sem formatação) */
    public static boolean validarCPF(String cpf) {
        if (cpf == null) return false;
        String c = cpf.replaceAll("[^0-9]", "");
        if (c.length() != 11) return false;
        if (c.chars().distinct().count() == 1) return false; // 111.111.111-11 etc

        int d1 = 0;
        for (int i = 0; i < 9; i++) d1 += Character.getNumericValue(c.charAt(i)) * (10 - i);
        d1 = 11 - (d1 % 11);
        if (d1 >= 10) d1 = 0;
        if (d1 != Character.getNumericValue(c.charAt(9))) return false;

        int d2 = 0;
        for (int i = 0; i < 10; i++) d2 += Character.getNumericValue(c.charAt(i)) * (11 - i);
        d2 = 11 - (d2 % 11);
        if (d2 >= 10) d2 = 0;
        return d2 == Character.getNumericValue(c.charAt(10));
    }

    /** Valida CNPJ (aceita com ou sem formatação) */
    public static boolean validarCNPJ(String cnpj) {
        if (cnpj == null) return false;
        String c = cnpj.replaceAll("[^0-9]", "");
        if (c.length() != 14) return false;
        if (c.chars().distinct().count() == 1) return false;

        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int d1 = calcularDigito(c.substring(0, 12), pesos1);
        if (d1 != Character.getNumericValue(c.charAt(12))) return false;

        int d2 = calcularDigito(c.substring(0, 13), pesos2);
        return d2 == Character.getNumericValue(c.charAt(13));
    }

    private static int calcularDigito(String str, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++)
            soma += Character.getNumericValue(str.charAt(i)) * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // =========================================================
    // Formatação
    // =========================================================

    /** Formata CPF: 000.000.000-00 */
    public static String formatarCPF(String cpf) {
        String c = cpf == null ? "" : cpf.replaceAll("[^0-9]", "");
        if (c.length() != 11) return cpf;
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." +
               c.substring(6, 9) + "-" + c.substring(9);
    }

    /** Formata CNPJ: 00.000.000/0000-00 */
    public static String formatarCNPJ(String cnpj) {
        String c = cnpj == null ? "" : cnpj.replaceAll("[^0-9]", "");
        if (c.length() != 14) return cnpj;
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." +
               c.substring(5, 8) + "/" + c.substring(8, 12) + "-" + c.substring(12);
    }

    /** Remove todos os caracteres não numéricos */
    public static String apenasNumeros(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }

    /** Detecta se é CPF ou CNPJ pelo tamanho (apenas dígitos) */
    public static String detectarTipo(String valor) {
        if (valor == null) return "";
        String nums = apenasNumeros(valor);
        if (nums.length() <= 11) return "CPF";
        return "CNPJ";
    }

    /** Valida CPF ou CNPJ automaticamente */
    public static boolean validarCpfOuCnpj(String valor) {
        if (valor == null || valor.isBlank()) return false;
        String nums = apenasNumeros(valor);
        if (nums.length() == 11) return validarCPF(nums);
        if (nums.length() == 14) return validarCNPJ(nums);
        return false;
    }

    /** Formata CPF ou CNPJ automaticamente */
    public static String formatarCpfOuCnpj(String valor) {
        if (valor == null) return "";
        String nums = apenasNumeros(valor);
        if (nums.length() == 11) return formatarCPF(nums);
        if (nums.length() == 14) return formatarCNPJ(nums);
        return valor;
    }

    // =========================================================
    // Máscaras dinâmicas para TextField
    // =========================================================

    /** Aplica máscara de CPF/CNPJ conforme o usuário digita */
    public static String aplicarMascaraCpfCnpj(String texto) {
        String nums = apenasNumeros(texto);
        if (nums.length() <= 11) {
            // Máscara CPF: 000.000.000-00
            if (nums.length() > 9)
                return nums.substring(0,3)+"."+nums.substring(3,6)+"."+nums.substring(6,9)+"-"+nums.substring(9);
            if (nums.length() > 6)
                return nums.substring(0,3)+"."+nums.substring(3,6)+"."+nums.substring(6);
            if (nums.length() > 3)
                return nums.substring(0,3)+"."+nums.substring(3);
            return nums;
        } else {
            // Máscara CNPJ: 00.000.000/0000-00
            String n = nums.substring(0, Math.min(14, nums.length()));
            if (n.length() > 12)
                return n.substring(0,2)+"."+n.substring(2,5)+"."+n.substring(5,8)+"/"+n.substring(8,12)+"-"+n.substring(12);
            if (n.length() > 8)
                return n.substring(0,2)+"."+n.substring(2,5)+"."+n.substring(5,8)+"/"+n.substring(8);
            if (n.length() > 5)
                return n.substring(0,2)+"."+n.substring(2,5)+"."+n.substring(5);
            if (n.length() > 2)
                return n.substring(0,2)+"."+n.substring(2);
            return n;
        }
    }

    /** Aplica máscara de CEP: 00000-000 */
    public static String aplicarMascaraCep(String texto) {
        String nums = apenasNumeros(texto);
        if (nums.length() > 5) return nums.substring(0,5)+"-"+nums.substring(5, Math.min(8,nums.length()));
        return nums;
    }

    /** Aplica máscara de telefone: (00) 0000-0000 ou (00) 00000-0000 */
    public static String aplicarMascaraTelefone(String texto) {
        String nums = apenasNumeros(texto);
        if (nums.length() > 10)
            return "("+nums.substring(0,2)+") "+nums.substring(2,7)+"-"+nums.substring(7,Math.min(11,nums.length()));
        if (nums.length() > 6)
            return "("+nums.substring(0,2)+") "+nums.substring(2,6)+"-"+nums.substring(6);
        if (nums.length() > 2)
            return "("+nums.substring(0,2)+") "+nums.substring(2);
        return nums;
    }

    // =========================================================
    // Validação de campos fiscais
    // =========================================================

    /** Valida NCM: 8 dígitos */
    public static boolean validarNCM(String ncm) {
        if (ncm == null) return false;
        return ncm.replaceAll("[^0-9]", "").length() == 8;
    }

    /** Valida CFOP: 4 dígitos iniciando com 1-9 */
    public static boolean validarCFOP(String cfop) {
        if (cfop == null) return false;
        String c = cfop.replaceAll("[^0-9]", "");
        return c.length() == 4 && c.charAt(0) >= '1';
    }
}
