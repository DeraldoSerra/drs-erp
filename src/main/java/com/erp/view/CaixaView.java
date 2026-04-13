package com.erp.view;

import com.erp.dao.CaixaDAO;
import com.erp.model.MovimentoCaixa;
import com.erp.model.SessaoCaixa;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import com.erp.util.Sessao;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CaixaView {

    private final CaixaDAO dao = new CaixaDAO();
    private SessaoCaixa sessaoAtual;
    private BorderPane root;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Region criar() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #1e2027;");
        root.setPadding(new Insets(24));
        carregarEstado();
        return root;
    }

    private void carregarEstado() {
        int lojaId = Sessao.getInstance().getLojaId();
        Optional<SessaoCaixa> opt = dao.getSessaoAberta(lojaId);
        if (opt.isPresent()) {
            sessaoAtual = opt.get();
            root.setCenter(criarPainelAberto());
        } else {
            sessaoAtual = null;
            root.setCenter(criarPainelFechado());
        }
    }

    // ===== PAINEL FECHADO (Abrir Caixa) =====

    private VBox criarPainelFechado() {
        VBox box = new VBox(24);
        box.setAlignment(Pos.CENTER);

        Label titulo = new Label("🏦 Controle de Caixa");
        titulo.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        // Status indicator
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);
        Circle dot = new Circle(6, Color.web("#fa5252"));
        Label statusLbl = new Label("Caixa Fechado");
        statusLbl.getStyleClass().add("caixa-status-fechado");
        statusRow.getChildren().addAll(dot, statusLbl);

        // Open card
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(480);
        card.setAlignment(Pos.CENTER);

        Label cardTitulo = new Label("🔓 Abrir Caixa");
        cardTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label lblValor = new Label("Valor de Abertura (R$)");
        lblValor.setStyle("-fx-text-fill: #9e9e9e;");

        TextField txtValorAbertura = new TextField("0,00");
        txtValorAbertura.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-background-color: #2a2d3e; "
                + "-fx-border-color: #3a3d4e; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 16;");
        txtValorAbertura.setMaxWidth(Double.MAX_VALUE);

        Button btnAbrir = new Button("🔓  ABRIR CAIXA");
        btnAbrir.setStyle("-fx-background-color: #40c057; -fx-text-fill: white; -fx-font-size: 16px; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 14 24; -fx-cursor: hand;");
        btnAbrir.setMaxWidth(Double.MAX_VALUE);
        btnAbrir.setOnAction(e -> {
            double valor = Formatador.parseMoeda(txtValorAbertura.getText());
            int usuarioId = Sessao.getInstance().getUsuario().getId();
            int lojaId = Sessao.getInstance().getLojaId();
            try {
                SessaoCaixa nova = dao.abrirCaixa(valor, usuarioId, lojaId);
                if (nova != null) {
                    Alerta.info("Caixa", "Caixa aberto com " + Formatador.formatarMoeda(valor) + " de abertura.");
                    carregarEstado();
                } else {
                    Alerta.erro("Erro", "Não foi possível abrir o caixa. Verifique o log do sistema.");
                }
            } catch (Exception ex) {
                Alerta.erro("Erro ao Abrir Caixa", ex.getMessage());
            }
        });

        card.getChildren().addAll(cardTitulo, lblValor, txtValorAbertura, btnAbrir);
        box.getChildren().addAll(titulo, statusRow, card);
        return box;
    }

    // ===== PAINEL ABERTO =====

    private VBox criarPainelAberto() {
        VBox box = new VBox(16);
        box.setStyle("-fx-background-color: #1e2027;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(6, Color.web("#40c057"));
        Label statusLbl = new Label("Caixa Aberto");
        statusLbl.getStyleClass().add("caixa-status-aberto");

        String aberturaTxt = sessaoAtual.getAbertura() != null
                ? "desde " + sessaoAtual.getAbertura().format(HH_MM)
                : "";
        Label lblInfo = new Label(aberturaTxt + " | Operador: " + nvl(sessaoAtual.getUsuarioNome()));
        lblInfo.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSangria = new Button("💸  Sangria");
        btnSangria.getStyleClass().add("btn-aviso");
        btnSangria.setOnAction(e -> abrirDialogoMovimento("SANGRIA"));

        Button btnSuprimento = new Button("💰  Suprimento");
        btnSuprimento.getStyleClass().add("btn-sucesso");
        btnSuprimento.setOnAction(e -> abrirDialogoMovimento("SUPRIMENTO"));

        Button btnFechar = new Button("🔒  FECHAR CAIXA");
        btnFechar.getStyleClass().add("btn-perigo");
        btnFechar.setOnAction(e -> abrirDialogoFechamento());

        header.getChildren().addAll(dot, statusLbl, lblInfo, spacer, btnSangria, btnSuprimento, btnFechar);

        // Totals row
        HBox totaisRow = criarTotaisRow();

        // Big total
        VBox totalBox = new VBox(4);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setStyle("-fx-background-color: #252836; -fx-background-radius: 12; -fx-padding: 16;");
        Label lblTotalLabel = new Label("TOTAL VENDAS");
        lblTotalLabel.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lblTotalVal = new Label(Formatador.formatarMoeda(sessaoAtual.getTotalVendas()));
        lblTotalVal.setStyle("-fx-text-fill: #40c057; -fx-font-size: 30px; -fx-font-weight: bold;");
        Label lblQtd = new Label(sessaoAtual.getQtdVendas() + " venda(s) finalizadas");
        lblQtd.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        totalBox.getChildren().addAll(lblTotalLabel, lblTotalVal, lblQtd);

        // Movements table
        VBox tabelaBox = criarTabelaMovimentos();
        VBox.setVgrow(tabelaBox, Priority.ALWAYS);

        box.getChildren().addAll(header, totaisRow, totalBox, tabelaBox);
        return box;
    }

    private HBox criarTotaisRow() {
        HBox row = new HBox(12);
        row.getChildren().addAll(
            totalCard("💵 Dinheiro",  Formatador.formatarMoeda(sessaoAtual.getTotalDinheiro()),  "card-verde"),
            totalCard("📱 PIX",       Formatador.formatarMoeda(sessaoAtual.getTotalPix()),       "card-azul"),
            totalCard("💳 Débito",    Formatador.formatarMoeda(sessaoAtual.getTotalDebito()),    "card-roxo"),
            totalCard("💳 Crédito",   Formatador.formatarMoeda(sessaoAtual.getTotalCredito()),   "card-roxo"),
            totalCard("💸 Sangrias",  Formatador.formatarMoeda(sessaoAtual.getTotalSangrias()),  "card-vermelho"),
            totalCard("💰 Suprimentos", Formatador.formatarMoeda(sessaoAtual.getTotalSuprimentos()), "card-azul")
        );
        for (var c : row.getChildren()) HBox.setHgrow(c, Priority.ALWAYS);
        return row;
    }

    private VBox totalCard(String titulo, String valor, String estilo) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("caixa-total-card", "card");
        if (!estilo.isEmpty()) card.getStyleClass().add(estilo);
        Label lTit = new Label(titulo);
        lTit.getStyleClass().add("card-titulo");
        Label lVal = new Label(valor);
        lVal.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        card.getChildren().addAll(lTit, lVal);
        return card;
    }

    @SuppressWarnings("unchecked")
    private VBox criarTabelaMovimentos() {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");

        Label titulo = new Label("📋 Movimentos do Caixa");
        titulo.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        TableView<MovimentoCaixa> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(260);

        TableColumn<MovimentoCaixa, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataHora() != null ? c.getValue().getDataHora().format(HH_MM) : ""));
        colHora.setPrefWidth(70);

        TableColumn<MovimentoCaixa, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo()));
        colTipo.setPrefWidth(130);

        TableColumn<MovimentoCaixa, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));

        TableColumn<MovimentoCaixa, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getValor())));
        colValor.setPrefWidth(120);

        tabela.getColumns().addAll(colHora, colTipo, colDesc, colValor);

        Task<List<MovimentoCaixa>> task = new Task<>() {
            @Override protected List<MovimentoCaixa> call() {
                return dao.listarMovimentos(sessaoAtual.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> tabela.getItems().setAll(task.getValue())));
        new Thread(task).start();

        box.getChildren().addAll(titulo, tabela);
        return box;
    }

    // ===== DIALOGS =====

    private void abrirDialogoMovimento(String tipo) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(tipo.equals("SANGRIA") ? "💸 Sangria" : "💰 Suprimento");
        dlg.setHeaderText(tipo.equals("SANGRIA")
                ? "Retirada de dinheiro do caixa"
                : "Entrada de dinheiro no caixa");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Motivo / Descrição");

        grid.add(new Label("Valor (R$):"), 0, 0);
        grid.add(txtValor, 1, 0);
        grid.add(new Label("Descrição:"), 0, 1);
        grid.add(txtDesc, 1, 1);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                double valor = Formatador.parseMoeda(txtValor.getText());
                if (valor <= 0) { Alerta.aviso("Atenção", "Informe um valor válido."); return; }
                String desc = txtDesc.getText().trim();
                int uid = Sessao.getInstance().getUsuario().getId();
                dao.registrarMovimento(sessaoAtual.getId(), tipo, valor, desc, uid);
                Alerta.info(tipo.equals("SANGRIA") ? "Sangria" : "Suprimento",
                        "Movimento de " + Formatador.formatarMoeda(valor) + " registrado.");
                carregarEstado();
            }
        });
    }

    private void abrirDialogoFechamento() {
        SessaoCaixa totais = dao.calcularTotais(sessaoAtual.getId());
        if (totais == null) totais = sessaoAtual;

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("🔒 Fechar Caixa");
        dlg.setHeaderText("Relatório de Fechamento");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().lookupButton(ButtonType.OK)
                .setStyle("-fx-background-color: #fa5252; -fx-text-fill: white;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        SessaoCaixa totaisFinal = totais;

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);

        int row = 0;
        grid.add(lblFechamento("Abertura:", Formatador.formatarMoeda(totaisFinal.getValorAbertura())), 0, row++);
        grid.add(lblFechamento("Total Vendas:", Formatador.formatarMoeda(totaisFinal.getTotalVendas())), 0, row++);
        grid.add(lblFechamento("  Dinheiro:", Formatador.formatarMoeda(totaisFinal.getTotalDinheiro())), 0, row++);
        grid.add(lblFechamento("  PIX:", Formatador.formatarMoeda(totaisFinal.getTotalPix())), 0, row++);
        grid.add(lblFechamento("  Débito:", Formatador.formatarMoeda(totaisFinal.getTotalDebito())), 0, row++);
        grid.add(lblFechamento("  Crédito:", Formatador.formatarMoeda(totaisFinal.getTotalCredito())), 0, row++);
        grid.add(lblFechamento("Suprimentos:", Formatador.formatarMoeda(totaisFinal.getTotalSuprimentos())), 0, row++);
        grid.add(lblFechamento("Sangrias:", "- " + Formatador.formatarMoeda(totaisFinal.getTotalSangrias())), 0, row++);
        grid.add(lblFechamento("Saldo Esperado:", Formatador.formatarMoeda(totaisFinal.getSaldoEsperado())), 0, row++);

        Separator sep = new Separator();

        Label lblContadoLabel = new Label("Valor Contado em Caixa (R$):");
        lblContadoLabel.setStyle("-fx-font-weight: bold;");
        TextField txtContado = new TextField(Formatador.formatarMoeda(totaisFinal.getSaldoEsperado()));

        Label lblDiferenca = new Label("Diferença: R$ 0,00");
        lblDiferenca.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        txtContado.textProperty().addListener((obs, ov, nv) -> {
            double contado = Formatador.parseMoeda(nv);
            double esperado = totaisFinal.getSaldoEsperado();
            double dif = contado - esperado;
            lblDiferenca.setText("Diferença: " + Formatador.formatarMoeda(dif));
            lblDiferenca.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                    + (dif >= 0 ? "#40c057" : "#fa5252") + ";");
        });

        Label lblObs = new Label("Observações:");
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(2);
        txtObs.setPromptText("Observações sobre o fechamento...");

        content.getChildren().addAll(grid, sep, lblContadoLabel, txtContado, lblDiferenca, lblObs, txtObs);
        dlg.getDialogPane().setContent(content);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (!Alerta.confirmar("Fechar Caixa", "Confirma o fechamento do caixa?")) return;
                double contado = Formatador.parseMoeda(txtContado.getText());
                String obs = txtObs.getText().trim();
                if (dao.fecharCaixa(sessaoAtual.getId(), contado, obs)) {
                    Alerta.info("Caixa", "Caixa fechado com sucesso!");
                    carregarEstado();
                } else {
                    Alerta.erro("Erro", "Não foi possível fechar o caixa.");
                }
            }
        });
    }

    private HBox lblFechamento(String label, String valor) {
        HBox row = new HBox(12);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #9e9e9e; -fx-min-width: 160;");
        Label val = new Label(valor);
        val.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
