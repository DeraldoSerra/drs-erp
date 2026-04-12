package com.erp.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ConsultaReceitaWS {

    public static class DadosCNPJ {
        public boolean valido;
        public String situacao      = "";
        public String razaoSocial   = "";
        public String nomeFantasia  = "";
        public String logradouro    = "";
        public String numero        = "";
        public String complemento   = "";
        public String bairro        = "";
        public String municipio     = "";
        public String uf            = "";
        public String cep           = "";
        public String telefone      = "";
        public String email         = "";
        public String abertura      = "";
        public String naturezaJuridica = "";
        public String capitalSocial = "";
        public String tipo          = "";
        public String mensagemErro;
    }

    /**
     * Consulta CNPJ na API ReceitaWS. Thread-safe. Must be called in background thread.
     */
    public static DadosCNPJ consultar(String cnpj) {
        DadosCNPJ result = new DadosCNPJ();
        String cnpjNumeros = cnpj.replaceAll("[^0-9]", "");

        if (cnpjNumeros.length() != 14) {
            result.mensagemErro = "CNPJ inválido (deve ter 14 dígitos)";
            return result;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.receitaws.com.br/v1/cnpj/" + cnpjNumeros))
                .header("User-Agent", "DRS-ERP/1.0")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                result.mensagemErro = "Limite de consultas atingido. Aguarde 1 minuto e tente novamente.";
                return result;
            }
            if (response.statusCode() != 200) {
                result.mensagemErro = "Erro na consulta (HTTP " + response.statusCode() + ")";
                return result;
            }

            String body = response.body();

            String status = extrairCampo(body, "status");
            if ("ERROR".equalsIgnoreCase(status)) {
                result.mensagemErro = extrairCampo(body, "message");
                if (result.mensagemErro == null || result.mensagemErro.isBlank())
                    result.mensagemErro = "CNPJ não encontrado na Receita Federal";
                return result;
            }

            result.situacao        = extrairCampo(body, "situacao");
            result.razaoSocial     = extrairCampo(body, "nome");
            result.nomeFantasia    = extrairCampo(body, "fantasia");
            result.logradouro      = extrairCampo(body, "logradouro");
            result.numero          = extrairCampo(body, "numero");
            result.complemento     = extrairCampo(body, "complemento");
            result.bairro          = extrairCampo(body, "bairro");
            result.municipio       = extrairCampo(body, "municipio");
            result.uf              = extrairCampo(body, "uf");
            result.cep             = extrairCampo(body, "cep");
            result.telefone        = extrairCampo(body, "telefone");
            result.email           = extrairCampo(body, "email");
            result.abertura        = extrairCampo(body, "abertura");
            result.naturezaJuridica= extrairCampo(body, "natureza_juridica");
            result.capitalSocial   = extrairCampo(body, "capital_social");
            result.tipo            = extrairCampo(body, "tipo");

            result.valido = "ATIVA".equalsIgnoreCase(result.situacao);
            if (!result.valido && (result.mensagemErro == null || result.mensagemErro.isBlank())) {
                result.mensagemErro = "CNPJ com situação: " + result.situacao;
            }

        } catch (java.net.ConnectException e) {
            result.mensagemErro = "Sem conexão com a internet";
        } catch (java.net.http.HttpTimeoutException e) {
            result.mensagemErro = "Timeout na consulta (ReceitaWS indisponível)";
        } catch (Exception e) {
            result.mensagemErro = "Erro: " + e.getMessage();
        }

        return result;
    }

    /** Simple JSON string field extractor without external library */
    private static String extrairCampo(String json, String campo) {
        String key = "\"" + campo + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        idx = json.indexOf(":", idx);
        if (idx < 0) return "";
        idx++;
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        if (idx >= json.length()) return "";
        if (json.charAt(idx) == '"') {
            int start = idx + 1;
            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case '"': sb.append('"'); i += 2; break;
                        case '\\': sb.append('\\'); i += 2; break;
                        case 'n': sb.append('\n'); i += 2; break;
                        case 'r': sb.append('\r'); i += 2; break;
                        case 't': sb.append('\t'); i += 2; break;
                        default: sb.append(json.charAt(i)); i++; break;
                    }
                } else {
                    sb.append(json.charAt(i));
                    i++;
                }
            }
            return sb.toString();
        } else {
            // Number, boolean, or null
            int start = idx;
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']')
                end++;
            return json.substring(start, end).trim();
        }
    }
}
