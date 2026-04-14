package com.erp.view;

import com.erp.dao.NFeConfigDAO;
import com.erp.dao.EmpresaDAO;
import com.erp.model.NFeConfig;
import com.erp.model.Empresa;
import com.erp.service.NFeService;
import com.erp.service.ValidacaoNFeCompleta;
import com.erp.util.Alerta;
import com.erp.util.ConsultaViaCEP;
import com.erp.util.ValidadorFiscal;
import com.erp.util.ValidacaoFeedback;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Optional;

public class NFeConfiguracaoView {

    private final NFeConfigDAO dao = new NFeConfigDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();

    // Campos
    private TextField tfCertPath;
    private PasswordField pfCertSenha;
    private TextField tfCnpj;
    private TextField tfIe;
    private TextField tfRazao;
    private TextField tfFantasia;
    private TextField tfLogradouro;
    private TextField tfNumero;
    private TextField tfBairro;
    private TextField tfMunicipio;
    private TextField tfCodMunicipio;
    private TextField tfUf;
    private TextField tfCep;
    private TextField tfTelefone;
    private ComboBox<String> cbRegime;
    private TextField tfSerie;
    private TextField tfProximoNumero;
    private ComboBox<String> cbAmbiente;
    private Label lblStatus;

    public Region criar() {
        VBox root = new VBox(0);
        root.getStyleClass().add("conteudo-area");

        // Cabeçalho
        HBox header = new HBox();
        header.setPadding(new Insets(24, 28, 16, 28));
        header.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("⚙️  Configurações NF-e");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        titulo.setTextFill(Color.web("#e8eaf6"));
        header.getChildren().add(titulo);
        root.getChildren().add(header);

        // Formulário em ScrollPane
        ScrollPane scroll = new ScrollPane(criarFormulario());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        // Barra inferior com botões
        HBox barraAcoes = criarBarraAcoes();
        root.getChildren().add(barraAcoes);

        carregarDados();
        configurarValidacoes();
        return root;
    }

    private GridPane criarFormulario() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8, 28, 24, 28));
        grid.setHgap(16);
        grid.setVgap(12);

        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        ColumnConstraints c3 = new ColumnConstraints(160);
        ColumnConstraints c4 = new ColumnConstraints();
        c4.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);

        int row = 0;

        // === CERTIFICADO ===
        grid.add(secHeader("🔐 Certificado Digital A1"), 0, row++, 4, 1);

        tfCertPath = new TextField();
        tfCertPath.setPromptText("Caminho do arquivo .pfx");
        tfCertPath.setEditable(false);
        Button btnBrowse = new Button("📂 Selecionar");
        btnBrowse.setOnAction(e -> selecionarCertificado());
        HBox certBox = new HBox(8, tfCertPath, btnBrowse);
        HBox.setHgrow(tfCertPath, Priority.ALWAYS);
        grid.add(label("Certificado .pfx:"), 0, row);
        grid.add(certBox, 1, row++, 3, 1);

        pfCertSenha = new PasswordField();
        pfCertSenha.setPromptText("Senha do certificado");
        grid.add(label("Senha:"), 0, row);
        grid.add(pfCertSenha, 1, row++);

        // === DADOS FISCAIS ===
        grid.add(secHeader("🏢 Dados Fiscais"), 0, row++, 4, 1);

        tfCnpj = campo("00.000.000/0000-00");
        tfIe = campo("");
        grid.add(label("CNPJ:"), 0, row);
        grid.add(tfCnpj, 1, row);
        grid.add(label("Inscrição Estadual:"), 2, row);
        grid.add(tfIe, 3, row++);

        tfRazao = campo("Razão Social");
        grid.add(label("Razão Social:"), 0, row);
        grid.add(tfRazao, 1, row++, 3, 1);

        tfFantasia = campo("Nome Fantasia");
        grid.add(label("Nome Fantasia:"), 0, row);
        grid.add(tfFantasia, 1, row++, 3, 1);

        // === ENDEREÇO ===
        grid.add(secHeader("📍 Endereço"), 0, row++, 4, 1);

        tfLogradouro = campo("Rua, Av...");
        tfNumero = campo("Nº");
        grid.add(label("Logradouro:"), 0, row);
        grid.add(tfLogradouro, 1, row);
        grid.add(label("Número:"), 2, row);
        grid.add(tfNumero, 3, row++);

        tfBairro = campo("Bairro");
        tfMunicipio = campo("Município");
        grid.add(label("Bairro:"), 0, row);
        grid.add(tfBairro, 1, row);
        grid.add(label("Município:"), 2, row);
        grid.add(tfMunicipio, 3, row++);

        tfCodMunicipio = campo("2927408");
        tfUf = campo("BA");
        tfUf.setPrefWidth(60);
        grid.add(label("Cód. IBGE Município:"), 0, row);
        grid.add(tfCodMunicipio, 1, row);
        grid.add(label("UF:"), 2, row);
        grid.add(tfUf, 3, row++);

        tfCep = campo("00000-000");
        tfTelefone = campo("(00) 00000-0000");
        grid.add(label("CEP:"), 0, row);
        grid.add(tfCep, 1, row);
        grid.add(label("Telefone:"), 2, row);
        grid.add(tfTelefone, 3, row++);

        // === CONFIGURAÇÃO NF-e ===
        grid.add(secHeader("📄 Parâmetros NF-e"), 0, row++, 4, 1);

        cbRegime = new ComboBox<>();
        cbRegime.getItems().addAll(
                "1 - Simples Nacional",
                "2 - Simples Nacional - Excesso",
                "3 - Regime Normal"
        );
        cbRegime.getSelectionModel().selectFirst();
        cbRegime.setMaxWidth(Double.MAX_VALUE);
        grid.add(label("Regime Tributário:"), 0, row);
        grid.add(cbRegime, 1, row++, 3, 1);

        tfSerie = campo("1");
        tfProximoNumero = campo("1");
        grid.add(label("Série NF-e:"), 0, row);
        grid.add(tfSerie, 1, row);
        grid.add(label("Próximo Número:"), 2, row);
        grid.add(tfProximoNumero, 3, row++);

        cbAmbiente = new ComboBox<>();
        cbAmbiente.getItems().addAll("PRODUCAO", "HOMOLOGACAO");
        cbAmbiente.getSelectionModel().selectFirst();
        cbAmbiente.setMaxWidth(Double.MAX_VALUE);
        Label lblAviso = new Label("⚠️  Use HOMOLOGACAO para testes. PRODUCAO gera NF-e com valor fiscal real.");
        lblAviso.setTextFill(Color.web("#ffa726"));
        lblAviso.setFont(Font.font(11));
        grid.add(label("Ambiente:"), 0, row);
        grid.add(cbAmbiente, 1, row++);
        grid.add(lblAviso, 1, row++, 3, 1);

        // Status da última operação
        lblStatus = new Label();
        lblStatus.setWrapText(true);
        grid.add(lblStatus, 0, row++, 4, 1);

        return grid;
    }

    private HBox criarBarraAcoes() {
        Button btnValidar = new Button("🧪  Validar e Habilitar NF-e");
        Button btnSalvar  = new Button("💾  Salvar Configuração");

        btnValidar.setStyle("""
            -fx-background-color: #2e7d32;
            -fx-text-fill: white;
            -fx-padding: 10 18;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-font-weight: bold;
            """);
        btnSalvar.setStyle("""
            -fx-background-color: #6c63ff;
            -fx-text-fill: white;
            -fx-padding: 10 18;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-font-weight: bold;
            """);

        btnValidar.setOnAction(e -> validarEHabilitar());
        btnSalvar.setOnAction(e -> salvar());

        HBox barra = new HBox(12, btnValidar, btnSalvar);
        barra.setPadding(new Insets(14, 28, 20, 28));
        barra.setAlignment(Pos.CENTER_RIGHT);
        barra.setStyle("-fx-background-color: #1a1c24; -fx-border-color: #2a2d3a; -fx-border-width: 1 0 0 0;");
        return barra;
    }

    private void carregarDados() {
        // 1. Tenta carregar NFeConfig salvo
        Optional<NFeConfig> cfgOpt = dao.carregar();
        cfgOpt.ifPresent(cfg -> {
            tfCertPath.setText(nvl(cfg.getCertificadoPath()));
            pfCertSenha.setText(nvl(cfg.getCertificadoSenha()));
            tfCnpj.setText(nvl(cfg.getCnpj()));
            tfIe.setText(nvl(cfg.getIe()));
            tfRazao.setText(nvl(cfg.getRazaoSocial()));
            tfFantasia.setText(nvl(cfg.getNomeFantasia()));
            tfLogradouro.setText(nvl(cfg.getLogradouro()));
            tfNumero.setText(nvl(cfg.getNumeroEnd()));
            tfBairro.setText(nvl(cfg.getBairro()));
            tfMunicipio.setText(nvl(cfg.getMunicipio()));
            tfCodMunicipio.setText(nvl(cfg.getCodMunicipio(), "2927408"));
            tfUf.setText(nvl(cfg.getUf(), "BA"));
            tfCep.setText(nvl(cfg.getCep()));
            tfTelefone.setText(nvl(cfg.getTelefone()));
            cbRegime.getSelectionModel().select(cfg.getRegimeTributario() - 1);
            tfSerie.setText(String.valueOf(cfg.getSerie()));
            tfProximoNumero.setText(String.valueOf(cfg.getProximoNumero()));
            cbAmbiente.setValue(nvl(cfg.getAmbiente(), "PRODUCAO"));
        });

        // 2. Se dados fiscais estiverem vazios, preenche a partir do cadastro de Empresa
        if (tfCnpj.getText().isBlank() || tfRazao.getText().isBlank()) {
            empresaDAO.carregar().ifPresent(emp -> {
                if (tfCnpj.getText().isBlank() && emp.getCnpj() != null)
                    tfCnpj.setText(emp.getCnpj());
                if (tfIe.getText().isBlank() && emp.getIe() != null)
                    tfIe.setText(emp.getIe());
                if (tfRazao.getText().isBlank() && emp.getRazaoSocial() != null)
                    tfRazao.setText(emp.getRazaoSocial());
                if (tfFantasia.getText().isBlank() && emp.getNomeFantasia() != null)
                    tfFantasia.setText(emp.getNomeFantasia());
                if (tfLogradouro.getText().isBlank() && emp.getLogradouro() != null)
                    tfLogradouro.setText(emp.getLogradouro());
                if (tfNumero.getText().isBlank() && emp.getNumero() != null)
                    tfNumero.setText(emp.getNumero());
                if (tfBairro.getText().isBlank() && emp.getBairro() != null)
                    tfBairro.setText(emp.getBairro());
                if (tfMunicipio.getText().isBlank() && emp.getCidade() != null)
                    tfMunicipio.setText(emp.getCidade());
                if (tfUf.getText().isBlank() && emp.getEstado() != null)
                    tfUf.setText(emp.getEstado());
                if (tfCep.getText().isBlank() && emp.getCep() != null)
                    tfCep.setText(emp.getCep());
                if (tfTelefone.getText().isBlank() && emp.getTelefone() != null)
                    tfTelefone.setText(emp.getTelefone());
                // Regime
                if (cfgOpt.isEmpty() && emp.getRegimeTributario() != null) {
                    int idx = switch (emp.getRegimeTributario()) {
                        case "SIMPLES_NACIONAL", "MEI" -> 0;
                        case "LUCRO_PRESUMIDO"         -> 1;
                        default                        -> 2;
                    };
                    cbRegime.getSelectionModel().select(idx);
                }
            });
        }
    }

    private void configurarValidacoes() {
        // CNPJ mask + digit validation on focus lost
        tfCnpj.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String nums = ValidadorFiscal.apenasNumeros(tfCnpj.getText());
                if (nums.isEmpty()) { ValidacaoFeedback.limpar(tfCnpj); return; }
                if (ValidadorFiscal.validarCNPJ(nums)) {
                    ValidacaoFeedback.aplicarValido(tfCnpj, "CNPJ válido");
                } else {
                    ValidacaoFeedback.aplicarInvalido(tfCnpj, "CNPJ inválido — verifique os dígitos");
                }
            }
        });

        // CEP auto-fill via ViaCEP
        tfCep.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String cepNums = ValidadorFiscal.apenasNumeros(tfCep.getText());
                if (cepNums.length() == 8) {
                    new Thread(() -> {
                        ConsultaViaCEP.Endereco end = ConsultaViaCEP.consultar(cepNums);
                        Platform.runLater(() -> {
                            if (end.encontrado) {
                                if (!end.logradouro.isEmpty()) tfLogradouro.setText(end.logradouro);
                                if (!end.bairro.isEmpty())     tfBairro.setText(end.bairro);
                                if (!end.localidade.isEmpty()) tfMunicipio.setText(end.localidade);
                                if (!end.uf.isEmpty())         tfUf.setText(end.uf);
                                if (!end.ibge.isEmpty())       tfCodMunicipio.setText(end.ibge);
                            }
                        });
                    }, "cep-lookup-nfe").start();
                }
            }
        });
    }

    private void salvar() {
        NFeConfig cfg = construirCfg();
        String cnpjVal = cfg.getCnpj();
        if (!cnpjVal.isBlank() && !ValidadorFiscal.validarCNPJ(ValidadorFiscal.apenasNumeros(cnpjVal))) {
            Alerta.aviso("Atenção", "CNPJ inválido. Verifique os dígitos antes de salvar.");
            return;
        }
        if (dao.salvar(cfg)) {
            setStatus("✅ Configuração salva com sucesso!", "#4caf50");
            Alerta.info("Sucesso", "Configuração NF-e salva com sucesso.");
        } else {
            setStatus("❌ Erro ao salvar configuração.", "#f44336");
            Alerta.erro("Erro", "Não foi possível salvar a configuração NF-e.");
        }
    }

    /**
     * Executa toda a validação real (certificado, Receita Federal, SEFAZ) via
     * {@link ValidacaoNFeCompleta}, exibe resultado passo a passo e, se aprovado,
     * salva a configuração e habilita a emissão de NF-e.
     */
    private void validarEHabilitar() {
        // Campos obrigatórios mínimos antes de disparar a thread
        if (tfCertPath.getText().isBlank()) {
            Alerta.aviso("Atenção", "Selecione o certificado .pfx antes de validar.");
            return;
        }
        if (pfCertSenha.getText().isBlank()) {
            Alerta.aviso("Atenção", "Informe a senha do certificado.");
            return;
        }
        if (tfCnpj.getText().isBlank() || tfRazao.getText().isBlank()) {
            Alerta.aviso("Atenção", "Preencha CNPJ e Razão Social antes de validar.");
            return;
        }
        if (!ValidadorFiscal.validarCNPJ(ValidadorFiscal.apenasNumeros(tfCnpj.getText()))) {
            Alerta.aviso("Atenção", "CNPJ inválido. Verifique os dígitos.");
            return;
        }

        setStatus("🔄 Executando validações (certificado → Receita Federal → SEFAZ)...", "#90caf9");
        NFeConfig cfg = construirCfg();

        new Thread(() -> {
            ValidacaoNFeCompleta.Resultado resultado = new ValidacaoNFeCompleta().validar(cfg);
            Platform.runLater(() -> mostrarResultadoValidacao(resultado, cfg));
        }, "nfe-validacao-completa").start();
    }

    /**
     * Exibe um diálogo com o resultado detalhado de cada etapa de validação.
     * Se todas as etapas passaram (sem ERRO), pergunta se deseja habilitar e persiste.
     */
    private void mostrarResultadoValidacao(ValidacaoNFeCompleta.Resultado resultado, NFeConfig cfg) {
        // ── Monta o conteúdo do diálogo ──────────────────────────────────────
        VBox conteudo = new VBox(6);
        conteudo.setPadding(new Insets(4, 0, 8, 0));
        conteudo.setPrefWidth(540);

        for (ValidacaoNFeCompleta.ItemCheck item : resultado.itens) {
            String icone;
            String corFundo;
            String corTexto;
            switch (item.status) {
                case OK    -> { icone = "✅"; corFundo = "#1b3a1e"; corTexto = "#81c784"; }
                case AVISO -> { icone = "⚠️"; corFundo = "#3a2e0a"; corTexto = "#ffd54f"; }
                default    -> { icone = "❌"; corFundo = "#3a0d0d"; corTexto = "#ef9a9a"; }
            }

            VBox card = new VBox(3);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.setStyle("-fx-background-color: " + corFundo + "; -fx-background-radius: 6;");

            Label titulo = new Label(icone + "  " + item.titulo);
            titulo.setFont(Font.font("System", FontWeight.BOLD, 12));
            titulo.setTextFill(Color.web(corTexto));

            Label detalhe = new Label(item.detalhe);
            detalhe.setFont(Font.font(11));
            detalhe.setTextFill(Color.web("#c8ccd8"));
            detalhe.setWrapText(true);

            card.getChildren().addAll(titulo, detalhe);
            conteudo.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(380);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // ── Diálogo ──────────────────────────────────────────────────────────
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Resultado da Validação NF-e");
        dlg.setHeaderText(resultado.aprovado
            ? "✅  Todas as validações foram aprovadas"
            : "❌  Uma ou mais validações falharam");

        dlg.getDialogPane().setContent(scroll);
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1c24;");
        dlg.getDialogPane().lookup(".header-panel")
            .setStyle("-fx-background-color: #1a1c24;");

        // cabeçalho em branco
        Label header = (Label) dlg.getDialogPane().lookup(".header-panel .label");
        if (header != null) header.setTextFill(Color.web(resultado.aprovado ? "#81c784" : "#ef9a9a"));

        if (resultado.aprovado) {
            dlg.getDialogPane().getButtonTypes().addAll(
                new ButtonType("🚀  Habilitar NF-e", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
            );
        } else {
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            setStatus("❌ Validação falhou — corrija os erros e tente novamente.", "#f44336");
        }

        dlg.showAndWait().ifPresent(bt -> {
            if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                habilitarNFe(cfg);
            }
        });
    }

    /** Persiste a configuração e ativa habilita_nfe na empresa após aprovação. */
    private void habilitarNFe(NFeConfig cfg) {
        boolean cfgSalva = dao.salvar(cfg);
        try {
            Empresa emp = empresaDAO.carregar().orElse(new Empresa());
            emp.setHabilitaNFe(true);
            emp.setTipoEmissaoNFe(cfg.getAmbiente());
            empresaDAO.salvar(emp);
        } catch (Exception ex) {
            // não bloqueia se empresa ainda não existir
        }
        if (cfgSalva) {
            setStatus("✅ NF-e habilitada com sucesso!", "#4caf50");
            Alerta.info("NF-e Habilitada! 🎉",
                "Configuração salva e NF-e habilitada.\n\n" +
                "O menu \"Notas Fiscais\" ficará disponível após fazer logout e entrar novamente.");
        } else {
            setStatus("⚠ Validações aprovadas, mas erro ao salvar configuração.", "#ffa726");
            Alerta.aviso("Atenção", "As validações passaram mas houve erro ao salvar a configuração.\nTente salvar manualmente.");
        }
    }

    private void testarConexao() {
        NFeConfig cfg = construirCfg();
        if (cfg.getCertificadoPath().isBlank()) {
            Alerta.aviso("Atenção", "Selecione o certificado .pfx antes de testar.");
            return;
        }
        setStatus("🔄 Testando conexão com SEFAZ...", "#90caf9");
        NFeService service = new NFeService();
        new Thread(() -> {
            String resultado = service.testarConexao(cfg);
            Platform.runLater(() -> {
                boolean ok = resultado.startsWith("✅");
                setStatus(resultado, ok ? "#4caf50" : "#ffa726");
            });
        }, "nfe-teste-conexao").start();
    }

    /** Monta um NFeConfig a partir dos campos do formulário atual */
    private NFeConfig construirCfg() {
        NFeConfig cfg = new NFeConfig();
        cfg.setCertificadoPath(tfCertPath.getText().trim());
        cfg.setCertificadoSenha(pfCertSenha.getText());
        cfg.setCnpj(tfCnpj.getText().trim());
        cfg.setIe(tfIe.getText().trim());
        cfg.setRazaoSocial(tfRazao.getText().trim());
        cfg.setNomeFantasia(tfFantasia.getText().trim());
        cfg.setLogradouro(tfLogradouro.getText().trim());
        cfg.setNumeroEnd(tfNumero.getText().trim());
        cfg.setBairro(tfBairro.getText().trim());
        cfg.setMunicipio(tfMunicipio.getText().trim());
        cfg.setCodMunicipio(tfCodMunicipio.getText().trim());
        cfg.setUf(tfUf.getText().trim());
        cfg.setCep(tfCep.getText().trim());
        cfg.setTelefone(tfTelefone.getText().trim());
        cfg.setRegimeTributario(cbRegime.getSelectionModel().getSelectedIndex() + 1);
        try { cfg.setSerie(Integer.parseInt(tfSerie.getText().trim())); }
        catch (NumberFormatException e) { cfg.setSerie(1); }
        try { cfg.setProximoNumero(Integer.parseInt(tfProximoNumero.getText().trim())); }
        catch (NumberFormatException e) { cfg.setProximoNumero(1); }
        cfg.setAmbiente(cbAmbiente.getValue());
        return cfg;
    }

    private void selecionarCertificado() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Certificado A1");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Certificado Digital (*.pfx, *.p12)", "*.pfx", "*.p12"));
        File f = fc.showOpenDialog(tfCertPath.getScene() != null ? tfCertPath.getScene().getWindow() : null);
        if (f != null) tfCertPath.setText(f.getAbsolutePath());
    }

    private void setStatus(String msg, String color) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(Color.web(color));
    }

    // ========= Helpers =========
    private Label secHeader(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setTextFill(Color.web("#9c8cfc"));
        l.setPadding(new Insets(12, 0, 4, 0));
        return l;
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
        return tf;
    }

    private String nvl(String v) { return v != null ? v : ""; }
    private String nvl(String v, String def) { return (v == null || v.isBlank()) ? def : v; }
}
