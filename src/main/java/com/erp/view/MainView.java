package com.erp.view;

import com.erp.util.ResolutionUtil;
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
        // Aplica escala de fonte responsiva baseada na resolução da tela
        root.setStyle(ResolutionUtil.estiloRaiz());

        // Maximiza a janela principal
        stage.setMaximized(true);

        // Verifica atualização em background ao abrir (silencioso — só avisa se tiver nova versão)
        javafx.application.Platform.runLater(() -> AtualizacaoView.verificar(true));

        return scene;
    }

    private javafx.scene.control.ScrollPane criarSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Largura proporcional: 15% da tela, escalada pelo ResolutionUtil
        double telW = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double sideW = Math.max(ResolutionUtil.px(200), Math.min(ResolutionUtil.px(260), telW * 0.15));
        sidebar.setPrefWidth(sideW);
        sidebar.setMinWidth(sideW);
        sidebar.setMaxWidth(sideW);

        javafx.scene.image.ImageView logoImg = new javafx.scene.image.ImageView();
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/icon.png");
            if (is != null) {
                logoImg.setImage(new javafx.scene.image.Image(is));
                logoImg.setFitWidth(28);
                logoImg.setFitHeight(28);
                logoImg.setPreserveRatio(true);
                logoImg.setSmooth(true);
            }
        } catch (Exception ignored) {}
        Label logo = new Label("DRS ERP", logoImg);
        logo.getStyleClass().add("sidebar-logo");
        logo.setGraphicTextGap(8);

        Label versaoSidebar = new Label(com.erp.util.AppInfo.getVersaoCompleta());
        versaoSidebar.setStyle("-fx-text-fill: #3d4059; " + ResolutionUtil.fs(10) + " -fx-padding: 0 20 8 20;");

        Label usuario = new Label("👤 " + Sessao.getInstance().getUsuario().getNome());
        usuario.setStyle("-fx-text-fill: #5a5d6e; " + ResolutionUtil.fs(11) + " -fx-padding: 0 20 12 20;");

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
        Button btnImpressora   = menuBtn("🖨️   Impressora");

        // Verificar perfil do usuário logado
        com.erp.model.Usuario usuarioLogado = Sessao.getInstance().getUsuario();
        boolean isAdmin    = usuarioLogado != null && "ADMIN".equals(usuarioLogado.getPerfil());
        boolean isGerente  = usuarioLogado != null && (isAdmin || "GERENTE".equals(usuarioLogado.getPerfil()));
        boolean isOperador = usuarioLogado != null && !isAdmin && !isGerente;

        // Verificar se a empresa habilitou NF-e
        boolean nfeHabilitado = false;
        if (isAdmin) {
            try {
                com.erp.dao.EmpresaDAO empresaDAO = new com.erp.dao.EmpresaDAO();
                nfeHabilitado = empresaDAO.carregar()
                        .map(com.erp.model.Empresa::isHabilitaNFe)
                        .orElse(false);
            } catch (Exception ignored) {}
        }

        // Configurar ações depois (botões já existem, sem problema de inicialização)
        btnDashboard   .setOnAction(e -> navegar(new DashboardView().criar(),       btnDashboard));
        btnVendas      .setOnAction(e -> navegar(new VendaView().criar(),           btnVendas));
        btnOrcamentos  .setOnAction(e -> navegar(new OrcamentoView().criar(),       btnOrcamentos));
        btnClientes    .setOnAction(e -> navegar(new ClienteView().criar(),         btnClientes));
        btnProdutos    .setOnAction(e -> navegar(
                isOperador ? new ProdutoView().criarConsulta() : new ProdutoView().criar(),
                btnProdutos));
        btnFornecedores.setOnAction(e -> navegar(new FornecedorView().criar(),      btnFornecedores));
        btnEstoque     .setOnAction(e -> navegar(new EstoqueView().criar(),         btnEstoque));
        btnFinanceiro  .setOnAction(e -> navegar(new FinanceiroView().criar(),      btnFinanceiro));
        btnRH          .setOnAction(e -> navegar(new FuncionarioView().criar(),     btnRH));
        btnCaixa       .setOnAction(e -> navegar(new CaixaView().criar(),           btnCaixa));
        btnRelatorios  .setOnAction(e -> navegar(new RelatorioView().criar(),       btnRelatorios));
        btnRelFiscal   .setOnAction(e -> navegar(new RelatorioFiscalView().criar(), btnRelFiscal));
        btnEmpresa     .setOnAction(e -> navegar(new EmpresaView().criar(),                       btnEmpresa));
        btnConfigNFe   .setOnAction(e -> navegar(new NFeConfiguracaoView().criar(),               btnConfigNFe));
        btnNotasFiscais.setOnAction(e -> navegar(new NFeView().criar(),                           btnNotasFiscais));
        btnLojas       .setOnAction(e -> navegar(new LojaView().criar(),                          btnLojas));
        btnImpressora  .setOnAction(e -> navegar(new ConfiguracaoImpressoraView().criar(),        btnImpressora));

        Label secPrincipal  = secLabel("PRINCIPAL");
        Label secCadastros  = secLabel("CADASTROS");
        Label secGestao     = secLabel("GESTÃO");
        Label secRelLabel   = secLabel("RELATÓRIOS");
        Label secConfig     = secLabel("CONFIGURAÇÕES");

        // === Restrições de perfil ===
        // SOMENTE ADMIN: Lojas, Funcionários
        btnLojas       .setVisible(isAdmin); btnLojas       .setManaged(isAdmin);
        btnRH          .setVisible(isAdmin); btnRH          .setManaged(isAdmin);

        // GERENTE ou ADMIN: Empresa (dados fiscais da empresa)
        btnEmpresa     .setVisible(isGerente); btnEmpresa     .setManaged(isGerente);

        // Config NF-e e Notas Fiscais: ADMIN + empresa com habilitaNFe = true
        boolean verNFe = isAdmin && nfeHabilitado;
        btnConfigNFe   .setVisible(isAdmin);  btnConfigNFe   .setManaged(isAdmin);
        btnNotasFiscais.setVisible(verNFe);   btnNotasFiscais.setManaged(verNFe);

        // Seção CONFIGURAÇÕES aparece para gerente ou admin
        secConfig.setVisible(isGerente); secConfig.setManaged(isGerente);

        // GERENTE ou ADMIN: Financeiro, Relatórios Fiscais
        btnFinanceiro  .setVisible(isGerente); btnFinanceiro  .setManaged(isGerente);
        btnRelFiscal   .setVisible(isGerente); btnRelFiscal   .setManaged(isGerente);

        // Seção CONFIGURAÇÕES só aparece para admins
        secConfig.setVisible(isAdmin); secConfig.setManaged(isAdmin);

        // Impressora: GERENTE ou ADMIN
        btnImpressora  .setVisible(isGerente); btnImpressora  .setManaged(isGerente);

        // OPERADOR: apenas Vendas, Produtos (consulta) e Caixa
        boolean naoOperador = !isOperador;
        btnDashboard   .setVisible(naoOperador); btnDashboard   .setManaged(naoOperador);
        btnOrcamentos  .setVisible(naoOperador); btnOrcamentos  .setManaged(naoOperador);
        btnClientes    .setVisible(naoOperador); btnClientes    .setManaged(naoOperador);
        btnFornecedores.setVisible(naoOperador); btnFornecedores.setManaged(naoOperador);
        btnEstoque     .setVisible(naoOperador); btnEstoque     .setManaged(naoOperador);
        btnRelatorios  .setVisible(naoOperador); btnRelatorios  .setManaged(naoOperador);
        // Seções de menu: ocultar labels que ficam sem conteúdo para OPERADOR
        secPrincipal   .setVisible(naoOperador); secPrincipal   .setManaged(naoOperador);
        secCadastros   .setVisible(naoOperador); secCadastros   .setManaged(naoOperador);
        secRelLabel    .setVisible(naoOperador); secRelLabel    .setManaged(naoOperador);

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
                secConfig, btnEmpresa, btnConfigNFe, btnNotasFiscais, btnLojas, btnImpressora,
                spacer, btnAtualizacao, btnSair
        );

        // Para OPERADOR, iniciar diretamente na tela de Vendas
        if (isOperador) {
            root.setCenter(new VendaView().criar());
            ativarBotao(btnVendas);
        } else {
            ativarBotao(btnDashboard);
        }

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
