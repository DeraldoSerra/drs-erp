package com.erp.view;

import com.erp.dao.NFeConfigDAO;
import com.erp.dao.NotaFiscalDAO;
import com.erp.model.NFeConfig;
import com.erp.model.NotaFiscal;
import com.erp.service.NFeResultado;
import com.erp.service.NFeService;
import com.erp.util.Alerta;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class NFeView {

    private final NotaFiscalDAO notaFiscalDAO = new NotaFiscalDAO();
    private final NFeConfigDAO configDAO = new NFeConfigDAO();
    private final NFeService nfeService = new NFeService();

    private TableView<NotaFiscal> tabela;
    private ObservableList<NotaFiscal> dados;
    private DatePicker dpInicio;
    private DatePicker dpFim;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Region criar() {
        VBox root = new VBox(0);
        root.getStyleClass().add("conteudo-area");

        // Cabeçalho
        HBox header = new HBox();
        header.setPadding(new Insets(24, 28, 16, 28));
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("🧾  Notas Fiscais Eletrônicas");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#e8eaf6"));
        header.getChildren().add(titulo);
        root.getChildren().add(header);

        // Filtros
        root.getChildren().add(criarFiltros());

        // Tabela
        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);
        root.getChildren().add(tabela);

        // Barra de ações
        root.getChildren().add(criarBarraAcoes());

        dados = FXCollections.observableArrayList();
        tabela.setItems(dados);
        pesquisar();
        return root;
    }

    private HBox criarFiltros() {
        dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpFim = new DatePicker(LocalDate.now());

        Button btnPesquisar = new Button("🔍  Pesquisar");
        btnPesquisar.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnPesquisar.setOnAction(e -> pesquisar());

        HBox filtros = new HBox(12,
                new Label("De:"), dpInicio,
                new Label("Até:"), dpFim,
                btnPesquisar
        );
        filtros.setPadding(new Insets(0, 28, 12, 28));
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getChildren().stream()
                .filter(n -> n instanceof Label)
                .forEach(n -> ((Label) n).setTextFill(Color.web("#a0a8c0")));
        return filtros;
    }

    @SuppressWarnings("unchecked")
    private TableView<NotaFiscal> criarTabela() {
        TableView<NotaFiscal> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<NotaFiscal, String> colNum = new TableColumn<>("Número");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNumero())));
        colNum.setPrefWidth(80);

        TableColumn<NotaFiscal, String> colSerie = new TableColumn<>("Série");
        colSerie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSerie()));
        colSerie.setPrefWidth(60);

        TableColumn<NotaFiscal, String> colData = new TableColumn<>("Emissão");
        colData.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataEmissao() != null ? c.getValue().getDataEmissao().format(FMT) : "-"));
        colData.setPrefWidth(130);

        TableColumn<NotaFiscal, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getClienteNome() != null ? c.getValue().getClienteNome() : "Consumidor Final"));
        colCliente.setPrefWidth(200);

        TableColumn<NotaFiscal, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("R$ %.2f", c.getValue().getTotalVenda()).replace(".", ",")));
        colTotal.setPrefWidth(100);

        TableColumn<NotaFiscal, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                String color = switch (status) {
                    case "AUTORIZADA" -> "#4caf50";
                    case "CANCELADA"  -> "#f44336";
                    case "DENEGADA"   -> "#ff9800";
                    case "ERRO"       -> "#e91e63";
                    default           -> "#ffeb3b";
                };
                setTextFill(Color.web(color));
                setStyle("-fx-font-weight: bold;");
            }
        });
        colStatus.setPrefWidth(110);

        TableColumn<NotaFiscal, String> colProtocolo = new TableColumn<>("Protocolo");
        colProtocolo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProtocolo() != null ? c.getValue().getProtocolo() : "-"));
        colProtocolo.setPrefWidth(140);

        TableColumn<NotaFiscal, String> colChave = new TableColumn<>("Chave de Acesso");
        colChave.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getChaveAcesso() != null ? c.getValue().getChaveAcesso() : "-"));
        colChave.setPrefWidth(350);

        tv.getColumns().addAll(colNum, colSerie, colData, colCliente, colTotal, colStatus, colProtocolo, colChave);
        return tv;
    }

    private HBox criarBarraAcoes() {
        Button btnVerXml = new Button("📄  Ver XML");
        Button btnCancelar = new Button("❌  Cancelar NF-e");
        Button btnSalvarXml = new Button("💾  Salvar XML");

        estilo(btnVerXml, "#3a3d4a", "#90caf9");
        estilo(btnCancelar, "#3a1c1c", "#f44336");
        estilo(btnSalvarXml, "#3a3d4a", "#a0a8c0");

        btnVerXml.setOnAction(e -> verXml());
        btnCancelar.setOnAction(e -> cancelarNFe());
        btnSalvarXml.setOnAction(e -> salvarXml());

        HBox barra = new HBox(10, btnVerXml, btnSalvarXml, btnCancelar);
        barra.setPadding(new Insets(12, 28, 18, 28));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setStyle("-fx-background-color: #1a1c24; -fx-border-color: #2a2d3a; -fx-border-width: 1 0 0 0;");
        return barra;
    }

    private void pesquisar() {
        if (dpInicio.getValue() == null || dpFim.getValue() == null) return;
        List<NotaFiscal> lista = notaFiscalDAO.listarPorPeriodo(dpInicio.getValue(), dpFim.getValue());
        dados = FXCollections.observableArrayList(lista);
        tabela.setItems(dados);
        tabela.refresh();
    }

    private void verXml() {
        NotaFiscal nf = tabela.getSelectionModel().getSelectedItem();
        if (nf == null) { Alerta.aviso("Atenção", "Selecione uma NF-e."); return; }
        String xml = nf.getXmlProcNfe() != null ? nf.getXmlProcNfe() : nf.getXmlNfe();
        if (xml == null || xml.isBlank()) { Alerta.info("XML", "XML não disponível para esta NF-e."); return; }

        Stage s = new Stage();
        s.setTitle("XML NF-e " + nf.getNumero());
        s.initModality(Modality.APPLICATION_MODAL);
        TextArea ta = new TextArea(formatarXml(xml));
        ta.setEditable(false);
        ta.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11;");
        VBox vb = new VBox(ta);
        VBox.setVgrow(ta, Priority.ALWAYS);
        s.setScene(new Scene(vb, 800, 600));
        s.show();
    }

    private void salvarXml() {
        NotaFiscal nf = tabela.getSelectionModel().getSelectedItem();
        if (nf == null) { Alerta.aviso("Atenção", "Selecione uma NF-e."); return; }
        String xml = nf.getXmlProcNfe() != null ? nf.getXmlProcNfe() : nf.getXmlNfe();
        if (xml == null || xml.isBlank()) { Alerta.aviso("Atenção", "XML não disponível."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Salvar XML NF-e");
        fc.setInitialFileName("NFe_" + nf.getChaveAcesso() + ".xml");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        File f = fc.showSaveDialog(tabela.getScene().getWindow());
        if (f == null) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(xml);
            Alerta.info("Sucesso", "XML salvo em:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            Alerta.erro("Erro", "Falha ao salvar XML: " + ex.getMessage());
        }
    }

    private void cancelarNFe() {
        NotaFiscal nf = tabela.getSelectionModel().getSelectedItem();
        if (nf == null) { Alerta.aviso("Atenção", "Selecione uma NF-e."); return; }
        if (!"AUTORIZADA".equals(nf.getStatus())) {
            Alerta.aviso("Atenção", "Apenas NF-e com status AUTORIZADA pode ser cancelada.");
            return;
        }

        // Solicitar justificativa
        Stage dlg = new Stage();
        dlg.setTitle("Cancelamento de NF-e");
        dlg.initModality(Modality.APPLICATION_MODAL);

        Label instrucao = new Label("Informe a justificativa para cancelamento (mínimo 15 caracteres):");
        instrucao.setTextFill(Color.web("#e8eaf6"));
        instrucao.setWrapText(true);

        TextArea taJust = new TextArea();
        taJust.setPromptText("Justificativa de cancelamento...");
        taJust.setWrapText(true);
        taJust.setPrefRowCount(4);

        Button btnConfirmar = new Button("Confirmar Cancelamento");
        Button btnCancelarDlg = new Button("Fechar");

        btnConfirmar.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 8 14; -fx-cursor: hand;");
        btnCancelarDlg.setStyle("-fx-background-color: #3a3d4a; -fx-text-fill: #a0a8c0; -fx-padding: 8 14; -fx-cursor: hand;");

        btnCancelarDlg.setOnAction(e -> dlg.close());
        btnConfirmar.setOnAction(e -> {
            String just = taJust.getText().trim();
            if (just.length() < 15) {
                Alerta.aviso("Atenção", "A justificativa deve ter no mínimo 15 caracteres.");
                return;
            }
            Optional<NFeConfig> cfg = configDAO.carregar();
            if (cfg.isEmpty()) {
                Alerta.erro("Erro", "Configuração NF-e não encontrada.");
                return;
            }
            dlg.close();
            new Thread(() -> {
                NFeResultado res = nfeService.cancelar(nf, just, cfg.get());
                Platform.runLater(() -> {
                    if (res.isAutorizada()) {
                        Alerta.info("Sucesso", "NF-e cancelada com sucesso.");
                    } else {
                        Alerta.erro("Erro no Cancelamento", res.getMensagem());
                    }
                    pesquisar();
                });
            }, "nfe-cancelamento").start();
        });

        HBox btns = new HBox(10, btnConfirmar, btnCancelarDlg);
        btns.setAlignment(Pos.CENTER_RIGHT);

        VBox vb = new VBox(12, instrucao, taJust, btns);
        vb.setPadding(new Insets(20));
        vb.setStyle("-fx-background-color: #1e2027;");
        dlg.setScene(new Scene(vb, 480, 220));
        dlg.show();
    }

    private String formatarXml(String xml) {
        // Indentação simples para visualização
        return xml.replaceAll("><", ">\n<");
    }

    private void estilo(Button b, String bg, String fg) {
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
    }
}
