package com.erp.view;

import com.erp.dao.FornecedorDAO;
import com.erp.model.Fornecedor;
import com.erp.util.Alerta;
import com.erp.util.ConsultaReceitaWS;
import com.erp.util.ConsultaViaCEP;
import com.erp.util.ValidadorFiscal;
import com.erp.util.ValidacaoFeedback;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FornecedorView {

    private final FornecedorDAO dao = new FornecedorDAO();
    private TableView<Fornecedor> tabela;

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("🏭 Fornecedores");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Cadastro e gestão de fornecedores");
        sub.getStyleClass().add("subtitulo-modulo");

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Buscar por nome ou CNPJ...");
        txtBusca.getStyleClass().add("campo-busca");
        txtBusca.setPrefWidth(350);
        txtBusca.textProperty().addListener((o, v, n) -> carregar(n));

        Button btnNovo = new Button("+ Novo Fornecedor");
        btnNovo.getStyleClass().add("btn-primario");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        HBox acoes = new HBox(12, txtBusca, new Region(), btnNovo);
        HBox.setHgrow(acoes.getChildren().get(1), Priority.ALWAYS);
        acoes.setAlignment(Pos.CENTER_LEFT);

        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);
        carregar("");

        root.getChildren().addAll(new VBox(4, titulo, sub), acoes, tabela);
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Fornecedor> criarTabela() {
        TableView<Fornecedor> tv = new TableView<>();
        tv.setPlaceholder(new Label("Nenhum fornecedor encontrado."));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Fornecedor, String> colRazao = new TableColumn<>("Razão Social");
        colRazao.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRazaoSocial()));
        colRazao.setPrefWidth(250);

        TableColumn<Fornecedor, String> colFantasia = new TableColumn<>("Nome Fantasia");
        colFantasia.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomeFantasia()));
        colFantasia.setPrefWidth(180);

        TableColumn<Fornecedor, String> colCnpj = new TableColumn<>("CNPJ");
        colCnpj.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCnpj()));
        colCnpj.setPrefWidth(150);

        TableColumn<Fornecedor, String> colTel = new TableColumn<>("Telefone");
        colTel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefone()));
        colTel.setPrefWidth(130);

        TableColumn<Fornecedor, String> colCidade = new TableColumn<>("Cidade");
        colCidade.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCidade() != null ? c.getValue().getCidade() + " - " + c.getValue().getEstado() : ""));
        colCidade.setPrefWidth(150);

        TableColumn<Fornecedor, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setPrefWidth(130);
        colAcoes.setCellFactory(c -> new TableCell<>() {
            final Button btnE = new Button("✏ Editar");
            final Button btnD = new Button("🗑");
            {
                btnE.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #3a3d4e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnD.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #fa5252; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnE.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnD.setOnAction(e -> {
                    Fornecedor f = getTableView().getItems().get(getIndex());
                    if (Alerta.confirmar("Excluir", "Inativar fornecedor '" + f.getRazaoSocial() + "'?")) {
                        dao.excluir(f.getId()); carregar("");
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(4, btnE, btnD));
            }
        });

        tv.getColumns().addAll(colRazao, colFantasia, colCnpj, colTel, colCidade, colAcoes);
        return tv;
    }

    private void carregar(String filtro) {
        tabela.setItems(FXCollections.observableArrayList(dao.listarPorFiltro(filtro, true)));
    }

    private void abrirFormulario(Fornecedor forn) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(forn == null ? "Novo Fornecedor" : "Editar Fornecedor");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #252836;");

        TextField razao  = c("Razão Social *"), fantasia = c("Nome Fantasia"), cnpj = c("CNPJ"),
                ie = c("IE"), email = c("E-mail"), tel = c("Telefone"), cel = c("Celular"),
                cep = c("CEP"), logradouro = c("Logradouro"), numero = c("Número"),
                comp = c("Complemento"), bairro = c("Bairro"), cidade = c("Cidade"),
                estado = c("Estado"), contato = c("Contato");
        TextArea obs = new TextArea(); obs.setPromptText("Observações"); obs.setPrefRowCount(2);

        // --- CNPJ lookup ---
        Button btnConsultarCnpj = new Button("🔍 Consultar CNPJ");
        btnConsultarCnpj.setVisible(false);
        ProgressIndicator piCnpj = new ProgressIndicator();
        piCnpj.setPrefSize(18, 18);
        piCnpj.setVisible(false);

        cnpj.focusedProperty().addListener((o2, old2, focused2) -> {
            if (!focused2) {
                String nums = ValidadorFiscal.apenasNumeros(cnpj.getText());
                if (nums.length() == 14 && ValidadorFiscal.validarCNPJ(nums)) {
                    ValidacaoFeedback.aplicarValido(cnpj, "CNPJ válido");
                    btnConsultarCnpj.setVisible(true);
                } else if (!nums.isEmpty()) {
                    ValidacaoFeedback.aplicarInvalido(cnpj, "CNPJ inválido — verifique os dígitos");
                    btnConsultarCnpj.setVisible(false);
                } else {
                    ValidacaoFeedback.limpar(cnpj);
                    btnConsultarCnpj.setVisible(false);
                }
            }
        });

        btnConsultarCnpj.setOnAction(ev -> {
            String cnpjNum = ValidadorFiscal.apenasNumeros(cnpj.getText());
            btnConsultarCnpj.setDisable(true);
            piCnpj.setVisible(true);
            new Thread(() -> {
                ConsultaReceitaWS.DadosCNPJ dados = ConsultaReceitaWS.consultar(cnpjNum);
                Platform.runLater(() -> {
                    piCnpj.setVisible(false);
                    btnConsultarCnpj.setDisable(false);
                    if (dados.mensagemErro != null && !dados.mensagemErro.isBlank()) {
                        Alerta.aviso("Consulta CNPJ", dados.mensagemErro);
                        return;
                    }
                    if (!dados.razaoSocial.isEmpty())  razao.setText(dados.razaoSocial);
                    if (!dados.nomeFantasia.isEmpty()) fantasia.setText(dados.nomeFantasia);
                    if (!dados.email.isEmpty())        email.setText(dados.email);
                    if (!dados.telefone.isEmpty())     tel.setText(dados.telefone);
                    if (!dados.cep.isEmpty())          cep.setText(dados.cep);
                    if (!dados.logradouro.isEmpty())   logradouro.setText(dados.logradouro);
                    if (!dados.numero.isEmpty())       numero.setText(dados.numero);
                    if (!dados.bairro.isEmpty())       bairro.setText(dados.bairro);
                    if (!dados.municipio.isEmpty())    cidade.setText(dados.municipio);
                    if (!dados.uf.isEmpty())           estado.setText(dados.uf);
                    Alerta.info("CNPJ Consultado",
                        "Situação: " + dados.situacao + "\n" + dados.razaoSocial);
                });
            }, "cnpj-consulta-forn").start();
        });

        // CEP auto-fill
        cep.focusedProperty().addListener((o2, old2, focused2) -> {
            if (!focused2) {
                String cepNums = ValidadorFiscal.apenasNumeros(cep.getText());
                if (cepNums.length() == 8) {
                    new Thread(() -> {
                        ConsultaViaCEP.Endereco end = ConsultaViaCEP.consultar(cepNums);
                        Platform.runLater(() -> {
                            if (end.encontrado) {
                                if (!end.logradouro.isEmpty()) logradouro.setText(end.logradouro);
                                if (!end.bairro.isEmpty())     bairro.setText(end.bairro);
                                if (!end.localidade.isEmpty()) cidade.setText(end.localidade);
                                if (!end.uf.isEmpty())         estado.setText(end.uf);
                            }
                        });
                    }, "cep-lookup-forn").start();
                }
            }
        });

        if (forn != null) {
            razao.setText(forn.getRazaoSocial()); fantasia.setText(forn.getNomeFantasia());
            cnpj.setText(forn.getCnpj()); ie.setText(forn.getIe()); email.setText(forn.getEmail());
            tel.setText(forn.getTelefone()); cel.setText(forn.getCelular()); cep.setText(forn.getCep());
            logradouro.setText(forn.getLogradouro()); numero.setText(forn.getNumero());
            comp.setText(forn.getComplemento()); bairro.setText(forn.getBairro());
            cidade.setText(forn.getCidade()); estado.setText(forn.getEstado());
            contato.setText(forn.getContato()); obs.setText(forn.getObservacoes());
        }

        int r = 0;
        form.add(l("Razão Social *"), 0, r); form.add(razao, 1, r++);
        form.add(l("Nome Fantasia"), 0, r); form.add(fantasia, 1, r++);
        HBox cnpjBox = new HBox(6, cnpj, piCnpj, btnConsultarCnpj);
        HBox.setHgrow(cnpj, Priority.ALWAYS);
        cnpjBox.setAlignment(Pos.CENTER_LEFT);
        form.add(l("CNPJ"), 0, r); form.add(cnpjBox, 1, r++);
        form.add(l("IE"), 0, r); form.add(ie, 1, r++);
        form.add(l("E-mail"), 0, r); form.add(email, 1, r++);
        form.add(l("Telefone"), 0, r); form.add(tel, 1, r++);
        form.add(l("Celular"), 0, r); form.add(cel, 1, r++);
        form.add(l("CEP"), 0, r); form.add(cep, 1, r++);
        form.add(l("Logradouro"), 0, r); form.add(logradouro, 1, r++);
        form.add(l("Número"), 0, r); form.add(numero, 1, r++);
        form.add(l("Complemento"), 0, r); form.add(comp, 1, r++);
        form.add(l("Bairro"), 0, r); form.add(bairro, 1, r++);
        form.add(l("Cidade"), 0, r); form.add(cidade, 1, r++);
        form.add(l("Estado"), 0, r); form.add(estado, 1, r++);
        form.add(l("Contato"), 0, r); form.add(contato, 1, r++);
        form.add(l("Observações"), 0, r); form.add(obs, 1, r++);

        ColumnConstraints cc1 = new ColumnConstraints(130);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(cc1, cc2);

        Button btnS = new Button("💾 Salvar"); btnS.getStyleClass().add("btn-primario");
        Button btnC = new Button("Cancelar"); btnC.getStyleClass().add("btn-secundario");
        btnC.setOnAction(e -> dlg.close());
        btnS.setOnAction(e -> {
            if (razao.getText().isBlank()) { Alerta.aviso("Atenção", "Razão Social é obrigatória."); return; }
            String cnpjVal = cnpj.getText().trim();
            if (!cnpjVal.isBlank() && !ValidadorFiscal.validarCNPJ(cnpjVal)) {
                Alerta.aviso("Atenção", "CNPJ inválido. Verifique os dígitos."); return;
            }
            Fornecedor f = forn != null ? forn : new Fornecedor();
            f.setRazaoSocial(razao.getText().trim()); f.setNomeFantasia(fantasia.getText().trim());
            f.setCnpj(ValidadorFiscal.apenasNumeros(cnpjVal)); f.setIe(ie.getText().trim()); f.setEmail(email.getText().trim());
            f.setTelefone(ValidadorFiscal.apenasNumeros(tel.getText())); f.setCelular(ValidadorFiscal.apenasNumeros(cel.getText())); f.setCep(ValidadorFiscal.apenasNumeros(cep.getText()));
            f.setLogradouro(logradouro.getText().trim()); f.setNumero(numero.getText().trim());
            f.setComplemento(comp.getText().trim()); f.setBairro(bairro.getText().trim());
            f.setCidade(cidade.getText().trim()); f.setEstado(estado.getText().trim());
            f.setContato(contato.getText().trim()); f.setObservacoes(obs.getText().trim());
            boolean ok = forn == null ? dao.salvar(f) : dao.atualizar(f);
            if (ok) { carregar(""); dlg.close(); }
            else Alerta.erro("Erro", "Não foi possível salvar.");
        });

        HBox botoes = new HBox(10, btnS, btnC);
        botoes.setPadding(new Insets(16)); botoes.setAlignment(Pos.CENTER_RIGHT);
        botoes.setStyle("-fx-background-color: #252836;");

        scroll.setContent(new VBox(0, form, botoes));
        Scene sc = new Scene(scroll, 580, 700);
        sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dlg.setScene(sc); dlg.showAndWait();
    }

    private TextField c(String p) { TextField tf = new TextField(); tf.setPromptText(p); return tf; }
    private Label l(String t) { Label lb = new Label(t); lb.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;"); return lb; }
}
