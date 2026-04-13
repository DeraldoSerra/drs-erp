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
        tabs.getTabs().addAll(criarTabLojas(), criarTabEmpresa(), criarTabTokens());
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

    // ==================== UTILITÁRIOS ====================

    private ColumnConstraints colConstraint(double minWidth, boolean grow) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setMinWidth(minWidth);
        if (grow) cc.setHgrow(Priority.ALWAYS);
        return cc;
    }
}
