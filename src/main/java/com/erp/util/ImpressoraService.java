package com.erp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de impressão direta.
 * Imprime PDFs direto na impressora configurada sem abrir diálogo.
 */
public class ImpressoraService {

    private static final Logger log = LoggerFactory.getLogger(ImpressoraService.class);

    /** Chave usada no ConfiguracaoDAO para armazenar o nome da impressora padrão. */
    public static final String CHAVE_IMPRESSORA = "impressora_padrao";

    /**
     * Lista todos os serviços de impressão disponíveis no sistema.
     * @return lista de nomes de impressora
     */
    public static List<String> listarImpressoras() {
        List<String> nomes = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            nomes.add(ps.getName());
        }
        return nomes;
    }

    /**
     * Retorna a impressora padrão do sistema.
     */
    public static String impressoraPadrao() {
        PrintService ps = PrintServiceLookup.lookupDefaultPrintService();
        return ps != null ? ps.getName() : null;
    }

    /**
     * Imprime um arquivo PDF diretamente na impressora informada, sem abrir diálogo.
     * Usa DocFlavor.INPUT_STREAM.AUTOSENSE para máxima compatibilidade com drivers Windows.
     *
     * @param caminhoPDF  caminho completo para o arquivo PDF
     * @param nomeImpressora nome da impressora (como retornado por listarImpressoras)
     * @return true se enviou para a fila de impressão com sucesso
     */
    public static boolean imprimirPDF(String caminhoPDF, String nomeImpressora) {
        PrintService alvo = encontrarImpressora(nomeImpressora);
        if (alvo == null) {
            log.warn("Impressora '{}' não encontrada. Disponíveis: {}", nomeImpressora, listarImpressoras());
            return false;
        }

        // Tenta AUTOSENSE primeiro (funciona para a maioria dos drivers modernos)
        DocFlavor[] sabores = {
            DocFlavor.INPUT_STREAM.PDF,
            DocFlavor.INPUT_STREAM.AUTOSENSE,
            DocFlavor.BYTE_ARRAY.PDF,
            DocFlavor.BYTE_ARRAY.AUTOSENSE
        };

        for (DocFlavor sabor : sabores) {
            if (alvo.isDocFlavorSupported(sabor)) {
                log.info("Imprimindo '{}' em '{}' com flavor {}", caminhoPDF, nomeImpressora, sabor);
                return enviarParaImpressora(caminhoPDF, alvo, sabor);
            }
        }

        // Fallback: tenta AUTOSENSE mesmo que o driver não declare suporte explícito
        log.warn("Impressora '{}' não declarou suporte a PDF/AUTOSENSE, tentando AUTOSENSE mesmo assim...", nomeImpressora);
        return enviarParaImpressora(caminhoPDF, alvo, DocFlavor.INPUT_STREAM.AUTOSENSE);
    }

    /**
     * Encontra a PrintService pelo nome (busca case-insensitive e parcial).
     */
    public static PrintService encontrarImpressora(String nome) {
        if (nome == null || nome.isBlank()) return null;
        PrintService[] servicos = PrintServiceLookup.lookupPrintServices(null, null);
        // Primeiro: correspondência exata
        for (PrintService ps : servicos) {
            if (ps.getName().equalsIgnoreCase(nome.trim())) return ps;
        }
        // Segundo: contém (para nomes parciais)
        for (PrintService ps : servicos) {
            if (ps.getName().toLowerCase().contains(nome.trim().toLowerCase())) return ps;
        }
        return null;
    }

    private static boolean enviarParaImpressora(String caminhoPDF, PrintService impressora, DocFlavor sabor) {
        try (InputStream stream = new FileInputStream(caminhoPDF)) {
            Doc doc = new SimpleDoc(stream, sabor, null);
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            DocPrintJob job = impressora.createPrintJob();
            job.print(doc, attrs);
            log.info("Documento enviado com sucesso para '{}'", impressora.getName());
            return true;
        } catch (Exception e) {
            log.error("Erro ao imprimir PDF em '{}'", impressora.getName(), e);
            return false;
        }
    }
}
