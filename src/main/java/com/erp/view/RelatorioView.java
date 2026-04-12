package com.erp.view;

import com.erp.dao.VendaDAO;
import com.erp.dao.ProdutoDAO;
import com.erp.dao.FinanceiroDAO;
import com.erp.model.Venda;
import com.erp.model.Produto;
import com.erp.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioView {

    private final VendaDAO vendaDAO       = new VendaDAO();
    private final ProdutoDAO produtoDAO   = new ProdutoDAO();
    private final FinanceiroDAO finDAO    = new FinanceiroDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("📈 Relatórios");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Análise de dados e estatísticas");
        sub.getStyleClass().add("subtitulo-modulo");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Tab tabVendas  = new Tab("📊 Vendas",            criarAbaVendas());
        Tab tabEstoque = new Tab("📦 Estoque Baixo",     criarAbaEstoqueBaixo());

        tabs.getTabs().addAll(tabVendas, tabEstoque);
        root.getChildren().addAll(new VBox(4, titulo, sub), tabs);
        return root;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaVendas() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        DatePicker dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker dpFim    = new DatePicker(LocalDate.now());
        Button btnFiltrar   = new Button("🔍 Gerar Relatório");
        btnFiltrar.getStyleClass().add("btn-primario");

        HBox toolbar = new HBox(12, new Label("De:"), dpInicio, new Label("Até:"), dpFim, btnFiltrar);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Resumo
        HBox resumo = new HBox(16);
        Label lblQtd   = criarLabelResumo("0",      "Vendas",       "#4dabf7");
        Label lblTotal = criarLabelResumo("R$ 0,00", "Faturamento", "#40c057");
        Label lblMedia = criarLabelResumo("R$ 0,00", "Ticket Médio","#6c63ff");
        resumo.getChildren().addAll(
                miniCardR("Vendas",        lblQtd),
                miniCardR("Faturamento",   lblTotal),
                miniCardR("Ticket Médio",  lblMedia)
        );

        // Tabela
        TableView<Venda> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Venda, String> colNum = new TableColumn<>("Nº Venda");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumero()));
        colNum.setPrefWidth(100);

        TableColumn<Venda, String> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataVenda() != null ? c.getValue().getDataVenda().format(FMT) : ""));
        colData.setPrefWidth(140);

        TableColumn<Venda, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getClienteNome() != null ? c.getValue().getClienteNome() : "Consumidor Final"));
        colCliente.setPrefWidth(200);

        TableColumn<Venda, String> colForma = new TableColumn<>("Pagamento");
        colForma.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormaPagamento()));
        colForma.setPrefWidth(130);

        TableColumn<Venda, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getTotal())));
        colTotal.setPrefWidth(110);

        TableColumn<Venda, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setPrefWidth(100);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("CANCELADA".equals(item) ? "-fx-text-fill: #fa5252;" : "-fx-text-fill: #40c057;");
            }
        });

        tabela.getColumns().addAll(colNum, colData, colCliente, colForma, colTotal, colStatus);

        btnFiltrar.setOnAction(e -> {
            List<Venda> vendas = vendaDAO.listarPorPeriodo(dpInicio.getValue(), dpFim.getValue());
            tabela.setItems(FXCollections.observableArrayList(vendas));

            long qtd = vendas.stream().filter(v -> "FINALIZADA".equals(v.getStatus())).count();
            double totalV = vendas.stream().filter(v -> "FINALIZADA".equals(v.getStatus()))
                    .mapToDouble(Venda::getTotal).sum();
            double media = qtd > 0 ? totalV / qtd : 0;

            lblQtd.setText(String.valueOf(qtd));
            lblTotal.setText(Formatador.formatarMoeda(totalV));
            lblMedia.setText(Formatador.formatarMoeda(media));
        });

        // Carregar período atual ao abrir
        List<Venda> vendas = vendaDAO.listarPorPeriodo(dpInicio.getValue(), dpFim.getValue());
        tabela.setItems(FXCollections.observableArrayList(vendas));

        box.getChildren().addAll(toolbar, resumo, tabela);
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaEstoqueBaixo() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        TableView<Produto> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Produto, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(100);

        TableColumn<Produto, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setPrefWidth(300);

        TableColumn<Produto, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategoriaNome() != null ? c.getValue().getCategoriaNome() : "-"));
        colCat.setPrefWidth(130);

        TableColumn<Produto, String> colAtual = new TableColumn<>("Estoque Atual");
        colAtual.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getEstoqueAtual())));
        colAtual.setPrefWidth(120);
        colAtual.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: #fa5252; -fx-font-weight: bold;");
            }
        });

        TableColumn<Produto, String> colMin = new TableColumn<>("Estoque Mínimo");
        colMin.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getEstoqueMinimo())));
        colMin.setPrefWidth(130);

        TableColumn<Produto, String> colPreco = new TableColumn<>("Preço Venda");
        colPreco.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoVenda())));
        colPreco.setPrefWidth(110);

        tabela.getColumns().addAll(colCod, colNome, colCat, colAtual, colMin, colPreco);

        List<Produto> baixo = produtoDAO.listarEstoqueBaixo();
        tabela.setItems(FXCollections.observableArrayList(baixo));

        Label lbl = new Label("⚠ " + baixo.size() + " produtos com estoque abaixo do mínimo");
        lbl.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 8 0;");

        box.getChildren().addAll(lbl, tabela);
        return box;
    }

    private Label criarLabelResumo(String valor, String titulo, String cor) {
        Label lbl = new Label(valor);
        lbl.setStyle("-fx-text-fill: " + cor + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        return lbl;
    }

    private VBox miniCardR(String titulo, Label valorLabel) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setStyle("-fx-background-color: #252836; -fx-background-radius: 10; -fx-min-width: 180;");
        Label lT = new Label(titulo);
        lT.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 11px; -fx-font-weight: bold;");
        card.getChildren().addAll(lT, valorLabel);
        return card;
    }
}
