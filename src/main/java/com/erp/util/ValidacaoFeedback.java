package com.erp.util;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;

/**
 * Visual feedback helpers for validation fields.
 * Applies green/red borders to TextFields and creates status labels.
 */
public class ValidacaoFeedback {

    private static final String STYLE_VALIDO =
        "-fx-border-color: #00cc66; -fx-border-width: 1.5; -fx-border-radius: 4;";
    private static final String STYLE_INVALIDO =
        "-fx-border-color: #ff4444; -fx-border-width: 1.5; -fx-border-radius: 4;";

    /** Applies a green border and optional tooltip to the field. */
    public static void aplicarValido(TextField field, String tooltip) {
        String base = baseStyle(field);
        field.setStyle(base + STYLE_VALIDO);
        if (tooltip != null && !tooltip.isBlank()) {
            field.setTooltip(new Tooltip(tooltip));
        }
    }

    /** Applies a red border and shows tooltip with the error message. */
    public static void aplicarInvalido(TextField field, String mensagem) {
        String base = baseStyle(field);
        field.setStyle(base + STYLE_INVALIDO);
        if (mensagem != null && !mensagem.isBlank()) {
            field.setTooltip(new Tooltip("⚠ " + mensagem));
        }
    }

    /** Resets border and tooltip. */
    public static void limpar(TextField field) {
        String base = baseStyle(field);
        field.setStyle(base);
        field.setTooltip(null);
    }

    /**
     * Creates a small inline label with icon and message.
     * @param valido true → green ✔, false → red ✘
     */
    public static Label criarLabel(boolean valido, String mensagem) {
        Label lbl = new Label((valido ? "✔ " : "✘ ") + mensagem);
        lbl.setTextFill(valido ? Color.LIMEGREEN : Color.TOMATO);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        return lbl;
    }

    /** Strips previously injected border rules so they don't accumulate. */
    private static String baseStyle(TextField field) {
        String current = field.getStyle();
        if (current == null) return "";
        // Remove any previously injected border rules
        current = current.replaceAll("-fx-border-color\\s*:[^;]+;?", "")
                         .replaceAll("-fx-border-width\\s*:[^;]+;?", "")
                         .replaceAll("-fx-border-radius\\s*:[^;]+;?", "");
        return current.trim().isEmpty() ? "" : (current.trim() + " ");
    }
}
