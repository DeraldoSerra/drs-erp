package com.erp.view;

import com.erp.dao.LojaDAO;
import com.erp.model.Loja;
import com.erp.util.Alerta;
import com.erp.util.ConsultaReceitaWS;
import com.erp.util.ValidadorFiscal;
import javafx.application.Platform;
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

        TextField txtNome  = new TextField(novo ? "" : nvl(loja.getNome()));
        TextField txtCnpj  = new TextField(novo ? "" : nvl(loja.getCnpj()));
        TextField txtEnd   = new TextField(novo ? "" : nvl(loja.getEndereco()));
        CheckBox  chkAtiva = new CheckBox("Ativa");
        Label     lblStatus   = new Label();
        Button    btnConsultar = new Button("🔍 Consultar");

        chkAtiva.setSelected(novo || loja.isAtiva());
        chkAtiva.setStyle("-fx-text-fill: #e0e0e0;");
        txtNome.setPromptText("Nome da loja");
        txtCnpj.setPromptText("00.000.000/0000-00");
        txtEnd.setPromptText("Endereço completo");
        txtNome.setPrefWidth(240);
        txtCnpj.setPrefWidth(180);
        lblStatus.setStyle("-fx-font-size: 11px;");
        btnConsultar.getStyleClass().add("btn-secundario");
        btnConsultar.setDisable(true);

        // Máscara e validação ao digitar
        txtCnpj.textProperty().addListener((obs, oldVal, newVal) -> {
            String masked = ValidadorFiscal.aplicarMascaraCpfCnpj(newVal);
            if (!masked.equals(newVal)) { txtCnpj.setText(masked); return; }
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

        // Consultar Receita Federal
        btnConsultar.setOnAction(ev -> {
            btnConsultar.setDisable(true);
            btnConsultar.setText("⏳...");
            String cnpjAtual = txtCnpj.getText();
            Thread t = new Thread(() -> {
                ConsultaReceitaWS.DadosCNPJ dados = ConsultaReceitaWS.consultar(cnpjAtual);
                Platform.runLater(() -> {
                    btnConsultar.setText("🔍 Consultar");
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
                        Alerta.aviso("CNPJ", dados.mensagemErro != null ? dados.mensagemErro : "Não encontrado");
                    }
                });
            });
            t.setDaemon(true);
            t.start();
        });

        Label lNome = new Label("Nome*:");
        Label lCnpj = new Label("CNPJ:");
        Label lEnd  = new Label("Endereço:");
        lNome.setStyle("-fx-text-fill: #9e9e9e;");
        lCnpj.setStyle("-fx-text-fill: #9e9e9e;");
        lEnd.setStyle("-fx-text-fill: #9e9e9e;");

        HBox cnpjBox = new HBox(8, txtCnpj, btnConsultar);
        cnpjBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(lNome,            0, 0); grid.add(txtNome,   1, 0);
        grid.add(lCnpj,            0, 1); grid.add(cnpjBox,   1, 1);
        grid.add(new Label(),      0, 2); grid.add(lblStatus, 1, 2);
        grid.add(lEnd,             0, 3); grid.add(txtEnd,    1, 3);
        grid.add(new Label(),      0, 4); grid.add(chkAtiva,  1, 4);

        dlg.getDialogPane().setContent(grid);

        // Validação ao clicar OK
        Button btnOk = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (txtNome.getText().trim().isEmpty()) {
                Alerta.aviso("Atenção", "O nome da loja é obrigatório.");
                ev.consume();
                return;
            }
            String cnpjDigitado = txtCnpj.getText().trim();
            if (!cnpjDigitado.isBlank()) {
                if (!ValidadorFiscal.validarCNPJ(ValidadorFiscal.apenasNumeros(cnpjDigitado))) {
                    Alerta.aviso("CNPJ inválido", "O CNPJ informado não é válido. Verifique e tente novamente.");
                    ev.consume();
                }
            }
        });

        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String cnpjNums = ValidadorFiscal.apenasNumeros(txtCnpj.getText().trim());
                String cnpjFormatado = cnpjNums.length() == 14 ? ValidadorFiscal.formatarCNPJ(cnpjNums) : null;
                Loja l = novo ? new Loja() : loja;
                l.setNome(txtNome.getText().trim());
                l.setCnpj(cnpjFormatado);
                l.setEndereco(txtEnd.getText().trim().isEmpty() ? null : txtEnd.getText().trim());
                l.setAtiva(chkAtiva.isSelected());
                try {
                    if (dao.salvar(l)) {
                        Alerta.info("Loja", (novo ? "Loja criada" : "Loja atualizada") + " com sucesso!");
                        carregarTabela();
                    } else {
                        Alerta.erro("Erro", "Não foi possível salvar a loja.");
                    }
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    if (msg != null && msg.contains("lojas_cnpj_unique")) {
                        Alerta.erro("CNPJ Duplicado", "Já existe outra loja com este CNPJ.");
                    } else {
                        Alerta.erro("Erro ao Salvar", "Falha: " + msg);
                    }
                }
            }
        });
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
