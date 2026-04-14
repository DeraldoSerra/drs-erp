package com.erp.service;

import com.erp.model.NFeConfig;
import com.erp.util.ValidadorFiscal;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Realiza validação completa antes de habilitar emissão de NF-e:
 *  1. Certificado A1 — existência, senha, validade, CNPJ embutido, cadeia ICP-Brasil
 *  2. CNPJ — consulta na Receita Federal via BrasilAPI (empresa ATIVA, razão social)
 *  3. SEFAZ — status do serviço em produção ou homologação
 *
 * Deve ser invocado em thread de background (faz I/O de rede).
 */
public class ValidacaoNFeCompleta {

    public enum StatusCheck { OK, AVISO, ERRO }

    public static class ItemCheck {
        public final String titulo;
        public final StatusCheck status;
        public final String detalhe;

        public ItemCheck(String titulo, StatusCheck status, String detalhe) {
            this.titulo  = titulo;
            this.status  = status;
            this.detalhe = detalhe;
        }
    }

    public static class Resultado {
        public final List<ItemCheck> itens = new ArrayList<>();
        /** false se qualquer item tiver status ERRO */
        public boolean aprovado = true;

        public void add(String titulo, StatusCheck status, String detalhe) {
            itens.add(new ItemCheck(titulo, status, detalhe));
            if (status == StatusCheck.ERRO) aprovado = false;
        }
    }

    // ===========================================================
    // Ponto de entrada
    // ===========================================================

    /**
     * Executa todos os checks em sequência e retorna o Resultado.
     * Deve ser chamado em thread background — faz I/O de rede.
     *
     * @param cfg configuração NF-e com certificado preenchido
     */
    public Resultado validar(NFeConfig cfg) {
        Resultado r = new Resultado();

        // 1. Certificado digital A1
        validarCertificado(cfg, r);

        // 2. CNPJ na Receita Federal (BrasilAPI)
        consultarCnpjReceita(cfg, r);

        // 3. Status SEFAZ
        verificarStatusSefaz(cfg, r);

        return r;
    }

    // ===========================================================
    // 1. Certificado A1
    // ===========================================================

    private void validarCertificado(NFeConfig cfg, Resultado r) {
        String path  = cfg.getCertificadoPath();
        String senha = cfg.getCertificadoSenha();

        // 1a. Arquivo existe?
        java.io.File f = new java.io.File(path);
        if (!f.exists()) {
            r.add("Arquivo do certificado", StatusCheck.ERRO,
                "Arquivo não encontrado:\n" + path);
            return;
        }
        r.add("Arquivo do certificado", StatusCheck.OK, "Arquivo encontrado: " + f.getName());

        // 1b. Abre o KeyStore com a senha fornecida
        KeyStore ks;
        X509Certificate cert = null;
        try {
            ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(f)) {
                ks.load(fis, senha.toCharArray());
            }
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    cert = (X509Certificate) ks.getCertificate(alias);
                    break;
                }
            }
            r.add("Senha do certificado", StatusCheck.OK, "Certificado aberto com sucesso");
        } catch (Exception e) {
            r.add("Senha do certificado", StatusCheck.ERRO,
                "Não foi possível abrir o certificado. Verifique a senha informada." +
                "\nDetalhe: " + e.getMessage());
            return;
        }

        if (cert == null) {
            r.add("Chave privada", StatusCheck.ERRO,
                "Nenhum certificado com chave privada encontrado no arquivo .pfx");
            return;
        }

        // 1c. Validade (não expirado, não prematuro)
        Date agora    = new Date();
        Date notAfter  = cert.getNotAfter();
        Date notBefore = cert.getNotBefore();
        long diasRestantes = (notAfter.getTime() - agora.getTime()) / 86_400_000L;

        if (agora.after(notAfter)) {
            r.add("Validade do certificado", StatusCheck.ERRO,
                "Certificado EXPIRADO em " + formatar(notAfter) +
                "\nRenove o certificado A1 junto à Autoridade Certificadora.");
        } else if (agora.before(notBefore)) {
            r.add("Validade do certificado", StatusCheck.ERRO,
                "Certificado ainda não é válido. Válido a partir de " + formatar(notBefore));
        } else if (diasRestantes <= 30) {
            r.add("Validade do certificado", StatusCheck.AVISO,
                "Certificado prestes a expirar: " + diasRestantes + " dias restantes" +
                "\nVálido até " + formatar(notAfter) +
                "\nRenove antes do vencimento para não interromper a emissão de NF-e.");
        } else {
            r.add("Validade do certificado", StatusCheck.OK,
                "Válido até " + formatar(notAfter) + " (" + diasRestantes + " dias restantes)");
        }

        // 1d. CNPJ no certificado confere com o cadastrado?
        String cnpjCert = extrairCnpjDoCertificado(cert);
        String cnpjCfg  = ValidadorFiscal.apenasNumeros(cfg.getCnpj());
        if (cnpjCert.isBlank()) {
            r.add("CNPJ no certificado", StatusCheck.AVISO,
                "Não foi possível extrair o CNPJ do certificado para comparação." +
                "\nSujeito: " + cert.getSubjectX500Principal().getName());
        } else if (cnpjCert.equals(cnpjCfg)) {
            r.add("CNPJ no certificado", StatusCheck.OK,
                "CNPJ do certificado confere: " + ValidadorFiscal.formatarCNPJ(cnpjCert));
        } else {
            r.add("CNPJ no certificado", StatusCheck.ERRO,
                "CNPJ do certificado diverge do CNPJ cadastrado!" +
                "\nCertificado: " + ValidadorFiscal.formatarCNPJ(cnpjCert) +
                "\nCadastrado:  " + ValidadorFiscal.formatarCNPJ(cnpjCfg));
        }

        // 1e. Emissor ICP-Brasil?
        String issuer = cert.getIssuerX500Principal().getName();
        if (issuer.contains("ICP-Brasil") || issuer.contains("AC ") ||
                issuer.contains("Certisign") || issuer.contains("Serasa") ||
                issuer.contains("SERPRO")    || issuer.contains("Receita")) {
            r.add("Cadeia ICP-Brasil", StatusCheck.OK,
                "Certificado emitido por AC ICP-Brasil autorizada");
        } else {
            r.add("Cadeia ICP-Brasil", StatusCheck.AVISO,
                "Emissor não identificado como ICP-Brasil — válido em homologação," +
                " mas a produção requer certificado ICP-Brasil." +
                "\nEmissor: " + issuer);
        }
    }

    /**
     * Extrai CNPJ (14 dígitos) do SubjectDN ou SubjectAlternativeNames.
     * Funciona com os formatos comuns dos certificados ICP-Brasil.
     */
    private String extrairCnpjDoCertificado(X509Certificate cert) {
        String dn = cert.getSubjectX500Principal().getName();

        // Padrão CNPJ formatado: XX.XXX.XXX/XXXX-XX
        Pattern fmtCnpj = Pattern.compile("(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})");
        Matcher mf = fmtCnpj.matcher(dn);
        if (mf.find()) return ValidadorFiscal.apenasNumeros(mf.group(1));

        // Padrão CNPJ numérico bruto: 14 dígitos no DN
        Pattern rawCnpj = Pattern.compile("\\b(\\d{14})\\b");
        Matcher mr = rawCnpj.matcher(dn);
        if (mr.find()) {
            String cand = mr.group(1);
            if (ValidadorFiscal.validarCNPJ(cand)) return cand;
        }

        // Subject Alternative Names
        try {
            var sans = cert.getSubjectAlternativeNames();
            if (sans != null) {
                for (var san : sans) {
                    String val = String.valueOf(san.get(1));
                    Matcher ms = rawCnpj.matcher(val);
                    if (ms.find() && ValidadorFiscal.validarCNPJ(ms.group(1)))
                        return ms.group(1);
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    // ===========================================================
    // 2. CNPJ — Receita Federal via BrasilAPI
    // ===========================================================

    private void consultarCnpjReceita(NFeConfig cfg, Resultado r) {
        String cnpj = ValidadorFiscal.apenasNumeros(cfg.getCnpj());
        if (cnpj.length() != 14) {
            r.add("CNPJ - Receita Federal", StatusCheck.ERRO,
                "CNPJ inválido (deve ter 14 dígitos). Corrija antes de habilitar.");
            return;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://brasilapi.com.br/api/cnpj/v1/" + cnpj))
                .header("User-Agent", "DRS-ERP/1.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 404) {
                r.add("CNPJ - Receita Federal", StatusCheck.ERRO,
                    "CNPJ não encontrado na Receita Federal: " + ValidadorFiscal.formatarCNPJ(cnpj) +
                    "\nVerifique se o CNPJ está correto.");
                return;
            }

            if (resp.statusCode() != 200) {
                r.add("CNPJ - Receita Federal", StatusCheck.AVISO,
                    "Consulta indisponível no momento (HTTP " + resp.statusCode() + ")." +
                    "\nTente novamente em instantes.");
                return;
            }

            String body = resp.body();

            // BrasilAPI retorna campos como descricao_situacao_cadastral
            String situacao   = extrairJson(body, "descricao_situacao_cadastral");
            String razaoRF    = extrairJson(body, "razao_social");
            String uf         = extrairJson(body, "uf");
            String municipio  = extrairJson(body, "municipio");
            String cnaeDesc   = extrairJson(body, "cnae_fiscal_descricao");

            if (situacao.isBlank()) situacao = extrairJson(body, "situacao");

            boolean ativa = situacao.toUpperCase().contains("ATIVA");

            String detalhe = "Razão Social (RF): " + razaoRF
                + "\nSituação: " + situacao
                + (uf.isBlank()       ? "" : "\nUF: " + uf)
                + (municipio.isBlank() ? "" : " / " + municipio)
                + (cnaeDesc.isBlank()  ? "" : "\nAtividade Principal: " + cnaeDesc);

            if (ativa) {
                r.add("CNPJ - Receita Federal", StatusCheck.OK, detalhe);
            } else {
                r.add("CNPJ - Receita Federal", StatusCheck.ERRO,
                    "Empresa NÃO está ATIVA na Receita Federal!\n" + detalhe +
                    "\n\nA emissão de NF-e não é permitida para empresas inativas.");
            }

            // Avisa se razão social diverge
            if (!razaoRF.isBlank() && !cfg.getRazaoSocial().isBlank()) {
                String rfNorm  = razaoRF.toUpperCase().trim();
                String cfgNorm = cfg.getRazaoSocial().toUpperCase().trim();
                if (!rfNorm.equals(cfgNorm)) {
                    r.add("Razão Social vs Receita Federal", StatusCheck.AVISO,
                        "A razão social cadastrada difere da Receita Federal." +
                        "\nCadastrada:       " + cfg.getRazaoSocial() +
                        "\nReceita Federal:  " + razaoRF +
                        "\nRecomenda-se usar a razão social exata da RF na NF-e.");
                }
            }

        } catch (java.net.ConnectException | java.net.http.HttpTimeoutException e) {
            r.add("CNPJ - Receita Federal", StatusCheck.AVISO,
                "Sem acesso à API da Receita Federal. Verifique a internet." +
                "\nA validação continuará, mas confirme manualmente que a empresa está ATIVA.");
        } catch (Exception e) {
            r.add("CNPJ - Receita Federal", StatusCheck.AVISO,
                "Erro na consulta à Receita Federal: " + e.getMessage());
        }
    }

    // ===========================================================
    // 3. Status SEFAZ
    // ===========================================================

    private void verificarStatusSefaz(NFeConfig cfg, Resultado r) {
        try {
            NFeWebService ws = new NFeWebService(
                cfg.getCertificadoPath(),
                cfg.getCertificadoSenha(),
                cfg.isProducao()
            );
            String resposta = ws.consultarStatus();

            String cStat    = extrairXml(resposta, "cStat");
            String xMotivo  = extrairXml(resposta, "xMotivo");
            String tpAmb    = extrairXml(resposta, "tpAmb");
            String dhRecbto = extrairXml(resposta, "dhRecbto");
            String tMed     = extrairXml(resposta, "tMed");
            String verAplic = extrairXml(resposta, "verAplic");

            String ambiente = "1".equals(tpAmb) ? "Produção" : "Homologação";
            String detalhe = "Status: [" + cStat + "] " + xMotivo
                + "\nAmbiente: " + ambiente
                + (dhRecbto.isBlank() ? "" : "\nData/hora SEFAZ: " + dhRecbto)
                + (tMed.isBlank()     ? "" : "\nTempo médio de resposta: " + tMed + "s")
                + (verAplic.isBlank() ? "" : "\nVersão aplicativo: " + verAplic);

            if ("107".equals(cStat)) {
                r.add("SEFAZ - Status do serviço", StatusCheck.OK, detalhe);
            } else if (cStat.isBlank()) {
                r.add("SEFAZ - Status do serviço", StatusCheck.ERRO,
                    "Sem resposta do SEFAZ. Verifique:" +
                    "\n• Certificado correto (UF/ambiente)" +
                    "\n• Conexão com a internet" +
                    "\n• Ambiente configurado (Produção ou Homologação)");
            } else {
                r.add("SEFAZ - Status do serviço", StatusCheck.AVISO, detalhe +
                    "\n\nO serviço pode estar em manutenção. Tente novamente mais tarde.");
            }

        } catch (Exception e) {
            r.add("SEFAZ - Status do serviço", StatusCheck.ERRO,
                "Erro ao conectar ao SEFAZ: " + e.getMessage() +
                "\n\nVerifique:\n• Certificado e senha corretos\n• Acesso à internet\n• Firewall/proxy bloqueando porta 443");
        }
    }

    // ===========================================================
    // Helpers
    // ===========================================================

    /**
     * Extrai valor de campo JSON simples (string, número, booleano).
     * Não trata arrays/objetos aninhados.
     */
    private String extrairJson(String json, String chave) {
        String token = "\"" + chave + "\"";
        int i = json.indexOf(token);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + token.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return "";

        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        }
        // número, booleano ou null
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        String val = json.substring(start, end).trim();
        return "null".equals(val) ? "" : val;
    }

    private String extrairXml(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int i = xml.indexOf(open);
        if (i < 0) return "";
        int j = xml.indexOf(close, i);
        if (j < 0) return "";
        return xml.substring(i + open.length(), j);
    }

    private String formatar(Date d) {
        return d.toInstant()
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
