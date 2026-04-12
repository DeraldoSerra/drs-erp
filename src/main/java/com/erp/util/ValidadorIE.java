package com.erp.util;

/**
 * Validação de Inscrição Estadual por UF.
 * BA: algoritmo completo (8 ou 9 dígitos).
 * SP: 12 dígitos + dígito verificador na posição 9.
 * Demais UFs: validação de formato/comprimento conforme SINTEGRA.
 */
public class ValidadorIE {

    private ValidadorIE() {}

    public static boolean validar(String ie, String uf) {
        if (ie == null || ie.isBlank() || uf == null || uf.isBlank()) return false;
        String digits = ie.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return false;

        switch (uf.toUpperCase().trim()) {
            case "BA": return validarBA(digits);
            case "SP": return validarSP(digits);
            case "MG": return digits.length() == 13;
            case "RJ": return digits.length() == 8;
            case "PR": return digits.length() == 10;
            case "RS": return validarRS(digits);
            case "SC": return digits.length() == 9;
            case "GO": return validarGO(digits);
            case "DF": return digits.length() == 13;
            case "ES": return digits.length() == 9;
            case "CE": return digits.length() == 9;
            case "PE": return digits.length() == 9 || digits.length() == 14;
            case "MA": return digits.length() == 9;
            case "PI": return digits.length() == 9;
            case "AL": return digits.length() == 9;
            case "SE": return digits.length() == 9;
            case "PB": return digits.length() == 9;
            case "RN": return digits.length() == 9 || digits.length() == 10;
            case "PA": return digits.length() == 9;
            case "AM": return digits.length() == 9;
            case "MT": return digits.length() == 11;
            case "MS": return digits.length() == 9;
            case "TO": return digits.length() == 11;
            case "AP": return digits.length() == 9;
            case "RO": return digits.length() == 14;
            case "RR": return digits.length() == 9;
            case "AC": return digits.length() == 13;
            default:   return digits.length() >= 8 && digits.length() <= 14;
        }
    }

    public static String mensagemErro(String ie, String uf) {
        if (ie == null || ie.isBlank()) return "IE não informada";
        String digits = ie.replaceAll("[^0-9]", "");
        String ufUpper = uf != null ? uf.toUpperCase().trim() : "";

        switch (ufUpper) {
            case "BA":
                if (digits.length() != 8 && digits.length() != 9)
                    return "IE-BA deve ter 8 ou 9 dígitos";
                return "IE-BA com dígitos verificadores inválidos";
            case "SP":
                if (digits.length() != 12)
                    return "IE-SP deve ter 12 dígitos";
                return "IE-SP com dígito verificador inválido";
            default:
                return "IE inválida para " + ufUpper;
        }
    }

    // ---- BA ----

    private static boolean validarBA(String d) {
        if (d.length() == 8) return validarBA8(d);
        if (d.length() == 9) return validarBA9(d);
        return false;
    }

    /**
     * BA 8 dígitos: D1..D6 são base, D7=1º verificador, D8=2º verificador.
     * Módulo: se D1 ∈ {6,7,8} → mod10, senão → mod11.
     * 2º verif (D8): pesos 7,6,5,4,3,2 sobre D1..D6.
     * 1º verif (D7): pesos 8,7,6,5,4,3,2 sobre D1..D6 + D8(calculado).
     */
    private static boolean validarBA8(String d) {
        int primo = d.charAt(0) - '0';
        int mod = (primo == 6 || primo == 7 || primo == 8) ? 10 : 11;

        // Calcular D8 (2º verificador)
        int[] w2 = {7, 6, 5, 4, 3, 2};
        int sum2 = 0;
        for (int i = 0; i < 6; i++) sum2 += (d.charAt(i) - '0') * w2[i];
        int rem2 = sum2 % mod;
        int v2 = calcVerif(rem2, mod);

        // Calcular D7 (1º verificador) usando D8 calculado
        int[] w1base = {8, 7, 6, 5, 4, 3};
        int sum1 = v2 * 2;
        for (int i = 0; i < 6; i++) sum1 += (d.charAt(i) - '0') * w1base[i];
        int rem1 = sum1 % mod;
        int v1 = calcVerif(rem1, mod);

        return v1 == (d.charAt(6) - '0') && v2 == (d.charAt(7) - '0');
    }

    /**
     * BA 9 dígitos (produtor rural): D1..D7 são base, D8=1º verificador, D9=2º verificador.
     * 2º verif (D9): pesos 8,7,6,5,4,3,2 sobre D1..D7.
     * 1º verif (D8): pesos 9,8,7,6,5,4,3,2 sobre D1..D7 + D9(calculado).
     */
    private static boolean validarBA9(String d) {
        int primo = d.charAt(0) - '0';
        int mod = (primo == 6 || primo == 7 || primo == 8) ? 10 : 11;

        // Calcular D9 (2º verificador)
        int[] w2 = {8, 7, 6, 5, 4, 3, 2};
        int sum2 = 0;
        for (int i = 0; i < 7; i++) sum2 += (d.charAt(i) - '0') * w2[i];
        int rem2 = sum2 % mod;
        int v2 = calcVerif(rem2, mod);

        // Calcular D8 (1º verificador)
        int[] w1base = {9, 8, 7, 6, 5, 4, 3};
        int sum1 = v2 * 2;
        for (int i = 0; i < 7; i++) sum1 += (d.charAt(i) - '0') * w1base[i];
        int rem1 = sum1 % mod;
        int v1 = calcVerif(rem1, mod);

        return v1 == (d.charAt(7) - '0') && v2 == (d.charAt(8) - '0');
    }

    private static int calcVerif(int resto, int modulo) {
        if (modulo == 10) {
            return resto == 0 ? 0 : 10 - resto;
        } else {
            return resto < 2 ? 0 : 11 - resto;
        }
    }

    // ---- SP ----

    /** SP: 12 dígitos, D9 é o verificador calculado de D1..D8. */
    private static boolean validarSP(String d) {
        if (d.length() != 12) return false;
        int[] weights = {1, 3, 2, 9, 8, 7, 6, 5};
        int sum = 0;
        for (int i = 0; i < 8; i++) sum += (d.charAt(i) - '0') * weights[i];
        int rem = sum % 11;
        int v = (rem < 2) ? rem : 11 - rem;
        return v == (d.charAt(8) - '0');
    }

    // ---- RS ----

    /** RS: 10 dígitos, último é verificador. */
    private static boolean validarRS(String d) {
        if (d.length() != 10) return false;
        int[] weights = {2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (d.charAt(i) - '0') * weights[i];
        int rem = sum % 11;
        int v = rem < 2 ? 0 : 11 - rem;
        return v == (d.charAt(9) - '0');
    }

    // ---- GO ----

    /** GO: 9 dígitos. */
    private static boolean validarGO(String d) {
        if (d.length() != 9) return false;
        // Goiás starts with 10, 11 or 15
        String prefix = d.substring(0, 2);
        return prefix.equals("10") || prefix.equals("11") || prefix.equals("15");
    }
}
