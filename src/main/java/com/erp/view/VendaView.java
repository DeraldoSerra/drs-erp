package com.erp.view;

import com.erp.dao.ClienteDAO;
import com.erp.dao.ProdutoDAO;
import com.erp.dao.VendaDAO;
import com.erp.dao.CaixaDAO;
import com.erp.dao.UsuarioDAO;
import com.erp.model.*;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import com.erp.util.Sessao;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class VendaView {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VendaDAO vendaDAO     = new VendaDAO();
    private final CaixaDAO caixaDAO     = new CaixaDAO();

    private Venda vendaAtual;
    private TableView<ItemVenda> tabelaItens;
    private ObservableList<ItemVenda> itens;

    private Label lblTotal, lblTotalBar, lblSubtotal, lblDesconto, lblItemCount, lblTroco;
    private ComboBox<Cliente> cmbCliente;
    private TextField txtBusca, txtQtd, txtDesconto, txtValorPago;
    private RadioButton rdReais, rdPercent;
    private Button btnDinheiro, btnPix, btnDebito, btnCredito;
    private VBox trocoBox;

    public Region criar() {
        vendaAtual = new Venda();
        vendaAtual.setUsuarioId(Sessao.getInstance().getUsuario().getId());
        itens = FXCollections.observableArrayList();

        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color: #1e2027;");
        split.setDividerPositions(0.60);
        split.getItems().addAll(criarPainelEsquerdo(), criarPainelDireito());

        // Register keyboard shortcuts when added to scene
        split.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case F2 -> { txtBusca.requestFocus(); event.consume(); }
                        case F3 -> { cmbCliente.requestFocus(); event.consume(); }
                        case F9 -> { finalizarVenda(); event.consume(); }
                        case ESCAPE -> {
                            if (!itens.isEmpty()) {
                                String motivo = pedirCodigoCancelamentoComMotivo();
                                if (motivo != null) {
                                    registrarCarrinhoCancelado(motivo);
                                    novaVenda();
                                }
                            } else novaVenda();
                            event.consume();
                        }
                        case DELETE -> {
                            ItemVenda sel = tabelaItens.getSelectionModel().getSelectedItem();
                            if (sel != null) { itens.remove(sel); vendaAtual.removerItem(sel); atualizarTotais(); }
                            event.consume();
                        }
                        default -> {}
                    }
                });
                Platform.runLater(() -> txtBusca.requestFocus());
            }
        });
        return split;
    }

    // ===== LEFT PANEL =====

    private VBox criarPainelEsquerdo() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("🛒 FRENTE DE CAIXA");
        titulo.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label hints = new Label("  F2=Buscar  |  F3=Cliente  |  F9=Finalizar  |  ESC=Nova Venda  |  DEL=Remover Item");
        hints.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 11px; "
                + "-fx-background-color: #252836; -fx-background-radius: 6; -fx-padding: 6 12;");
        hints.setMaxWidth(Double.MAX_VALUE);

        HBox buscaRow = new HBox(8);
        buscaRow.setAlignment(Pos.CENTER_LEFT);

        txtBusca = new TextField();
        txtBusca.setPromptText("📦  Código de barras ou nome do produto...");
        txtBusca.getStyleClass().add("pdv-campo-codigo");
        HBox.setHgrow(txtBusca, Priority.ALWAYS);

        txtQtd = new TextField("1");
        txtQtd.setPromptText("Qtd");
        txtQtd.setPrefWidth(70);
        txtQtd.setStyle("-fx-background-color: #2a2d3e; -fx-text-fill: white; "
                + "-fx-border-color: #3a3d4e; -fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-padding: 12 10; -fx-font-size: 14px;");

        Button btnAdd = new Button("+ Adicionar");
        btnAdd.getStyleClass().add("btn-primario");
        btnAdd.setPrefHeight(44);
        buscaRow.getChildren().addAll(txtBusca, txtQtd, btnAdd);

        txtBusca.setOnAction(e -> adicionarProduto());
        btnAdd.setOnAction(e -> adicionarProduto());

        tabelaItens = criarTabelaItens();
        VBox.setVgrow(tabelaItens, Priority.ALWAYS);

        lblItemCount = new Label("0 itens | 0 unidades");
        lblItemCount.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 12px;");

        HBox totaisBar = new HBox(24);
        totaisBar.setAlignment(Pos.CENTER_RIGHT);
        totaisBar.setPadding(new Insets(14, 18, 14, 18));
        totaisBar.setStyle("-fx-background-color: #252836; -fx-background-radius: 10;");

        lblSubtotal = new Label("Subtotal: R$ 0,00");
        lblSubtotal.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
        lblDesconto = new Label("Desconto: R$ 0,00");
        lblDesconto.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 14px;");
        lblTotalBar = new Label("TOTAL: R$ 0,00");
        lblTotalBar.setStyle("-fx-text-fill: #40c057; -fx-font-size: 22px; -fx-font-weight: bold;");
        totaisBar.getChildren().addAll(lblSubtotal, lblDesconto, lblTotalBar);

        box.getChildren().addAll(titulo, hints, buscaRow, tabelaItens, lblItemCount, totaisBar);
        return box;
    }

    @SuppressWarnings("unchecked")
    private TableView<ItemVenda> criarTabelaItens() {
        TableView<ItemVenda> tv = new TableView<>();
        tv.setItems(itens);
        tv.setPlaceholder(new Label("Adicione produtos usando o campo acima"));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ItemVenda, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProdutoCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<ItemVenda, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProdutoNome()));

        TableColumn<ItemVenda, String> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getQuantidade())));
        colQtd.setPrefWidth(70);

        TableColumn<ItemVenda, String> colUnit = new TableColumn<>("Unit.");
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoUnit())));
        colUnit.setPrefWidth(100);

        TableColumn<ItemVenda, String> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getSubtotal())));
        colSub.setPrefWidth(110);

        TableColumn<ItemVenda, Void> colDel = new TableColumn<>("");
        colDel.setPrefWidth(50);
        colDel.setResizable(false);
        colDel.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #fa525233; -fx-text-fill: #fa5252; "
                        + "-fx-border-color: transparent; -fx-cursor: hand; -fx-background-radius: 4;");
                btn.setOnAction(e -> {
                    ItemVenda item = getTableView().getItems().get(getIndex());
                    itens.remove(item);
                    vendaAtual.removerItem(item);
                    atualizarTotais();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
        tv.getColumns().addAll(colCod, colNome, colQtd, colUnit, colSub, colDel);
        return tv;
    }

    // ===== RIGHT PANEL =====

    private VBox criarPainelDireito() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #252836;");

        // Customer
        Label lblClienteTitle = new Label("👤 Cliente");
        lblClienteTitle.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        cmbCliente = new ComboBox<>();
        cmbCliente.setPromptText("Consumidor Final");
        cmbCliente.setMaxWidth(Double.MAX_VALUE);
        cmbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));

        // Discount
        Label lblDescTitle = new Label("🏷 Desconto");
        lblDescTitle.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox rdBox = new HBox(16);
        rdBox.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup tgDesc = new ToggleGroup();
        rdReais = new RadioButton("R$");
        rdReais.setToggleGroup(tgDesc);
        rdReais.setSelected(true);
        rdReais.setStyle("-fx-text-fill: #e0e0e0;");
        rdPercent = new RadioButton("%");
        rdPercent.setToggleGroup(tgDesc);
        rdPercent.setStyle("-fx-text-fill: #e0e0e0;");
        rdBox.getChildren().addAll(rdReais, rdPercent);
        txtDesconto = new TextField("0");
        txtDesconto.setPromptText("0,00");
        txtDesconto.setMaxWidth(Double.MAX_VALUE);
        txtDesconto.textProperty().addListener((o, v, n) -> aplicarDesconto());
        tgDesc.selectedToggleProperty().addListener((o, v, n) -> aplicarDesconto());

        // Payment buttons
        Label lblPagTitle = new Label("💳 Forma de Pagamento");
        lblPagTitle.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        btnDinheiro = criarBtnPag("💵\nDINHEIRO");
        btnPix      = criarBtnPag("📱\nPIX");
        btnDebito   = criarBtnPag("💳\nDÉBITO");
        btnCredito  = criarBtnPag("💳\nCRÉDITO");
        HBox pagRow1 = new HBox(8);
        HBox pagRow2 = new HBox(8);
        HBox.setHgrow(btnDinheiro, Priority.ALWAYS);
        HBox.setHgrow(btnPix,      Priority.ALWAYS);
        HBox.setHgrow(btnDebito,   Priority.ALWAYS);
        HBox.setHgrow(btnCredito,  Priority.ALWAYS);
        pagRow1.getChildren().addAll(btnDinheiro, btnPix);
        pagRow2.getChildren().addAll(btnDebito, btnCredito);

        btnDinheiro.setOnAction(e -> selecionarPagamento("DINHEIRO",         btnDinheiro));
        btnPix     .setOnAction(e -> selecionarPagamento("PIX",              btnPix));
        btnDebito  .setOnAction(e -> selecionarPagamento("CARTÃO DÉBITO",    btnDebito));
        btnCredito .setOnAction(e -> selecionarPagamento("CARTÃO CRÉDITO",   btnCredito));

        // Troco section (only visible for DINHEIRO)
        trocoBox = new VBox(6);
        Label lblPagoTitle = new Label("Valor Recebido (R$)");
        lblPagoTitle.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        txtValorPago = new TextField();
        txtValorPago.setPromptText("0,00");
        txtValorPago.setMaxWidth(Double.MAX_VALUE);
        txtValorPago.textProperty().addListener((o, v, n) -> {
            vendaAtual.setValorPago(parseDouble(n));
            atualizarTroco();
        });
        lblTroco = new Label("Troco: R$ 0,00");
        lblTroco.getStyleClass().add("pdv-troco-label");
        lblTroco.setMaxWidth(Double.MAX_VALUE);
        lblTroco.setAlignment(Pos.CENTER);
        trocoBox.getChildren().addAll(lblPagoTitle, txtValorPago, lblTroco);

        // Total box
        VBox totalBox = new VBox(4);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setStyle("-fx-background-color: #1e2027; -fx-background-radius: 10; -fx-padding: 14;");
        Label lblTotalLabel = new Label("TOTAL A PAGAR");
        lblTotalLabel.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lblTotalDisplay = new Label("R$ 0,00");
        lblTotalDisplay.getStyleClass().add("pdv-total");
        // Re-assign lblTotal to the right panel's total label
        lblTotal = lblTotalDisplay;
        totalBox.getChildren().addAll(lblTotalLabel, lblTotalDisplay);

        // Buttons
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnFinalizar = new Button("✅  FINALIZAR  [F9]");
        btnFinalizar.setMaxWidth(Double.MAX_VALUE);
        btnFinalizar.setPrefHeight(58);
        btnFinalizar.setStyle("-fx-background-color: #2ea44f; -fx-text-fill: white; -fx-font-size: 16px; "
                + "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        btnFinalizar.setOnAction(e -> finalizarVenda());

        HBox btnRow = new HBox(8);
        Button btnNova    = new Button("🔄  Nova Venda");
        Button btnCancelar = new Button("❌  Cancelar");
        btnNova.getStyleClass().add("btn-secundario");
        btnCancelar.getStyleClass().add("btn-perigo");
        btnNova.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnNova, Priority.ALWAYS);
        HBox.setHgrow(btnCancelar, Priority.ALWAYS);
        btnRow.getChildren().addAll(btnNova, btnCancelar);

        btnNova.setOnAction(e -> {
            if (itens.isEmpty() || Alerta.confirmar("Nova Venda", "Deseja iniciar uma nova venda?"))
                novaVenda();
        });
        btnCancelar.setOnAction(e -> {
            String motivo = pedirCodigoCancelamentoComMotivo();
            if (motivo != null) {
                if (!itens.isEmpty()) registrarCarrinhoCancelado(motivo);
                novaVenda();
            }
        });

        Button btnCancelarVenda = new Button("🚫  Cancelar Venda Anterior");
        btnCancelarVenda.setMaxWidth(Double.MAX_VALUE);
        btnCancelarVenda.setStyle("-fx-background-color: #6f0000; -fx-text-fill: #ffcccc; "
                + "-fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 12;");
        btnCancelarVenda.setOnAction(e -> abrirDialogoCancelarVenda());

        Button btnConsultarVendas = new Button("📋  Consultar Vendas do Dia");
        btnConsultarVendas.setMaxWidth(Double.MAX_VALUE);
        btnConsultarVendas.setStyle("-fx-background-color: #1864ab; -fx-text-fill: #d0ebff; "
                + "-fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 12;");
        btnConsultarVendas.setOnAction(e -> abrirConsultaVendas());

        selecionarPagamento("DINHEIRO", btnDinheiro);

        box.getChildren().addAll(
                lblClienteTitle, cmbCliente,
                new Separator(),
                lblDescTitle, rdBox, txtDesconto,
                new Separator(),
                lblPagTitle, pagRow1, pagRow2,
                trocoBox, totalBox, spacer,
                btnFinalizar, btnRow, btnCancelarVenda, btnConsultarVendas
        );
        return box;
    }

    private Button criarBtnPag(String texto) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("btn-pdv-pagamento");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(80);
        btn.setWrapText(true);
        btn.setAlignment(Pos.CENTER);
        return btn;
    }

    private void selecionarPagamento(String forma, Button btn) {
        vendaAtual.setFormaPagamento(forma);
        for (Button b : new Button[]{btnDinheiro, btnPix, btnDebito, btnCredito}) {
            b.getStyleClass().remove("btn-pdv-pagamento-ativo");
        }
        btn.getStyleClass().add("btn-pdv-pagamento-ativo");
        boolean isDinheiro = "DINHEIRO".equals(forma);
        trocoBox.setVisible(isDinheiro);
        trocoBox.setManaged(isDinheiro);
        if (!isDinheiro) vendaAtual.setValorPago(vendaAtual.getTotal());
    }

    // ===== LOGIC =====

    private void adicionarProduto() {
        String codigo = txtBusca.getText().trim();
        if (codigo.isBlank()) return;
        double qtd = parseDouble(txtQtd.getText());
        if (qtd <= 0) qtd = 1;

        java.util.Optional<Produto> opt = produtoDAO.buscarPorCodigoBarras(codigo);
        if (opt.isEmpty()) {
            List<Produto> lista = produtoDAO.listarPorFiltro(codigo, true);
            if (lista.isEmpty()) { Alerta.aviso("Produto", "Produto não encontrado: " + codigo); return; }
            if (lista.size() == 1) {
                opt = java.util.Optional.of(lista.get(0));
            } else {
                ChoiceDialog<Produto> dlg = new ChoiceDialog<>(lista.get(0), lista);
                dlg.setTitle("Selecionar Produto");
                dlg.setHeaderText("Vários produtos encontrados:");
                dlg.setContentText("Produto:");
                opt = dlg.showAndWait();
            }
        }
        if (opt.isEmpty()) return;
        Produto produto = opt.get();

        for (ItemVenda existente : itens) {
            if (existente.getProdutoId() == produto.getId()) {
                existente.setQuantidade(existente.getQuantidade() + qtd);
                tabelaItens.refresh();
                vendaAtual.recalcular();
                atualizarTotais();
                txtBusca.clear();
                txtBusca.requestFocus();
                return;
            }
        }

        ItemVenda novoItem = new ItemVenda(produto, qtd);
        itens.add(novoItem);
        vendaAtual.adicionarItem(novoItem);
        atualizarTotais();
        txtBusca.clear();
        txtQtd.setText("1");
        txtBusca.requestFocus();
    }

    private void aplicarDesconto() {
        try {
            double val = parseDouble(txtDesconto.getText());
            if (rdPercent.isSelected()) {
                vendaAtual.setDesconto(vendaAtual.getSubtotal() * val / 100.0);
            } else {
                vendaAtual.setDesconto(val);
            }
            atualizarTotais();
        } catch (Exception ex) { /* ignore bad input */ }
    }

    private void atualizarTotais() {
        lblSubtotal.setText("Subtotal: " + Formatador.formatarMoeda(vendaAtual.getSubtotal()));
        lblDesconto.setText("Desconto: " + Formatador.formatarMoeda(vendaAtual.getDesconto()));
        String totalFmt = Formatador.formatarMoeda(vendaAtual.getTotal());
        lblTotal.setText(totalFmt);
        if (lblTotalBar != null) lblTotalBar.setText("TOTAL: " + totalFmt);
        int qtdItens = itens.size();
        double qtdUn  = itens.stream().mapToDouble(ItemVenda::getQuantidade).sum();
        lblItemCount.setText(qtdItens + (qtdItens == 1 ? " item" : " itens") + " | "
                + Formatador.formatarQuantidade(qtdUn) + " unidades");
        atualizarTroco();
    }

    private void atualizarTroco() {
        lblTroco.setText("Troco: " + Formatador.formatarMoeda(vendaAtual.getTroco()));
    }

    private void finalizarVenda() {
        if (itens.isEmpty()) { Alerta.aviso("Atenção", "Adicione pelo menos um produto à venda."); return; }
        if (cmbCliente.getValue() != null) {
            vendaAtual.setClienteId(cmbCliente.getValue().getId());
            vendaAtual.setClienteNome(cmbCliente.getValue().getNome());
        }
        if ("DINHEIRO".equals(vendaAtual.getFormaPagamento())) {
            double pago = parseDouble(txtValorPago.getText());
            if (pago < vendaAtual.getTotal()) { Alerta.aviso("Pagamento", "Valor pago menor que o total."); return; }
            vendaAtual.setValorPago(pago);
        } else {
            vendaAtual.setValorPago(vendaAtual.getTotal());
        }
        vendaAtual.setStatus("FINALIZADA");
        vendaAtual.setDataVenda(java.time.LocalDateTime.now()); // garante data/hora da finalização
        if (vendaDAO.salvar(vendaAtual)) {
            // Registrar no caixa se houver sessão aberta
            int lojaId = Sessao.getInstance().getLojaId();
            caixaDAO.getSessaoAberta(lojaId).ifPresentOrElse(sessao -> {
                String forma = vendaAtual.getFormaPagamento();
                String tipo = switch (forma != null ? forma.toUpperCase() : "") {
                    case "PIX" -> "VENDA_PIX";
                    case "CARTÃO DÉBITO" -> "VENDA_DEBITO";
                    case "CARTÃO CRÉDITO" -> "VENDA_CREDITO";
                    default -> "VENDA_DINHEIRO";
                };
                caixaDAO.registrarMovimento(sessao.getId(), tipo, vendaAtual.getTotal(),
                        "Venda #" + vendaAtual.getNumero(), Sessao.getInstance().getUsuario().getId());
            }, () -> {
                // Sem caixa aberto — apenas avisa mas não bloqueia
                Alerta.aviso("Caixa", "Nenhum caixa aberto. A venda foi registrada, mas não foi contabilizada no caixa.");
            });
            Venda vendaSalva = vendaAtual;
            new NotaFiscalView(vendaSalva).mostrar();
            novaVenda();
        } else {
            Alerta.erro("Erro", "Não foi possível finalizar a venda.");
        }
    }

    private void abrirConsultaVendas() {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("📋 Consultar Vendas — " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.setWidth(1400);
        stage.setHeight(660);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #1e2027;");

        // ── Filtros ──
        DatePicker dpData = new DatePicker(java.time.LocalDate.now());
        TextField txtBuscaNum = new TextField();
        txtBuscaNum.setPromptText("Buscar por nº venda...");
        txtBuscaNum.setPrefWidth(180);
        ComboBox<String> cmbStatus = new ComboBox<>();
        cmbStatus.getItems().addAll("Todos", "FINALIZADA", "CANCELADA");
        cmbStatus.setValue("Todos");
        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.getStyleClass().add("btn-primario");
        Label lblInfo = new Label("Carregando...");
        lblInfo.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 11px;");
        HBox filtrosBox = new HBox(10, new Label("Data:") {{ setStyle("-fx-text-fill: #dee2e6;"); }},
                dpData, txtBuscaNum, new Label("Status:") {{ setStyle("-fx-text-fill: #dee2e6;"); }},
                cmbStatus, btnFiltrar, lblInfo);
        filtrosBox.setAlignment(Pos.CENTER_LEFT);

        // ── Tabela ──
        TableView<Venda> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        tabela.setStyle("-fx-background-color: #252836;");

        TableColumn<Venda, String> cNum = new TableColumn<>("Nº Venda");
        cNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        cNum.setPrefWidth(110);

        TableColumn<Venda, String> cData = new TableColumn<>("Data/Hora");
        cData.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataVenda() != null
                        ? c.getValue().getDataVenda().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : ""));
        cData.setPrefWidth(130);

        TableColumn<Venda, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        cStatus.setPrefWidth(100);
        cStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("CANCELADA".equals(item)
                        ? "-fx-text-fill: #fa5252; -fx-font-weight: bold;"
                        : "-fx-text-fill: #51cf66; -fx-font-weight: bold;");
            }
        });

        TableColumn<Venda, String> cPag = new TableColumn<>("Pagamento");
        cPag.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormaPagamento()));
        cPag.setPrefWidth(100);

        TableColumn<Venda, String> cSubtotal = new TableColumn<>("Subtotal");
        cSubtotal.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getSubtotal())));
        cSubtotal.setPrefWidth(90);

        TableColumn<Venda, String> cDesconto = new TableColumn<>("Desconto");
        cDesconto.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getDesconto())));
        cDesconto.setPrefWidth(90);

        TableColumn<Venda, String> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getTotal())));
        cTotal.setPrefWidth(90);

        TableColumn<Venda, String> cRecebido = new TableColumn<>("Recebido");
        cRecebido.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getValorPago())));
        cRecebido.setPrefWidth(90);

        TableColumn<Venda, String> cTroco = new TableColumn<>("Troco");
        cTroco.setCellValueFactory(c -> new SimpleStringProperty(Formatador.formatarMoeda(c.getValue().getTroco())));
        cTroco.setPrefWidth(90);

        TableColumn<Venda, String> cReembolsado = new TableColumn<>("Reembolsado");
        cReembolsado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isReembolsado() ? "✅ Sim" : "—"));
        cReembolsado.setPrefWidth(100);
        cReembolsado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.startsWith("✅") ? "-fx-text-fill: #51cf66; -fx-font-weight: bold;" : "-fx-text-fill: #adb5bd;");
            }
        });

        TableColumn<Venda, String> cMotivo = new TableColumn<>("Motivo/Obs");
        cMotivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getObservacoes() != null ? c.getValue().getObservacoes() : ""));
        cMotivo.setPrefWidth(200);

        tabela.getColumns().addAll(cNum, cData, cStatus, cPag, cSubtotal, cDesconto, cTotal, cRecebido, cTroco, cReembolsado, cMotivo);

        // ── Botões de ação ──
        Button btnCancelarSel  = new Button("🚫 Cancelar Venda Selecionada");
        btnCancelarSel.setStyle("-fx-background-color: #9b2335; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        Button btnReembolso    = new Button("💸 Emitir Comprovante de Reembolso");
        btnReembolso.setStyle("-fx-background-color: #1864ab; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        Button btnFechar       = new Button("✖ Fechar");
        btnFechar.setStyle("-fx-background-color: #495057; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");

        HBox acoesBox = new HBox(10, btnCancelarSel, btnReembolso, new javafx.scene.layout.Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, btnFechar);
        acoesBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(filtrosBox, tabela, new Separator(), acoesBox);

        // ── Lógica de carregamento ──
        Runnable carregar = () -> {
            java.time.LocalDate data = dpData.getValue() != null ? dpData.getValue() : java.time.LocalDate.now();
            java.util.List<Venda> lista = vendaDAO.listarPorPeriodo(data, data);
            String filtroNum = txtBuscaNum.getText().trim().toLowerCase();
            String filtroStatus = cmbStatus.getValue();
            java.util.List<Venda> filtradas = lista.stream()
                    .filter(v -> filtroNum.isBlank() || v.getNumero().toLowerCase().contains(filtroNum))
                    .filter(v -> "Todos".equals(filtroStatus) || filtroStatus.equals(v.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            tabela.setItems(FXCollections.observableArrayList(filtradas));
            long total = filtradas.stream().filter(v -> "FINALIZADA".equals(v.getStatus())).count();
            long canc  = filtradas.stream().filter(v -> "CANCELADA".equals(v.getStatus())).count();
            lblInfo.setText(total + " finalizada(s) | " + canc + " cancelada(s)");
        };

        btnFiltrar.setOnAction(e -> carregar.run());
        txtBuscaNum.setOnAction(e -> carregar.run());
        Platform.runLater(carregar::run);

        // ── Cancelar venda selecionada ──
        btnCancelarSel.setOnAction(e -> {
            Venda sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma venda na lista."); return; }
            if ("CANCELADA".equals(sel.getStatus())) { Alerta.aviso("Atenção", "Esta venda já está cancelada."); return; }
            String motivo = pedirCodigoCancelamentoComMotivo();
            if (motivo == null) return;
            int uid = Sessao.getInstance().getUsuario().getId();
            if (vendaDAO.cancelar(sel.getId(), uid, motivo)) {
                Alerta.info("✅ Cancelado", "Venda #" + sel.getNumero() + " cancelada.\nMotivo: " + motivo);
                carregar.run();
            } else {
                Alerta.erro("Erro", "Não foi possível cancelar a venda.");
            }
        });

        // ── Reembolso ──
        btnReembolso.setOnAction(e -> {
            Venda sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma venda cancelada para emitir o reembolso."); return; }
            if (!"CANCELADA".equals(sel.getStatus())) { Alerta.aviso("Atenção", "Só é possível emitir reembolso para vendas CANCELADAS."); return; }
            // Buscar com itens completos
            Venda completa = vendaDAO.buscarPorNumero(sel.getNumero()).orElse(sel);
            btnReembolso.setDisable(true);
            btnReembolso.setText("⏳ Gerando...");
            javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
                @Override protected String call() {
                    return com.erp.util.RelatorioReembolsoPDF.gerar(completa);
                }
            };
            task.setOnSucceeded(ev -> Platform.runLater(() -> {
                btnReembolso.setDisable(false);
                btnReembolso.setText("💸 Emitir Comprovante de Reembolso");
                String path = task.getValue();
                if (path != null) {
                    vendaDAO.marcarReembolsado(completa.getId());
                    carregar.run();
                    try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                    catch (Exception ex) { Alerta.info("PDF", "Salvo em:\n" + path); }
                } else Alerta.erro("Erro", "Não foi possível gerar o PDF.");
            }));
            task.setOnFailed(ev -> Platform.runLater(() -> {
                btnReembolso.setDisable(false);
                btnReembolso.setText("💸 Emitir Comprovante de Reembolso");
                Alerta.erro("Erro", task.getException().getMessage());
            }));
            new Thread(task).start();
        });

        btnFechar.setOnAction(e -> stage.close());

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    private void abrirDialogoCancelarVenda() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("🚫 Cancelar Venda");
        dlg.setHeaderText("Cancelamento requer autorização de supervisor");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("Confirmar Cancelamento");
        btnOk.setStyle("-fx-background-color: #fa5252; -fx-text-fill: white;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setPadding(new javafx.geometry.Insets(16));
        content.setPrefWidth(500);

        // ── Seção: Busca da venda ─────────────────────────────────────────────
        Label lblNumero = new Label("Número da Venda:");
        TextField txtNumero = new TextField();
        txtNumero.setPromptText("Ex: 00000042");
        Button btnBuscar = new Button("🔍 Buscar");
        btnBuscar.getStyleClass().add("btn-primario");
        javafx.scene.layout.HBox buscaRow = new javafx.scene.layout.HBox(8, txtNumero, btnBuscar);
        javafx.scene.layout.HBox.setHgrow(txtNumero, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.TextArea txtDetalhes = new javafx.scene.control.TextArea();
        txtDetalhes.setEditable(false);
        txtDetalhes.setPrefRowCount(5);
        txtDetalhes.setPromptText("Detalhes da venda aparecerão aqui após a busca...");

        // ── Seção: Motivo do cancelamento ─────────────────────────────────────
        Separator sep1 = new Separator();
        Label lblMotivo = new Label("Motivo do Cancelamento (obrigatório):");
        lblMotivo.setStyle("-fx-font-weight: bold;");
        javafx.scene.control.TextArea txtMotivo = new javafx.scene.control.TextArea();
        txtMotivo.setPromptText("Descreva o motivo do cancelamento...");
        txtMotivo.setPrefRowCount(3);
        txtMotivo.setWrapText(true);

        // ── Seção: Código de cancelamento ────────────────────────────────────
        Separator sep2 = new Separator();
        Label lblCodigo = new Label("🔐 Código de Cancelamento:");
        lblCodigo.setStyle("-fx-font-weight: bold;");
        PasswordField txtCodigo = new PasswordField();
        txtCodigo.setPromptText("Digite o código de cancelamento");
        txtCodigo.setMaxWidth(280);

        final com.erp.model.Venda[] vendaEncontrada = {null};

        btnBuscar.setOnAction(e -> {
            String num = txtNumero.getText().trim();
            if (num.isBlank()) { Alerta.aviso("Atenção", "Informe o número da venda."); return; }
            java.util.Optional<com.erp.model.Venda> opt = vendaDAO.buscarPorNumero(num);
            if (opt.isEmpty()) {
                txtDetalhes.setText("⚠ Venda não encontrada: " + num);
                vendaEncontrada[0] = null;
            } else {
                com.erp.model.Venda v = opt.get();
                vendaEncontrada[0] = v;
                StringBuilder sb = new StringBuilder();
                sb.append("Nº: ").append(v.getNumero()).append("\n");
                if (v.getDataVenda() != null)
                    sb.append("Data: ").append(v.getDataVenda().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
                sb.append("Status: ").append(v.getStatus()).append("\n");
                if (v.getClienteNome() != null && !v.getClienteNome().isBlank())
                    sb.append("Cliente: ").append(v.getClienteNome()).append("\n");
                sb.append("Total: ").append(Formatador.formatarMoeda(v.getTotal())).append("\n");
                sb.append("Pagamento: ").append(v.getFormaPagamento()).append("\n");
                if (v.getItens() != null && !v.getItens().isEmpty()) {
                    sb.append("Itens:\n");
                    for (com.erp.model.ItemVenda item : v.getItens()) {
                        sb.append("  - ").append(item.getProdutoNome())
                          .append(" x").append(Formatador.formatarQuantidade(item.getQuantidade()))
                          .append(" = ").append(Formatador.formatarMoeda(item.getSubtotal())).append("\n");
                    }
                }
                txtDetalhes.setText(sb.toString());
            }
        });

        content.getChildren().addAll(
                lblNumero, buscaRow, txtDetalhes,
                sep1,
                lblMotivo, txtMotivo,
                sep2,
                lblCodigo, txtCodigo
        );
        dlg.getDialogPane().setContent(content);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            // 1. Validar venda encontrada
            if (vendaEncontrada[0] == null) { Alerta.aviso("Atenção", "Nenhuma venda selecionada."); return; }
            com.erp.model.Venda v = vendaEncontrada[0];
            if ("CANCELADA".equals(v.getStatus())) { Alerta.aviso("Atenção", "Esta venda já está cancelada."); return; }

            // 2. Validar motivo
            String motivo = txtMotivo.getText().trim();
            if (motivo.isBlank()) { Alerta.aviso("Atenção", "Informe o motivo do cancelamento."); return; }

            // 3. Validar código de cancelamento
            String codigoDigitado = txtCodigo.getText();
            String codigoCorreto = new com.erp.dao.ConfiguracaoDAO().get("codigo_cancelamento", "53332");
            if (!codigoCorreto.equals(codigoDigitado)) {
                Alerta.erro("Código Inválido", "Código de cancelamento incorreto.\nOperação negada.");
                return;
            }

            // 4. Confirmar e cancelar
            if (!Alerta.confirmar("Cancelar Venda", "Confirma o cancelamento da Venda #" + v.getNumero()
                    + "\nTotal: " + Formatador.formatarMoeda(v.getTotal())
                    + "\nMotivo: " + motivo
                    + "\n\nO estoque será estornado automaticamente.")) return;

            int uid = Sessao.getInstance().getUsuario().getId();
            if (vendaDAO.cancelar(v.getId(), uid, motivo)) {
                Alerta.info("Cancelamento", "Venda #" + v.getNumero() + " cancelada com sucesso.\nEstoque estornado.\nMotivo registrado.");
            } else {
                Alerta.erro("Erro", "Não foi possível cancelar a venda.");
            }
        });
    }

    /** Pede código + motivo. Retorna o motivo se válido, null se cancelado/inválido. */
    private String pedirCodigoCancelamentoComMotivo() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("🔐 Cancelar Venda");
        dlg.setHeaderText("Informe o código e o motivo do cancelamento");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        PasswordField pf = new PasswordField();
        pf.setPromptText("Código de cancelamento");
        javafx.scene.control.TextArea txtMotivo = new javafx.scene.control.TextArea();
        txtMotivo.setPromptText("Motivo do cancelamento (obrigatório)...");
        txtMotivo.setPrefRowCount(3);
        txtMotivo.setWrapText(true);
        VBox box = new VBox(8,
            new Label("Código:"), pf,
            new Label("Motivo:"), txtMotivo);
        box.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(box);
        try { dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
        java.util.Optional<ButtonType> result = dlg.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return null;
        String codigoCorreto = new com.erp.dao.ConfiguracaoDAO().get("codigo_cancelamento", "53332");
        if (!codigoCorreto.equals(pf.getText())) {
            Alerta.erro("Código Inválido", "Código de cancelamento incorreto.");
            return null;
        }
        String motivo = txtMotivo.getText().trim();
        if (motivo.isBlank()) {
            Alerta.aviso("Atenção", "Informe o motivo do cancelamento.");
            return null;
        }
        return motivo;
    }

    /** Salva o carrinho atual como venda CANCELADA no banco (para registro). */
    private void registrarCarrinhoCancelado(String motivo) {
        if (itens.isEmpty()) return;
        try {
            Venda v = vendaAtual;
            v.setStatus("CANCELADA");
            v.setObservacoes(motivo);
            if (v.getFormaPagamento() == null || v.getFormaPagamento().isBlank())
                v.setFormaPagamento("CANCELADO");
            // Calcular totais se não calculados
            double sub = itens.stream().mapToDouble(i -> i.getPrecoUnit() * i.getQuantidade()).sum();
            if (v.getSubtotal() == 0) v.setSubtotal(sub);
            if (v.getTotal() == 0) v.setTotal(sub - v.getDesconto() + v.getAcrescimo());
            vendaDAO.salvar(v);
        } catch (Exception e) {
            // log silencioso — não bloquear o cancelamento
        }
    }

    private boolean pedirCodigoCancelamento() {
        String m = pedirCodigoCancelamentoComMotivo();
        return m != null;
    }

    private void novaVenda() {
        itens.clear();
        vendaAtual = new Venda();
        vendaAtual.setUsuarioId(Sessao.getInstance().getUsuario().getId());
        cmbCliente.setValue(null);
        txtDesconto.setText("0");
        txtValorPago.clear();
        rdReais.setSelected(true);
        selecionarPagamento("DINHEIRO", btnDinheiro);
        atualizarTotais();
        txtBusca.requestFocus();
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return 0; }
    }
}
