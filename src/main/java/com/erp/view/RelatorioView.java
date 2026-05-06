package com.erp.view;

import com.erp.dao.VendaDAO;
import com.erp.dao.ProdutoDAO;
import com.erp.dao.FinanceiroDAO;
import com.erp.model.Venda;
import com.erp.model.Produto;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import com.erp.util.RelatorioVendasPDF;
import com.erp.util.RelatorioEstoquePDF;
import com.erp.util.RelatorioCapitalPDF;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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

        Tab tabVendas       = new Tab("📊 Vendas",                criarAbaVendas());
        Tab tabEstoque      = new Tab("📦 Estoque Baixo",         criarAbaEstoqueBaixo());
        Tab tabCapital      = new Tab("💰 Capital em Estoque",    criarAbaCapital());
        Tab tabCancelamentos = new Tab("🚫 Cancelamentos",        criarAbaCancelamentos());

        tabs.getTabs().addAll(tabVendas, tabEstoque, tabCapital, tabCancelamentos);
        root.getChildren().addAll(new VBox(4, titulo, sub), tabs);
        return root;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaVendas() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        DatePicker dpInicio = new DatePicker(LocalDate.now());
        DatePicker dpFim    = new DatePicker(LocalDate.now());
        Button btnFiltrar   = new Button("🔍 Gerar Relatório");
        btnFiltrar.getStyleClass().add("btn-primario");
        Button btnHoje = new Button("📅 Hoje");
        btnHoje.getStyleClass().add("btn-secundario");
        Button btnMes  = new Button("📆 Este Mês");
        btnMes.getStyleClass().add("btn-secundario");
        Button btnPDF = new Button("📄 Exportar PDF");
        btnPDF.getStyleClass().add("btn-secundario");
        btnPDF.setDisable(true);

        HBox toolbar = new HBox(10, new Label("De:"), dpInicio, new Label("Até:"), dpFim,
                btnHoje, btnMes, btnFiltrar, btnPDF);
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

        TableColumn<Venda, String> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getSubtotal())));
        colSubtotal.setPrefWidth(110);

        TableColumn<Venda, String> colDesc = new TableColumn<>("Desconto");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getDesconto())));
        colDesc.setPrefWidth(100);

        TableColumn<Venda, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getTotal())));
        colTotal.setPrefWidth(110);

        TableColumn<Venda, String> colRecebido = new TableColumn<>("Recebido");
        colRecebido.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getValorPago())));
        colRecebido.setPrefWidth(110);

        TableColumn<Venda, String> colTroco = new TableColumn<>("Troco");
        colTroco.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getTroco())));
        colTroco.setPrefWidth(100);

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

        tabela.getColumns().addAll(colNum, colData, colCliente, colForma, colSubtotal, colDesc, colTotal, colRecebido, colTroco, colStatus);

        // Guardar referência dos dados atuais para o PDF
        @SuppressWarnings("unchecked")
        final List<Venda>[] vendasAtual = new List[1];

        Runnable carregarDados = () -> {
            List<Venda> vendas = vendaDAO.listarPorPeriodo(dpInicio.getValue(), dpFim.getValue());
            vendasAtual[0] = vendas;
            tabela.setItems(FXCollections.observableArrayList(vendas));
            long qtd = vendas.stream().filter(v -> "FINALIZADA".equals(v.getStatus())).count();
            double totalV = vendas.stream().filter(v -> "FINALIZADA".equals(v.getStatus()))
                    .mapToDouble(Venda::getTotal).sum();
            double media = qtd > 0 ? totalV / qtd : 0;
            lblQtd.setText(String.valueOf(qtd));
            lblTotal.setText(Formatador.formatarMoeda(totalV));
            lblMedia.setText(Formatador.formatarMoeda(media));
            btnPDF.setDisable(vendas.isEmpty());
        };

        btnFiltrar.setOnAction(e -> carregarDados.run());
        btnHoje.setOnAction(e -> {
            dpInicio.setValue(LocalDate.now());
            dpFim.setValue(LocalDate.now());
            carregarDados.run();
        });
        btnMes.setOnAction(e -> {
            dpInicio.setValue(LocalDate.now().withDayOfMonth(1));
            dpFim.setValue(LocalDate.now());
            carregarDados.run();
        });
        // Carrega automaticamente ao abrir (hoje por padrão)
        javafx.application.Platform.runLater(carregarDados::run);

        btnPDF.setOnAction(e -> {
            if (vendasAtual[0] == null || vendasAtual[0].isEmpty()) {
                Alerta.aviso("Atenção", "Nenhum dado para exportar. Clique em 'Gerar Relatório' primeiro.");
                return;
            }
            btnPDF.setDisable(true);
            btnPDF.setText("⏳ Gerando PDF...");
            LocalDate inicio = dpInicio.getValue();
            LocalDate fim    = dpFim.getValue();
            List<Venda> dados = vendasAtual[0];

            Task<String> task = new Task<>() {
                @Override protected String call() { return RelatorioVendasPDF.gerar(inicio, fim, dados); }
            };
            task.setOnSucceeded(ev -> {
                Platform.runLater(() -> {
                    btnPDF.setDisable(false);
                    btnPDF.setText("📄 Exportar PDF");
                    String caminho = task.getValue();
                    if (caminho != null) {
                        try { java.awt.Desktop.getDesktop().open(new java.io.File(caminho)); }
                        catch (Exception ex) { Alerta.info("PDF Gerado", "Arquivo salvo em:\n" + caminho); }
                    } else {
                        Alerta.erro("Erro", "Não foi possível gerar o PDF.");
                    }
                });
            });
            task.setOnFailed(ev -> Platform.runLater(() -> {
                btnPDF.setDisable(false);
                btnPDF.setText("📄 Exportar PDF");
                Alerta.erro("Erro", "Erro ao gerar PDF: " + task.getException().getMessage());
            }));
            new Thread(task).start();
        });

        // Carregar período atual ao abrir
        carregarDados.run();

        box.getChildren().addAll(toolbar, resumo, tabela);
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaEstoqueBaixo() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        // Toolbar com botão PDF
        Button btnPDF = new Button("📄 Exportar PDF");
        btnPDF.getStyleClass().add("btn-primario");
        HBox toolbar = new HBox(btnPDF);
        toolbar.setAlignment(Pos.CENTER_LEFT);

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

        btnPDF.setDisable(baixo.isEmpty());
        btnPDF.setOnAction(e -> {
            List<Produto> dados = new java.util.ArrayList<>(tabela.getItems());
            if (dados.isEmpty()) { Alerta.aviso("Atenção", "Nenhum produto para exportar."); return; }
            btnPDF.setDisable(true);
            btnPDF.setText("⏳ Gerando PDF...");
            Task<String> task = new Task<>() {
                @Override protected String call() { return RelatorioEstoquePDF.gerar(dados); }
            };
            task.setOnSucceeded(ev -> Platform.runLater(() -> {
                btnPDF.setDisable(false);
                btnPDF.setText("📄 Exportar PDF");
                String caminho = task.getValue();
                if (caminho != null) {
                    try { java.awt.Desktop.getDesktop().open(new java.io.File(caminho)); }
                    catch (Exception ex) { Alerta.info("PDF Gerado", "Arquivo salvo em:\n" + caminho); }
                } else {
                    Alerta.erro("Erro", "Não foi possível gerar o PDF.");
                }
            }));
            task.setOnFailed(ev -> Platform.runLater(() -> {
                btnPDF.setDisable(false);
                btnPDF.setText("📄 Exportar PDF");
                Alerta.erro("Erro", "Erro ao gerar PDF: " + task.getException().getMessage());
            }));
            new Thread(task).start();
        });

        box.getChildren().addAll(toolbar, lbl, tabela);
        return box;
    }

    private VBox criarAbaCapital() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        // Cards de resumo
        List<Produto> todosProdutos = produtoDAO.listarTodos().stream()
                .filter(p -> p.getEstoqueAtual() > 0)
                .toList();

        double capitalCusto  = todosProdutos.stream().mapToDouble(p -> p.getEstoqueAtual() * p.getPrecoCusto()).sum();
        double capitalVenda  = todosProdutos.stream().mapToDouble(p -> p.getEstoqueAtual() * p.getPrecoVenda()).sum();
        double totalQtd      = todosProdutos.stream().mapToDouble(Produto::getEstoqueAtual).sum();

        HBox resumo = new HBox(16,
                miniCardR("📦 SKUs com Estoque",  criarLabelResumo(String.valueOf(todosProdutos.size()), "", "#4dabf7")),
                miniCardR("🔢 Total de Itens",     criarLabelResumo(Formatador.formatarQuantidade(totalQtd), "", "#6c63ff")),
                miniCardR("💲 Capital (Custo)",    criarLabelResumo(Formatador.formatarMoeda(capitalCusto), "", "#fd7e14")),
                miniCardR("💰 Capital (Venda)",    criarLabelResumo(Formatador.formatarMoeda(capitalVenda), "", "#40c057"))
        );
        resumo.setAlignment(Pos.CENTER_LEFT);

        // Tabela
        TableView<Produto> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        tabela.setPlaceholder(new Label("Nenhum produto com estoque."));

        TableColumn<Produto, String> colCod  = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<Produto, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setPrefWidth(260);

        TableColumn<Produto, String> colCat  = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategoriaNome() != null ? c.getValue().getCategoriaNome() : "-"));
        colCat.setPrefWidth(120);

        TableColumn<Produto, String> colUn   = new TableColumn<>("UN");
        colUn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidade()));
        colUn.setPrefWidth(55);

        TableColumn<Produto, String> colQtd  = new TableColumn<>("Qtd Atual");
        colQtd.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarQuantidade(c.getValue().getEstoqueAtual())));
        colQtd.setPrefWidth(90);

        TableColumn<Produto, String> colPCusto = new TableColumn<>("Preço Custo");
        colPCusto.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoCusto())));
        colPCusto.setPrefWidth(110);

        TableColumn<Produto, String> colCapCusto = new TableColumn<>("Capital Custo");
        colCapCusto.setCellValueFactory(c -> {
            double v = c.getValue().getEstoqueAtual() * c.getValue().getPrecoCusto();
            return new SimpleStringProperty(Formatador.formatarMoeda(v));
        });
        colCapCusto.setPrefWidth(120);

        TableColumn<Produto, String> colPVenda = new TableColumn<>("Preço Venda");
        colPVenda.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getPrecoVenda())));
        colPVenda.setPrefWidth(110);

        TableColumn<Produto, String> colCapVenda = new TableColumn<>("Capital Venda");
        colCapVenda.setCellValueFactory(c -> {
            double v = c.getValue().getEstoqueAtual() * c.getValue().getPrecoVenda();
            return new SimpleStringProperty(Formatador.formatarMoeda(v));
        });
        colCapVenda.setPrefWidth(120);

        tabela.getColumns().addAll(colCod, colNome, colCat, colUn, colQtd, colPCusto, colCapCusto, colPVenda, colCapVenda);
        tabela.setItems(FXCollections.observableArrayList(todosProdutos));

        // Toolbar com filtro e botão PDF
        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Buscar produto...");
        txtBusca.getStyleClass().add("campo-busca");
        txtBusca.setPrefWidth(260);
        txtBusca.textProperty().addListener((o, v, n) -> {
            String filtro = n.toLowerCase();
            tabela.setItems(FXCollections.observableArrayList(
                todosProdutos.stream().filter(p ->
                    p.getNome().toLowerCase().contains(filtro) ||
                    nvl(p.getCodigo(), "").toLowerCase().contains(filtro)
                ).toList()));
        });

        Button btnPDF = new Button("📄 Exportar PDF");
        btnPDF.getStyleClass().add("btn-primario");
        btnPDF.setDisable(todosProdutos.isEmpty());

        btnPDF.setOnAction(e -> {
            List<Produto> dados = new java.util.ArrayList<>(tabela.getItems());
            if (dados.isEmpty()) { Alerta.aviso("Atenção", "Nenhum produto para exportar."); return; }
            btnPDF.setDisable(true);
            btnPDF.setText("⏳ Gerando PDF...");
            Task<String> task = new Task<>() {
                @Override protected String call() { return RelatorioCapitalPDF.gerar(dados); }
            };
            task.setOnSucceeded(ev -> Platform.runLater(() -> {
                btnPDF.setDisable(false);
                btnPDF.setText("📄 Exportar PDF");
                String caminho = task.getValue();
                if (caminho != null) {
                    try { java.awt.Desktop.getDesktop().open(new java.io.File(caminho)); }
                    catch (Exception ex) { Alerta.info("PDF Gerado", "Arquivo salvo em:\n" + caminho); }
                } else {
                    Alerta.erro("Erro", "Não foi possível gerar o PDF.");
                }
            }));
            task.setOnFailed(ev -> Platform.runLater(() -> {
                btnPDF.setDisable(false);
                btnPDF.setText("📄 Exportar PDF");
                Alerta.erro("Erro", "Erro ao gerar PDF: " + task.getException().getMessage());
            }));
            new Thread(task).start();
        });

        HBox toolbar = new HBox(12, txtBusca, new Region(), btnPDF);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("💰 Capital total em estoque (produtos com qty > 0)");
        lbl.setStyle("-fx-text-fill: #4dabf7; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 0 4 0;");

        box.getChildren().addAll(lbl, resumo, toolbar, tabela);
        return box;
    }

    private static String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private Label criarLabelResumo(String valor, String titulo, String cor) {
        Label lbl = new Label(valor);
        lbl.setStyle("-fx-text-fill: " + cor + "; -fx-font-size: 24px; -fx-font-weight: bold;");
        return lbl;
    }

    @SuppressWarnings("unchecked")
    private VBox criarAbaCancelamentos() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #1e2027;");

        DatePicker dpInicio = new DatePicker(LocalDate.now());
        DatePicker dpFim    = new DatePicker(LocalDate.now());
        Button btnFiltrar   = new Button("🔍 Buscar Cancelamentos");
        btnFiltrar.getStyleClass().add("btn-primario");
        Button btnHoje = new Button("📅 Hoje");
        btnHoje.getStyleClass().add("btn-secundario");
        Button btnMes  = new Button("📆 Este Mês");
        btnMes.getStyleClass().add("btn-secundario");
        Button btnPDF  = new Button("📄 Exportar PDF");
        btnPDF.getStyleClass().add("btn-secundario");
        btnPDF.setDisable(true);

        HBox toolbar = new HBox(10, new Label("De:"), dpInicio, new Label("Até:"), dpFim,
                btnHoje, btnMes, btnFiltrar, btnPDF);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Resumo
        Label lblQtdCanc  = criarLabelResumo("0",       "Cancelamentos", "#fa5252");
        Label lblTotalCanc = criarLabelResumo("R$ 0,00", "Valor Cancelado", "#ffa94d");
        HBox resumo = new HBox(16,
                miniCardR("Cancelamentos",   lblQtdCanc),
                miniCardR("Valor Cancelado", lblTotalCanc));

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

        TableColumn<Venda, String> colOp = new TableColumn<>("Operador");
        colOp.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUsuarioNome() != null ? c.getValue().getUsuarioNome() : "—"));
        colOp.setPrefWidth(150);

        TableColumn<Venda, String> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getSubtotal())));
        colSubtotal.setPrefWidth(110);

        TableColumn<Venda, String> colDescCanc = new TableColumn<>("Desconto");
        colDescCanc.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getDesconto())));
        colDescCanc.setPrefWidth(100);

        TableColumn<Venda, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getTotal())));
        colTotal.setPrefWidth(110);

        TableColumn<Venda, String> colReemb = new TableColumn<>("Reembolsado");
        colReemb.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isReembolsado() ? "✅ Sim" : "—"));
        colReemb.setPrefWidth(110);

        TableColumn<Venda, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getObservacoes() != null ? c.getValue().getObservacoes() : "—"));
        colMotivo.setPrefWidth(300);
        colMotivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: #ffa94d;");
            }
        });

        tabela.getColumns().addAll(colNum, colData, colOp, colSubtotal, colDescCanc, colTotal, colReemb, colMotivo);

        final List<Venda>[] dadosAtual = new List[1];

        Runnable carregar = () -> {
            List<Venda> lista = vendaDAO.listarCanceladas(dpInicio.getValue(), dpFim.getValue());
            dadosAtual[0] = lista;
            tabela.setItems(FXCollections.observableArrayList(lista));
            lblQtdCanc.setText(String.valueOf(lista.size()));
            double totalCanc = lista.stream().mapToDouble(Venda::getTotal).sum();
            lblTotalCanc.setText(Formatador.formatarMoeda(totalCanc));
            btnPDF.setDisable(lista.isEmpty());
        };

        btnFiltrar.setOnAction(e -> carregar.run());
        btnHoje.setOnAction(e -> { dpInicio.setValue(LocalDate.now()); dpFim.setValue(LocalDate.now()); carregar.run(); });
        btnMes.setOnAction(e -> { dpInicio.setValue(LocalDate.now().withDayOfMonth(1)); dpFim.setValue(LocalDate.now()); carregar.run(); });
        Platform.runLater(carregar::run);

        btnPDF.setOnAction(e -> {
            if (dadosAtual[0] == null || dadosAtual[0].isEmpty()) { Alerta.aviso("Atenção", "Nenhum dado para exportar."); return; }
            btnPDF.setDisable(true);
            btnPDF.setText("⏳ Gerando PDF...");
            List<Venda> dados = dadosAtual[0];
            LocalDate ini = dpInicio.getValue(), fim = dpFim.getValue();
            javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
                @Override protected String call() {
                    return com.erp.util.RelatorioCancelamentosPDF.gerar(ini, fim, dados);
                }
            };
            task.setOnSucceeded(ev -> Platform.runLater(() -> {
                btnPDF.setDisable(false); btnPDF.setText("📄 Exportar PDF");
                String path = task.getValue();
                if (path != null) { try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); } catch (Exception ex) { Alerta.info("PDF", "Salvo em:\n" + path); } }
                else Alerta.erro("Erro", "Não foi possível gerar o PDF.");
            }));
            task.setOnFailed(ev -> Platform.runLater(() -> { btnPDF.setDisable(false); btnPDF.setText("📄 Exportar PDF"); Alerta.erro("Erro", task.getException().getMessage()); }));
            new Thread(task).start();
        });

        box.getChildren().addAll(toolbar, resumo, tabela);
        return box;
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
