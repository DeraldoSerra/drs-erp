package com.erp.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class Formatador {

    private static final Locale BRASIL = new Locale("pt", "BR");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(BRASIL);
    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QUANTIDADE = new DecimalFormat("#,##0.###");

    public static String formatarMoeda(double valor) {
        return MOEDA.format(valor);
    }

    public static String formatarDecimal(double valor) {
        return DECIMAL.format(valor);
    }

    public static String formatarQuantidade(double valor) {
        return QUANTIDADE.format(valor);
    }

    public static double parseMoeda(String texto) {
        if (texto == null || texto.isBlank()) return 0;
        try {
            texto = texto.replaceAll("[^0-9,.]", "").replace(",", ".");
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatarCpf(String cpf) {
        if (cpf == null) return "";
        cpf = cpf.replaceAll("[^0-9]", "");
        if (cpf.length() == 11) {
            return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return cpf;
    }

    public static String formatarCnpj(String cnpj) {
        if (cnpj == null) return "";
        cnpj = cnpj.replaceAll("[^0-9]", "");
        if (cnpj.length() == 14) {
            return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return cnpj;
    }

    public static String formatarTelefone(String tel) {
        if (tel == null) return "";
        tel = tel.replaceAll("[^0-9]", "");
        if (tel.length() == 11) {
            return tel.replaceAll("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        } else if (tel.length() == 10) {
            return tel.replaceAll("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }
        return tel;
    }

    public static String formatarCep(String cep) {
        if (cep == null) return "";
        cep = cep.replaceAll("[^0-9]", "");
        if (cep.length() == 8) {
            return cep.replaceAll("(\\d{5})(\\d{3})", "$1-$2");
        }
        return cep;
    }

    public static String apenasNumeros(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^0-9]", "");
    }
}
