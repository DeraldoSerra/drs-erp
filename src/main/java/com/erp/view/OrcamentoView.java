package com.erp.view;

import com.erp.dao.ClienteDAO;
import com.erp.dao.OrcamentoDAO;
import com.erp.dao.ProdutoDAO;
import com.erp.model.*;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import com.erp.util.Sessao;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrcamentoView {

    private final OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
    private final ProdutoDAO   produtoDAO   = new ProdutoDAO();
    private final ClienteDAO   clienteDAO   = new ClienteDAO();

    private TableView<Orcamento> tabela;
    private ObservableList<Orcamento> lista;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Region criar() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: #1e2027;");

        Label titulo    = new Label("📋 Orçamentos");
        titulo.getStyleClass().add("titulo-modulo");
        Label subtitulo = new Label("Gestão de orçamentos e propostas comerciais");
        subtitulo.getStyleClass().add("subtitulo-modulo");

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button btnNovo       = new Button("➕  Novo Orçamento");
        Button btnConverter  = new Button("✅  Converter em Venda");
        Button btnCancelar   = new Button("❌  Cancelar Orçamento");
        Button btnAtualizar  = new Button("🔄  Atualizar");
        btnNovo.getStyleClass().add("btn-primario");
        btnConverter.getStyleClass().add("btn-sucesso");
        btnCancelar.getStyleClass().add("btn-perigo");
        btnAtualizar.getStyleClass().add("btn-secundario");
        toolbar.getChildren().addAll(btnNovo, btnConverter, btnCancelar, btnAtualizar);

        // Table
        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);

        // Actions
        btnNovo.setOnAction(e -> abrirDialogoNovo());
        btnConverter.setOnAction(e -> converterSelecionado());
        btnCancelar.setOnAction(e -> cancelarSelecionado());
        btnAtualizar.setOnAction(e -> recarregar());

        box.getChildren().addAll(new VBox(4, titulo, subtitulo), toolbar, tabela);
        recarregar();
        return box;
    }

    @SuppressWarnings("unchecked")
    private TableView<Orcamento> criarTabela() {
        TableView<Orcamento> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("Nenhum orçamento cadastrado."));

        TableColumn<Orcamento, String> colNum = new TableColumn<>("Nº");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        colNum.setPrefWidth(120);

        TableColumn<Orcamento, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataCriacao() != null
                        ? c.getValue().getDataCriacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : ""));
        colData.setPrefWidth(140);

        TableColumn<Orcamento, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getClienteNome() != null ? c.getValue().getClienteNome() : "Consumidor Final"));

        TableColumn<Orcamento, String> colValidade = new TableColumn<>("Validade");
        colValidade.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getValidade() != null ? c.getValue().getValidade().format(FMT) : ""));
        colValidade.setPrefWidth(100);

        TableColumn<Orcamento, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getTotal())));
        colTotal.setPrefWidth(110);

        TableColumn<Orcamento, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatusDisplay()));
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                String bg = switch (s) {
                    case "APROVADO"  -> "-fx-text-fill: #40c057;";
                    case "CANCELADO" -> "-fx-text-fill: #fa5252;";
                    case "EXPIRADO"  -> "-fx-text-fill: #fd7e14;";
                    default          -> "-fx-text-fill: #4dabf7;";
                };
                setStyle(bg + " -fx-font-weight: bold;");
            }
        });

        tv.getColumns().addAll(colNum, colData, colCliente, colValidade, colTotal, colStatus);
        return tv;
    }

    private void recarregar() {
        try {
            lista = FXCollections.observableArrayList(orcamentoDAO.listarTodos());
            tabela.setItems(lista);
        } catch (Exception e) {
            Alerta.erro("Erro", "Não foi possível carregar os orçamentos.");
        }
    }

    private void abrirDialogoNovo() {
        Stage dialogo = new Stage();
        dialogo.setTitle("Novo Orçamento");
        dialogo.initModality(Modality.APPLICATION_MODAL);
        dialogo.setWidth(900);
        dialogo.setHeight(650);

        Orcamento orc = new Orcamento();
        orc.setUsuarioId(Sessao.getInstance().getUsuario().getId());
        ObservableList<ItemOrcamento> itens = FXCollections.observableArrayList();

        // LEFT panel – product search + items
        VBox left = new VBox(10);
        left.setPadding(new Insets(16));
        left.setStyle("-fx-background-color: #1e2027;");

        Label lTit = new Label("📋 Novo Orçamento");
        lTit.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox buscaRow = new HBox(8);
        TextField txtBusca = new TextField();
        txtBusca.setPromptText("📦 Código de barras ou nome do produto...");
        txtBusca.getStyleClass().add("pdv-campo-codigo");
        HBox.setHgrow(txtBusca, Priority.ALWAYS);
        TextField txtQtd = new TextField("1");
        txtQtd.setPrefWidth(65);
        txtQtd.setStyle("-fx-background-color: #2a2d3e; -fx-text-fill: white; -fx-border-color: #3a3d4e; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");
        Button btnAdd = new Button("+ Add");
        btnAdd.getStyleClass().add("btn-primario");
        buscaRow.getChildren().addAll(txtBusca, txtQtd, btnAdd);

        TableView<ItemOrcamento> tblItens = criarTabelaItens(itens, orc);
        VBox.setVgrow(tblItens, Priority.ALWAYS);

        Label lblTotal = new Label("TOTAL: R$ 0,00");
        lblTotal.setStyle("-fx-text-fill: #40c057; -fx-font-size: 20px; -fx-font-weight: bold;");

        Runnable recalc = () -> {
            orc.recalcular();
            lblTotal.setText("TOTAL: " + Formatador.formatarMoeda(orc.getTotal()));
        };

        Runnable adicionarProduto = () -> {
            String cod = txtBusca.getText().trim();
            if (cod.isBlank()) return;
            double qtd = parseDouble(txtQtd.getText());
            if (qtd <= 0) qtd = 1;
            java.util.Optional<Produto> opt = produtoDAO.buscarPorCodigoBarras(cod);
            if (opt.isEmpty()) {
                List<Produto> prods = produtoDAO.listarPorFiltro(cod, true);
                if (prods.isEmpty()) { Alerta.aviso("Produto", "Não encontrado: " + cod); return; }
                opt = prods.size() == 1 ? java.util.Optional.of(prods.get(0)) : java.util.Optional.empty();
                if (opt.isEmpty()) { Alerta.aviso("Produto", "Selecione o produto na lista."); return; }
            }
            Produto p = opt.get();
            for (ItemOrcamento ex : itens) {
                if (ex.getProdutoId() == p.getId()) {
                    ex.setQuantidade(ex.getQuantidade() + qtd);
                    tblItens.refresh();
                    recalc.run();
                    txtBusca.clear();
                    return;
                }
            }
            ItemOrcamento item = new ItemOrcamento(p, qtd);
            itens.add(item);
            orc.adicionarItem(item);
            recalc.run();
            txtBusca.clear();
        };

        txtBusca.setOnAction(e -> adicionarProduto.run());
        btnAdd.setOnAction(e -> adicionarProduto.run());

        left.getChildren().addAll(lTit, buscaRow, tblItens, lblTotal);

        // RIGHT panel – customer, validity, discount, save
        VBox right = new VBox(12);
        right.setPadding(new Insets(16));
        right.setStyle("-fx-background-color: #252836;");
        right.setPrefWidth(280);

        Label lCli = new Label("👤 Cliente");
        lCli.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        ComboBox<Cliente> cmbCliente = new ComboBox<>();
        cmbCliente.setPromptText("Consumidor Final");
        cmbCliente.setMaxWidth(Double.MAX_VALUE);
        cmbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));

        Label lVal = new Label("📅 Validade (dias)");
        lVal.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        TextField txtValidade = new TextField("30");
        txtValidade.setMaxWidth(Double.MAX_VALUE);

        Label lDesc = new Label("🏷 Desconto (R$)");
        lDesc.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        TextField txtDesc = new TextField("0");
        txtDesc.setMaxWidth(Double.MAX_VALUE);
        txtDesc.textProperty().addListener((o, v, n) -> {
            orc.setDesconto(parseDouble(n));
            recalc.run();
        });

        Label lObs = new Label("📝 Observações");
        lObs.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);
        txtObs.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnSalvar  = new Button("💾  Salvar Orçamento");
        Button btnFechar  = new Button("✖  Fechar");
        btnSalvar.getStyleClass().add("btn-sucesso");
        btnFechar.getStyleClass().add("btn-secundario");
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnFechar.setMaxWidth(Double.MAX_VALUE);

        btnSalvar.setOnAction(e -> {
            if (itens.isEmpty()) { Alerta.aviso("Atenção", "Adicione pelo menos um produto."); return; }
            int diasVal = parseIntSafe(txtValidade.getText(), 30);
            orc.setValidade(LocalDate.now().plusDays(diasVal));
            if (cmbCliente.getValue() != null) {
                orc.setClienteId(cmbCliente.getValue().getId());
                orc.setClienteNome(cmbCliente.getValue().getNome());
            }
            orc.setObservacoes(txtObs.getText());
            orc.recalcular();
            if (orcamentoDAO.salvar(orc)) {
                Alerta.info("Sucesso", "Orçamento salvo com número: " + orc.getNumero());
                recarregar();
                dialogo.close();
            } else {
                Alerta.erro("Erro", "Não foi possível salvar o orçamento.");
            }
        });
        btnFechar.setOnAction(e -> dialogo.close());

        right.getChildren().addAll(lCli, cmbCliente, lVal, txtValidade, lDesc, txtDesc, lObs, txtObs, spacer, btnSalvar, btnFechar);

        HBox layout = new HBox();
        HBox.setHgrow(left, Priority.ALWAYS);
        layout.getChildren().addAll(left, right);
        layout.setStyle("-fx-background-color: #1e2027;");

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogo.setScene(scene);
        dialogo.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private TableView<ItemOrcamento> criarTabelaItens(ObservableList<ItemOrcamento> itens, Orcamento orc) {
        TableView<ItemOrcamento> tv = new TableView<>(itens);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("Nenhum item adicionado."));

        TableColumn<ItemOrcamento, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProdutoNome()));

        TableColumn<ItemOrcamento, String> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getQuantidade())));
        colQtd.setPrefWidth(70);

        TableColumn<ItemOrcamento, String> colUnit = new TableColumn<>("Unit.");
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoUnit())));
        colUnit.setPrefWidth(90);

        TableColumn<ItemOrcamento, String> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getSubtotal())));
        colSub.setPrefWidth(100);

        TableColumn<ItemOrcamento, Void> colDel = new TableColumn<>("");
        colDel.setPrefWidth(45);
        colDel.setResizable(false);
        colDel.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #fa525233; -fx-text-fill: #fa5252; "
                        + "-fx-border-color: transparent; -fx-cursor: hand; -fx-background-radius: 4;");
                btn.setOnAction(e -> {
                    ItemOrcamento item = getTableView().getItems().get(getIndex());
                    itens.remove(item);
                    orc.removerItem(item);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
        tv.getColumns().addAll(colNome, colQtd, colUnit, colSub, colDel);
        return tv;
    }

    private void converterSelecionado() {
        Orcamento sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Alerta.aviso("Atenção", "Selecione um orçamento."); return; }
        if (!"ABERTO".equals(sel.getStatus()) && !"ABERTO".equals(sel.getStatusDisplay())) {
            Alerta.aviso("Atenção", "Apenas orçamentos com status ABERTO podem ser convertidos."); return;
        }
        if (!Alerta.confirmar("Converter", "Converter orçamento #" + sel.getNumero() + " em venda?")) return;
        int uid = Sessao.getInstance().getUsuario().getId();
        if (orcamentoDAO.converterEmVenda(sel.getId(), uid)) {
            Alerta.info("Sucesso", "Orçamento convertido em venda com sucesso!");
            recarregar();
        } else {
            Alerta.erro("Erro", "Não foi possível converter o orçamento.");
        }
    }

    private void cancelarSelecionado() {
        Orcamento sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Alerta.aviso("Atenção", "Selecione um orçamento."); return; }
        if (!Alerta.confirmar("Cancelar", "Cancelar orçamento #" + sel.getNumero() + "?")) return;
        if (orcamentoDAO.cancelar(sel.getId())) {
            Alerta.info("Cancelado", "Orçamento cancelado com sucesso.");
            recarregar();
        } else {
            Alerta.erro("Erro", "Não foi possível cancelar (verifique se está ABERTO).");
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return 0; }
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return def; }
    }
}
