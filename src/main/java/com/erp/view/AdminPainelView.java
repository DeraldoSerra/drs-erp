package com.erp.view;

import com.erp.dao.EmpresaDAO;
import com.erp.dao.LojaDAO;
import com.erp.dao.TokenLojaDAO;
import com.erp.model.Empresa;
import com.erp.model.Loja;
import com.erp.util.Alerta;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Painel do administrador DRS — acessível somente pelo botão 🛡️ pré-login.
 * Permite gerenciar lojas, empresa, tokens e usuários.
 */
public class AdminPainelView {

    public VBox criar() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        Label titulo = new Label("🛡️ Painel do Administrador DRS");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4dabf7;");

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(criarTabLojas(), criarTabEmpresa(), criarTabTokens(), criarTabTokensUsuario(), criarTabManutencao());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        root.getChildren().addAll(titulo, tabs);
        return root;
    }

    // ==================== ABA LOJAS ====================

    private Tab criarTabLojas() {
        Tab tab = new Tab("🏪 Lojas / Clientes");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TableView<Loja> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<Loja, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50);

        TableColumn<Loja, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Loja, String> colCnpj = new TableColumn<>("CNPJ");
        colCnpj.setCellValueFactory(new PropertyValueFactory<>("cnpj"));

        TableColumn<Loja, String> colEnd = new TableColumn<>("Endereço");
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endereco"));

        TableColumn<Loja, Boolean> colAtiva = new TableColumn<>("Ativa");
        colAtiva.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v ? "✅" : "❌");
            }
        });
        colAtiva.setCellValueFactory(new PropertyValueFactory<>("ativa"));

        TableColumn<Loja, Boolean> colStatus = new TableColumn<>("Acesso");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                Loja l = (Loja) getTableRow().getItem();
                if (l.isBloqueada()) {
                    setText("🔒 BLOQUEADA");
                    setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                } else {
                    setText("🟢 LIBERADA");
                    setStyle("-fx-text-fill: #51cf66; -fx-font-weight: bold;");
                }
            }
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("bloqueada"));

        TableColumn<Loja, String> colMotivo = new TableColumn<>("Motivo Bloqueio");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivoBloqueio"));

        tabela.getColumns().addAll(colId, colNome, colCnpj, colEnd, colAtiva, colStatus, colMotivo);

        // ---- Botões ----
        Button btnNova       = new Button("➕ Nova Loja");
        Button btnEditar     = new Button("✏️ Editar");
        Button btnExcluir    = new Button("🗑️ Excluir");
        Button btnBloquear   = new Button("🔒 Bloquear");
        Button btnDesbloquear= new Button("🔓 Desbloquear");
        Button btnAtualizar  = new Button("🔄 Atualizar");

        btnNova.setStyle("-fx-background-color: #2f9e44; -fx-text-fill: white; -fx-font-weight: bold;");
        btnEditar.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white;");
        btnExcluir.setStyle("-fx-background-color: #e03131; -fx-text-fill: white; -fx-font-weight: bold;");
        btnBloquear.setStyle("-fx-background-color: #c2410c; -fx-text-fill: white;");
        btnDesbloquear.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white;");

        btnNova.setOnAction(e -> abrirDialogLoja(null, tabela));
        btnEditar.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja para editar."); return; }
            abrirDialogLoja(sel, tabela);
        });
        btnExcluir.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja para excluir."); return; }
            if (!Alerta.confirmar("Excluir Loja", "⚠️ Excluir \"" + sel.getNome() + "\" permanentemente?\n\nTodos os dados (vendas, clientes, produtos) serão perdidos!")) return;
            if (new LojaDAO().excluir(sel.getId())) {
                Alerta.info("Excluído", "Loja excluída com sucesso.");
                carregarLojas(tabela);
            } else {
                Alerta.erro("Erro", "Não foi possível excluir a loja.\nVerifique se existem registros vinculados.");
            }
        });
        btnBloquear.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (sel.isBloqueada()) { Alerta.aviso("Atenção", "Esta loja já está bloqueada."); return; }
            TextInputDialog dlg = new TextInputDialog("Inadimplência");
            dlg.setTitle("Bloquear Loja");
            dlg.setHeaderText("Bloquear: " + sel.getNome());
            dlg.setContentText("Motivo do bloqueio:");
            dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dlg.showAndWait().ifPresent(motivo -> {
                if (new LojaDAO().bloquear(sel.getId(), motivo)) {
                    Alerta.info("Bloqueado", "Loja \"" + sel.getNome() + "\" bloqueada.\nO acesso é negado imediatamente.");
                    carregarLojas(tabela);
                } else Alerta.erro("Erro", "Não foi possível bloquear.");
            });
        });
        btnDesbloquear.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!sel.isBloqueada()) { Alerta.aviso("Atenção", "Esta loja não está bloqueada."); return; }
            if (Alerta.confirmar("Desbloquear", "Desbloquear \"" + sel.getNome() + "\"?")) {
                if (new LojaDAO().desbloquear(sel.getId())) {
                    Alerta.info("Desbloqueado", "Acesso liberado com sucesso.");
                    carregarLojas(tabela);
                } else Alerta.erro("Erro", "Não foi possível desbloquear.");
            }
        });
        btnAtualizar.setOnAction(e -> carregarLojas(tabela));

        HBox botoes = new HBox(8, btnAtualizar, btnNova, btnEditar, btnExcluir, btnBloquear, btnDesbloquear);
        botoes.setAlignment(Pos.CENTER_LEFT);

        carregarLojas(tabela);
        content.getChildren().addAll(botoes, tabela);
        tab.setContent(content);
        return tab;
    }

    private void abrirDialogLoja(Loja lojaExistente, TableView<Loja> tabela) {
        boolean editando = lojaExistente != null;

        Stage dlgStage = new Stage();
        dlgStage.setTitle(editando ? "✏️ Editar Loja" : "➕ Nova Loja");
        dlgStage.initModality(Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getColumnConstraints().addAll(
            colConstraint(130, false), colConstraint(280, true)
        );

        TextField fNome  = new TextField(editando ? lojaExistente.getNome() : "");
        TextField fCnpj  = new TextField(editando && lojaExistente.getCnpj() != null ? lojaExistente.getCnpj() : "");
        TextField fEnd   = new TextField(editando && lojaExistente.getEndereco() != null ? lojaExistente.getEndereco() : "");
        CheckBox  cbAtiva = new CheckBox("Ativa");
        cbAtiva.setSelected(editando ? lojaExistente.isAtiva() : true);

        grid.addRow(0, new Label("Nome:*"), fNome);
        grid.addRow(1, new Label("CNPJ:"), fCnpj);
        grid.addRow(2, new Label("Endereço:"), fEnd);
        grid.addRow(3, new Label(""), cbAtiva);

        Button btnSalvar  = new Button(editando ? "💾 Salvar Alterações" : "➕ Criar Loja");
        Button btnCancelar = new Button("Cancelar");
        btnSalvar.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSalvar.setOnAction(ev -> {
            if (fNome.getText().trim().isEmpty()) { Alerta.aviso("Atenção", "Nome é obrigatório."); return; }
            Loja l = editando ? lojaExistente : new Loja();
            l.setNome(fNome.getText().trim());
            l.setCnpj(fCnpj.getText().trim());
            l.setEndereco(fEnd.getText().trim());
            l.setAtiva(cbAtiva.isSelected());
            try {
                new LojaDAO().salvar(l);
                Alerta.info("Sucesso", editando ? "Loja atualizada com sucesso." : "Loja criada com sucesso.");
                carregarLojas(tabela);
                dlgStage.close();
            } catch (Exception ex) {
                Alerta.erro("Erro", "Erro ao salvar loja: " + ex.getMessage());
            }
        });
        btnCancelar.setOnAction(ev -> dlgStage.close());

        HBox botoes = new HBox(10, btnSalvar, btnCancelar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, grid, botoes);
        root.setPadding(new Insets(16));
        Scene scene = new Scene(root, 460, 230);
        try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
        dlgStage.setScene(scene);
        dlgStage.showAndWait();
    }

    private void carregarLojas(TableView<Loja> tabela) {
        tabela.setItems(FXCollections.observableArrayList(new LojaDAO().listarTodas()));
    }

    // ==================== ABA EMPRESA ====================

    private Tab criarTabEmpresa() {
        Tab tab = new Tab("🏢 Dados da Empresa");

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));

        Label lblInfo = new Label("Selecione a loja para visualizar e editar os dados da empresa:");
        lblInfo.setStyle("-fx-text-fill: #adb5bd;");

        ComboBox<Loja> cbLoja = new ComboBox<>();
        cbLoja.setMaxWidth(300);
        cbLoja.setPromptText("Selecione uma loja...");
        List<Loja> lojas = new LojaDAO().listarTodas();
        cbLoja.setItems(FXCollections.observableArrayList(lojas));

        // Formulário
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(
            colConstraint(140, false), colConstraint(260, true),
            colConstraint(140, false), colConstraint(260, true)
        );

        TextField fRazaoSocial   = new TextField(); fRazaoSocial.setPromptText("Razão Social");
        TextField fNomeFantasia  = new TextField(); fNomeFantasia.setPromptText("Nome Fantasia");
        TextField fCnpj          = new TextField(); fCnpj.setPromptText("CNPJ");
        TextField fIe            = new TextField(); fIe.setPromptText("Inscrição Estadual");
        TextField fIm            = new TextField(); fIm.setPromptText("Inscrição Municipal");
        TextField fEmail         = new TextField(); fEmail.setPromptText("Email");
        TextField fTelefone      = new TextField(); fTelefone.setPromptText("Telefone");
        TextField fCelular       = new TextField(); fCelular.setPromptText("Celular");
        TextField fCep           = new TextField(); fCep.setPromptText("CEP");
        TextField fLogradouro    = new TextField(); fLogradouro.setPromptText("Logradouro");
        TextField fNumero        = new TextField(); fNumero.setPromptText("Número");
        TextField fComplemento   = new TextField(); fComplemento.setPromptText("Complemento");
        TextField fBairro        = new TextField(); fBairro.setPromptText("Bairro");
        TextField fCidade        = new TextField(); fCidade.setPromptText("Cidade");
        TextField fEstado        = new TextField(); fEstado.setPromptText("UF"); fEstado.setMaxWidth(60);
        ComboBox<String> cbRegime = new ComboBox<>();
        cbRegime.getItems().addAll("SIMPLES_NACIONAL", "LUCRO_PRESUMIDO", "LUCRO_REAL", "MEI");
        cbRegime.setValue("SIMPLES_NACIONAL");

        grid.addRow(0, new Label("Razão Social:*"), fRazaoSocial, new Label("Nome Fantasia:"), fNomeFantasia);
        grid.addRow(1, new Label("CNPJ:"), fCnpj, new Label("Regime Tributário:"), cbRegime);
        grid.addRow(2, new Label("Insc. Estadual:"), fIe, new Label("Insc. Municipal:"), fIm);
        grid.addRow(3, new Label("Email:"), fEmail, new Label("Telefone:"), fTelefone);
        grid.addRow(4, new Label("Celular:"), fCelular, new Label("CEP:"), fCep);
        grid.addRow(5, new Label("Logradouro:"), fLogradouro, new Label("Número:"), fNumero);
        grid.addRow(6, new Label("Complemento:"), fComplemento, new Label("Bairro:"), fBairro);
        grid.addRow(7, new Label("Cidade:"), fCidade, new Label("Estado (UF):"), fEstado);

        // Botões
        Button btnCarregar = new Button("📂 Carregar");
        Button btnSalvar   = new Button("💾 Salvar Empresa");
        Button btnLimpar   = new Button("🗑️ Limpar Dados");
        btnCarregar.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white;");
        btnSalvar.setStyle("-fx-background-color: #2f9e44; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLimpar.setStyle("-fx-background-color: #e03131; -fx-text-fill: white;");

        // Referência para armazenar empresa carregada
        final Empresa[] empAtual = {null};

        btnCarregar.setOnAction(e -> {
            Loja sel = cbLoja.getValue();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            java.util.Optional<Empresa> opt = new EmpresaDAO().carregarParaLoja(sel.getId());
            if (opt.isPresent()) {
                Empresa em = opt.get();
                empAtual[0] = em;
                fRazaoSocial .setText(nvl(em.getRazaoSocial()));
                fNomeFantasia.setText(nvl(em.getNomeFantasia()));
                fCnpj        .setText(nvl(em.getCnpj()));
                fIe          .setText(nvl(em.getIe()));
                fIm          .setText(nvl(em.getIm()));
                fEmail       .setText(nvl(em.getEmail()));
                fTelefone    .setText(nvl(em.getTelefone()));
                fCelular     .setText(nvl(em.getCelular()));
                fCep         .setText(nvl(em.getCep()));
                fLogradouro  .setText(nvl(em.getLogradouro()));
                fNumero      .setText(nvl(em.getNumero()));
                fComplemento .setText(nvl(em.getComplemento()));
                fBairro      .setText(nvl(em.getBairro()));
                fCidade      .setText(nvl(em.getCidade()));
                fEstado      .setText(nvl(em.getEstado()));
                cbRegime.setValue(em.getRegimeTributario() != null ? em.getRegimeTributario() : "SIMPLES_NACIONAL");
                Alerta.info("Carregado", "Dados carregados para: " + sel.getNome());
            } else {
                empAtual[0] = new Empresa();
                limparCamposEmpresa(fRazaoSocial,fNomeFantasia,fCnpj,fIe,fIm,fEmail,fTelefone,fCelular,fCep,fLogradouro,fNumero,fComplemento,fBairro,fCidade,fEstado);
                cbRegime.setValue("SIMPLES_NACIONAL");
                Alerta.info("Novo Cadastro", "Nenhuma empresa cadastrada para esta loja.\nPreencha e salve para criar.");
            }
        });

        btnSalvar.setOnAction(e -> {
            Loja sel = cbLoja.getValue();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (fRazaoSocial.getText().trim().isEmpty()) { Alerta.aviso("Atenção", "Razão Social é obrigatória."); return; }
            Empresa em = empAtual[0] != null ? empAtual[0] : new Empresa();
            em.setRazaoSocial(fRazaoSocial.getText().trim());
            em.setNomeFantasia(fNomeFantasia.getText().trim());
            em.setCnpj(fCnpj.getText().trim());
            em.setIe(fIe.getText().trim());
            em.setIm(fIm.getText().trim());
            em.setEmail(fEmail.getText().trim());
            em.setTelefone(fTelefone.getText().trim());
            em.setCelular(fCelular.getText().trim());
            em.setCep(fCep.getText().trim());
            em.setLogradouro(fLogradouro.getText().trim());
            em.setNumero(fNumero.getText().trim());
            em.setComplemento(fComplemento.getText().trim());
            em.setBairro(fBairro.getText().trim());
            em.setCidade(fCidade.getText().trim());
            em.setEstado(fEstado.getText().trim());
            em.setRegimeTributario(cbRegime.getValue());
            if (new EmpresaDAO().salvarParaLoja(em, sel.getId())) {
                Alerta.info("Salvo", "Dados da empresa salvos para: " + sel.getNome());
            } else {
                Alerta.erro("Erro", "Não foi possível salvar os dados.");
            }
        });

        btnLimpar.setOnAction(e -> {
            Loja sel = cbLoja.getValue();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!Alerta.confirmar("Limpar Dados", "Apagar dados de empresa da loja \"" + sel.getNome() + "\"?")) return;
            limparCamposEmpresa(fRazaoSocial,fNomeFantasia,fCnpj,fIe,fIm,fEmail,fTelefone,fCelular,fCep,fLogradouro,fNumero,fComplemento,fBairro,fCidade,fEstado);
            empAtual[0] = new Empresa();
        });

        HBox topoBox = new HBox(10, cbLoja, btnCarregar);
        topoBox.setAlignment(Pos.CENTER_LEFT);

        HBox botoes = new HBox(10, btnSalvar, btnLimpar);
        botoes.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        content.getChildren().addAll(lblInfo, topoBox, new Separator(), scroll, botoes);
        tab.setContent(content);
        return tab;
    }

    private void limparCamposEmpresa(TextField... fields) {
        for (TextField f : fields) f.setText("");
    }

    private String nvl(String v) { return v == null ? "" : v; }

    // ==================== ABA TOKENS ====================

    private Tab criarTabTokens() {
        Tab tab = new Tab("🔑 Tokens de Ativação");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TableView<TokenLojaDAO.InfoToken> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<TokenLojaDAO.InfoToken, String> colToken = new TableColumn<>("Token");
        colToken.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().token()));

        TableColumn<TokenLojaDAO.InfoToken, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().descricao()));

        TableColumn<TokenLojaDAO.InfoToken, String> colUsado = new TableColumn<>("Status");
        colUsado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                TokenLojaDAO.InfoToken t = (TokenLojaDAO.InfoToken) getTableRow().getItem();
                if (!t.usado() || t.token().equals("DRS-MASTER-2026")) {
                    setText(t.token().equals("DRS-MASTER-2026") ? "♾️ Ilimitado" : "✅ Disponível");
                    setStyle("-fx-text-fill: #51cf66;");
                } else {
                    setText("❌ Usado");
                    setStyle("-fx-text-fill: #adb5bd;");
                }
            }
        });
        colUsado.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));

        TableColumn<TokenLojaDAO.InfoToken, String> colLoja = new TableColumn<>("Loja Ativada");
        colLoja.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().lojaId()));

        TableColumn<TokenLojaDAO.InfoToken, String> colData = new TableColumn<>("Criado em");
        colData.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().criadoEm()));

        tabela.getColumns().addAll(colToken, colDesc, colUsado, colLoja, colData);

        Button btnGerar    = new Button("➕ Gerar Tokens");
        Button btnAtualizar = new Button("🔄 Atualizar");
        btnGerar.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold;");

        btnGerar.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog("1");
            dlg.setTitle("Gerar Tokens");
            dlg.setHeaderText("Quantos tokens deseja gerar?");
            dlg.setContentText("Quantidade (1-50):");
            dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dlg.showAndWait().ifPresent(qtd -> {
                try {
                    int n = Integer.parseInt(qtd.trim());
                    if (n < 1 || n > 50) { Alerta.aviso("Atenção", "Gere entre 1 e 50 tokens."); return; }
                    List<String> gerados = new TokenLojaDAO().gerarTokens(n, "Cliente");
                    StringBuilder sb = new StringBuilder("Tokens gerados:\n\n");
                    gerados.forEach(t -> sb.append("• ").append(t).append("\n"));
                    mostrarTokens(sb.toString());
                    carregarTokens(tabela);
                } catch (NumberFormatException ex) {
                    Alerta.aviso("Atenção", "Informe um número válido.");
                }
            });
        });

        btnAtualizar.setOnAction(e -> carregarTokens(tabela));

        HBox botoes = new HBox(10, btnAtualizar, btnGerar);
        botoes.setAlignment(Pos.CENTER_LEFT);

        carregarTokens(tabela);
        content.getChildren().addAll(botoes, tabela);
        tab.setContent(content);
        return tab;
    }

    private void carregarTokens(TableView<TokenLojaDAO.InfoToken> tabela) {
        tabela.setItems(FXCollections.observableArrayList(new TokenLojaDAO().listarTodos()));
    }

    private void mostrarTokens(String texto) {
        Stage stage = new Stage();
        stage.setTitle("Tokens Gerados");
        stage.initModality(Modality.APPLICATION_MODAL);
        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
        area.setPrefSize(420, 300);
        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white;");
        btnFechar.setOnAction(e -> stage.close());
        Label aviso = new Label("💡 Copie os tokens e envie para os clientes.");
        aviso.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");
        VBox root = new VBox(12, area, aviso, btnFechar);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.CENTER_RIGHT);
        Scene scene = new Scene(root);
        try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    // ==================== ABA MANUTENÇÃO ====================

    private Tab criarTabManutencao() {
        Tab tab = new Tab("🔧 Manutenção");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1e2027; -fx-background-color: #1e2027;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1e2027;");

        Label lblTitulo = new Label("⚠️ Área de Manutenção");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;");
        Label lblAviso = new Label("Operações PERMANENTES — não podem ser desfeitas. Use com cautela.");
        lblAviso.setStyle("-fx-text-fill: #ffa94d; -fx-font-size: 12px;");

        // Seleção de loja
        ComboBox<Loja> cbLoja = new ComboBox<>();
        cbLoja.setMaxWidth(340);
        cbLoja.setPromptText("Escolha uma loja...");
        cbLoja.setItems(javafx.collections.FXCollections.observableArrayList(new LojaDAO().listarTodas()));
        cbLoja.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Loja l, boolean empty) {
                super.updateItem(l, empty);
                setText(empty || l == null ? null : "[" + l.getId() + "] " + l.getNome());
            }
        });
        cbLoja.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Loja l, boolean empty) {
                super.updateItem(l, empty);
                setText(empty || l == null ? null : "[" + l.getId() + "] " + l.getNome());
            }
        });

        Label lblStats = new Label("Selecione uma loja para ver os dados.");
        lblStats.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");
        lblStats.setWrapText(true);
        VBox statsBox = new VBox(8, lblStats);
        statsBox.setStyle("-fx-background-color: #2c2f36; -fx-padding: 14; -fx-border-color: #495057; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Helper: confirmar com nome da loja
        java.util.function.BiFunction<Loja, String, Boolean> confirmarComNome = (loja, acao) -> {
            if (!Alerta.confirmar("⚠️ " + acao, "Executar:\n  " + acao + "\n\nLoja: \"" + loja.getNome() + "\"\n\nIRREVERSÍVEL. Continuar?")) return false;
            javafx.scene.control.TextInputDialog dlg2 = new javafx.scene.control.TextInputDialog();
            dlg2.setTitle("Confirmação Final");
            dlg2.setHeaderText("⛔ Digite o nome da loja para confirmar");
            dlg2.setContentText("Nome: \"" + loja.getNome() + "\"");
            try { dlg2.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
            java.util.Optional<String> r2 = dlg2.showAndWait();
            if (r2.isEmpty() || !r2.get().trim().equalsIgnoreCase(loja.getNome().trim())) {
                Alerta.aviso("Cancelado", "Nome não confere. Operação cancelada.");
                return false;
            }
            return true;
        };

        // Atualizar estatísticas
        Runnable atualizarStats = () -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { lblStats.setText("Selecione uma loja."); return; }
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                long v = 0, i = 0, s = 0, f = 0, p = 0;
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM vendas WHERE loja_id=?")) { ps.setInt(1, loja.getId()); java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) v = rs.getLong(1); }
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM itens_venda iv JOIN vendas vv ON iv.venda_id=vv.id WHERE vv.loja_id=?")) { ps.setInt(1, loja.getId()); java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) i = rs.getLong(1); }
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM sessoes_caixa WHERE loja_id=?")) { ps.setInt(1, loja.getId()); java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) s = rs.getLong(1); } catch (Exception ignored) {}
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM produtos WHERE loja_id=?")) { ps.setInt(1, loja.getId()); java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) p = rs.getLong(1); } catch (Exception ignored) {}
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT (SELECT COUNT(*) FROM contas_pagar WHERE loja_id=?) + (SELECT COUNT(*) FROM contas_receber WHERE loja_id=?)")) { ps.setInt(1, loja.getId()); ps.setInt(2, loja.getId()); java.sql.ResultSet rs = ps.executeQuery(); if (rs.next()) f = rs.getLong(1); } catch (Exception ignored) {}
                final String txt = String.format("📊 %s (ID %d)\n  Vendas: %d  |  Itens vendidos: %d  |  Sessões de caixa: %d\n  Produtos: %d  |  Contas (pagar+receber): %d", loja.getNome(), loja.getId(), v, i, s, p, f);
                javafx.application.Platform.runLater(() -> lblStats.setText(txt));
            } catch (Exception e) { javafx.application.Platform.runLater(() -> lblStats.setText("Erro: " + e.getMessage())); }
        };

        cbLoja.setOnAction(e -> atualizarStats.run());

        // ---- BLOCO: Vendas e Caixa ----
        VBox blocoVendas = criarBlocoManutencao("🛒 Vendas e Caixa", "#c92a2a");
        Button btnResetVendas = criarBtnAcao("🗑️ Resetar Vendas e Caixa", "#c92a2a");
        btnResetVendas.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!confirmarComNome.apply(loja, "Resetar Vendas e Caixa")) return;
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                conn.setAutoCommit(false);
                try {
                    execSql(conn, "DELETE FROM itens_venda WHERE venda_id IN (SELECT id FROM vendas WHERE loja_id=?)", loja.getId());
                    try { execSql(conn, "DELETE FROM vendas_canceladas WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    execSql(conn, "DELETE FROM vendas WHERE loja_id=?", loja.getId());
                    try { execSql(conn, "DELETE FROM sessoes_caixa WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    conn.commit();
                    javafx.application.Platform.runLater(() -> { Alerta.info("✅ Concluído", "Vendas e caixa apagados. Dashboard agora mostra zero."); atualizarStats.run(); });
                } catch (Exception ex) { conn.rollback(); Alerta.erro("Erro", ex.getMessage()); }
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        blocoVendas.getChildren().addAll(labelDescricao("Apaga todas as vendas, itens e sessões de caixa. O dashboard volta a zero."), btnResetVendas);

        // ---- BLOCO: Estoque ----
        VBox blocoEstoque = criarBlocoManutencao("📦 Estoque", "#d9480f");
        Button btnResetMovEstoque = criarBtnAcao("🗑️ Limpar Histórico de Movimentações", "#d9480f");
        btnResetMovEstoque.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!confirmarComNome.apply(loja, "Limpar Histórico de Movimentações")) return;
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                execSql(conn, "DELETE FROM movimentacoes_estoque WHERE loja_id=?", loja.getId());
                javafx.application.Platform.runLater(() -> { Alerta.info("✅ Concluído", "Histórico limpo. Saldos atuais preservados."); atualizarStats.run(); });
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        Button btnZerarEstoque = criarBtnAcao("⚠️ Zerar Saldo de Estoque", "#c92a2a");
        btnZerarEstoque.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!confirmarComNome.apply(loja, "Zerar Saldo de Estoque")) return;
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                execSql(conn, "UPDATE produtos SET estoque_atual=0 WHERE loja_id=?", loja.getId());
                javafx.application.Platform.runLater(() -> Alerta.info("✅ Concluído", "Estoque de todos os produtos zerado."));
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        blocoEstoque.getChildren().addAll(
            labelDescricao("Apaga apenas o histórico de entradas/saídas, sem alterar o saldo atual."), btnResetMovEstoque,
            labelDescricao("Zera o saldo atual (estoque_atual=0) de todos os produtos da loja."), btnZerarEstoque
        );

        // ---- BLOCO: Financeiro ----
        VBox blocoFin = criarBlocoManutencao("💰 Financeiro", "#862e9c");
        Button btnResetFin = criarBtnAcao("🗑️ Resetar Contas a Pagar e Receber", "#862e9c");
        btnResetFin.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!confirmarComNome.apply(loja, "Resetar Financeiro")) return;
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                conn.setAutoCommit(false);
                try {
                    execSql(conn, "DELETE FROM contas_pagar WHERE loja_id=?", loja.getId());
                    execSql(conn, "DELETE FROM contas_receber WHERE loja_id=?", loja.getId());
                    conn.commit();
                    javafx.application.Platform.runLater(() -> { Alerta.info("✅ Concluído", "Contas a pagar e receber apagadas."); atualizarStats.run(); });
                } catch (Exception ex) { conn.rollback(); Alerta.erro("Erro", ex.getMessage()); }
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        blocoFin.getChildren().addAll(labelDescricao("Apaga todas as contas a pagar e a receber da loja."), btnResetFin);

        // ---- BLOCO: Cadastros ----
        VBox blocoCad = criarBlocoManutencao("👥 Cadastros", "#1864ab");
        Button btnResetClientes = criarBtnAcao("🗑️ Apagar Clientes sem Histórico", "#1864ab");
        btnResetClientes.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!confirmarComNome.apply(loja, "Apagar Clientes sem Histórico")) return;
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                execSql(conn, "DELETE FROM clientes WHERE id NOT IN (SELECT DISTINCT COALESCE(cliente_id,-1) FROM vendas WHERE cliente_id IS NOT NULL)");
                javafx.application.Platform.runLater(() -> Alerta.info("✅ Concluído", "Clientes sem vínculo com vendas foram removidos."));
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        Button btnResetForn = criarBtnAcao("🗑️ Apagar Todos os Fornecedores", "#1864ab");
        btnResetForn.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!confirmarComNome.apply(loja, "Apagar Todos os Fornecedores")) return;
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                execSql(conn, "DELETE FROM fornecedores");
                javafx.application.Platform.runLater(() -> Alerta.info("✅ Concluído", "Todos os fornecedores removidos."));
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        blocoCad.getChildren().addAll(
            labelDescricao("Remove clientes que não possuem nenhuma venda registrada."), btnResetClientes,
            labelDescricao("Remove todos os fornecedores cadastrados."), btnResetForn
        );

        // ---- BLOCO: Usuários (consulta) ----
        VBox blocoUsuarios = criarBlocoManutencao("👤 Usuários", "#495057");
        Button btnVerUsuarios = criarBtnAcao("👁️ Ver Usuários da Loja", "#495057");
        Label lblUsuarios = new Label("");
        lblUsuarios.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");
        lblUsuarios.setWrapText(true);
        btnVerUsuarios.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, nome, login, perfil, ativo FROM usuarios WHERE loja_id=? ORDER BY nome")) {
                ps.setInt(1, loja.getId());
                java.sql.ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (rs.next()) sb.append(String.format("[%d] %s (%s) — %s — %s\n", rs.getInt("id"), rs.getString("nome"), rs.getString("login"), rs.getString("perfil"), rs.getBoolean("ativo") ? "✅ Ativo" : "❌ Inativo"));
                lblUsuarios.setText(sb.length() > 0 ? sb.toString() : "Nenhum usuário encontrado.");
            } catch (Exception ex) { lblUsuarios.setText("Erro: " + ex.getMessage()); }
        });
        blocoUsuarios.getChildren().addAll(labelDescricao("Lista todos os usuários vinculados à loja selecionada."), btnVerUsuarios, lblUsuarios);

        // ---- BLOCO: Reset Total ----
        VBox blocoWipe = criarBlocoManutencao("💥 Reset Total da Loja", "#c92a2a");
        Button btnWipe = criarBtnAcao("💥 RESET TOTAL — Apagar TUDO Operacional", "#7b1010");
        btnWipe.setStyle(btnWipe.getStyle() + " -fx-border-color: #ff6b6b; -fx-border-width: 2;");
        btnWipe.setOnAction(e -> {
            Loja loja = cbLoja.getValue();
            if (loja == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!Alerta.confirmar("💥 RESET TOTAL", "⚠️ ATENÇÃO!\n\nApaga TUDO da loja:\n  • Vendas + itens + caixa\n  • Histórico de estoque\n  • Saldos zerados\n  • Financeiro\n\nLoja: \"" + loja.getNome() + "\"\nTem certeza?")) return;
            if (!Alerta.confirmar("💥 SEGUNDA CONFIRMAÇÃO", "ÚLTIMA CHANCE de cancelar.\n\nLoja: \"" + loja.getNome() + "\"")) return;
            javafx.scene.control.TextInputDialog dlg3 = new javafx.scene.control.TextInputDialog();
            dlg3.setTitle("Confirmação Final");
            dlg3.setHeaderText("Digite:  RESET TOTAL");
            dlg3.setContentText("Texto:");
            try { dlg3.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
            java.util.Optional<String> r3 = dlg3.showAndWait();
            if (r3.isEmpty() || !r3.get().trim().equals("RESET TOTAL")) { Alerta.aviso("Cancelado", "Texto incorreto. Operação cancelada."); return; }
            try (java.sql.Connection conn = com.erp.config.DatabaseConfig.getConexao()) {
                conn.setAutoCommit(false);
                try {
                    execSql(conn, "DELETE FROM itens_venda WHERE venda_id IN (SELECT id FROM vendas WHERE loja_id=?)", loja.getId());
                    try { execSql(conn, "DELETE FROM vendas_canceladas WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    execSql(conn, "DELETE FROM vendas WHERE loja_id=?", loja.getId());
                    try { execSql(conn, "DELETE FROM sessoes_caixa WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    try { execSql(conn, "DELETE FROM movimentacoes_estoque WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    try { execSql(conn, "UPDATE produtos SET estoque_atual=0 WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    try { execSql(conn, "DELETE FROM contas_pagar WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    try { execSql(conn, "DELETE FROM contas_receber WHERE loja_id=?", loja.getId()); } catch (Exception ignored) {}
                    conn.commit();
                    javafx.application.Platform.runLater(() -> { Alerta.info("✅ Reset Total Concluído", "Todos os dados operacionais foram apagados.\nProdutos e usuários preservados."); atualizarStats.run(); });
                } catch (Exception ex) { conn.rollback(); Alerta.erro("Erro", ex.getMessage()); }
            } catch (Exception ex) { Alerta.erro("Erro", ex.getMessage()); }
        });
        blocoWipe.getChildren().addAll(labelDescricao("Apaga vendas, caixa, financeiro, histórico e zera estoques. Produtos e usuários são preservados."), btnWipe);

        // ---- BLOCO: Código de Cancelamento ----
        VBox blocoCodigo = criarBlocoManutencao("🔑 Código de Cancelamento do PDV", "#2b8a3e");
        Label lblCodigoAtual = new Label("Carregando...");
        lblCodigoAtual.setStyle("-fx-text-fill: #40c057; -fx-font-size: 13px; -fx-font-weight: bold;");
        Button btnCarregarCodigo = criarBtnAcao("🔄 Ver Código Atual", "#2b8a3e");
        TextField txtNovoCodigo = new TextField();
        txtNovoCodigo.setPromptText("Novo código (ex: 53332)");
        txtNovoCodigo.setMaxWidth(200);
        Button btnSalvarCodigo = criarBtnAcao("💾 Salvar Código", "#2b8a3e");
        btnCarregarCodigo.setOnAction(e -> {
            String codigo = new com.erp.dao.ConfiguracaoDAO().get("codigo_cancelamento", "53332");
            lblCodigoAtual.setText("Código atual: " + codigo);
        });
        btnSalvarCodigo.setOnAction(e -> {
            String novo = txtNovoCodigo.getText().trim();
            if (novo.isBlank()) { Alerta.aviso("Atenção", "Informe o novo código."); return; }
            if (novo.length() < 4) { Alerta.aviso("Atenção", "O código deve ter ao menos 4 caracteres."); return; }
            new com.erp.dao.ConfiguracaoDAO().set("codigo_cancelamento", novo);
            lblCodigoAtual.setText("Código atual: " + novo);
            txtNovoCodigo.clear();
            Alerta.info("✅ Salvo", "Código de cancelamento atualizado para: " + novo);
        });
        blocoCodigo.getChildren().addAll(
            labelDescricao("Código numérico exigido ao cancelar qualquer venda no PDV. Padrão: 53332"),
            lblCodigoAtual,
            new HBox(8, txtNovoCodigo, btnSalvarCodigo), btnCarregarCodigo
        );

        // Carregar código inicial
        javafx.application.Platform.runLater(() -> {
            String codigo = new com.erp.dao.ConfiguracaoDAO().get("codigo_cancelamento", "53332");
            lblCodigoAtual.setText("Código atual: " + codigo);
        });

        Button btnAtualizarStats = criarBtnAcao("🔄 Atualizar Estatísticas", "#343a40");
        btnAtualizarStats.setOnAction(ev -> atualizarStats.run());

        content.getChildren().addAll(
            lblTitulo, lblAviso, new Separator(),
            new HBox(10, new Label("Loja:") {{ setStyle("-fx-text-fill: #dee2e6; -fx-font-weight: bold;"); }}, cbLoja, btnAtualizarStats),
            statsBox, new Separator(),
            blocoCodigo, new Separator(),
            blocoVendas, blocoEstoque, blocoFin, blocoCad, blocoUsuarios, blocoWipe
        );

        scroll.setContent(content);
        tab.setContent(scroll);
        return tab;
    }

    private VBox criarBlocoManutencao(String titulo, String cor) {
        Label lbl = new Label(titulo);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");
        VBox box = new VBox(8, lbl);
        box.setStyle("-fx-background-color: #25282f; -fx-padding: 14; -fx-border-color: " + cor + "55; -fx-border-radius: 6; -fx-background-radius: 6;");
        return box;
    }

    private Button criarBtnAcao(String texto, String cor) {
        Button btn = new Button(texto);
        btn.setStyle("-fx-background-color: " + cor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        return btn;
    }

    private Label labelDescricao(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #868e96; -fx-font-size: 11px;");
        l.setWrapText(true);
        return l;
    }

    private void execSql(java.sql.Connection conn, String sql) throws java.sql.SQLException {
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
    }

    private void execSql(java.sql.Connection conn, String sql, int param) throws java.sql.SQLException {
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, param); ps.executeUpdate(); }
    }

    // ==================== UTILITÁRIOS ====================

    // ==================== ABA TOKENS USUÁRIO ====================

    private Tab criarTabTokensUsuario() {
        Tab tab = new Tab("👤 Tokens de Usuário");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label lblInfo = new Label("Gere tokens para permitir o cadastro de novos usuários no sistema. Cada token é de uso único.");
        lblInfo.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");
        lblInfo.setWrapText(true);

        TableView<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario, String> colToken = new TableColumn<>("Token");
        colToken.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().token()));
        colToken.setPrefWidth(210);

        TableColumn<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().descricao()));

        TableColumn<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().ilimitado() ? "♾️ Ilimitado" : "1x Uso"));
        colTipo.setMaxWidth(100);

        TableColumn<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario t = getTableRow().getItem();
                if (t.ilimitado() || !t.usado()) {
                    setText(t.ilimitado() ? "♾️ Ativo" : "✅ Disponível");
                    setStyle("-fx-text-fill: #51cf66;");
                } else {
                    setText("❌ Usado");
                    setStyle("-fx-text-fill: #adb5bd;");
                }
            }
        });
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colStatus.setMaxWidth(100);

        TableColumn<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario, String> colData = new TableColumn<>("Criado em");
        colData.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().criadoEm()));

        tabela.getColumns().addAll(colToken, colDesc, colTipo, colStatus, colData);

        Button btnGerar     = new Button("➕ Gerar Token");
        Button btnAtualizar = new Button("🔄 Atualizar");
        Button btnLimpar    = new Button("🗑️ Limpar Usados");
        btnGerar.setStyle("-fx-background-color: #1971c2; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLimpar.setStyle("-fx-background-color: #c92a2a; -fx-text-fill: white;");

        btnGerar.setOnAction(e -> {
            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("Gerar Token de Usuário");
            dlg.setHeaderText("Configure o novo token");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            try { dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new Insets(16));

            TextField txtDesc = new TextField();
            txtDesc.setPromptText("Ex: Para João - Operador");
            txtDesc.setPrefWidth(260);

            Spinner<Integer> spnQtd = new Spinner<>(1, 20, 1);
            spnQtd.setPrefWidth(80);

            CheckBox chkIlimitado = new CheckBox("Token ilimitado (pode ser usado múltiplas vezes)");
            chkIlimitado.setStyle("-fx-text-fill: #adb5bd;");

            grid.addRow(0, new Label("Descrição:"), txtDesc);
            grid.addRow(1, new Label("Quantidade:"), spnQtd);
            grid.addRow(2, new Label(""), chkIlimitado);
            dlg.getDialogPane().setContent(grid);

            dlg.showAndWait().ifPresent(btn -> {
                if (btn != ButtonType.OK) return;
                int qtd = spnQtd.getValue();
                String desc = txtDesc.getText().trim();
                boolean ilimitado = chkIlimitado.isSelected();
                List<String> gerados = new com.erp.dao.TokenUsuarioDAO().gerarTokens(qtd, desc, ilimitado);
                if (gerados.isEmpty()) { Alerta.erro("Erro", "Não foi possível gerar os tokens."); return; }
                StringBuilder sb = new StringBuilder("🔑 Tokens gerados:\n\n");
                gerados.forEach(t -> sb.append("  ").append(t).append("\n"));
                sb.append("\n💡 Envie este token para a pessoa que vai cadastrar o usuário.");
                mostrarTokens(sb.toString());
                carregarTokensUsuario(tabela);
            });
        });

        btnAtualizar.setOnAction(e -> carregarTokensUsuario(tabela));
        btnLimpar.setOnAction(e -> {
            int removed = new com.erp.dao.TokenUsuarioDAO().limparUsados();
            Alerta.info("Limpeza", removed + " token(s) usados removidos.");
            carregarTokensUsuario(tabela);
        });

        HBox botoes = new HBox(10, btnAtualizar, btnGerar, btnLimpar);
        botoes.setAlignment(Pos.CENTER_LEFT);

        carregarTokensUsuario(tabela);
        content.getChildren().addAll(lblInfo, botoes, tabela);
        tab.setContent(content);
        return tab;
    }

    private void carregarTokensUsuario(TableView<com.erp.dao.TokenUsuarioDAO.InfoTokenUsuario> tabela) {
        new com.erp.dao.TokenUsuarioDAO().criarTabelaSeNecessario();
        tabela.setItems(FXCollections.observableArrayList(new com.erp.dao.TokenUsuarioDAO().listarTodos()));
    }

    private ColumnConstraints colConstraint(double minWidth, boolean grow) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setMinWidth(minWidth);
        if (grow) cc.setHgrow(Priority.ALWAYS);
        return cc;
    }
}
