package com.erp.view;

import com.erp.dao.ProdutoDAO;
import com.erp.model.Produto;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ProdutoView {

    private final ProdutoDAO dao = new ProdutoDAO();
    private TableView<Produto> tabela;

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("📦 Produtos");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Cadastro e gestão de produtos");
        sub.getStyleClass().add("subtitulo-modulo");

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Buscar por nome, código ou código de barras...");
        txtBusca.getStyleClass().add("campo-busca");
        txtBusca.setPrefWidth(400);
        txtBusca.textProperty().addListener((o, v, n) -> carregarDados(n));

        Button btnNovo = new Button("+ Novo Produto");
        btnNovo.getStyleClass().add("btn-primario");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        HBox acoes = new HBox(12, txtBusca, new Region(), btnNovo);
        HBox.setHgrow(acoes.getChildren().get(1), Priority.ALWAYS);
        acoes.setAlignment(Pos.CENTER_LEFT);

        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);
        carregarDados("");

        root.getChildren().addAll(new VBox(4, titulo, sub), acoes, tabela);
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Produto> criarTabela() {
        TableView<Produto> tv = new TableView<>();
        tv.setPlaceholder(new Label("Nenhum produto encontrado."));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Produto, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCod.setPrefWidth(100);

        TableColumn<Produto, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setPrefWidth(250);

        TableColumn<Produto, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoriaNome"));
        colCat.setPrefWidth(120);

        TableColumn<Produto, String> colUn = new TableColumn<>("UN");
        colUn.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colUn.setPrefWidth(60);

        TableColumn<Produto, String> colCusto = new TableColumn<>("Custo");
        colCusto.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoCusto())));
        colCusto.setPrefWidth(100);

        TableColumn<Produto, String> colVenda = new TableColumn<>("Venda");
        colVenda.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoVenda())));
        colVenda.setPrefWidth(100);

        TableColumn<Produto, String> colEstoque = new TableColumn<>("Estoque");
        colEstoque.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getEstoqueAtual())));
        colEstoque.setPrefWidth(90);
        colEstoque.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                Produto p = getTableView().getItems().get(getIndex());
                setStyle(p.isEstoqueBaixo()
                        ? "-fx-text-fill: #fa5252; -fx-font-weight: bold;"
                        : "-fx-text-fill: #40c057;");
            }
        });

        TableColumn<Produto, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setPrefWidth(130);
        colAcoes.setCellFactory(c -> new TableCell<>() {
            final Button btnEditar  = new Button("✏ Editar");
            final Button btnExcluir = new Button("🗑");
            {
                btnEditar.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #3a3d4e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnExcluir.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #fa5252; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnExcluir.setOnAction(e -> {
                    Produto p = getTableView().getItems().get(getIndex());
                    if (Alerta.confirmar("Excluir", "Inativar o produto '" + p.getNome() + "'?")) {
                        dao.excluir(p.getId());
                        carregarDados("");
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(4, btnEditar, btnExcluir));
            }
        });

        tv.getColumns().addAll(colCod, colNome, colCat, colUn, colCusto, colVenda, colEstoque, colAcoes);
        return tv;
    }

    private void carregarDados(String filtro) {
        List<Produto> lista = dao.listarPorFiltro(filtro, true);
        tabela.setItems(FXCollections.observableArrayList(lista));
    }

    private void abrirFormulario(Produto produto) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(produto == null ? "Novo Produto" : "Editar Produto");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #252836;");

        TextField txtCodigo     = camp("Código *");
        TextField txtCodBarras  = camp("Código de Barras");
        TextField txtNome       = camp("Nome *");
        TextArea  txaDesc       = new TextArea(); txaDesc.setPromptText("Descrição"); txaDesc.setPrefRowCount(2);
        TextField txtUnidade    = camp("Unidade (UN, KG, L...)");
        TextField txtCusto      = camp("Preço de Custo");
        TextField txtVenda      = camp("Preço de Venda *");
        TextField txtMargem     = camp("Margem Lucro (%)");
        TextField txtEstAtual   = camp("Estoque Atual");
        TextField txtEstMin     = camp("Estoque Mínimo");
        TextField txtEstMax     = camp("Estoque Máximo");
        TextField txtNcm        = camp("NCM");
        TextField txtCfop       = camp("CFOP");
        TextField txtIcms       = camp("ICMS Alíquota (%)");

        // Calcular margem automaticamente
        txtCusto.focusedProperty().addListener((o, v, n) -> {
            if (!n) calcularMargem(txtCusto, txtVenda, txtMargem);
        });
        txtVenda.focusedProperty().addListener((o, v, n) -> {
            if (!n) calcularMargem(txtCusto, txtVenda, txtMargem);
        });

        if (produto != null) {
            txtCodigo.setText(produto.getCodigo());
            txtCodBarras.setText(produto.getCodigoBarras());
            txtNome.setText(produto.getNome());
            txaDesc.setText(produto.getDescricao());
            txtUnidade.setText(produto.getUnidade());
            txtCusto.setText(String.format("%.2f", produto.getPrecoCusto()));
            txtVenda.setText(String.format("%.2f", produto.getPrecoVenda()));
            txtMargem.setText(String.format("%.2f", produto.getMargemLucro()));
            txtEstAtual.setText(String.format("%.3f", produto.getEstoqueAtual()));
            txtEstMin.setText(String.format("%.3f", produto.getEstoqueMinimo()));
            txtEstMax.setText(String.format("%.3f", produto.getEstoqueMaximo()));
            txtNcm.setText(produto.getNcm());
            txtCfop.setText(produto.getCfop());
            txtIcms.setText(String.format("%.2f", produto.getIcmsAliquota()));
        } else {
            txtUnidade.setText("UN");
            txtCusto.setText("0.00");
            txtVenda.setText("0.00");
            txtEstAtual.setText("0.000");
            txtEstMin.setText("0.000");
        }

        int r = 0;
        form.add(lbl("Código *"), 0, r); form.add(txtCodigo, 1, r++);
        form.add(lbl("Cód. Barras"), 0, r); form.add(txtCodBarras, 1, r++);
        form.add(lbl("Nome *"), 0, r); form.add(txtNome, 1, r++);
        form.add(lbl("Descrição"), 0, r); form.add(txaDesc, 1, r++);
        form.add(lbl("Unidade"), 0, r); form.add(txtUnidade, 1, r++);
        form.add(lbl("Preço Custo"), 0, r); form.add(txtCusto, 1, r++);
        form.add(lbl("Preço Venda"), 0, r); form.add(txtVenda, 1, r++);
        form.add(lbl("Margem %"), 0, r); form.add(txtMargem, 1, r++);
        form.add(lbl("Estoque Atual"), 0, r); form.add(txtEstAtual, 1, r++);
        form.add(lbl("Est. Mínimo"), 0, r); form.add(txtEstMin, 1, r++);
        form.add(lbl("Est. Máximo"), 0, r); form.add(txtEstMax, 1, r++);
        form.add(lbl("NCM"), 0, r); form.add(txtNcm, 1, r++);
        form.add(lbl("CFOP"), 0, r); form.add(txtCfop, 1, r++);
        form.add(lbl("ICMS %"), 0, r); form.add(txtIcms, 1, r++);

        ColumnConstraints cc1 = new ColumnConstraints(130);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(cc1, cc2);

        Button btnSalvar   = new Button("💾 Salvar");
        btnSalvar.getStyleClass().add("btn-primario");
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> dialog.close());

        btnSalvar.setOnAction(e -> {
            if (txtCodigo.getText().isBlank() || txtNome.getText().isBlank()) {
                Alerta.aviso("Atenção", "Código e Nome são obrigatórios.");
                return;
            }
            Produto p = produto != null ? produto : new Produto();
            p.setCodigo(txtCodigo.getText().trim());
            p.setCodigoBarras(txtCodBarras.getText().trim());
            p.setNome(txtNome.getText().trim());
            p.setDescricao(txaDesc.getText().trim());
            p.setUnidade(txtUnidade.getText().trim().toUpperCase());
            p.setPrecoCusto(parseDouble(txtCusto.getText()));
            p.setPrecoVenda(parseDouble(txtVenda.getText()));
            p.setMargemLucro(parseDouble(txtMargem.getText()));
            p.setEstoqueAtual(parseDouble(txtEstAtual.getText()));
            p.setEstoqueMinimo(parseDouble(txtEstMin.getText()));
            p.setEstoqueMaximo(parseDouble(txtEstMax.getText()));
            p.setNcm(txtNcm.getText().trim());
            p.setCfop(txtCfop.getText().trim());
            p.setIcmsAliquota(parseDouble(txtIcms.getText()));

            boolean ok = produto == null ? dao.salvar(p) : dao.atualizar(p);
            if (ok) { carregarDados(""); dialog.close(); }
            else Alerta.erro("Erro", "Não foi possível salvar o produto.");
        });

        HBox botoes = new HBox(10, btnSalvar, btnCancelar);
        botoes.setPadding(new Insets(16));
        botoes.setAlignment(Pos.CENTER_RIGHT);
        botoes.setStyle("-fx-background-color: #252836;");

        scroll.setContent(new VBox(0, form, botoes));
        Scene scene = new Scene(scroll, 580, 720);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void calcularMargem(TextField custo, TextField venda, TextField margem) {
        double c = parseDouble(custo.getText());
        double v = parseDouble(venda.getText());
        if (c > 0 && v > 0) {
            margem.setText(String.format("%.2f", ((v - c) / c) * 100));
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return 0; }
    }

    private TextField camp(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); return tf;
    }

    private Label lbl(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;"); return l;
    }
}
