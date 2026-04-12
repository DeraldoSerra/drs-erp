package com.erp.view;

import com.erp.dao.EmpresaDAO;
import com.erp.model.Empresa;
import com.erp.util.Alerta;
import com.erp.util.ConsultaReceitaWS;
import com.erp.util.ConsultaViaCEP;
import com.erp.util.ValidadorFiscal;
import com.erp.util.ValidadorIE;
import com.erp.util.ValidacaoFeedback;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;

public class EmpresaView {

    private final EmpresaDAO dao = new EmpresaDAO();

    // Campos de identificação
    private final TextField fRazaoSocial   = campo("Razão Social*");
    private final TextField fNomeFantasia  = campo("Nome Fantasia");
    private final TextField fCnpj          = campo("CNPJ*");
    private final TextField fIe            = campo("Inscrição Estadual");
    private final TextField fIm            = campo("Inscrição Municipal");
    private final ComboBox<String> cbRegime;

    // Contato
    private final TextField fEmail         = campo("E-mail");
    private final TextField fTelefone      = campo("Telefone");
    private final TextField fCelular       = campo("Celular");
    private final TextField fSite          = campo("Site");

    // Endereço
    private final TextField fCep           = campo("CEP");
    private final TextField fLogradouro    = campo("Logradouro");
    private final TextField fNumero        = campo("Número");
    private final TextField fComplemento   = campo("Complemento");
    private final TextField fBairro        = campo("Bairro");
    private final TextField fCidade        = campo("Cidade");
    private final ComboBox<String> cbEstado;

    // Logo
    private final TextField fLogoPath      = campo("Caminho da logo (opcional)");
    private final TextArea  taObs          = new TextArea();

    // Label de validação do CNPJ
    private final Label             lblCnpjStatus    = new Label();
    private final Button            btnConsultarCnpj = new Button("🔍 Consultar CNPJ");
    private final ProgressIndicator piCnpj           = new ProgressIndicator();

    public EmpresaView() {
        cbRegime = new ComboBox<>(FXCollections.observableArrayList(
                "SIMPLES_NACIONAL", "MEI", "LUCRO_PRESUMIDO", "LUCRO_REAL"
        ));
        cbRegime.setPromptText("Regime Tributário");
        cbRegime.setMaxWidth(Double.MAX_VALUE);
        estilizarCombo(cbRegime);

        cbEstado = new ComboBox<>(FXCollections.observableArrayList(
                "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG",
                "PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"
        ));
        cbEstado.setPromptText("UF");
        cbEstado.setMaxWidth(Double.MAX_VALUE);
        estilizarCombo(cbEstado);

        configurarMascaras();
        configurarValidacaoCnpj();
        configurarCep();
        configurarIe();
        carregarDados();
    }

    public Region criar() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #1e2027;");

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #1e2027;");

        Label titulo = new Label("🏢  Cadastro da Empresa");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        titulo.setTextFill(Color.WHITE);

        root.getChildren().addAll(
                titulo,
                secao("Identificação Fiscal", criarGridIdentificacao()),
                secao("Contato",              criarGridContato()),
                secao("Endereço",             criarGridEndereco()),
                secao("Observações",          criarGridObs()),
                criarBotoes()
        );

        scroll.setContent(root);
        return scroll;
    }

    // ---------------------------------------------------------------
    // Grids de campos
    // ---------------------------------------------------------------

    private GridPane criarGridIdentificacao() {
        GridPane g = grid();

        // Razão Social - linha 0, span 3
        g.add(label("Razão Social *"), 0, 0); g.add(fRazaoSocial, 1, 0, 3, 1);

        // Nome Fantasia - linha 1, span 3
        g.add(label("Nome Fantasia"),  0, 1); g.add(fNomeFantasia, 1, 1, 3, 1);

        // CNPJ + status + button
        g.add(label("CNPJ *"),         0, 2);
        piCnpj.setPrefSize(18, 18);
        piCnpj.setMaxSize(18, 18);
        HBox cnpjBox = new HBox(6, fCnpj, lblCnpjStatus, piCnpj, btnConsultarCnpj);
        cnpjBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fCnpj, Priority.ALWAYS);
        g.add(cnpjBox, 1, 2, 3, 1);

        // IE / IM / Regime
        g.add(label("Insc. Estadual"), 0, 3); g.add(fIe, 1, 3);
        g.add(label("Insc. Municipal"),2, 3); g.add(fIm, 3, 3);

        g.add(label("Regime Tributário"), 0, 4); g.add(cbRegime, 1, 4, 3, 1);

        return g;
    }

    private GridPane criarGridContato() {
        GridPane g = grid();
        g.add(label("E-mail"),    0, 0); g.add(fEmail,    1, 0, 3, 1);
        g.add(label("Telefone"),  0, 1); g.add(fTelefone, 1, 1);
        g.add(label("Celular"),   2, 1); g.add(fCelular,  3, 1);
        g.add(label("Site"),      0, 2); g.add(fSite,     1, 2, 3, 1);
        return g;
    }

    private GridPane criarGridEndereco() {
        GridPane g = grid();
        g.add(label("CEP"),         0, 0); g.add(fCep,        1, 0);
        g.add(label("Logradouro"),  0, 1); g.add(fLogradouro, 1, 1, 2, 1);
        g.add(label("Número"),      3, 1); g.add(fNumero,     4, 1);
        g.add(label("Complemento"), 0, 2); g.add(fComplemento,1, 2, 4, 1);
        g.add(label("Bairro"),      0, 3); g.add(fBairro,     1, 3);
        g.add(label("Cidade"),      2, 3); g.add(fCidade,     3, 3);
        g.add(label("UF"),          4, 3); g.add(cbEstado,    5, 3);
        g.add(label("Logo"),        0, 4);
        Button btnLogo = new Button("📂 Selecionar");
        estilizarBotaoSecundario(btnLogo);
        btnLogo.setOnAction(e -> escolherLogo());
        HBox logoBox = new HBox(6, fLogoPath, btnLogo);
        HBox.setHgrow(fLogoPath, Priority.ALWAYS);
        g.add(logoBox, 1, 4, 4, 1);
        return g;
    }

    private GridPane criarGridObs() {
        GridPane g = grid();
        taObs.setPrefRowCount(3);
        taObs.setStyle("-fx-background-color: #2a2d36; -fx-text-fill: white; -fx-border-color: #3a3d4a;");
        g.add(taObs, 0, 0, 4, 1);
        return g;
    }

    private HBox criarBotoes() {
        Button btnSalvar  = new Button("💾  Salvar Empresa");
        Button btnLimpar  = new Button("🗑  Limpar");

        estilizarBotaoPrimario(btnSalvar);
        estilizarBotaoSecundario(btnLimpar);

        btnSalvar.setOnAction(e -> salvar());
        btnLimpar.setOnAction(e -> limpar());

        HBox box = new HBox(10, btnSalvar, btnLimpar);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ---------------------------------------------------------------
    // Lógica
    // ---------------------------------------------------------------

    private void configurarMascaras() {
        fCnpj.textProperty().addListener((obs, old, novo) -> {
            if (novo.equals(old)) return;
            String masked = ValidadorFiscal.aplicarMascaraCpfCnpj(novo);
            if (!masked.equals(novo)) {
                fCnpj.setText(masked);
                fCnpj.positionCaret(masked.length());
            }
        });

        fCep.textProperty().addListener((obs, old, novo) -> {
            if (novo.equals(old)) return;
            String masked = ValidadorFiscal.aplicarMascaraCep(novo);
            if (!masked.equals(novo)) {
                fCep.setText(masked);
                fCep.positionCaret(masked.length());
            }
        });

        fTelefone.textProperty().addListener((obs, old, novo) -> {
            if (novo.equals(old)) return;
            String masked = ValidadorFiscal.aplicarMascaraTelefone(novo);
            if (!masked.equals(novo)) {
                fTelefone.setText(masked);
                fTelefone.positionCaret(masked.length());
            }
        });

        fCelular.textProperty().addListener((obs, old, novo) -> {
            if (novo.equals(old)) return;
            String masked = ValidadorFiscal.aplicarMascaraTelefone(novo);
            if (!masked.equals(novo)) {
                fCelular.setText(masked);
                fCelular.positionCaret(masked.length());
            }
        });
    }

    private void configurarValidacaoCnpj() {
        lblCnpjStatus.setFont(Font.font("System", FontWeight.BOLD, 12));

        // Setup button and progress indicator
        btnConsultarCnpj.setVisible(false);
        estilizarBotaoSecundario(btnConsultarCnpj);
        btnConsultarCnpj.setOnAction(e -> consultarCnpjReceita());
        piCnpj.setVisible(false);

        fCnpj.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String cnpj = fCnpj.getText();
                if (cnpj == null || cnpj.isBlank()) {
                    lblCnpjStatus.setText("");
                    btnConsultarCnpj.setVisible(false);
                    ValidacaoFeedback.limpar(fCnpj);
                    return;
                }
                if (ValidadorFiscal.validarCNPJ(cnpj)) {
                    lblCnpjStatus.setText("✔ válido");
                    lblCnpjStatus.setTextFill(Color.LIMEGREEN);
                    ValidacaoFeedback.aplicarValido(fCnpj, "CNPJ válido");
                    btnConsultarCnpj.setVisible(true);
                } else {
                    lblCnpjStatus.setText("✘ inválido");
                    lblCnpjStatus.setTextFill(Color.TOMATO);
                    ValidacaoFeedback.aplicarInvalido(fCnpj, "CNPJ inválido — verifique os dígitos");
                    btnConsultarCnpj.setVisible(false);
                }
            }
        });
    }

    private void consultarCnpjReceita() {
        String cnpj = ValidadorFiscal.apenasNumeros(fCnpj.getText());
        btnConsultarCnpj.setDisable(true);
        piCnpj.setVisible(true);
        lblCnpjStatus.setText("🔄 Consultando...");
        lblCnpjStatus.setTextFill(Color.LIGHTBLUE);

        new Thread(() -> {
            ConsultaReceitaWS.DadosCNPJ dados = ConsultaReceitaWS.consultar(cnpj);
            Platform.runLater(() -> {
                piCnpj.setVisible(false);
                btnConsultarCnpj.setDisable(false);

                if (dados.mensagemErro != null && !dados.mensagemErro.isBlank()) {
                    lblCnpjStatus.setText("⚠ Erro");
                    lblCnpjStatus.setTextFill(Color.ORANGE);
                    Alerta.aviso("Consulta CNPJ", dados.mensagemErro);
                    return;
                }

                if (dados.valido) {
                    lblCnpjStatus.setText("✔ ATIVA");
                    lblCnpjStatus.setTextFill(Color.LIMEGREEN);
                    // Auto-fill fields from API response
                    if (!dados.razaoSocial.isEmpty())  fRazaoSocial.setText(dados.razaoSocial);
                    if (!dados.nomeFantasia.isEmpty()) fNomeFantasia.setText(dados.nomeFantasia);
                    if (!dados.email.isEmpty())        fEmail.setText(dados.email);
                    if (!dados.telefone.isEmpty()) {
                        String tel = dados.telefone.replaceAll("[^0-9]", "");
                        fTelefone.setText(ValidadorFiscal.aplicarMascaraTelefone(tel));
                    }
                    if (!dados.logradouro.isEmpty())  fLogradouro.setText(dados.logradouro);
                    if (!dados.numero.isEmpty())      fNumero.setText(dados.numero);
                    if (!dados.complemento.isEmpty()) fComplemento.setText(dados.complemento);
                    if (!dados.bairro.isEmpty())      fBairro.setText(dados.bairro);
                    if (!dados.municipio.isEmpty())   fCidade.setText(dados.municipio);
                    if (!dados.uf.isEmpty())          cbEstado.setValue(dados.uf);
                    if (!dados.cep.isEmpty()) {
                        String cepNum = dados.cep.replaceAll("[^0-9]", "");
                        fCep.setText(ValidadorFiscal.aplicarMascaraCep(cepNum));
                    }
                    Alerta.info("CNPJ Consultado",
                        "Dados preenchidos automaticamente.\nSituação: " + dados.situacao +
                        "\nTipo: " + dados.tipo);
                } else {
                    lblCnpjStatus.setText("⚠ " + dados.situacao);
                    lblCnpjStatus.setTextFill(Color.ORANGE);
                    Alerta.aviso("Situação do CNPJ",
                        "CNPJ com situação: " + dados.situacao +
                        (dados.razaoSocial.isEmpty() ? "" : "\nEmpresa: " + dados.razaoSocial));
                }
            });
        }, "cnpj-lookup-empresa").start();
    }

    private void configurarCep() {
        fCep.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) buscarCep();
        });
        fCep.setOnAction(e -> buscarCep());
    }

    private void buscarCep() {
        String cepNums = ValidadorFiscal.apenasNumeros(fCep.getText());
        if (cepNums.length() != 8) return;
        new Thread(() -> {
            ConsultaViaCEP.Endereco end = ConsultaViaCEP.consultar(cepNums);
            Platform.runLater(() -> {
                if (end.encontrado) {
                    if (!end.logradouro.isEmpty()) fLogradouro.setText(end.logradouro);
                    if (!end.bairro.isEmpty())     fBairro.setText(end.bairro);
                    if (!end.localidade.isEmpty()) fCidade.setText(end.localidade);
                    if (!end.uf.isEmpty())         cbEstado.setValue(end.uf);
                }
            });
        }, "cep-lookup-empresa").start();
    }

    private void configurarIe() {
        fIe.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String ie = fIe.getText().trim();
                String uf = cbEstado.getValue() != null ? cbEstado.getValue() : "BA";
                if (ie.isBlank()) { ValidacaoFeedback.limpar(fIe); return; }
                if (ValidadorIE.validar(ie, uf)) {
                    ValidacaoFeedback.aplicarValido(fIe, "IE válida para " + uf);
                } else {
                    ValidacaoFeedback.aplicarInvalido(fIe, ValidadorIE.mensagemErro(ie, uf));
                }
            }
        });
    }

    private void carregarDados() {
        dao.carregar().ifPresent(e -> {
            fRazaoSocial.setText(n(e.getRazaoSocial()));
            fNomeFantasia.setText(n(e.getNomeFantasia()));
            fCnpj.setText(n(e.getCnpj()));
            fIe.setText(n(e.getIe()));
            fIm.setText(n(e.getIm()));
            cbRegime.setValue(e.getRegimeTributario());
            fEmail.setText(n(e.getEmail()));
            fTelefone.setText(n(e.getTelefone()));
            fCelular.setText(n(e.getCelular()));
            fSite.setText(n(e.getSite()));
            fCep.setText(n(e.getCep()));
            fLogradouro.setText(n(e.getLogradouro()));
            fNumero.setText(n(e.getNumero()));
            fComplemento.setText(n(e.getComplemento()));
            fBairro.setText(n(e.getBairro()));
            fCidade.setText(n(e.getCidade()));
            cbEstado.setValue(e.getEstado());
            fLogoPath.setText(n(e.getLogoPath()));
            taObs.setText(n(e.getObservacoes()));
        });
    }

    private void salvar() {
        if (fRazaoSocial.getText().isBlank()) {
            Alerta.aviso("Atenção", "Razão Social é obrigatória.");
            return;
        }
        String cnpj = fCnpj.getText();
        if (!cnpj.isBlank() && !ValidadorFiscal.validarCNPJ(cnpj)) {
            Alerta.aviso("Atenção", "CNPJ inválido. Verifique os dígitos.");
            return;
        }

        Empresa e = new Empresa();
        e.setRazaoSocial(fRazaoSocial.getText().trim());
        e.setNomeFantasia(fNomeFantasia.getText().trim());
        e.setCnpj(ValidadorFiscal.apenasNumeros(cnpj));
        e.setIe(fIe.getText().trim());
        e.setIm(fIm.getText().trim());
        e.setRegimeTributario(cbRegime.getValue());
        e.setEmail(fEmail.getText().trim());
        e.setTelefone(ValidadorFiscal.apenasNumeros(fTelefone.getText()));
        e.setCelular(ValidadorFiscal.apenasNumeros(fCelular.getText()));
        e.setSite(fSite.getText().trim());
        e.setCep(ValidadorFiscal.apenasNumeros(fCep.getText()));
        e.setLogradouro(fLogradouro.getText().trim());
        e.setNumero(fNumero.getText().trim());
        e.setComplemento(fComplemento.getText().trim());
        e.setBairro(fBairro.getText().trim());
        e.setCidade(fCidade.getText().trim());
        e.setEstado(cbEstado.getValue());
        e.setLogoPath(fLogoPath.getText().trim());
        e.setObservacoes(taObs.getText().trim());

        if (dao.salvar(e)) {
            Alerta.info("Sucesso", "Empresa salva com sucesso!");
        } else {
            Alerta.erro("Erro", "Erro ao salvar empresa. Verifique os dados.");
        }
    }

    private void limpar() {
        fRazaoSocial.clear(); fNomeFantasia.clear(); fCnpj.clear();
        fIe.clear(); fIm.clear(); cbRegime.setValue(null);
        fEmail.clear(); fTelefone.clear(); fCelular.clear(); fSite.clear();
        fCep.clear(); fLogradouro.clear(); fNumero.clear(); fComplemento.clear();
        fBairro.clear(); fCidade.clear(); cbEstado.setValue(null);
        fLogoPath.clear(); taObs.clear(); lblCnpjStatus.setText("");
    }

    private void escolherLogo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar logo da empresa");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
        File arquivo = fc.showOpenDialog(null);
        if (arquivo != null) fLogoPath.setText(arquivo.getAbsolutePath());
    }

    // ---------------------------------------------------------------
    // Helpers de estilo
    // ---------------------------------------------------------------

    private TitledPane secao(String titulo, Region conteudo) {
        TitledPane tp = new TitledPane(titulo, conteudo);
        tp.setExpanded(true);
        tp.setStyle("""
            -fx-background-color: #2a2d36;
            -fx-text-fill: #a0a8c0;
            -fx-font-weight: bold;
            -fx-border-color: #3a3d4a;
            -fx-border-radius: 6;
            """);
        return tp;
    }

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new Insets(12));
        g.setStyle("-fx-background-color: #2a2d36;");
        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(
                new ColumnConstraints(90), cc,
                new ColumnConstraints(90), cc
        );
        return g;
    }

    private Label label(String texto) {
        Label l = new Label(texto);
        l.setTextFill(Color.web("#a0a8c0"));
        l.setFont(Font.font(12));
        return l;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("""
            -fx-background-color: #1e2027;
            -fx-text-fill: white;
            -fx-prompt-text-fill: #6b7280;
            -fx-border-color: #3a3d4a;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 6 10;
            """);
        return tf;
    }

    private void estilizarCombo(ComboBox<?> cb) {
        cb.setStyle("""
            -fx-background-color: #1e2027;
            -fx-text-fill: white;
            -fx-border-color: #3a3d4a;
            -fx-border-radius: 4;
            """);
    }

    private void estilizarBotaoPrimario(Button btn) {
        btn.setStyle("""
            -fx-background-color: #6c63ff;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 10 20;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);
    }

    private void estilizarBotaoSecundario(Button btn) {
        btn.setStyle("""
            -fx-background-color: #3a3d4a;
            -fx-text-fill: #a0a8c0;
            -fx-padding: 8 16;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);
    }

    private String n(String s) { return s != null ? s : ""; }
}
