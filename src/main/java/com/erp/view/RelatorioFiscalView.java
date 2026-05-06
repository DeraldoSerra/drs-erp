package com.erp.view;

import com.erp.dao.VendaDAO;
import com.erp.model.Venda;
import com.erp.service.LivroVendasXmlService;
import com.erp.service.SpedFiscalService;
import com.erp.util.Alerta;
import com.erp.util.RelatorioVendasPDF;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

public class RelatorioFiscalView {

    public Region criar() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("📊 Relatórios Fiscais");
        titulo.getStyleClass().add("titulo-modulo");
        Label subtitulo = new Label("Gere arquivos fiscais e relatórios do período");
        subtitulo.getStyleClass().add("subtitulo-modulo");

        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        card.setMaxWidth(640);

        // Date range
        GridPane dateGrid = new GridPane();
        dateGrid.setHgap(16);
        dateGrid.setVgap(8);

        Label lblInicio = new Label("Data Início:");
        lblInicio.setStyle("-fx-text-fill: #9e9e9e;");
        DatePicker dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpInicio.setMaxWidth(Double.MAX_VALUE);

        Label lblFim = new Label("Data Fim:");
        lblFim.setStyle("-fx-text-fill: #9e9e9e;");
        DatePicker dpFim = new DatePicker(LocalDate.now());
        dpFim.setMaxWidth(Double.MAX_VALUE);

        dateGrid.add(lblInicio, 0, 0); dateGrid.add(dpInicio, 1, 0);
        dateGrid.add(lblFim,    0, 1); dateGrid.add(dpFim, 1, 1);
        ColumnConstraints cl = new ColumnConstraints(120);
        ColumnConstraints cr = new ColumnConstraints(); cr.setHgrow(Priority.ALWAYS);
        dateGrid.getColumnConstraints().addAll(cl, cr);

        // Report type selection
        Label lblTipo = new Label("Tipo de Relatório:");
        lblTipo.setStyle("-fx-text-fill: #9e9e9e; -fx-font-weight: bold;");

        ToggleGroup grupo = new ToggleGroup();
        RadioButton rdSped = new RadioButton("📄  SPED Fiscal EFD (.txt)");
        rdSped.setToggleGroup(grupo);
        rdSped.setSelected(true);
        rdSped.setStyle("-fx-text-fill: #e0e0e0;");

        RadioButton rdLivro = new RadioButton("📋  Livro de Vendas XML (.xml)");
        rdLivro.setToggleGroup(grupo);
        rdLivro.setStyle("-fx-text-fill: #e0e0e0;");

        RadioButton rdPDF  = new RadioButton("📄  Relatório PDF - Resumo de Vendas");
        rdPDF.setToggleGroup(grupo);
        rdPDF.setStyle("-fx-text-fill: #e0e0e0;");

        VBox rdBox = new VBox(10, rdSped, rdLivro, rdPDF);
        rdBox.setPadding(new Insets(8, 0, 0, 0));

        // Progress indicator
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        Label lblStatus = new Label();
        lblStatus.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");

        // Generate button
        Button btnGerar = new Button("📄  Gerar Relatório");
        btnGerar.getStyleClass().add("btn-primario");
        btnGerar.setMaxWidth(Double.MAX_VALUE);
        btnGerar.setPrefHeight(48);
        btnGerar.setOnAction(e -> {
            LocalDate inicio = dpInicio.getValue();
            LocalDate fim = dpFim.getValue();
            if (inicio == null || fim == null) {
                Alerta.aviso("Atenção", "Selecione as datas de início e fim.");
                return;
            }
            if (inicio.isAfter(fim)) {
                Alerta.aviso("Atenção", "A data inicial não pode ser posterior à data final.");
                return;
            }

            boolean isPDF  = rdPDF.isSelected();
            boolean isSped = rdSped.isSelected();

            // ── OPÇÃO PDF ─────────────────────────────────────────────────────
            if (isPDF) {
                btnGerar.setDisable(true);
                progressBar.setVisible(true);
                progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                lblStatus.setText("Gerando PDF...");

                Task<String> taskPDF = new Task<>() {
                    @Override
                    protected String call() {
                        List<Venda> vendas = new VendaDAO().listarPorPeriodo(inicio, fim);
                        return RelatorioVendasPDF.gerar(inicio, fim, vendas);
                    }
                };
                taskPDF.setOnSucceeded(ev -> Platform.runLater(() -> {
                    btnGerar.setDisable(false);
                    progressBar.setVisible(false);
                    String caminho = taskPDF.getValue();
                    if (caminho != null) {
                        progressBar.setProgress(1.0);
                        lblStatus.setText("✅ PDF gerado: " + caminho);
                        lblStatus.setStyle("-fx-text-fill: #40c057;");
                        try { java.awt.Desktop.getDesktop().open(new java.io.File(caminho)); }
                        catch (Exception ex) { Alerta.info("PDF Gerado", "Arquivo salvo em:\n" + caminho); }
                    } else {
                        lblStatus.setText("❌ Erro ao gerar PDF");
                        lblStatus.setStyle("-fx-text-fill: #fa5252;");
                        Alerta.erro("Erro", "Não foi possível gerar o relatório PDF.");
                    }
                }));
                taskPDF.setOnFailed(ev -> Platform.runLater(() -> {
                    btnGerar.setDisable(false);
                    progressBar.setVisible(false);
                    lblStatus.setText("❌ Erro: " + taskPDF.getException().getMessage());
                    lblStatus.setStyle("-fx-text-fill: #fa5252;");
                    Alerta.erro("Erro", "Erro ao gerar PDF:\n" + taskPDF.getException().getMessage());
                }));
                new Thread(taskPDF).start();
                return;
            }

            // ── OPÇÕES SPED / XML ─────────────────────────────────────────────
            String ext  = isSped ? "*.txt" : "*.xml";
            String desc = isSped ? "Arquivo SPED EFD (*.txt)" : "Arquivo XML (*.xml)";

            FileChooser fc = new FileChooser();
            fc.setTitle("Salvar Relatório");
            fc.setInitialFileName(isSped ? "SPED_EFD.txt" : "LivroVendas.xml");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, ext));
            Stage stage = (Stage) btnGerar.getScene().getWindow();
            File destino = fc.showSaveDialog(stage);
            if (destino == null) return;

            btnGerar.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            lblStatus.setText("Gerando arquivo...");

            Task<File> task = new Task<>() {
                @Override
                protected File call() throws Exception {
                    if (isSped) {
                        return new SpedFiscalService().gerarEFD(inicio, fim);
                    } else {
                        return new LivroVendasXmlService().gerarXml(inicio, fim);
                    }
                }
            };

            task.setOnSucceeded(ev -> {
                try {
                    File gerado = task.getValue();
                    Files.copy(gerado.toPath(), destino.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    gerado.delete();
                    progressBar.setProgress(1.0);
                    lblStatus.setText("✅ Arquivo salvo: " + destino.getAbsolutePath());
                    lblStatus.setStyle("-fx-text-fill: #40c057;");
                    Alerta.info("Relatório Gerado", "Arquivo salvo em:\n" + destino.getAbsolutePath());
                } catch (Exception ex) {
                    lblStatus.setText("❌ Erro ao salvar: " + ex.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #fa5252;");
                } finally {
                    btnGerar.setDisable(false);
                    progressBar.setVisible(false);
                }
            });

            task.setOnFailed(ev -> {
                Throwable ex = task.getException();
                progressBar.setVisible(false);
                btnGerar.setDisable(false);
                lblStatus.setText("❌ Erro: " + ex.getMessage());
                lblStatus.setStyle("-fx-text-fill: #fa5252;");
                Alerta.erro("Erro", "Erro ao gerar relatório:\n" + ex.getMessage());
            });

            new Thread(task).start();
        });

        card.getChildren().addAll(dateGrid, new Separator(), lblTipo, rdBox,
                new Separator(), btnGerar, progressBar, lblStatus);

        box.getChildren().addAll(new VBox(4, titulo, subtitulo), card);
        return box;
    }
}
