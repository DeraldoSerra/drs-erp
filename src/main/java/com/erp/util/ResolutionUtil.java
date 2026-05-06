package com.erp.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * Utilitário para adaptar a interface a diferentes resoluções de monitor.
 *
 * Resolução de referência: 1920 × 1080 (Full HD).
 * O fator de escala é calculado uma única vez e reutilizado durante toda
 * a sessão. Fica entre 0,80 (telas pequenas) e 1,30 (4 K / alta resolução).
 *
 * Como usar nas Views:
 *   // 1) Aplique o estilo raiz na Scene para que a herança CSS funcione:
 *   scene.getRoot().setStyle(ResolutionUtil.estiloRaiz());
 *
 *   // 2) Use ResolutionUtil.px() para valores de layout em código Java:
 *   double largura = ResolutionUtil.px(240);   // escala 240 px de base
 */
public final class ResolutionUtil {

    private static final double BASE_LARGURA = 1920.0;
    private static final double BASE_ALTURA  = 1080.0;
    private static final double FONTE_BASE   = 13.0;

    // Limites do fator de escala
    private static final double ESCALA_MIN = 0.80;
    private static final double ESCALA_MAX = 1.30;

    private static double escala = -1;

    private ResolutionUtil() {}

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /** Fator de escala em relação à resolução de referência (1920 × 1080). */
    public static double escala() {
        if (escala < 0) calcular();
        return escala;
    }

    /**
     * Retorna o tamanho de fonte raiz escalado (em px).
     * Ex.: em 1366 × 768 → ≈ 11 px; em 2560 × 1440 → ≈ 17 px.
     */
    public static double fontePx() {
        return Math.round(FONTE_BASE * escala());
    }

    /**
     * Retorna o estilo CSS inline para aplicar no nó raiz da Scene.
     * Com as classes CSS usando unidades "em", todos os elementos herdam
     * automaticamente este tamanho de fonte escalado.
     *
     * Exemplo de uso:
     *   root.setStyle(ResolutionUtil.estiloRaiz());
     */
    public static String estiloRaiz() {
        return String.format("-fx-font-size: %dpx;", (int) fontePx());
    }

    /**
     * Escala um valor de pixels conforme o fator de escala atual.
     * Útil para calcular larguras, alturas e espaçamentos em código Java.
     */
    public static double px(double valorBase) {
        return valorBase * escala();
    }

    /**
     * Retorna uma string CSS para font-size em pixels escalados.
     * Útil em {@code setStyle()} inline quando não é possível usar "em".
     */
    public static String fs(double basePx) {
        return String.format("-fx-font-size: %dpx;", (int) Math.round(basePx * escala()));
    }

    // -------------------------------------------------------------------------
    // Cálculo interno
    // -------------------------------------------------------------------------

    private static void calcular() {
        try {
            Rectangle2D tela = Screen.getPrimary().getVisualBounds();
            double sx = tela.getWidth()  / BASE_LARGURA;
            double sy = tela.getHeight() / BASE_ALTURA;
            // Usa o menor fator para não cortar conteúdo em nenhum eixo
            double fator = Math.min(sx, sy);
            escala = Math.max(ESCALA_MIN, Math.min(ESCALA_MAX, fator));
        } catch (Exception e) {
            escala = 1.0; // fallback seguro
        }
    }
}
