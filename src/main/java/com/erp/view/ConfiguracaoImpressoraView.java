package com.erp.view;

import com.erp.dao.ConfiguracaoDAO;
import com.erp.util.Alerta;
import com.erp.util.ImpressoraService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Tela de configuração da impressora padrão para impressão de cupons/notas.
 * Acessível via menu CONFIGURAÇÕES → Impressora.
 */
public class ConfiguracaoImpressoraView {

    public VBox criar() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #13151f;");

        // ── Cabeçalho ──────────────────────────────────────────────────
        HBox header = new HBox();
        header.setPadding(new Insets(24, 32, 20, 32));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1a1c2a; -fx-border-color: #2a2d3e; -fx-border-width: 0 0 1 0;");

        Label titulo = new Label("🖨️  Configuração de Impressora");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.WHITE);

        header.getChildren().add(titulo);

        // ── Conteúdo ───────────────────────────────────────────────────
        VBox content = new VBox(24);
        content.setPadding(new Insets(32));
        content.setMaxWidth(640);
        VBox.setVgrow(content, Priority.ALWAYS);

        // Card de configuração
        VBox card = new VBox(20);
        card.setPadding(new Insets(24));
        card.setStyle("""
            -fx-background-color: #1a1c2a;
            -fx-background-radius: 10;
            -fx-border-color: #2a2d3e;
            -fx-border-radius: 10;
            -fx-border-width: 1;
            """);

        Label lblTitCard = new Label("Impressora Padrão para Cupons");
        lblTitCard.setFont(Font.font("System", FontWeight.BOLD, 15));
        lblTitCard.setTextFill(Color.WHITE);

        Label lblDesc = new Label(
            "Selecione a impressora que receberá os cupons de venda quando você clicar em \"Imprimir Cupom\".\n" +
            "Se nenhuma impressora for configurada, o sistema abrirá o PDF para impressão manual.");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #8b8fa8; -fx-font-size: 12px;");

        // Impressora atual
        ConfiguracaoDAO dao = new ConfiguracaoDAO();
        dao.criarTabelaSeNecessario();
        String impressoraAtual = dao.get(ImpressoraService.CHAVE_IMPRESSORA);

        Label lblAtual = new Label(impressoraAtual != null
            ? "✅ Configurada: " + impressoraAtual
            : "⚠️  Nenhuma impressora configurada");
        lblAtual.setStyle(impressoraAtual != null
            ? "-fx-text-fill: #51cf66; -fx-font-size: 12px; -fx-font-weight: bold;"
            : "-fx-text-fill: #fcc419; -fx-font-size: 12px;");
        lblAtual.setWrapText(true);

        // ComboBox com impressoras disponíveis
        Label lblCombo = new Label("Impressoras disponíveis:");
        lblCombo.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");

        ComboBox<String> cbImpressoras = new ComboBox<>();
        cbImpressoras.setMaxWidth(Double.MAX_VALUE);
        cbImpressoras.setStyle("-fx-background-color: #2a2d3e; -fx-text-fill: white;");
        cbImpressoras.setPromptText("Selecionar impressora...");

        // Botão atualizar lista
        Button btnAtualizar = new Button("🔄 Atualizar Lista");
        btnAtualizar.setStyle("-fx-background-color: #2a2d3e; -fx-text-fill: #74c0fc; -fx-cursor: hand;");

        Runnable carregarImpressoras = () -> {
            List<String> lista = ImpressoraService.listarImpressoras();
            cbImpressoras.getItems().setAll(lista);
            if (lista.isEmpty()) {
                cbImpressoras.setPromptText("Nenhuma impressora encontrada");
            } else {
                // Pré-seleciona a configurada (ou padrão do sistema)
                String cfg = dao.get(ImpressoraService.CHAVE_IMPRESSORA);
                if (cfg != null && lista.contains(cfg)) {
                    cbImpressoras.getSelectionModel().select(cfg);
                } else {
                    String padrao = ImpressoraService.impressoraPadrao();
                    if (padrao != null && lista.contains(padrao)) {
                        cbImpressoras.getSelectionModel().select(padrao);
                    }
                }
            }
        };

        carregarImpressoras.run();
        btnAtualizar.setOnAction(e -> carregarImpressoras.run());

        HBox comboRow = new HBox(10, cbImpressoras, btnAtualizar);
        HBox.setHgrow(cbImpressoras, Priority.ALWAYS);
        comboRow.setAlignment(Pos.CENTER_LEFT);

        // Botões de ação
        Button btnSalvar  = new Button("💾  Salvar Impressora");
        Button btnLimpar  = new Button("🗑️  Remover Configuração");
        Button btnTestar  = new Button("🧪  Testar Impressão");

        btnSalvar.setStyle("""
            -fx-background-color: #1971c2;
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;
            """);
        btnLimpar.setStyle("""
            -fx-background-color: #c92a2a;
            -fx-text-fill: white;
            -fx-padding: 10 16; -fx-background-radius: 6; -fx-cursor: hand;
            """);
        btnTestar.setStyle("""
            -fx-background-color: #2f9e44;
            -fx-text-fill: white;
            -fx-padding: 10 16; -fx-background-radius: 6; -fx-cursor: hand;
            """);

        btnSalvar.setOnAction(e -> {
            String sel = cbImpressoras.getSelectionModel().getSelectedItem();
            if (sel == null || sel.isBlank()) {
                Alerta.aviso("Atenção", "Selecione uma impressora na lista.");
                return;
            }
            dao.set(ImpressoraService.CHAVE_IMPRESSORA, sel);
            lblAtual.setText("✅ Configurada: " + sel);
            lblAtual.setStyle("-fx-text-fill: #51cf66; -fx-font-size: 12px; -fx-font-weight: bold;");
            Alerta.info("Impressora Salva",
                "Impressora configurada com sucesso!\n\nImpressora: " + sel +
                "\n\nAgora ao clicar em \"Imprimir Cupom\" o documento será enviado diretamente para essa impressora.");
        });

        btnLimpar.setOnAction(e -> {
            dao.remove(ImpressoraService.CHAVE_IMPRESSORA);
            cbImpressoras.getSelectionModel().clearSelection();
            lblAtual.setText("⚠️  Nenhuma impressora configurada");
            lblAtual.setStyle("-fx-text-fill: #fcc419; -fx-font-size: 12px;");
            Alerta.info("Configuração Removida", "A impressora configurada foi removida.\nO sistema voltará a abrir o PDF manualmente.");
        });

        btnTestar.setOnAction(e -> {
            String sel = cbImpressoras.getSelectionModel().getSelectedItem();
            if (sel == null || sel.isBlank()) {
                Alerta.aviso("Atenção", "Selecione uma impressora para testar.");
                return;
            }
            // Gera um PDF de teste simples e imprime
            try {
                String temp = gerarPDFTeste();
                if (temp == null) {
                    Alerta.erro("Erro", "Não foi possível gerar o PDF de teste.");
                    return;
                }
                boolean ok = ImpressoraService.imprimirPDF(temp, sel);
                if (ok) {
                    Alerta.info("Teste Enviado",
                        "✅ Página de teste enviada para:\n" + sel +
                        "\n\nSe nada sair, verifique se a impressora está online e aceitando PDF.");
                } else {
                    Alerta.aviso("Aviso",
                        "Não foi possível enviar para essa impressora via API Java.\n\n" +
                        "Tente outra impressora ou verifique se o driver está instalado corretamente.");
                }
            } catch (Exception ex) {
                Alerta.erro("Erro no Teste", ex.getMessage());
            }
        });

        HBox botoesRow = new HBox(10, btnSalvar, btnTestar, btnLimpar);
        botoesRow.setAlignment(Pos.CENTER_LEFT);

        // Dica
        Label lblDica = new Label(
            "💡 Dica: Para impressoras térmicas (bobina 58mm/80mm), configure-a como impressora padrão no Windows " +
            "e selecione-a aqui. O cupom será enviado diretamente sem abrir nenhum diálogo.");
        lblDica.setWrapText(true);
        lblDica.setStyle("""
            -fx-text-fill: #5a5d6e;
            -fx-font-size: 11.5px;
            -fx-padding: 10 12;
            -fx-background-color: #1e2027;
            -fx-background-radius: 6;
            """);

        card.getChildren().addAll(
            lblTitCard, lblDesc, lblAtual, lblCombo, comboRow, botoesRow, lblDica
        );

        content.getChildren().add(card);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #13151f; -fx-background: #13151f;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll);
        return root;
    }

    /** Gera um PDF de teste mínimo usando iText. */
    private String gerarPDFTeste() {
        try {
            java.io.File tmp = java.io.File.createTempFile("teste-impressao-", ".pdf");
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(tmp));
            doc.open();
            com.itextpdf.text.Font f = com.itextpdf.text.FontFactory.getFont(
                com.itextpdf.text.FontFactory.HELVETICA_BOLD, 14);
            doc.add(new com.itextpdf.text.Paragraph("🖨️  TESTE DE IMPRESSORA — DRS ERP", f));
            doc.add(new com.itextpdf.text.Paragraph(" "));
            doc.add(new com.itextpdf.text.Paragraph(
                "Se esta página chegou até aqui, a impressora está corretamente configurada!",
                com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 12)));
            doc.add(new com.itextpdf.text.Paragraph(" "));
            doc.add(new com.itextpdf.text.Paragraph(
                "Data/Hora: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            doc.close();
            return tmp.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
