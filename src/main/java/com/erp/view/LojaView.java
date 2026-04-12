package com.erp.view;

import com.erp.dao.LojaDAO;
import com.erp.model.Loja;
import com.erp.util.Alerta;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Optional;

public class LojaView {

    private final LojaDAO dao = new LojaDAO();
    private TableView<Loja> tabela;

    public Region criar() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("🏪 Gerenciamento de Lojas");
        titulo.getStyleClass().add("titulo-modulo");
        Label subtitulo = new Label("Cadastre e gerencie as lojas do sistema");
        subtitulo.getStyleClass().add("subtitulo-modulo");

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button btnNova = new Button("➕  Nova Loja");
        btnNova.getStyleClass().add("btn-primario");
        Button btnEditar = new Button("✏️  Editar");
        btnEditar.getStyleClass().add("btn-secundario");
        Button btnToggle = new Button("🔄  Ativar/Desativar");
        btnToggle.getStyleClass().add("btn-aviso");
        toolbar.getChildren().addAll(btnNova, btnEditar, btnToggle);

        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);
        carregarTabela();

        btnNova.setOnAction(e -> abrirForm(null));
        btnEditar.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja para editar."); return; }
            abrirForm(sel);
        });
        btnToggle.setOnAction(e -> {
            Loja sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Alerta.aviso("Atenção", "Selecione uma loja."); return; }
            String acao = sel.isAtiva() ? "desativar" : "ativar";
            if (!Alerta.confirmar("Confirmar", "Deseja " + acao + " a loja " + sel.getNome() + "?")) return;
            boolean ok = sel.isAtiva() ? dao.desativar(sel.getId()) : dao.ativar(sel.getId());
            if (ok) { Alerta.info("Loja", "Loja " + acao + "da com sucesso!"); carregarTabela(); }
            else Alerta.erro("Erro", "Não foi possível alterar o status da loja.");
        });

        box.getChildren().addAll(new VBox(4, titulo, subtitulo), toolbar, tabela);
        return box;
    }

    @SuppressWarnings("unchecked")
    private TableView<Loja> criarTabela() {
        TableView<Loja> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPlaceholder(new Label("Nenhuma loja cadastrada."));

        TableColumn<Loja, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Loja, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));

        TableColumn<Loja, String> colCnpj = new TableColumn<>("CNPJ");
        colCnpj.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCnpj())));
        colCnpj.setPrefWidth(160);

        TableColumn<Loja, String> colEnd = new TableColumn<>("Endereço");
        colEnd.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEndereco())));

        TableColumn<Loja, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAtiva() ? "✅ Ativa" : "❌ Inativa"));
        colStatus.setPrefWidth(100);

        tv.getColumns().addAll(colId, colNome, colCnpj, colEnd, colStatus);
        return tv;
    }

    private void carregarTabela() {
        tabela.getItems().setAll(dao.listarTodas());
    }

    private void abrirForm(Loja loja) {
        boolean novo = loja == null;
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(novo ? "Nova Loja" : "Editar Loja");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        TextField txtNome = new TextField(novo ? "" : nvl(loja.getNome()));
        TextField txtCnpj = new TextField(novo ? "" : nvl(loja.getCnpj()));
        TextField txtEnd  = new TextField(novo ? "" : nvl(loja.getEndereco()));
        CheckBox chkAtiva = new CheckBox("Ativa");
        chkAtiva.setSelected(novo || loja.isAtiva());
        chkAtiva.setStyle("-fx-text-fill: #e0e0e0;");

        txtNome.setPromptText("Nome da loja");
        txtCnpj.setPromptText("00.000.000/0000-00");
        txtEnd.setPromptText("Endereço completo");

        grid.add(new Label("Nome*:"), 0, 0);   grid.add(txtNome, 1, 0);
        grid.add(new Label("CNPJ:"),  0, 1);   grid.add(txtCnpj, 1, 1);
        grid.add(new Label("Endereço:"), 0, 2); grid.add(txtEnd, 1, 2);
        grid.add(chkAtiva, 1, 3);

        for (int i = 0; i < 4; i++) {
            Label l = (Label) grid.getChildren().get(i * 2);
            l.setStyle("-fx-text-fill: #9e9e9e;");
        }

        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String nome = txtNome.getText().trim();
                if (nome.isEmpty()) { Alerta.aviso("Atenção", "O nome da loja é obrigatório."); return; }
                Loja l = novo ? new Loja() : loja;
                l.setNome(nome);
                l.setCnpj(txtCnpj.getText().trim());
                l.setEndereco(txtEnd.getText().trim());
                l.setAtiva(chkAtiva.isSelected());
                if (dao.salvar(l)) {
                    Alerta.info("Loja", (novo ? "Loja criada" : "Loja atualizada") + " com sucesso!");
                    carregarTabela();
                } else {
                    Alerta.erro("Erro", "Não foi possível salvar a loja.");
                }
            }
        });
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
