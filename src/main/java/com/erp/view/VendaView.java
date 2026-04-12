package com.erp.view;

import com.erp.dao.ClienteDAO;
import com.erp.dao.ProdutoDAO;
import com.erp.dao.VendaDAO;
import com.erp.dao.CaixaDAO;
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

    private Label lblTotal, lblSubtotal, lblDesconto, lblItemCount, lblTroco;
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
                                if (Alerta.confirmar("Nova Venda", "Deseja cancelar e iniciar uma nova venda?"))
                                    novaVenda();
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
        lblTotal = new Label("TOTAL: R$ 0,00");
        lblTotal.setStyle("-fx-text-fill: #40c057; -fx-font-size: 22px; -fx-font-weight: bold;");
        totaisBar.getChildren().addAll(lblSubtotal, lblDesconto, lblTotal);

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
            if (Alerta.confirmar("Cancelar", "Deseja cancelar esta venda?")) novaVenda();
        });

        selecionarPagamento("DINHEIRO", btnDinheiro);

        box.getChildren().addAll(
                lblClienteTitle, cmbCliente,
                new Separator(),
                lblDescTitle, rdBox, txtDesconto,
                new Separator(),
                lblPagTitle, pagRow1, pagRow2,
                trocoBox, totalBox, spacer,
                btnFinalizar, btnRow
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
        lblTotal.setText(Formatador.formatarMoeda(vendaAtual.getTotal()));
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
