package com.erp.view;

import com.erp.dao.ProdutoDAO;
import com.erp.model.Produto;
import com.erp.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class EstoqueView {

    private final ProdutoDAO dao = new ProdutoDAO();
    private TableView<Produto> tabela;

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("📋 Controle de Estoque");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Visualize o estoque atual de todos os produtos");
        sub.getStyleClass().add("subtitulo-modulo");

        // Filtros
        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Buscar produto...");
        txtBusca.getStyleClass().add("campo-busca");
        txtBusca.setPrefWidth(300);

        ComboBox<String> cmbFiltro = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Estoque Baixo", "Sem Estoque"));
        cmbFiltro.setValue("Todos");

        Button btnAtualizar = new Button("🔄 Atualizar");
        btnAtualizar.getStyleClass().add("btn-secundario");

        HBox acoes = new HBox(12, txtBusca, cmbFiltro, new Region(), btnAtualizar);
        HBox.setHgrow(acoes.getChildren().get(2), Priority.ALWAYS);
        acoes.setAlignment(Pos.CENTER_LEFT);

        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);

        txtBusca.textProperty().addListener((o, v, n) -> filtrar(n, cmbFiltro.getValue()));
        cmbFiltro.setOnAction(e -> filtrar(txtBusca.getText(), cmbFiltro.getValue()));
        btnAtualizar.setOnAction(e -> filtrar(txtBusca.getText(), cmbFiltro.getValue()));

        // Cards resumo
        long total      = dao.contar();
        long baixo      = dao.listarEstoqueBaixo().size();
        long semEstoque = dao.listarPorFiltro("", true).stream()
                .filter(p -> p.getEstoqueAtual() <= 0).count();

        HBox resumo = new HBox(16,
                miniCard("📦 Total Produtos",   String.valueOf(total),      "#4dabf7"),
                miniCard("⚠ Estoque Baixo",     String.valueOf(baixo),      "#fd7e14"),
                miniCard("❌ Sem Estoque",        String.valueOf(semEstoque), "#fa5252")
        );

        carregarTodos();
        root.getChildren().addAll(new VBox(4, titulo, sub), resumo, acoes, tabela);
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Produto> criarTabela() {
        TableView<Produto> tv = new TableView<>();
        tv.setPlaceholder(new Label("Nenhum produto encontrado."));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Produto, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(100);

        TableColumn<Produto, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setPrefWidth(280);

        TableColumn<Produto, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategoriaNome() != null ? c.getValue().getCategoriaNome() : "-"));
        colCat.setPrefWidth(120);

        TableColumn<Produto, String> colUn = new TableColumn<>("UN");
        colUn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidade()));
        colUn.setPrefWidth(60);

        TableColumn<Produto, String> colAtual = new TableColumn<>("Atual");
        colAtual.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getEstoqueAtual())));
        colAtual.setPrefWidth(90);
        colAtual.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                Produto p = getTableView().getItems().get(getIndex());
                if (p.getEstoqueAtual() <= 0)
                    setStyle("-fx-text-fill: #fa5252; -fx-font-weight: bold;");
                else if (p.isEstoqueBaixo())
                    setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #40c057;");
            }
        });

        TableColumn<Produto, String> colMin = new TableColumn<>("Mínimo");
        colMin.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getEstoqueMinimo())));
        colMin.setPrefWidth(90);

        TableColumn<Produto, String> colPreco = new TableColumn<>("Preço Venda");
        colPreco.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoVenda())));
        colPreco.setPrefWidth(110);

        TableColumn<Produto, String> colSit = new TableColumn<>("Situação");
        colSit.setCellValueFactory(c -> {
            Produto p = c.getValue();
            if (p.getEstoqueAtual() <= 0) return new SimpleStringProperty("SEM ESTOQUE");
            if (p.isEstoqueBaixo()) return new SimpleStringProperty("ESTOQUE BAIXO");
            return new SimpleStringProperty("OK");
        });
        colSit.setPrefWidth(120);
        colSit.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "SEM ESTOQUE"  -> setStyle("-fx-text-fill: #fa5252; -fx-font-weight: bold;");
                    case "ESTOQUE BAIXO"-> setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                    default             -> setStyle("-fx-text-fill: #40c057;");
                }
            }
        });

        tv.getColumns().addAll(colCod, colNome, colCat, colUn, colAtual, colMin, colPreco, colSit);
        return tv;
    }

    private void carregarTodos() {
        tabela.setItems(FXCollections.observableArrayList(dao.listarTodos()));
    }

    private void filtrar(String texto, String filtroTipo) {
        var lista = dao.listarPorFiltro(texto, true);
        if ("Estoque Baixo".equals(filtroTipo)) lista = lista.stream().filter(Produto::isEstoqueBaixo).toList();
        else if ("Sem Estoque".equals(filtroTipo)) lista = lista.stream().filter(p -> p.getEstoqueAtual() <= 0).toList();
        tabela.setItems(FXCollections.observableArrayList(lista));
    }

    private VBox miniCard(String titulo, String valor, String cor) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12 , 20, 12, 20));
        card.setStyle("-fx-background-color: #252836; -fx-background-radius: 8; -fx-border-color: " + cor + "; -fx-border-width: 0 0 0 3; -fx-border-radius: 8;");
        Label lTitulo = new Label(titulo);
        lTitulo.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label lValor = new Label(valor);
        lValor.setStyle("-fx-text-fill: " + cor + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        card.getChildren().addAll(lTitulo, lValor);
        return card;
    }
}
