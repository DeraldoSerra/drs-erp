package com.erp.view;

import com.erp.dao.FinanceiroDAO;
import com.erp.model.ContaPagar;
import com.erp.model.ContaReceber;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FinanceiroView {

    private final FinanceiroDAO dao = new FinanceiroDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("💰 Financeiro");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Controle de contas a pagar e a receber");
        sub.getStyleClass().add("subtitulo-modulo");

        // Cards resumo
        double totalPagar    = dao.totalPagarAberto();
        double totalReceber  = dao.totalReceberAberto();
        double saldo         = totalReceber - totalPagar;

        HBox cards = new HBox(16,
                cardFinanceiro("📤 Total a Pagar",   Formatador.formatarMoeda(totalPagar),   "#fa5252"),
                cardFinanceiro("📥 Total a Receber",  Formatador.formatarMoeda(totalReceber), "#40c057"),
                cardFinanceiro("📊 Saldo Previsto",   Formatador.formatarMoeda(saldo),        saldo >= 0 ? "#40c057" : "#fa5252")
        );

        // Abas
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Tab tabPagar   = new Tab("📤 Contas a Pagar",   criarAbaPagar());
        Tab tabReceber = new Tab("📥 Contas a Receber", criarAbaReceber());
        tabs.getTabs().addAll(tabPagar, tabReceber);

        root.getChildren().addAll(new VBox(4, titulo, sub), cards, tabs);
        return root;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaPagar() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        ComboBox<String> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(
                "TODAS", "ABERTA", "PAGA", "VENCIDA", "CANCELADA"));
        cmbStatus.setValue("ABERTA");

        DatePicker dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker dpFim    = new DatePicker(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.getStyleClass().add("btn-secundario");

        Button btnNova = new Button("+ Nova Conta a Pagar");
        btnNova.getStyleClass().add("btn-primario");

        HBox toolbar = new HBox(10, new Label("Status:"), cmbStatus,
                new Label("De:"), dpInicio, new Label("Até:"), dpFim,
                btnFiltrar, new Region(), btnNova);
        HBox.setHgrow(toolbar.getChildren().get(7), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TableView<ContaPagar> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<ContaPagar, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colDesc.setPrefWidth(250);

        TableColumn<ContaPagar, String> colForn = new TableColumn<>("Fornecedor");
        colForn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFornecedorNome() != null ? c.getValue().getFornecedorNome() : "-"));
        colForn.setPrefWidth(180);

        TableColumn<ContaPagar, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getValor())));
        colValor.setPrefWidth(110);

        TableColumn<ContaPagar, String> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataVencimento() != null ? c.getValue().getDataVencimento().format(FMT) : "-"));
        colVenc.setPrefWidth(110);

        TableColumn<ContaPagar, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setPrefWidth(90);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "PAGA"      -> "-fx-text-fill: #40c057; -fx-font-weight: bold;";
                    case "VENCIDA"   -> "-fx-text-fill: #fa5252; -fx-font-weight: bold;";
                    case "CANCELADA" -> "-fx-text-fill: #9e9e9e;";
                    default          -> "-fx-text-fill: #fd7e14; -fx-font-weight: bold;";
                });
            }
        });

        TableColumn<ContaPagar, Void> colAcao = new TableColumn<>("Pagar");
        colAcao.setPrefWidth(80);
        colAcao.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("✅ Pagar");
            {
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #40c05733; -fx-text-fill: #40c057; -fx-border-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    ContaPagar cp = getTableView().getItems().get(getIndex());
                    if ("PAGA".equals(cp.getStatus())) return;
                    if (dao.pagarConta(cp.getId(), cp.getValor(), "DINHEIRO", LocalDate.now())) {
                        Alerta.info("Pago", "Conta quitada com sucesso!");
                        tabela.setItems(FXCollections.observableArrayList(
                                dao.listarContasPagar(cmbStatus.getValue(), dpInicio.getValue(), dpFim.getValue())));
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tabela.getColumns().addAll(colDesc, colForn, colValor, colVenc, colStatus, colAcao);

        Runnable carregar = () -> tabela.setItems(FXCollections.observableArrayList(
                dao.listarContasPagar(cmbStatus.getValue(), dpInicio.getValue(), dpFim.getValue())));

        btnFiltrar.setOnAction(e -> carregar.run());
        btnNova.setOnAction(e -> abrirFormPagar(tabela, dpInicio, dpFim, cmbStatus));
        carregar.run();

        box.getChildren().addAll(toolbar, tabela);
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaReceber() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        ComboBox<String> cmbStatus = new ComboBox<>(FXCollections.observableArrayList(
                "TODAS", "ABERTA", "RECEBIDA", "VENCIDA", "CANCELADA"));
        cmbStatus.setValue("ABERTA");

        DatePicker dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker dpFim    = new DatePicker(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.getStyleClass().add("btn-secundario");
        Button btnNova = new Button("+ Nova Conta a Receber");
        btnNova.getStyleClass().add("btn-primario");

        HBox toolbar = new HBox(10, new Label("Status:"), cmbStatus,
                new Label("De:"), dpInicio, new Label("Até:"), dpFim,
                btnFiltrar, new Region(), btnNova);
        HBox.setHgrow(toolbar.getChildren().get(7), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TableView<ContaReceber> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<ContaReceber, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colDesc.setPrefWidth(250);

        TableColumn<ContaReceber, String> colCli = new TableColumn<>("Cliente");
        colCli.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getClienteNome() != null ? c.getValue().getClienteNome() : "-"));
        colCli.setPrefWidth(180);

        TableColumn<ContaReceber, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getValor())));
        colValor.setPrefWidth(110);

        TableColumn<ContaReceber, String> colVenc = new TableColumn<>("Vencimento");
        colVenc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataVencimento() != null ? c.getValue().getDataVencimento().format(FMT) : "-"));
        colVenc.setPrefWidth(110);

        TableColumn<ContaReceber, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setPrefWidth(100);

        TableColumn<ContaReceber, Void> colAcao = new TableColumn<>("Receber");
        colAcao.setPrefWidth(90);
        colAcao.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("✅ Receber");
            {
                btn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #40c05733; -fx-text-fill: #40c057; -fx-border-color: transparent; -fx-background-radius: 6; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    ContaReceber cr = getTableView().getItems().get(getIndex());
                    if ("RECEBIDA".equals(cr.getStatus())) return;
                    if (dao.receberConta(cr.getId(), cr.getValor(), "DINHEIRO", LocalDate.now())) {
                        Alerta.info("Recebido", "Conta recebida com sucesso!");
                        tabela.setItems(FXCollections.observableArrayList(
                                dao.listarContasReceber(cmbStatus.getValue(), dpInicio.getValue(), dpFim.getValue())));
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tabela.getColumns().addAll(colDesc, colCli, colValor, colVenc, colStatus, colAcao);

        Runnable carregar = () -> tabela.setItems(FXCollections.observableArrayList(
                dao.listarContasReceber(cmbStatus.getValue(), dpInicio.getValue(), dpFim.getValue())));

        btnFiltrar.setOnAction(e -> carregar.run());
        btnNova.setOnAction(e -> {
            // Form simplificado
            Stage dlg = new Stage();
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Nova Conta a Receber");
            GridPane form = new GridPane();
            form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(20));
            form.setStyle("-fx-background-color: #252836;");
            TextField desc = new TextField(); TextField valor = new TextField();
            DatePicker dpV = new DatePicker(LocalDate.now().plusDays(30));
            form.add(lbl("Descrição *"), 0, 0); form.add(desc, 1, 0);
            form.add(lbl("Valor *"), 0, 1); form.add(valor, 1, 1);
            form.add(lbl("Vencimento *"), 0, 2); form.add(dpV, 1, 2);
            Button btnS = new Button("💾 Salvar"); btnS.getStyleClass().add("btn-primario");
            btnS.setOnAction(ev -> {
                if (desc.getText().isBlank()) return;
                ContaReceber cr = new ContaReceber();
                cr.setDescricao(desc.getText()); cr.setValor(parseD(valor.getText()));
                cr.setDataVencimento(dpV.getValue());
                if (dao.salvarReceber(cr)) { carregar.run(); dlg.close(); }
            });
            form.add(btnS, 1, 3);
            Scene sc = new Scene(form, 440, 250);
            sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dlg.setScene(sc); dlg.showAndWait();
        });
        carregar.run();

        box.getChildren().addAll(toolbar, tabela);
        return box;
    }

    private void abrirFormPagar(TableView<ContaPagar> tabela, DatePicker dpI, DatePicker dpF, ComboBox<String> status) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Nova Conta a Pagar");
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #252836;");
        TextField desc = new TextField(); TextField valor = new TextField();
        DatePicker dpV = new DatePicker(LocalDate.now().plusDays(30));
        TextField cat = new TextField(); cat.setPromptText("Categoria (opcional)");
        form.add(lbl("Descrição *"), 0, 0); form.add(desc, 1, 0);
        form.add(lbl("Valor *"), 0, 1); form.add(valor, 1, 1);
        form.add(lbl("Vencimento *"), 0, 2); form.add(dpV, 1, 2);
        form.add(lbl("Categoria"), 0, 3); form.add(cat, 1, 3);
        Button btnS = new Button("💾 Salvar"); btnS.getStyleClass().add("btn-primario");
        btnS.setOnAction(ev -> {
            if (desc.getText().isBlank()) return;
            ContaPagar cp = new ContaPagar();
            cp.setDescricao(desc.getText()); cp.setValor(parseD(valor.getText()));
            cp.setDataVencimento(dpV.getValue()); cp.setCategoria(cat.getText());
            if (dao.salvarPagar(cp)) {
                tabela.setItems(FXCollections.observableArrayList(
                        dao.listarContasPagar(status.getValue(), dpI.getValue(), dpF.getValue())));
                dlg.close();
            }
        });
        form.add(btnS, 1, 4);
        Scene sc = new Scene(form, 440, 300);
        sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dlg.setScene(sc); dlg.showAndWait();
    }

    private VBox cardFinanceiro(String titulo, String valor, String cor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 24, 16, 24));
        card.setStyle("-fx-background-color: #252836; -fx-background-radius: 10; -fx-border-color: " + cor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 10;");
        Label lT = new Label(titulo); lT.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        Label lV = new Label(valor);  lV.setStyle("-fx-text-fill: " + cor + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        card.getChildren().addAll(lT, lV);
        return card;
    }

    private Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill: #9e9e9e;"); return l;
    }

    private double parseD(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }
}
