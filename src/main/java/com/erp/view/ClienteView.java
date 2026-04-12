package com.erp.view;

import com.erp.dao.ClienteDAO;
import com.erp.model.Cliente;
import com.erp.util.Alerta;
import com.erp.util.ConsultaReceitaWS;
import com.erp.util.ConsultaViaCEP;
import com.erp.util.Formatador;
import com.erp.util.ValidadorEmail;
import com.erp.util.ValidadorFiscal;
import com.erp.util.ValidacaoFeedback;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ClienteView {

    private final ClienteDAO dao = new ClienteDAO();
    private TableView<Cliente> tabela;
    private ObservableList<Cliente> dados;

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        // Cabeçalho
        Label titulo = new Label("👥 Clientes");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Cadastro e gestão de clientes");
        sub.getStyleClass().add("subtitulo-modulo");

        // Barra de ações
        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Buscar por nome ou CPF/CNPJ...");
        txtBusca.getStyleClass().add("campo-busca");
        txtBusca.setPrefWidth(350);
        txtBusca.textProperty().addListener((o, v, n) -> filtrar(n));

        Button btnNovo = new Button("+ Novo Cliente");
        btnNovo.getStyleClass().add("btn-primario");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        HBox acoes = new HBox(12, txtBusca, new Region(), btnNovo);
        HBox.setHgrow(acoes.getChildren().get(1), Priority.ALWAYS);
        acoes.setAlignment(Pos.CENTER_LEFT);

        // Tabela
        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);

        carregarDados("");

        root.getChildren().addAll(new VBox(4, titulo, sub), acoes, tabela);
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Cliente> criarTabela() {
        TableView<Cliente> tv = new TableView<>();
        tv.setPlaceholder(new Label("Nenhum cliente encontrado."));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Cliente, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setPrefWidth(250);

        TableColumn<Cliente, String> colDoc = new TableColumn<>("CPF/CNPJ");
        colDoc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isPessoaJuridica()
                        ? Formatador.formatarCnpj(c.getValue().getCpfCnpj())
                        : Formatador.formatarCpf(c.getValue().getCpfCnpj())));
        colDoc.setPrefWidth(160);

        TableColumn<Cliente, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(
                "J".equals(c.getValue().getTipoPessoa()) ? "Jurídica" : "Física"));
        colTipo.setPrefWidth(80);

        TableColumn<Cliente, String> colTel = new TableColumn<>("Telefone");
        colTel.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCelular() != null ? c.getValue().getCelular() : c.getValue().getTelefone()));
        colTel.setPrefWidth(130);

        TableColumn<Cliente, String> colCidade = new TableColumn<>("Cidade");
        colCidade.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCidade() != null ? c.getValue().getCidade() + " - " + c.getValue().getEstado() : ""));
        colCidade.setPrefWidth(150);

        TableColumn<Cliente, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAtivo() ? "Ativo" : "Inativo"));
        colStatus.setPrefWidth(80);

        TableColumn<Cliente, Void> colAcoes = criarColunaAcoes();

        tv.getColumns().addAll(colNome, colDoc, colTipo, colTel, colCidade, colStatus, colAcoes);
        return tv;
    }

    private TableColumn<Cliente, Void> criarColunaAcoes() {
        TableColumn<Cliente, Void> col = new TableColumn<>("Ações");
        col.setPrefWidth(130);
        col.setCellFactory(c -> new TableCell<>() {
            final Button btnEditar  = new Button("✏ Editar");
            final Button btnExcluir = new Button("🗑");
            {
                btnEditar.getStyleClass().add("btn-secundario");
                btnEditar.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnExcluir.getStyleClass().add("btn-perigo");
                btnExcluir.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnExcluir.setOnAction(e -> {
                    Cliente c2 = getTableView().getItems().get(getIndex());
                    if (Alerta.confirmar("Excluir", "Deseja inativar o cliente '" + c2.getNome() + "'?")) {
                        dao.excluir(c2.getId());
                        carregarDados("");
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(4, btnEditar, btnExcluir));
            }
        });
        return col;
    }

    private void carregarDados(String filtro) {
        List<Cliente> lista = dao.listarPorFiltro(filtro, true);
        dados = FXCollections.observableArrayList(lista);
        tabela.setItems(dados);
    }

    private void filtrar(String texto) {
        carregarDados(texto);
    }

    private void abrirFormulario(Cliente cliente) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(cliente == null ? "Novo Cliente" : "Editar Cliente");
        dialog.setMinWidth(600);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #252836;");

        // Campos
        TextField txtNome       = criarCampo("Nome *");
        TextField txtCpfCnpj    = criarCampo("CPF/CNPJ");
        ComboBox<String> cmbTipo = new ComboBox<>(FXCollections.observableArrayList("Pessoa Física", "Pessoa Jurídica"));
        cmbTipo.setValue("Pessoa Física");
        TextField txtRgIe       = criarCampo("RG/IE");
        TextField txtEmail      = criarCampo("E-mail");
        TextField txtTelefone   = criarCampo("Telefone");
        TextField txtCelular    = criarCampo("Celular");
        TextField txtCep        = criarCampo("CEP");
        TextField txtLogradouro = criarCampo("Logradouro");
        TextField txtNumero     = criarCampo("Número");
        TextField txtComplemento= criarCampo("Complemento");
        TextField txtBairro     = criarCampo("Bairro");
        TextField txtCidade     = criarCampo("Cidade");
        TextField txtEstado     = criarCampo("Estado (UF)");
        TextField txtLimite     = criarCampo("Limite de Crédito");
        TextArea  txaObs        = new TextArea();
        txaObs.setPromptText("Observações");
        txaObs.setPrefRowCount(3);

        // --- CNPJ lookup button ---
        Button btnConsultarCnpj = new Button("🔍 Consultar CNPJ");
        btnConsultarCnpj.setVisible(false);
        ProgressIndicator piCnpj = new ProgressIndicator();
        piCnpj.setPrefSize(18, 18);
        piCnpj.setVisible(false);

        // Show button when PJ + valid CNPJ typed
        txtCpfCnpj.focusedProperty().addListener((o2, old2, focused2) -> {
            if (!focused2) {
                String nums = ValidadorFiscal.apenasNumeros(txtCpfCnpj.getText());
                boolean isPJ = "Pessoa Jurídica".equals(cmbTipo.getValue());
                if (isPJ && nums.length() == 14 && ValidadorFiscal.validarCNPJ(nums)) {
                    ValidacaoFeedback.aplicarValido(txtCpfCnpj, "CNPJ válido");
                    btnConsultarCnpj.setVisible(true);
                } else if (nums.length() == 11 && ValidadorFiscal.validarCPF(nums)) {
                    ValidacaoFeedback.aplicarValido(txtCpfCnpj, "CPF válido");
                    btnConsultarCnpj.setVisible(false);
                } else if (!nums.isEmpty()) {
                    ValidacaoFeedback.aplicarInvalido(txtCpfCnpj, "CPF/CNPJ inválido");
                    btnConsultarCnpj.setVisible(false);
                } else {
                    ValidacaoFeedback.limpar(txtCpfCnpj);
                    btnConsultarCnpj.setVisible(false);
                }
            }
        });
        cmbTipo.valueProperty().addListener((o2, old2, n2) -> {
            if (!"Pessoa Jurídica".equals(n2)) btnConsultarCnpj.setVisible(false);
        });

        btnConsultarCnpj.setOnAction(ev -> {
            String cnpjNum = ValidadorFiscal.apenasNumeros(txtCpfCnpj.getText());
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
                    if (!dados.razaoSocial.isEmpty()) txtNome.setText(dados.razaoSocial);
                    if (!dados.email.isEmpty())       txtEmail.setText(dados.email);
                    if (!dados.telefone.isEmpty())    txtTelefone.setText(dados.telefone);
                    if (!dados.cep.isEmpty())         txtCep.setText(dados.cep);
                    if (!dados.logradouro.isEmpty())  txtLogradouro.setText(dados.logradouro);
                    if (!dados.numero.isEmpty())      txtNumero.setText(dados.numero);
                    if (!dados.bairro.isEmpty())      txtBairro.setText(dados.bairro);
                    if (!dados.municipio.isEmpty())   txtCidade.setText(dados.municipio);
                    if (!dados.uf.isEmpty())          txtEstado.setText(dados.uf);
                    Alerta.info("CNPJ Consultado",
                        "Situação: " + dados.situacao + "\n" + dados.razaoSocial);
                });
            }, "cnpj-consulta-cliente").start();
        });

        // CEP auto-fill via ViaCEP
        txtCep.focusedProperty().addListener((o2, old2, focused2) -> {
            if (!focused2) {
                String cepNums = ValidadorFiscal.apenasNumeros(txtCep.getText());
                if (cepNums.length() == 8) {
                    new Thread(() -> {
                        ConsultaViaCEP.Endereco end = ConsultaViaCEP.consultar(cepNums);
                        Platform.runLater(() -> {
                            if (end.encontrado) {
                                if (!end.logradouro.isEmpty()) txtLogradouro.setText(end.logradouro);
                                if (!end.bairro.isEmpty())     txtBairro.setText(end.bairro);
                                if (!end.localidade.isEmpty()) txtCidade.setText(end.localidade);
                                if (!end.uf.isEmpty())         txtEstado.setText(end.uf);
                            }
                        });
                    }, "cep-lookup-cliente").start();
                }
            }
        });

        // Email format validation
        txtEmail.focusedProperty().addListener((o2, old2, focused2) -> {
            if (!focused2) {
                String email = txtEmail.getText().trim();
                if (email.isEmpty()) { ValidacaoFeedback.limpar(txtEmail); return; }
                if (ValidadorEmail.validarFormato(email)) {
                    ValidacaoFeedback.aplicarValido(txtEmail, "E-mail com formato válido");
                } else {
                    ValidacaoFeedback.aplicarInvalido(txtEmail, "Formato de e-mail inválido");
                }
            }
        });

        // Preencher se edição
        if (cliente != null) {
            txtNome.setText(cliente.getNome());
            txtCpfCnpj.setText(cliente.getCpfCnpj());
            cmbTipo.setValue("J".equals(cliente.getTipoPessoa()) ? "Pessoa Jurídica" : "Pessoa Física");
            txtRgIe.setText(cliente.getRgIe());
            txtEmail.setText(cliente.getEmail());
            txtTelefone.setText(cliente.getTelefone());
            txtCelular.setText(cliente.getCelular());
            txtCep.setText(cliente.getCep());
            txtLogradouro.setText(cliente.getLogradouro());
            txtNumero.setText(cliente.getNumero());
            txtComplemento.setText(cliente.getComplemento());
            txtBairro.setText(cliente.getBairro());
            txtCidade.setText(cliente.getCidade());
            txtEstado.setText(cliente.getEstado());
            txtLimite.setText(String.valueOf(cliente.getLimiteCredito()));
            txaObs.setText(cliente.getObservacoes());
        }

        int r = 0;
        form.add(label("Nome *"), 0, r); form.add(txtNome, 1, r++);
        form.add(label("Tipo Pessoa"), 0, r); form.add(cmbTipo, 1, r++);
        HBox cpfCnpjBox = new HBox(6, txtCpfCnpj, piCnpj, btnConsultarCnpj);
        HBox.setHgrow(txtCpfCnpj, Priority.ALWAYS);
        cpfCnpjBox.setAlignment(Pos.CENTER_LEFT);
        form.add(label("CPF/CNPJ"), 0, r); form.add(cpfCnpjBox, 1, r++);
        form.add(label("RG/IE"), 0, r); form.add(txtRgIe, 1, r++);
        form.add(label("E-mail"), 0, r); form.add(txtEmail, 1, r++);
        form.add(label("Telefone"), 0, r); form.add(txtTelefone, 1, r++);
        form.add(label("Celular"), 0, r); form.add(txtCelular, 1, r++);
        form.add(label("CEP"), 0, r); form.add(txtCep, 1, r++);
        form.add(label("Logradouro"), 0, r); form.add(txtLogradouro, 1, r++);
        form.add(label("Número"), 0, r); form.add(txtNumero, 1, r++);
        form.add(label("Complemento"), 0, r); form.add(txtComplemento, 1, r++);
        form.add(label("Bairro"), 0, r); form.add(txtBairro, 1, r++);
        form.add(label("Cidade"), 0, r); form.add(txtCidade, 1, r++);
        form.add(label("Estado"), 0, r); form.add(txtEstado, 1, r++);
        form.add(label("Limite de Crédito"), 0, r); form.add(txtLimite, 1, r++);
        form.add(label("Observações"), 0, r); form.add(txaObs, 1, r++);

        ColumnConstraints cc1 = new ColumnConstraints(140);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(cc1, cc2);

        Button btnSalvar  = new Button("💾 Salvar");
        btnSalvar.getStyleClass().add("btn-primario");
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> dialog.close());

        btnSalvar.setOnAction(e -> {
            if (txtNome.getText().isBlank()) {
                Alerta.aviso("Atenção", "O campo Nome é obrigatório.");
                return;
            }
            String cpfCnpj = txtCpfCnpj.getText().trim();
            if (!cpfCnpj.isBlank() && !ValidadorFiscal.validarCpfOuCnpj(cpfCnpj)) {
                Alerta.aviso("Atenção", "CPF/CNPJ inválido. Verifique os dígitos.");
                return;
            }
            Cliente c = cliente != null ? cliente : new Cliente();
            c.setNome(txtNome.getText().trim());
            c.setTipoPessoa("Pessoa Jurídica".equals(cmbTipo.getValue()) ? "J" : "F");
            c.setCpfCnpj(ValidadorFiscal.apenasNumeros(cpfCnpj));
            c.setRgIe(txtRgIe.getText().trim());
            c.setEmail(txtEmail.getText().trim());
            c.setTelefone(ValidadorFiscal.apenasNumeros(txtTelefone.getText()));
            c.setCelular(ValidadorFiscal.apenasNumeros(txtCelular.getText()));
            c.setCep(ValidadorFiscal.apenasNumeros(txtCep.getText()));
            c.setLogradouro(txtLogradouro.getText().trim());
            c.setNumero(txtNumero.getText().trim());
            c.setComplemento(txtComplemento.getText().trim());
            c.setBairro(txtBairro.getText().trim());
            c.setCidade(txtCidade.getText().trim());
            c.setEstado(txtEstado.getText().trim());
            try { c.setLimiteCredito(Double.parseDouble(txtLimite.getText().replace(",", "."))); }
            catch (NumberFormatException ex) { c.setLimiteCredito(0); }
            c.setObservacoes(txaObs.getText().trim());

            boolean ok = cliente == null ? dao.salvar(c) : dao.atualizar(c);
            if (ok) {
                carregarDados("");
                dialog.close();
            } else {
                Alerta.erro("Erro", "Não foi possível salvar o cliente.");
            }
        });

        HBox botoes = new HBox(10, btnSalvar, btnCancelar);
        botoes.setPadding(new Insets(16));
        botoes.setAlignment(Pos.CENTER_RIGHT);
        botoes.setStyle("-fx-background-color: #252836;");

        VBox conteudo = new VBox(0, form, botoes);
        scroll.setContent(conteudo);

        Scene scene = new Scene(scroll, 600, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private TextField criarCampo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    private Label label(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        return l;
    }
}
