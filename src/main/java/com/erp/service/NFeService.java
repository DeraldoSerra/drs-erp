package com.erp.service;

import com.erp.dao.ClienteDAO;
import com.erp.dao.NFeConfigDAO;
import com.erp.dao.NotaFiscalDAO;
import com.erp.dao.VendaDAO;
import com.erp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra o fluxo completo de emissão de NF-e:
 * carrega configuração → constrói XML → assina → envia ao SEFAZ → persiste resultado.
 */
public class NFeService {

    private static final Logger log = LoggerFactory.getLogger(NFeService.class);

    private final NFeConfigDAO configDAO = new NFeConfigDAO();
    private final NotaFiscalDAO notaFiscalDAO = new NotaFiscalDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    /**
     * Emite a NF-e para a venda informada.
     */
    public NFeResultado emitir(Venda venda) {
        // 1. Carrega configuração NF-e
        Optional<NFeConfig> optCfg = configDAO.carregar();
        if (optCfg.isEmpty()) {
            return NFeResultado.erro("Configuração NF-e não encontrada. Configure em CONFIGURAÇÕES → Config. NF-e.");
        }
        NFeConfig config = optCfg.get();
        if (!config.isConfigurado()) {
            return NFeResultado.erro("Configuração NF-e incompleta. Verifique certificado, CNPJ e Razão Social.");
        }

        // 2. Carrega itens e cliente
        List<ItemVenda> itens = venda.getItens().isEmpty()
                ? vendaDAO.listarItens(venda.getId())
                : venda.getItens();
        if (itens.isEmpty()) {
            return NFeResultado.erro("A venda não possui itens.");
        }

        Cliente cliente = null;
        if (venda.getClienteId() > 0) {
            cliente = clienteDAO.buscarPorId(venda.getClienteId()).orElse(null);
        }

        // 3. Obtém próximo número (incrementa no banco atomicamente)
        int numero = configDAO.obterProximoNumero();

        // 4. Constrói XML
        NFeXmlBuilder builder = new NFeXmlBuilder();
        String[] resultado = builder.buildXml(config, venda, itens, cliente, numero);
        String xmlNfe = resultado[0];
        String chaveAcesso = resultado[1];

        log.info("XML NF-e construído. Chave: {}", chaveAcesso);

        // 5. Assina XML
        String xmlAssinado;
        try {
            NFeAssinador assinador = new NFeAssinador(config.getCertificadoPath(), config.getCertificadoSenha());
            xmlAssinado = assinador.assinar(xmlNfe);
            log.info("XML NF-e assinado com sucesso");
        } catch (Exception e) {
            log.error("Erro ao assinar NF-e", e);
            return NFeResultado.erro("Erro ao assinar NF-e: " + e.getMessage());
        }

        // Persiste a NF como PENDENTE antes de enviar
        NotaFiscal nf = new NotaFiscal();
        nf.setVendaId(venda.getId());
        nf.setChaveAcesso(chaveAcesso);
        nf.setNumero(numero);
        nf.setSerie(String.format("%03d", config.getSerie()));
        nf.setDataEmissao(LocalDateTime.now());
        nf.setXmlNfe(xmlAssinado);
        nf.setStatus("PENDENTE");
        nf.setAmbiente(config.getAmbiente());
        notaFiscalDAO.salvar(nf);

        // 6. Envia para SEFAZ
        String xmlProcNfe;
        String protocolo;
        try {
            NFeWebService ws = new NFeWebService(
                    config.getCertificadoPath(),
                    config.getCertificadoSenha(),
                    config.isProducao()
            );
            String resposta = ws.autorizarNFe(xmlAssinado);
            log.info("Resposta SEFAZ: {}", resposta.substring(0, Math.min(500, resposta.length())));

            // 7. Parse da resposta
            String cStat = extrairTag(resposta, "cStat");
            String xMotivo = extrairTag(resposta, "xMotivo");
            protocolo = extrairTag(resposta, "nProt");
            xmlProcNfe = resposta;

            if ("100".equals(cStat)) {
                // Autorizada
                nf.setStatus("AUTORIZADA");
                nf.setProtocolo(protocolo);
                nf.setDataAutorizacao(LocalDateTime.now());
                nf.setXmlProcNfe(xmlProcNfe);
                nf.setMotivo(xMotivo);
                notaFiscalDAO.atualizar(nf);

                NFeResultado res = NFeResultado.sucesso(chaveAcesso, protocolo, xmlProcNfe);
                res.setXmlNfe(xmlAssinado);
                res.setNotaFiscalId(nf.getId());
                return res;

            } else if ("301".equals(cStat) || "302".equals(cStat)) {
                // Uso denegado
                nf.setStatus("DENEGADA");
                nf.setMotivo(cStat + " - " + xMotivo);
                notaFiscalDAO.atualizar(nf);
                return NFeResultado.erro("NF-e denegada pelo SEFAZ: [" + cStat + "] " + xMotivo);

            } else {
                // Outro erro
                nf.setStatus("ERRO");
                nf.setMotivo(cStat + " - " + xMotivo);
                notaFiscalDAO.atualizar(nf);
                return NFeResultado.erro("SEFAZ recusou a NF-e: [" + cStat + "] " + xMotivo);
            }

        } catch (Exception e) {
            log.error("Erro ao comunicar com SEFAZ", e);
            nf.setStatus("ERRO");
            nf.setMotivo(e.getMessage());
            notaFiscalDAO.atualizar(nf);
            return NFeResultado.erro("Erro de comunicação com SEFAZ: " + e.getMessage());
        }
    }

    /**
     * Cancela uma NF-e já autorizada enviando evento de cancelamento ao SEFAZ.
     */
    public NFeResultado cancelar(NotaFiscal nf, String justificativa, NFeConfig config) {
        if (!"AUTORIZADA".equals(nf.getStatus())) {
            return NFeResultado.erro("Somente NF-e com status AUTORIZADA pode ser cancelada.");
        }
        if (justificativa == null || justificativa.trim().length() < 15) {
            return NFeResultado.erro("A justificativa de cancelamento deve ter no mínimo 15 caracteres.");
        }

        try {
            String xmlEvento = buildXmlCancelamento(nf, justificativa, config);
            NFeAssinador assinador = new NFeAssinador(config.getCertificadoPath(), config.getCertificadoSenha());
            String xmlAssinado = assinador.assinar(xmlEvento);

            NFeWebService ws = new NFeWebService(
                    config.getCertificadoPath(),
                    config.getCertificadoSenha(),
                    config.isProducao()
            );
            String resposta = ws.enviarEvento(xmlAssinado);

            String cStat = extrairTag(resposta, "cStat");
            String xMotivo = extrairTag(resposta, "xMotivo");

            if ("135".equals(cStat) || "155".equals(cStat)) {
                nf.setStatus("CANCELADA");
                nf.setMotivo(cStat + " - " + xMotivo);
                nf.setXmlProcNfe(resposta);
                notaFiscalDAO.atualizar(nf);
                return NFeResultado.sucesso(nf.getChaveAcesso(), nf.getProtocolo(), resposta);
            } else {
                return NFeResultado.erro("Cancelamento recusado pelo SEFAZ: [" + cStat + "] " + xMotivo);
            }

        } catch (Exception e) {
            log.error("Erro ao cancelar NF-e", e);
            return NFeResultado.erro("Erro ao cancelar NF-e: " + e.getMessage());
        }
    }

    /** Testa a conexão com o SEFAZ consultando o status do serviço */
    public String testarConexao(NFeConfig config) {
        try {
            NFeWebService ws = new NFeWebService(
                    config.getCertificadoPath(),
                    config.getCertificadoSenha(),
                    config.isProducao()
            );
            String resposta = ws.consultarStatus();
            String cStat = extrairTag(resposta, "cStat");
            String xMotivo = extrairTag(resposta, "xMotivo");
            if ("107".equals(cStat)) {
                return "✅ SEFAZ em operação normal: " + xMotivo;
            } else {
                return "⚠️ Resposta SEFAZ: [" + cStat + "] " + xMotivo;
            }
        } catch (Exception e) {
            return "❌ Erro de conexão: " + e.getMessage();
        }
    }

    // ========== HELPERS ==========

    private String extrairTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int i = xml.indexOf(open);
        if (i < 0) return "";
        int j = xml.indexOf(close, i);
        if (j < 0) return "";
        return xml.substring(i + open.length(), j);
    }

    private String buildXmlCancelamento(NotaFiscal nf, String justificativa, NFeConfig config) {
        String dhEvento = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "-03:00";
        String cnpj = config.getCnpjNumerico();
        String tpAmb = config.isProducao() ? "1" : "2";
        String nSeq = "1";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<envEvento xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"1.00\">"
                + "<idLote>" + System.currentTimeMillis() + "</idLote>"
                + "<evento versao=\"1.00\">"
                + "<infEvento Id=\"ID11010" + nf.getChaveAcesso() + nSeq + "\">"
                + "<cOrgao>29</cOrgao>"
                + "<tpAmb>" + tpAmb + "</tpAmb>"
                + "<CNPJ>" + cnpj + "</CNPJ>"
                + "<chNFe>" + nf.getChaveAcesso() + "</chNFe>"
                + "<dhEvento>" + dhEvento + "</dhEvento>"
                + "<tpEvento>110111</tpEvento>"
                + "<nSeqEvento>" + nSeq + "</nSeqEvento>"
                + "<verEvento>1.00</verEvento>"
                + "<detEvento versao=\"1.00\">"
                + "<descEvento>Cancelamento</descEvento>"
                + "<nProt>" + nf.getProtocolo() + "</nProt>"
                + "<xJust>" + escapeXml(justificativa) + "</xJust>"
                + "</detEvento>"
                + "</infEvento>"
                + "</evento>"
                + "</envEvento>";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
