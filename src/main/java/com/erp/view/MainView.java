package com.erp.view;

import com.erp.util.Sessao;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainView {

    private final Stage stage;
    private BorderPane root;
    private Button botaoAtivo;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    public Scene criarScene() {
        root = new BorderPane();
        root.setCenter(new DashboardView().criar());
        root.setLeft(criarSidebar());

        // Usa 90% da tela como tamanho inicial; maximiza automaticamente
        javafx.geometry.Rectangle2D tela = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, tela.getWidth() * 0.9, tela.getHeight() * 0.9);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // Maximiza a janela principal
        stage.setMaximized(true);

        // Verifica atualização em background ao abrir (silencioso — só avisa se tiver nova versão)
        javafx.application.Platform.runLater(() -> AtualizacaoView.verificar(true));

        return scene;
    }

    private javafx.scene.control.ScrollPane criarSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Largura proporcional: 15% da tela, mínimo 200px, máximo 260px
        double telW = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double sideW = Math.max(200, Math.min(260, telW * 0.15));
        sidebar.setPrefWidth(sideW);
        sidebar.setMinWidth(sideW);
        sidebar.setMaxWidth(sideW);

        Label logo = new Label("🛒 DRS ERP");
        logo.getStyleClass().add("sidebar-logo");

        Label versaoSidebar = new Label(com.erp.util.AppInfo.getVersaoCompleta());
        versaoSidebar.setStyle("-fx-text-fill: #3d4059; -fx-font-size: 10px; -fx-padding: 0 20 8 20;");

        Label usuario = new Label("👤 " + Sessao.getInstance().getUsuario().getNome());
        usuario.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 11px; -fx-padding: 0 20 12 20;");

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));

        // Criar botões primeiro (sem auto-referência)
        Button btnDashboard    = menuBtn("📊  Dashboard");
        Button btnVendas       = menuBtn("🛒  Vendas / PDV");
        Button btnOrcamentos   = menuBtn("📋  Orçamentos");
        Button btnClientes     = menuBtn("👥  Clientes");
        Button btnProdutos     = menuBtn("📦  Produtos");
        Button btnFornecedores = menuBtn("🏭  Fornecedores");
        Button btnEstoque      = menuBtn("📋  Estoque");
        Button btnFinanceiro   = menuBtn("💰  Financeiro");
        Button btnRH           = menuBtn("👔  Funcionários");
        Button btnRelatorios   = menuBtn("📈  Relatórios");
        Button btnEmpresa      = menuBtn("🏢  Empresa");
        Button btnConfigNFe    = menuBtn("⚙️   Config. NF-e");
        Button btnNotasFiscais = menuBtn("🧾  Notas Fiscais");
        Button btnCaixa        = menuBtn("🏦  Caixa");
        Button btnRelFiscal    = menuBtn("📊  Relatórios Fiscais");
        Button btnLojas        = menuBtn("🏪  Lojas");

        // Configurar ações depois (botões já existem, sem problema de inicialização)
        btnDashboard   .setOnAction(e -> navegar(new DashboardView().criar(),       btnDashboard));
        btnVendas      .setOnAction(e -> navegar(new VendaView().criar(),           btnVendas));
        btnOrcamentos  .setOnAction(e -> navegar(new OrcamentoView().criar(),       btnOrcamentos));
        btnClientes    .setOnAction(e -> navegar(new ClienteView().criar(),         btnClientes));
        btnProdutos    .setOnAction(e -> navegar(new ProdutoView().criar(),         btnProdutos));
        btnFornecedores.setOnAction(e -> navegar(new FornecedorView().criar(),      btnFornecedores));
        btnEstoque     .setOnAction(e -> navegar(new EstoqueView().criar(),         btnEstoque));
        btnFinanceiro  .setOnAction(e -> navegar(new FinanceiroView().criar(),      btnFinanceiro));
        btnRH          .setOnAction(e -> navegar(new FuncionarioView().criar(),     btnRH));
        btnCaixa       .setOnAction(e -> navegar(new CaixaView().criar(),           btnCaixa));
        btnRelatorios  .setOnAction(e -> navegar(new RelatorioView().criar(),       btnRelatorios));
        btnRelFiscal   .setOnAction(e -> navegar(new RelatorioFiscalView().criar(), btnRelFiscal));
        btnEmpresa     .setOnAction(e -> navegar(new EmpresaView().criar(),         btnEmpresa));
        btnConfigNFe   .setOnAction(e -> navegar(new NFeConfiguracaoView().criar(), btnConfigNFe));
        btnNotasFiscais.setOnAction(e -> navegar(new NFeView().criar(),             btnNotasFiscais));
        btnLojas       .setOnAction(e -> navegar(new LojaView().criar(),            btnLojas));

        Label secPrincipal  = secLabel("PRINCIPAL");
        Label secCadastros  = secLabel("CADASTROS");
        Label secGestao     = secLabel("GESTÃO");
        Label secRelLabel   = secLabel("RELATÓRIOS");
        Label secConfig     = secLabel("CONFIGURAÇÕES");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnSair = new Button("🚪  Sair do Sistema");
        btnSair.getStyleClass().add("sidebar-item");
        btnSair.setStyle("-fx-text-fill: #fa5252;");
        btnSair.setMaxWidth(Double.MAX_VALUE);
        btnSair.setOnAction(e -> {
            Sessao.getInstance().encerrar();
            stage.setMaximized(false);
            javafx.geometry.Rectangle2D tela2 = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setWidth(Math.min(800, tela2.getWidth() * 0.7));
            stage.setHeight(Math.min(620, tela2.getHeight() * 0.75));
            stage.setScene(new LoginView(stage).criarScene());
            stage.centerOnScreen();
        });

        Button btnAtualizacao = new Button("🔄  Verificar Atualização");
        btnAtualizacao.getStyleClass().add("sidebar-item");
        btnAtualizacao.setStyle("-fx-text-fill: #74c0fc;");
        btnAtualizacao.setMaxWidth(Double.MAX_VALUE);
        btnAtualizacao.setOnAction(e -> AtualizacaoView.verificar(false));

        sidebar.getChildren().addAll(
                logo, versaoSidebar, usuario, sep,
                secPrincipal, btnDashboard, btnVendas, btnOrcamentos,
                secCadastros, btnClientes, btnProdutos, btnFornecedores,
                secGestao, btnEstoque, btnFinanceiro, btnRH, btnCaixa,
                secRelLabel, btnRelatorios, btnRelFiscal,
                secConfig, btnEmpresa, btnConfigNFe, btnNotasFiscais, btnLojas,
                spacer, btnAtualizacao, btnSair
        );

        ativarBotao(btnDashboard);

        // Envolve sidebar em ScrollPane para telas pequenas
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(sidebar);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: -cor-sidebar; -fx-background: -cor-sidebar; -fx-border-color: transparent;");
        scroll.setPrefWidth(sideW);
        scroll.setMinWidth(sideW);
        scroll.setMaxWidth(sideW);
        return scroll;
    }

    /** Cria um botão de menu sem ação (ação é configurada depois para evitar auto-referência). */
    private Button menuBtn(String texto) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    private Label secLabel(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("sidebar-titulo");
        return l;
    }

    private void navegar(Region conteudo, Button btn) {
        root.setCenter(conteudo);
        ativarBotao(btn);
    }

    private void ativarBotao(Button btn) {
        if (botaoAtivo != null) {
            botaoAtivo.getStyleClass().remove("sidebar-item-ativo");
        }
        btn.getStyleClass().add("sidebar-item-ativo");
        botaoAtivo = btn;
    }
}
