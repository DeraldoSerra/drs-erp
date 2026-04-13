package com.erp.view;

import com.erp.service.AtualizacaoService;
import com.erp.service.AtualizacaoService.Versao;
import com.erp.util.AppInfo;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

public class AtualizacaoView {

    /**
     * Verifica atualização em background thread e exibe diálogo se houver.
     * @param silencioso se true, não mostra nada quando está na última versão (uso automático)
     */
    public static void verificar(boolean silencioso) {
        Thread t = new Thread(() -> {
            Versao nova = AtualizacaoService.verificarAtualizacao();
            Platform.runLater(() -> {
                if (nova != null) {
                    mostrarDialogoAtualizacao(nova);
                } else if (!silencioso) {
                    mostrarNaUltimaVersao();
                }
            });
        }, "thread-atualizacao");
        t.setDaemon(true);
        t.start();
    }

    private static void mostrarDialogoAtualizacao(Versao nova) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Atualização Disponível");

        // Header
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(420);

        Label icone = new Label("🚀");
        icone.setStyle("-fx-font-size: 36px;");

        Label titulo = new Label("Nova versão disponível!");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4dabf7;");

        HBox versoes = new HBox(8);
        versoes.setAlignment(Pos.CENTER_LEFT);
        Label lblAtual = new Label("Versão atual:  " + AppInfo.getVersao());
        lblAtual.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 13px;");
        Label seta = new Label("  →");
        seta.setStyle("-fx-text-fill: #51cf66; -fx-font-size: 13px;");
        Label lblNova = new Label("v" + nova.versao());
        lblNova.setStyle("-fx-text-fill: #51cf66; -fx-font-size: 14px; -fx-font-weight: bold;");
        versoes.getChildren().addAll(lblAtual, seta, lblNova);

        Separator sep = new Separator();

        Label lblDescTitulo = new Label("📋 O que há de novo:");
        lblDescTitulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #dee2e6;");

        Label lblDesc = new Label(nova.descricao() != null && !nova.descricao().isBlank()
                ? nova.descricao() : "Melhorias e correções gerais.");
        lblDesc.setWrapText(true);
        lblDesc.setTextAlignment(TextAlignment.LEFT);
        lblDesc.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");

        if (nova.obrigatoria()) {
            Label aviso = new Label("⚠️  Esta atualização é OBRIGATÓRIA.");
            aviso.setStyle("-fx-text-fill: #ffa94d; -fx-font-weight: bold;");
            content.getChildren().addAll(icone, titulo, versoes, sep, lblDescTitulo, lblDesc, aviso);
        } else {
            content.getChildren().addAll(icone, titulo, versoes, sep, lblDescTitulo, lblDesc);
        }

        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getStylesheets().add(
            AtualizacaoView.class.getResource("/css/style.css").toExternalForm());

        ButtonType btnAtualizar = new ButtonType("✅  Atualizar agora", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnDepois    = new ButtonType("⏱  Lembrar depois",   ButtonBar.ButtonData.CANCEL_CLOSE);

        if (nova.obrigatoria()) {
            dlg.getDialogPane().getButtonTypes().add(btnAtualizar);
        } else {
            dlg.getDialogPane().getButtonTypes().addAll(btnAtualizar, btnDepois);
        }

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == btnAtualizar) {
                String url = nova.urlDownload();
                if (url != null && !url.isBlank()) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.INFORMATION,
                            "Acesse: " + url, ButtonType.OK).showAndWait();
                    }
                } else {
                    new Alert(Alert.AlertType.INFORMATION,
                        "Peça o instalador atualizado para o administrador do sistema.",
                        ButtonType.OK).showAndWait();
                }
            }
        });
    }

    private static void mostrarNaUltimaVersao() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Verificar Atualizações");
        alert.setHeaderText(null);
        alert.setContentText("✅  Você já está na versão mais recente!\n\nVersão atual: " + AppInfo.getVersaoCompleta());
        alert.showAndWait();
    }
}
