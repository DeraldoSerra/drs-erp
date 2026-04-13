package com.erp.view;

import com.erp.dao.LojaDAO;
import com.erp.dao.TokenLojaDAO;
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
 * Painel do administrador DRS para gerenciar lojas/clientes.
 * Acessível pelo menu lateral (perfil ADMIN).
 */
public class AdminPainelView {

    public VBox criar() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        Label titulo = new Label("🛡️ Painel do Administrador");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4dabf7;");

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(criarTabLojas(), criarTabTokens());
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

        // Tabela
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

        TableColumn<Loja, Boolean> colAtiva = new TableColumn<>("Ativa");
        colAtiva.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v ? "✅" : "❌");
            }
        });
        colAtiva.setCellValueFactory(new PropertyValueFactory<>("ativa"));

        TableColumn<Loja, Boolean> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                Loja l = (Loja) getTableRow().getItem();
                if (l.isBloqueada()) {
                    setText("🔒 BLOQUEADA");
                    setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                } else {
                    setText("🟢 ATIVA");
                    setStyle("-fx-text-fill: #51cf66; -fx-font-weight: bold;");
                }
            }
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("bloqueada"));

        TableColumn<Loja, String> colMotivo = new TableColumn<>("Motivo Bloqueio");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivoBloqueio"));

        tabela.getColumns().addAll(colId, colNome, colCnpj, colAtiva, colStatus, colMotivo);

        // Botões de ação
        Button btnBloquear   = new Button("🔒 Bloquear Acesso");
        Button btnDesbloquear = new Button("🔓 Desbloquear");
        Button btnAtualizar  = new Button("🔄 Atualizar");

        btnBloquear.getStyleClass().add("btn-danger");
        btnBloquear.setStyle("-fx-background-color: #e03131; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDesbloquear.getStyleClass().add("btn-primario");
        btnAtualizar.getStyleClass().add("btn-secundario");

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
                    Alerta.info("Bloqueado", "Loja \"" + sel.getNome() + "\" bloqueada.\nO cliente perderá acesso imediatamente.");
                    carregarLojas(tabela);
                } else {
                    Alerta.erro("Erro", "Não foi possível bloquear a loja.");
                }
            });
        });

        btnDesbloquear.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            if (!sel.isBloqueada()) { Alerta.aviso("Atenção", "Esta loja não está bloqueada."); return; }

            if (Alerta.confirmar("Desbloquear", "Desbloquear \"" + sel.getNome() + "\"?\nO cliente voltará a ter acesso.")) {
                if (new LojaDAO().desbloquear(sel.getId())) {
                    Alerta.info("Desbloqueado", "Loja \"" + sel.getNome() + "\" desbloqueada com sucesso.");
                    carregarLojas(tabela);
                } else {
                    Alerta.erro("Erro", "Não foi possível desbloquear a loja.");
                }
            }
        });

        btnAtualizar.setOnAction(e -> carregarLojas(tabela));

        HBox botoes = new HBox(10, btnAtualizar, btnDesbloquear, btnBloquear);
        botoes.setAlignment(Pos.CENTER_LEFT);

        carregarLojas(tabela);
        content.getChildren().addAll(botoes, tabela);
        tab.setContent(content);
        return tab;
    }

    private void carregarLojas(TableView<Loja> tabela) {
        List<Loja> lojas = new LojaDAO().listarTodas();
        tabela.setItems(FXCollections.observableArrayList(lojas));
    }

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
                if (t.token().equals("DRS-MASTER-2026") || !t.usado()) {
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

        // Botões
        Button btnGerar   = new Button("➕ Gerar Tokens");
        Button btnAtualizar = new Button("🔄 Atualizar");
        btnGerar.getStyleClass().add("btn-primario");
        btnAtualizar.getStyleClass().add("btn-secundario");

        btnGerar.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog("1");
            dlg.setTitle("Gerar Tokens");
            dlg.setHeaderText("Quantos tokens deseja gerar?");
            dlg.setContentText("Quantidade:");
            dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dlg.showAndWait().ifPresent(qtd -> {
                try {
                    int n = Integer.parseInt(qtd.trim());
                    if (n < 1 || n > 50) { Alerta.aviso("Atenção", "Gere entre 1 e 50 tokens."); return; }
                    List<String> gerados = new TokenLojaDAO().gerarTokens(n, "Cliente");
                    StringBuilder sb = new StringBuilder("Tokens gerados:\n\n");
                    gerados.forEach(t -> sb.append("• ").append(t).append("\n"));
                    // Mostrar em janela separada para fácil cópia
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
        btnFechar.getStyleClass().add("btn-primario");
        btnFechar.setOnAction(e -> stage.close());

        Label aviso = new Label("💡 Copie os tokens acima e envie para os clientes.");
        aviso.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");

        VBox root = new VBox(12, area, aviso, btnFechar);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.CENTER_RIGHT);

        Scene scene = new Scene(root);
        try { scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm()); } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }
}
