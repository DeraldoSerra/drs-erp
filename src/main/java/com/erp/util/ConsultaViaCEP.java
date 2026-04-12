package com.erp.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ConsultaViaCEP {

    public static class Endereco {
        public boolean encontrado;
        public String logradouro  = "";
        public String complemento = "";
        public String bairro      = "";
        public String localidade  = "";
        public String uf          = "";
        public String cep         = "";
        public String ibge        = "";
        public String ddd         = "";
        public String mensagemErro;
    }

    /**
     * Consulta CEP na API ViaCEP. Must be called in background thread.
     */
    public static Endereco consultar(String cep) {
        Endereco result = new Endereco();
        String cepNumeros = cep.replaceAll("[^0-9]", "");

        if (cepNumeros.length() != 8) {
            result.mensagemErro = "CEP inválido (deve ter 8 dígitos)";
            return result;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://viacep.com.br/ws/" + cepNumeros + "/json/"))
                .header("User-Agent", "DRS-ERP/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                result.mensagemErro = "Erro na consulta (HTTP " + response.statusCode() + ")";
                return result;
            }

            String body = response.body();

            if (body.contains("\"erro\"")) {
                result.mensagemErro = "CEP não encontrado";
                return result;
            }

            result.logradouro  = extrairCampo(body, "logradouro");
            result.complemento = extrairCampo(body, "complemento");
            result.bairro      = extrairCampo(body, "bairro");
            result.localidade  = extrairCampo(body, "localidade");
            result.uf          = extrairCampo(body, "uf");
            result.cep         = extrairCampo(body, "cep");
            result.ibge        = extrairCampo(body, "ibge");
            result.ddd         = extrairCampo(body, "ddd");
            result.encontrado  = !result.localidade.isEmpty();

        } catch (java.net.ConnectException e) {
            result.mensagemErro = "Sem conexão com a internet";
        } catch (java.net.http.HttpTimeoutException e) {
            result.mensagemErro = "Timeout na consulta (ViaCEP indisponível)";
        } catch (Exception e) {
            result.mensagemErro = "Erro: " + e.getMessage();
        }

        return result;
    }

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
            int end = json.indexOf('"', start);
            if (end < 0) return "";
            return json.substring(start, end);
        }
        return "";
    }
}
