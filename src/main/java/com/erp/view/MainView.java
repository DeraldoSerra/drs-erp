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

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        return scene;
    }

    private VBox criarSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        Label logo = new Label("🛒 DRS ERP");
        logo.getStyleClass().add("sidebar-logo");

        Label versaoSidebar = new Label(com.erp.util.AppInfo.getVersaoCompleta());
        versaoSidebar.setStyle("-fx-text-fill: #3d4059; -fx-font-size: 10px; -fx-padding: 0 20 8 20;");

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
            stage.setScene(new LoginView(stage).criarScene());
            stage.setMaximized(false);
            stage.centerOnScreen();
        });

        sidebar.getChildren().addAll(
                logo, versaoSidebar, usuario, sep,
                secPrincipal, btnDashboard, btnVendas, btnOrcamentos,
                secCadastros, btnClientes, btnProdutos, btnFornecedores,
                secGestao, btnEstoque, btnFinanceiro, btnRH, btnCaixa,
                secRelLabel, btnRelatorios, btnRelFiscal,
                secConfig, btnEmpresa, btnConfigNFe, btnNotasFiscais, btnLojas,
                spacer, btnSair
        );

        ativarBotao(btnDashboard);
        return sidebar;
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
