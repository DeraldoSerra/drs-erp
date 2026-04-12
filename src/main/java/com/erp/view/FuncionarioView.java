package com.erp.view;

import com.erp.dao.FuncionarioDAO;
import com.erp.model.Funcionario;
import com.erp.util.Alerta;
import com.erp.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FuncionarioView {

    private final FuncionarioDAO dao = new FuncionarioDAO();
    private TableView<Funcionario> tabela;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Region criar() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("👔 Funcionários - RH");
        titulo.getStyleClass().add("titulo-modulo");
        Label sub = new Label("Gestão do quadro de funcionários");
        sub.getStyleClass().add("subtitulo-modulo");

        long total = dao.contar();
        Label cardTotal = new Label("👔 " + total + " funcionários ativos");
        cardTotal.setStyle("-fx-text-fill: #4dabf7; -fx-font-size: 14px; -fx-background-color: #252836; -fx-background-radius: 8; -fx-padding: 12 20;");

        TextField txtBusca = new TextField();
        txtBusca.setPromptText("🔍 Buscar por nome ou CPF...");
        txtBusca.getStyleClass().add("campo-busca");
        txtBusca.setPrefWidth(350);
        txtBusca.textProperty().addListener((o, v, n) -> carregar(n));

        Button btnNovo = new Button("+ Novo Funcionário");
        btnNovo.getStyleClass().add("btn-primario");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        HBox acoes = new HBox(12, txtBusca, new Region(), btnNovo);
        HBox.setHgrow(acoes.getChildren().get(1), Priority.ALWAYS);
        acoes.setAlignment(Pos.CENTER_LEFT);

        tabela = criarTabela();
        VBox.setVgrow(tabela, Priority.ALWAYS);
        carregar("");

        root.getChildren().addAll(new VBox(4, titulo, sub), cardTotal, acoes, tabela);
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Funcionario> criarTabela() {
        TableView<Funcionario> tv = new TableView<>();
        tv.setPlaceholder(new Label("Nenhum funcionário encontrado."));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Funcionario, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colNome.setPrefWidth(220);

        TableColumn<Funcionario, String> colCpf = new TableColumn<>("CPF");
        colCpf.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarCpf(c.getValue().getCpf())));
        colCpf.setPrefWidth(130);

        TableColumn<Funcionario, String> colCargo = new TableColumn<>("Cargo");
        colCargo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCargoNome()));
        colCargo.setPrefWidth(130);

        TableColumn<Funcionario, String> colSalario = new TableColumn<>("Salário");
        colSalario.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.formatarMoeda(c.getValue().getSalario())));
        colSalario.setPrefWidth(110);

        TableColumn<Funcionario, String> colAdm = new TableColumn<>("Admissão");
        colAdm.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataAdmissao() != null ? c.getValue().getDataAdmissao().format(FMT) : ""));
        colAdm.setPrefWidth(100);

        TableColumn<Funcionario, String> colCidade = new TableColumn<>("Cidade");
        colCidade.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCidade() != null ? c.getValue().getCidade() : ""));
        colCidade.setPrefWidth(130);

        TableColumn<Funcionario, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setPrefWidth(130);
        colAcoes.setCellFactory(c -> new TableCell<>() {
            final Button btnE = new Button("✏ Editar");
            final Button btnD = new Button("🗑");
            {
                btnE.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #3a3d4e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnD.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #fa5252; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                btnE.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnD.setOnAction(e -> {
                    Funcionario f = getTableView().getItems().get(getIndex());
                    if (Alerta.confirmar("Demitir", "Confirma a demissão de '" + f.getNome() + "'?")) {
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

        tv.getColumns().addAll(colNome, colCpf, colCargo, colSalario, colAdm, colCidade, colAcoes);
        return tv;
    }

    private void carregar(String filtro) {
        tabela.setItems(FXCollections.observableArrayList(dao.listarPorFiltro(filtro, true)));
    }

    private void abrirFormulario(Funcionario func) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(func == null ? "Novo Funcionário" : "Editar Funcionário");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #252836;");

        TextField nome = c("Nome *"), cpf = c("CPF"), rg = c("RG"),
                email = c("E-mail"), tel = c("Telefone"), cel = c("Celular"),
                salario = c("Salário"), cep = c("CEP"), logradouro = c("Logradouro"),
                numero = c("Número"), comp = c("Complemento"), bairro = c("Bairro"),
                cidade = c("Cidade"), estado = c("Estado");
        DatePicker dpNasc = new DatePicker(); dpNasc.setPromptText("Nascimento");
        DatePicker dpAdm  = new DatePicker(LocalDate.now()); dpAdm.setPromptText("Admissão");
        TextArea obs = new TextArea(); obs.setPromptText("Observações"); obs.setPrefRowCount(2);

        if (func != null) {
            nome.setText(func.getNome()); cpf.setText(func.getCpf()); rg.setText(func.getRg());
            email.setText(func.getEmail()); tel.setText(func.getTelefone()); cel.setText(func.getCelular());
            salario.setText(String.format("%.2f", func.getSalario()));
            if (func.getDataNascimento() != null) dpNasc.setValue(func.getDataNascimento());
            if (func.getDataAdmissao() != null) dpAdm.setValue(func.getDataAdmissao());
            cep.setText(func.getCep()); logradouro.setText(func.getLogradouro()); numero.setText(func.getNumero());
            comp.setText(func.getComplemento()); bairro.setText(func.getBairro());
            cidade.setText(func.getCidade()); estado.setText(func.getEstado());
            obs.setText(func.getObservacoes());
        }

        int r = 0;
        form.add(l("Nome *"), 0, r); form.add(nome, 1, r++);
        form.add(l("CPF"), 0, r); form.add(cpf, 1, r++);
        form.add(l("RG"), 0, r); form.add(rg, 1, r++);
        form.add(l("Nascimento"), 0, r); form.add(dpNasc, 1, r++);
        form.add(l("Admissão"), 0, r); form.add(dpAdm, 1, r++);
        form.add(l("Salário"), 0, r); form.add(salario, 1, r++);
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
        form.add(l("Observações"), 0, r); form.add(obs, 1, r++);

        ColumnConstraints cc1 = new ColumnConstraints(130);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(cc1, cc2);

        Button btnS = new Button("💾 Salvar"); btnS.getStyleClass().add("btn-primario");
        Button btnC = new Button("Cancelar"); btnC.getStyleClass().add("btn-secundario");
        btnC.setOnAction(e -> dlg.close());
        btnS.setOnAction(e -> {
            if (nome.getText().isBlank()) { Alerta.aviso("Atenção", "Nome é obrigatório."); return; }
            Funcionario f = func != null ? func : new Funcionario();
            f.setNome(nome.getText().trim()); f.setCpf(cpf.getText().trim()); f.setRg(rg.getText().trim());
            f.setDataNascimento(dpNasc.getValue()); f.setDataAdmissao(dpAdm.getValue() != null ? dpAdm.getValue() : LocalDate.now());
            f.setSalario(parseD(salario.getText())); f.setEmail(email.getText().trim());
            f.setTelefone(tel.getText().trim()); f.setCelular(cel.getText().trim());
            f.setCep(cep.getText().trim()); f.setLogradouro(logradouro.getText().trim());
            f.setNumero(numero.getText().trim()); f.setComplemento(comp.getText().trim());
            f.setBairro(bairro.getText().trim()); f.setCidade(cidade.getText().trim());
            f.setEstado(estado.getText().trim()); f.setObservacoes(obs.getText().trim());
            boolean ok = func == null ? dao.salvar(f) : dao.atualizar(f);
            if (ok) { carregar(""); dlg.close(); }
            else Alerta.erro("Erro", "Não foi possível salvar.");
        });

        HBox botoes = new HBox(10, btnS, btnC);
        botoes.setPadding(new Insets(16)); botoes.setAlignment(Pos.CENTER_RIGHT);
        botoes.setStyle("-fx-background-color: #252836;");

        scroll.setContent(new VBox(0, form, botoes));
        Scene sc = new Scene(scroll, 580, 720);
        sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dlg.setScene(sc); dlg.showAndWait();
    }

    private TextField c(String p) { TextField tf = new TextField(); tf.setPromptText(p); return tf; }
    private Label l(String t) { Label lb = new Label(t); lb.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;"); return lb; }
    private double parseD(String s) { try { return Double.parseDouble(s.replace(",", ".")); } catch (Exception e) { return 0; } }
}
