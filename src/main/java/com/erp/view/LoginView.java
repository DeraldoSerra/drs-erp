package com.erp.view;

import com.erp.config.DatabaseConfig;
import com.erp.dao.LojaDAO;
import com.erp.dao.UsuarioDAO;
import com.erp.model.Loja;
import com.erp.model.Usuario;
import com.erp.util.Alerta;
import com.erp.util.ConsultaReceitaWS;
import com.erp.util.Sessao;
import com.erp.util.ValidadorFiscal;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class LoginView {

    private final Stage stage;
    private Scene scene;
    private StackPane root;

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public Scene criarScene() {
        root = new StackPane();
        root.getStyleClass().add("login-container");

        // Inicializa banco antes de mostrar seleção de loja
        try { DatabaseConfig.inicializar(); } catch (Exception ignored) {}

        mostrarSelecaoLoja();

        // Tamanho proporcional à tela (mín 700x550, máx 1100x800)
        javafx.geometry.Rectangle2D tela = javafx.stage.Screen.getPrimary().getVisualBounds();
        double w = Math.max(700, Math.min(1100, tela.getWidth()  * 0.65));
        double h = Math.max(550, Math.min(800,  tela.getHeight() * 0.75));
        scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        return scene;
    }

    // =========================================================
    // TELA 1 — Seleção de Loja
    // =========================================================
    private void mostrarSelecaoLoja() {
        VBox container = new VBox(24);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(640);
        container.setPadding(new Insets(40));

        Label titulo = new Label("🛒 DRS ERP");
        titulo.getStyleClass().add("login-titulo");

        Label versaoLabel = new Label(com.erp.util.AppInfo.getVersaoCompleta());
        versaoLabel.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 11px;");

        Label subtitulo = new Label("Selecione a loja para acessar");
        subtitulo.getStyleClass().add("login-subtitulo");


        // DB config expandível
        TitledPane dbPane = criarPainelBanco();
        dbPane.setExpanded(false);
        dbPane.setMaxWidth(400);

        // Carregar lojas
        List<Loja> lojas = carregarLojas();

        HBox lojasBox = new HBox(24);
        lojasBox.setAlignment(Pos.CENTER);

        // Ícones alternados por loja
        String[] icones = {"🏪", "🏬", "🏢", "🏭"};
        String[] cores  = {"-cor-primaria", "-cor-secundaria", "-cor-sucesso", "-cor-aviso"};

        final Loja[] lojaSelecionada = {lojas.isEmpty() ? null : lojas.get(0)};
        VBox[] cards = new VBox[lojas.size()];

        for (int i = 0; i < lojas.size(); i++) {
            Loja loja = lojas.get(i);
            int idx = i;

            VBox card = new VBox(12);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().addAll("loja-card");

            Label icone = new Label(icones[i % icones.length]);
            icone.getStyleClass().add("loja-icone");

            Label nome = new Label(loja.getNome());
            nome.getStyleClass().add("loja-nome");
            nome.setWrapText(true);
            nome.setAlignment(Pos.CENTER);

            Label info = new Label(loja.getCnpj() != null && !loja.getCnpj().isBlank()
                    ? "CNPJ: " + loja.getCnpj() : "Loja " + (i + 1));
            info.getStyleClass().add("loja-info");

            card.getChildren().addAll(icone, nome, info);
            cards[i] = card;

            // Seleciona ao clicar
            card.setOnMouseClicked(e -> {
                lojaSelecionada[0] = loja;
                for (VBox c : cards) c.getStyleClass().remove("loja-card-selecionado");
                card.getStyleClass().add("loja-card-selecionado");
            });

            lojasBox.getChildren().add(card);
        }

        // Marca a primeira como selecionada por padrão
        if (!lojas.isEmpty()) cards[0].getStyleClass().add("loja-card-selecionado");

        Button btnContinuar = new Button("Continuar  ➜");
        btnContinuar.getStyleClass().add("btn-primario");
        btnContinuar.setPrefWidth(220);
        btnContinuar.setPrefHeight(44);
        btnContinuar.setStyle("-fx-font-size: 14px;");

        Button btnCadastrar = new Button("➕ Cadastrar Nova Loja");
        btnCadastrar.getStyleClass().add("btn-secundario");
        btnCadastrar.setPrefWidth(220);
        btnCadastrar.setPrefHeight(36);

        btnCadastrar.setOnAction(e -> {
            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("Nova Loja");
            dlg.setHeaderText("Cadastrar Nova Loja");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dlg.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(12);
            grid.setPadding(new Insets(16));

            TextField txtToken = new TextField();
            Label     lblTokenStatus = new Label();
            TextField txtNome = new TextField();
            TextField txtCnpj = new TextField();
            TextField txtEnd  = new TextField();
            Label     lblStatus = new Label();
            Button    btnConsultar = new Button("🔍 Consultar CNPJ");

            txtToken.setPromptText("XXXXXXXX-XXXX-XXXX");
            txtToken.setPrefWidth(260);
            txtToken.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
            lblTokenStatus.setStyle("-fx-font-size: 11px;");

            // Validar token ao digitar
            txtToken.textProperty().addListener((obs, o, nv) -> {
                String t = nv.trim().toUpperCase();
                if (t.length() >= 18) {
                    if (new com.erp.dao.TokenLojaDAO().tokenValido(t)) {
                        lblTokenStatus.setText("✅ Token válido");
                        lblTokenStatus.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");
                    } else {
                        lblTokenStatus.setText("❌ Token inválido ou já utilizado");
                        lblTokenStatus.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px;");
                    }
                } else {
                    lblTokenStatus.setText("");
                }
            });

            txtNome.setPromptText("Ex: Loja Centro");
            txtCnpj.setPromptText("00.000.000/0000-00");
            txtEnd.setPromptText("Rua, número, bairro, cidade");
            txtNome.setPrefWidth(260);
            txtCnpj.setPrefWidth(200);
            txtEnd.setPrefWidth(260);
            lblStatus.setStyle("-fx-font-size: 11px;");
            btnConsultar.getStyleClass().add("btn-secundario");
            btnConsultar.setDisable(true);

            // Máscara e validação ao digitar CNPJ
            txtCnpj.textProperty().addListener((obs, oldVal, newVal) -> {
                String masked = ValidadorFiscal.aplicarMascaraCpfCnpj(newVal);
                if (!masked.equals(newVal)) {
                    txtCnpj.setText(masked);
                    return;
                }
                String nums = ValidadorFiscal.apenasNumeros(newVal);
                if (nums.length() == 14) {
                    if (ValidadorFiscal.validarCNPJ(nums)) {
                        lblStatus.setText("✅ CNPJ válido");
                        lblStatus.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");
                        btnConsultar.setDisable(false);
                    } else {
                        lblStatus.setText("❌ CNPJ inválido");
                        lblStatus.setStyle("-fx-text-fill: #f44336; -fx-font-size: 11px;");
                        btnConsultar.setDisable(true);
                    }
                } else {
                    lblStatus.setText("");
                    btnConsultar.setDisable(true);
                }
            });

            // Consultar CNPJ na Receita Federal
            btnConsultar.setOnAction(ev -> {
                btnConsultar.setDisable(true);
                btnConsultar.setText("⏳ Consultando...");
                String cnpjAtual = txtCnpj.getText();
                Thread t = new Thread(() -> {
                    ConsultaReceitaWS.DadosCNPJ dados = ConsultaReceitaWS.consultar(cnpjAtual);
                    Platform.runLater(() -> {
                        btnConsultar.setText("🔍 Consultar CNPJ");
                        btnConsultar.setDisable(false);
                        if (dados.valido) {
                            String nomeEmpresa = dados.nomeFantasia != null && !dados.nomeFantasia.isBlank()
                                    ? dados.nomeFantasia : dados.razaoSocial;
                            if (txtNome.getText().isBlank()) txtNome.setText(nomeEmpresa);
                            String end = "";
                            if (!dados.logradouro.isBlank()) end += dados.logradouro;
                            if (!dados.numero.isBlank())     end += ", " + dados.numero;
                            if (!dados.bairro.isBlank())     end += " - " + dados.bairro;
                            if (!dados.municipio.isBlank())  end += ", " + dados.municipio;
                            if (!dados.uf.isBlank())         end += "/" + dados.uf;
                            if (txtEnd.getText().isBlank())  txtEnd.setText(end);
                            lblStatus.setText("✅ " + dados.razaoSocial);
                            lblStatus.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");
                        } else {
                            Alerta.aviso("CNPJ", dados.mensagemErro != null ? dados.mensagemErro : "CNPJ não encontrado");
                        }
                    });
                });
                t.setDaemon(true);
                t.start();
            });

            Label lToken = new Label("Token*:");
            Label lNome = new Label("Nome*:");
            Label lCnpj = new Label("CNPJ:");
            Label lEnd  = new Label("Endereço:");
            lToken.setStyle("-fx-text-fill: #9e9e9e;");
            lNome.setStyle("-fx-text-fill: #9e9e9e;");
            lCnpj.setStyle("-fx-text-fill: #9e9e9e;");
            lEnd.setStyle("-fx-text-fill: #9e9e9e;");

            HBox cnpjBox = new HBox(8, txtCnpj, btnConsultar);
            cnpjBox.setAlignment(Pos.CENTER_LEFT);

            grid.add(lToken, 0, 0); grid.add(txtToken,  1, 0);
            grid.add(new Label(), 0, 1); grid.add(lblTokenStatus, 1, 1);
            grid.add(lNome, 0, 2); grid.add(txtNome,  1, 2);
            grid.add(lCnpj, 0, 3); grid.add(cnpjBox,  1, 3);
            grid.add(new Label(), 0, 4); grid.add(lblStatus, 1, 4);
            grid.add(lEnd,  0, 5); grid.add(txtEnd,   1, 5);

            dlg.getDialogPane().setContent(grid);

            // Desabilita OK enquanto token/CNPJ inválidos
            Button btnOk = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
            btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                String token = txtToken.getText().trim().toUpperCase();
                if (token.isBlank()) {
                    Alerta.aviso("Token obrigatório", "Informe o token de ativação para cadastrar uma nova loja.");
                    ev.consume();
                    return;
                }
                if (!new com.erp.dao.TokenLojaDAO().tokenValido(token)) {
                    Alerta.erro("Token inválido", "O token informado é inválido ou já foi utilizado.\nSolicite um novo token ao administrador.");
                    ev.consume();
                    return;
                }
                String cnpjDigitado = txtCnpj.getText().trim();
                if (!cnpjDigitado.isBlank()) {
                    String nums = ValidadorFiscal.apenasNumeros(cnpjDigitado);
                    if (!ValidadorFiscal.validarCNPJ(nums)) {
                        Alerta.aviso("CNPJ inválido", "O CNPJ informado não é válido. Verifique e tente novamente.");
                        ev.consume();
                        return;
                    }
                }
                if (txtNome.getText().trim().isEmpty()) {
                    Alerta.aviso("Atenção", "O nome da loja é obrigatório.");
                    ev.consume();
                }
            });

            dlg.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    String token = txtToken.getText().trim().toUpperCase();
                    String nome = txtNome.getText().trim();
                    String cnpjFormatado = ValidadorFiscal.formatarCNPJ(ValidadorFiscal.apenasNumeros(txtCnpj.getText().trim()));
                    Loja nova = new Loja();
                    nova.setNome(nome);
                    nova.setCnpj(cnpjFormatado.isBlank() ? null : cnpjFormatado);
                    nova.setEndereco(txtEnd.getText().trim());
                    nova.setAtiva(true);
                    try {
                        if (new LojaDAO().salvar(nova)) {
                            // Marcar token como usado
                            new com.erp.dao.TokenLojaDAO().marcarUsado(token, nova.getId());
                            Alerta.info("Loja", "Loja \"" + nome + "\" cadastrada com sucesso!");
                            mostrarSelecaoLoja();
                        } else {
                            Alerta.erro("Erro", "Não foi possível cadastrar a loja.");
                        }
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        if (msg != null && msg.contains("lojas_cnpj_unique")) {
                            Alerta.erro("CNPJ Duplicado", "Já existe uma loja cadastrada com este CNPJ.");
                        } else {
                            Alerta.erro("Erro ao Cadastrar", "Falha: " + msg);
                        }
                    }
                }
            });
        });

        btnContinuar.setOnAction(e -> {
            if (lojaSelecionada[0] == null) {
                Alerta.aviso("Atenção", "Selecione uma loja para continuar.");
                return;
            }
            // Verificar bloqueio no servidor (checagem em tempo real)
            Loja loja = lojaSelecionada[0];
            new LojaDAO().buscarPorId(loja.getId()).ifPresentOrElse(lojaAtual -> {
                if (lojaAtual.isBloqueada()) {
                    String motivo = lojaAtual.getMotivoBloqueio();
                    String msg = "🔒 Acesso bloqueado!\n\n";
                    if (motivo != null && !motivo.isBlank()) {
                        msg += "Motivo: " + motivo + "\n\n";
                    }
                    msg += "Entre em contato com o suporte DRS para regularizar.";
                    Alerta.erro("Acesso Bloqueado", msg);
                    return;
                }
                Sessao.getInstance().setLojaId(lojaAtual.getId());
                Sessao.getInstance().setLojaNome(lojaAtual.getNome());
                trocarTela(criarPainelLogin());
            }, () -> {
                Alerta.erro("Erro", "Não foi possível verificar o status da loja.");
            });
        });

        container.getChildren().addAll(titulo, versaoLabel, subtitulo, dbPane, lojasBox, btnContinuar, btnCadastrar);

        root.getChildren().setAll(container);
        StackPane.setAlignment(container, Pos.CENTER);
        animar(container);
    }

    // =========================================================
    // TELA 2 — Login
    // =========================================================
    private VBox criarPainelLogin() {
        VBox card = new VBox(16);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setAlignment(Pos.TOP_CENTER);

        // Header com loja selecionada
        String nomeLoja = Sessao.getInstance().getLojaNome();
        Label lblLoja = new Label("🏪 " + nomeLoja);
        lblLoja.setStyle("-fx-background-color: #6c63ff22; -fx-text-fill: -cor-primaria; " +
                "-fx-background-radius: 20; -fx-padding: 6 16; -fx-font-weight: bold;");

        Label titulo = new Label("Bem-vindo de volta!");
        titulo.getStyleClass().add("login-titulo");
        titulo.setStyle("-fx-font-size: 22px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label subtitulo = new Label("Insira suas credenciais para entrar");
        subtitulo.getStyleClass().add("login-subtitulo");

        Separator sep = new Separator();

        Label lblLogin = new Label("Usuário");
        lblLogin.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        TextField txtLogin = new TextField();
        txtLogin.setPromptText("Digite seu login");

        Label lblSenha = new Label("Senha");
        lblSenha.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText("Digite sua senha");

        Label lblErro = new Label();
        lblErro.setStyle("-fx-text-fill: #fa5252; -fx-font-size: 12px;");

        Button btnEntrar = new Button("🔓  Entrar");
        btnEntrar.getStyleClass().add("btn-primario");
        btnEntrar.setMaxWidth(Double.MAX_VALUE);
        btnEntrar.setPrefHeight(42);
        btnEntrar.setDefaultButton(true);

        Button btnVoltar = new Button("← Trocar Loja");
        btnVoltar.getStyleClass().add("btn-secundario");
        btnVoltar.setMaxWidth(Double.MAX_VALUE);
        btnVoltar.setOnAction(e -> {
            Sessao.getInstance().encerrar();
            mostrarSelecaoLoja();
        });

        Button btnCadastrarUsuario = new Button("➕  Cadastrar Usuário");
        btnCadastrarUsuario.setMaxWidth(Double.MAX_VALUE);
        btnCadastrarUsuario.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: -cor-primaria; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-underline: true;");
        btnCadastrarUsuario.setOnAction(e -> abrirCadastroUsuario());

        btnEntrar.setOnAction(e -> {
            String login = txtLogin.getText().trim();
            String senha = txtSenha.getText();
            if (login.isEmpty() || senha.isEmpty()) {
                lblErro.setText("⚠ Preencha todos os campos.");
                return;
            }
            lblErro.setText("");
            btnEntrar.setDisable(true);
            btnEntrar.setText("Verificando...");

            new Thread(() -> {
                try {
                    UsuarioDAO dao = new UsuarioDAO();
                    dao.autenticar(login, senha).ifPresentOrElse(u -> {
                        Sessao.getInstance().iniciar(u);
                        javafx.application.Platform.runLater(this::abrirSistema);
                    }, () -> javafx.application.Platform.runLater(() -> {
                        lblErro.setText("❌ Login ou senha incorretos.");
                        btnEntrar.setDisable(false);
                        btnEntrar.setText("🔓  Entrar");
                        txtSenha.clear();
                        txtSenha.requestFocus();
                    }));
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        lblErro.setText("❌ Erro ao conectar ao banco de dados.");
                        btnEntrar.setDisable(false);
                        btnEntrar.setText("🔓  Entrar");
                    });
                }
            }, "thread-login").start();
        });

        card.getChildren().addAll(lblLoja, titulo, subtitulo, sep,
                lblLogin, txtLogin, lblSenha, txtSenha, lblErro, btnEntrar, btnVoltar,
                new Separator(), btnCadastrarUsuario);
        return card;
    }

    // =========================================================
    // Cadastro de Usuário
    // =========================================================
    private void abrirCadastroUsuario() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Cadastrar Usuário");
        dlg.setHeaderText("Novo usuário do sistema");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());
        dlg.getDialogPane().setPrefWidth(420);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        // Campos
        TextField txtNome  = new TextField();
        TextField txtLogin = new TextField();
        PasswordField txtSenha  = new PasswordField();
        PasswordField txtSenha2 = new PasswordField();

        txtNome.setPromptText("Nome completo");
        txtLogin.setPromptText("Login de acesso (sem espaços)");
        txtSenha.setPromptText("Mínimo 4 caracteres");
        txtSenha2.setPromptText("Repita a senha");

        // Perfil — cartões visuais
        Label lblPerfil = new Label("Perfil de acesso:");
        lblPerfil.setStyle("-fx-text-fill: #9e9e9e;");

        ToggleGroup tgPerfil = new ToggleGroup();

        ToggleButton tbFunc    = perfilToggle("👷 Funcionário",  "OPERADOR",  "Acesso ao PDV e operações básicas", tgPerfil);
        ToggleButton tbGerente = perfilToggle("👔 Gerente",      "GERENTE",   "Acesso completo exceto configurações", tgPerfil);
        ToggleButton tbAdmin   = perfilToggle("🛡️ Administrador","ADMIN",     "Acesso total ao sistema", tgPerfil);

        tbFunc.setSelected(true); // padrão: Funcionário

        HBox perfilBox = new HBox(10, tbFunc, tbGerente, tbAdmin);
        perfilBox.setAlignment(Pos.CENTER_LEFT);

        // Loja vinculada
        Label lblLoja = new Label("Loja:");
        lblLoja.setStyle("-fx-text-fill: #9e9e9e;");
        ComboBox<Loja> cbLoja = new ComboBox<>();
        cbLoja.setMaxWidth(Double.MAX_VALUE);
        try {
            cbLoja.getItems().addAll(new LojaDAO().listarAtivas());
            if (!cbLoja.getItems().isEmpty()) cbLoja.getSelectionModel().selectFirst();
        } catch (Exception ignored) {}

        // Label de erro inline
        Label lblErro = new Label();
        lblErro.setStyle("-fx-text-fill: #fa5252; -fx-font-size: 11px;");
        lblErro.setWrapText(true);

        // Estilo de labels
        String[] rotulos = {"Nome*:", "Login*:", "Senha*:", "Confirmar*:"};
        for (String r : rotulos) {
            Label l = new Label(r);
            l.setStyle("-fx-text-fill: #9e9e9e;");
        }

        int row = 0;
        addRow(grid, "Nome*:",        txtNome,  row++);
        addRow(grid, "Login*:",       txtLogin, row++);
        addRow(grid, "Senha*:",       txtSenha, row++);
        addRow(grid, "Confirmar*:",   txtSenha2,row++);
        grid.add(lblPerfil, 0, row); grid.add(perfilBox, 1, row++);
        grid.add(lblLoja,   0, row); grid.add(cbLoja,    1, row++);
        grid.add(lblErro, 0, row, 2, 1);

        dlg.getDialogPane().setContent(grid);

        // Desabilita OK até validar
        Button btnOk = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setText("✅  Cadastrar");

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            String nome   = txtNome.getText().trim();
            String login  = txtLogin.getText().trim().toLowerCase().replaceAll("\\s+", "");
            String senha  = txtSenha.getText();
            String senha2 = txtSenha2.getText();
            String perfil = (String) tgPerfil.getSelectedToggle().getUserData();
            Loja loja = cbLoja.getSelectionModel().getSelectedItem();

            // Validações
            if (nome.isEmpty() || login.isEmpty() || senha.isEmpty()) {
                Alerta.aviso("Atenção", "Preencha todos os campos obrigatórios (*).");
                return;
            }
            if (login.length() < 3) {
                Alerta.aviso("Login inválido", "O login deve ter pelo menos 3 caracteres.");
                return;
            }
            if (senha.length() < 4) {
                Alerta.aviso("Senha fraca", "A senha deve ter pelo menos 4 caracteres.");
                return;
            }
            if (!senha.equals(senha2)) {
                Alerta.erro("Senhas diferentes", "As senhas não conferem. Tente novamente.");
                return;
            }

            Usuario novo = new Usuario();
            novo.setNome(nome);
            novo.setLogin(login);
            novo.setPerfil(perfil);
            novo.setAtivo(true);

            boolean ok = new UsuarioDAO().salvar(novo, senha);
            if (ok) {
                String perfilNome = perfil.equals("OPERADOR") ? "Funcionário"
                        : perfil.equals("GERENTE") ? "Gerente" : "Administrador";
                Alerta.info("Usuário Cadastrado",
                        "✅ " + perfilNome + " \"" + nome + "\" cadastrado!\n\nLogin: " + login);
            } else {
                Alerta.erro("Erro", "Não foi possível cadastrar. O login \"" + login + "\" já pode estar em uso.");
            }
        });
    }

    private ToggleButton perfilToggle(String texto, String userData, String descricao, ToggleGroup group) {
        ToggleButton tb = new ToggleButton(texto);
        tb.setToggleGroup(group);
        tb.setUserData(userData);
        tb.setWrapText(true);
        tb.setPrefWidth(118);
        tb.setPrefHeight(56);
        tb.setStyle(
            "-fx-background-color: #2a2d3e; -fx-text-fill: #e0e0e0; " +
            "-fx-background-radius: 10; -fx-font-size: 11px; -fx-cursor: hand; " +
            "-fx-border-radius: 10; -fx-border-color: #3a3d4e; -fx-border-width: 2;");
        tb.selectedProperty().addListener((obs, old, sel) -> {
            if (sel) {
                tb.setStyle(
                    "-fx-background-color: #1e1c3a; -fx-text-fill: #6c63ff; " +
                    "-fx-background-radius: 10; -fx-font-size: 11px; -fx-cursor: hand; " +
                    "-fx-border-radius: 10; -fx-border-color: #6c63ff; -fx-border-width: 2; " +
                    "-fx-font-weight: bold;");
            } else {
                tb.setStyle(
                    "-fx-background-color: #2a2d3e; -fx-text-fill: #e0e0e0; " +
                    "-fx-background-radius: 10; -fx-font-size: 11px; -fx-cursor: hand; " +
                    "-fx-border-radius: 10; -fx-border-color: #3a3d4e; -fx-border-width: 2;");
            }
        });
        Tooltip.install(tb, new Tooltip(descricao));
        return tb;
    }

    private void addRow(GridPane grid, String rotulo, javafx.scene.control.Control campo, int row) {
        Label l = new Label(rotulo);
        l.setStyle("-fx-text-fill: #9e9e9e;");
        campo.setMaxWidth(Double.MAX_VALUE);
        grid.add(l, 0, row);
        grid.add(campo, 1, row);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }

    // =========================================================
    // Helpers
    // =========================================================
    private void trocarTela(VBox novoConteudo) {
        root.getChildren().setAll(novoConteudo);
        StackPane.setAlignment(novoConteudo, Pos.CENTER);
        animar(novoConteudo);
        // Auto-focus no campo login
        javafx.application.Platform.runLater(() -> {
            if (!novoConteudo.getChildren().isEmpty()) {
                novoConteudo.getChildren().stream()
                        .filter(n -> n instanceof TextField)
                        .findFirst()
                        .ifPresent(n -> n.requestFocus());
            }
        });
    }

    private void animar(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private List<Loja> carregarLojas() {
        try {
            return new LojaDAO().listarAtivas();
        } catch (Exception e) {
            // Fallback: loja padrão
            Loja padrao = new Loja();
            padrao.setId(1);
            padrao.setNome("Loja Principal");
            return List.of(padrao);
        }
    }

    private TitledPane criarPainelBanco() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        TextField txtHost  = new TextField(DatabaseConfig.getHost());
        TextField txtPorta = new TextField(String.valueOf(DatabaseConfig.getPorta()));
        TextField txtBanco = new TextField(DatabaseConfig.getBanco());
        TextField txtUser  = new TextField(DatabaseConfig.getUsuario());
        PasswordField txtPass = new PasswordField();
        txtPass.setText(DatabaseConfig.getSenha());

        grid.add(new Label("Host:"),    0, 0); grid.add(txtHost,  1, 0);
        grid.add(new Label("Porta:"),   0, 1); grid.add(txtPorta, 1, 1);
        grid.add(new Label("Banco:"),   0, 2); grid.add(txtBanco, 1, 2);
        grid.add(new Label("Usuário:"), 0, 3); grid.add(txtUser,  1, 3);
        grid.add(new Label("Senha:"),   0, 4); grid.add(txtPass,  1, 4);

        Button btnTestar = new Button("Testar Conexão");
        btnTestar.getStyleClass().add("btn-secundario");
        btnTestar.setOnAction(e -> {
            boolean ok = DatabaseConfig.testarConexao(
                    txtHost.getText(), Integer.parseInt(txtPorta.getText()),
                    txtBanco.getText(), txtUser.getText(), txtPass.getText());
            if (ok) {
                DatabaseConfig.inicializar(txtHost.getText(),
                        Integer.parseInt(txtPorta.getText()),
                        txtBanco.getText(), txtUser.getText(), txtPass.getText());
                Alerta.info("Conexão", "Conexão bem-sucedida!");
            } else {
                Alerta.erro("Erro", "Não foi possível conectar ao banco.");
            }
        });

        grid.add(btnTestar, 1, 5);
        TitledPane pane = new TitledPane("⚙ Configuração do Banco de Dados", grid);
        pane.setStyle("-fx-text-fill: #9e9e9e;");
        return pane;
    }

    private void abrirSistema() {
        MainView mainView = new MainView(stage);
        stage.setScene(mainView.criarScene());
        stage.setMaximized(true);
        stage.setTitle("DRS ERP — " + Sessao.getInstance().getUsuario().getNome()
                + " | " + Sessao.getInstance().getLojaNome());
    }
}
