package com.erp.view;

import com.erp.dao.NFeConfigDAO;
import com.erp.dao.NotaFiscalDAO;
import com.erp.model.NFeConfig;
import com.erp.model.NotaFiscal;
import com.erp.model.Venda;
import com.erp.service.NFeResultado;
import com.erp.service.NFeService;
import com.erp.util.Alerta;
import com.erp.util.NotaFiscalUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class NotaFiscalView {

    private final Venda venda;

    public NotaFiscalView(Venda venda) {
        this.venda = venda;
    }

    public void mostrar() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Cupom Fiscal - Venda Nº " + venda.getNumero());
        stage.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1e2027;");
        root.setPrefWidth(360);

        // Ícone de sucesso
        Label icone = new Label("✅");
        icone.setFont(Font.font(48));

        Label titulo = new Label("Venda Finalizada!");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 18));
        titulo.setTextFill(Color.WHITE);

        // Dados resumidos
        VBox resumo = new VBox(6);
        resumo.setAlignment(Pos.CENTER_LEFT);
        resumo.setPadding(new Insets(12));
        resumo.setStyle("-fx-background-color: #2a2d36; -fx-background-radius: 8;");
        resumo.setMaxWidth(Double.MAX_VALUE);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        resumo.getChildren().addAll(
                linhaResumo("Nº da Venda:",    venda.getNumero()),
                linhaResumo("Data/Hora:",       venda.getDataVenda() != null ? fmt.format(venda.getDataVenda()) : "-"),
                linhaResumo("Cliente:",         venda.getClienteNome() != null ? venda.getClienteNome() : "Consumidor Final"),
                linhaResumo("Pagamento:",       venda.getFormaPagamento()),
                linhaResumo("Total:",           "R$ " + String.format("%.2f", venda.getTotal()).replace(".", ",")),
                venda.getTroco() > 0 ? linhaResumo("Troco:", "R$ " + String.format("%.2f", venda.getTroco()).replace(".", ",")) : new Label()
        );

        // Botões
        Button btnNFe      = new Button("📤  Emitir NF-e SEFAZ");
        Button btnImprimir = new Button("🖨  Imprimir Cupom (PDF)");
        Button btnSalvar   = new Button("💾  Salvar PDF");
        Button btnFechar   = new Button("✖  Fechar");

        estilizarBotaoNFe(btnNFe);
        estilizarBotaoPrimario(btnImprimir);
        estilizarBotaoSecundario(btnSalvar);
        estilizarBotaoPerigo(btnFechar);

        btnNFe.setMaxWidth(Double.MAX_VALUE);
        btnImprimir.setMaxWidth(Double.MAX_VALUE);
        btnSalvar.setMaxWidth(Double.MAX_VALUE);
        btnFechar.setMaxWidth(Double.MAX_VALUE);

        btnNFe.setOnAction(e -> emitirNFe(stage));
        btnImprimir.setOnAction(e -> imprimirCupom(stage));
        btnSalvar.setOnAction(e -> salvarCupom());
        btnFechar.setOnAction(e -> stage.close());

        VBox botoes = new VBox(8, btnNFe, btnImprimir, btnSalvar, btnFechar);

        root.getChildren().addAll(icone, titulo, resumo, botoes);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void emitirNFe(Stage parentStage) {
        // Verifica configuração
        NFeConfigDAO configDAO = new NFeConfigDAO();
        Optional<NFeConfig> optCfg = configDAO.carregar();
        if (optCfg.isEmpty() || !optCfg.get().isConfigurado()) {
            Alerta.aviso("Configuração Incompleta",
                    "Configuração NF-e não encontrada ou incompleta.\n" +
                    "Acesse CONFIGURAÇÕES → Config. NF-e e preencha os dados antes de emitir.");
            return;
        }

        // Verifica se já tem NF-e emitida para esta venda
        NotaFiscalDAO nfDAO = new NotaFiscalDAO();
        nfDAO.buscarPorVendaId(venda.getId()).ifPresent(nfExistente -> {
            if ("AUTORIZADA".equals(nfExistente.getStatus())) {
                Alerta.info("NF-e já emitida",
                        "Esta venda já possui uma NF-e autorizada.\nChave: " + nfExistente.getChaveAcesso());
            }
        });

        // Diálogo de progresso
        Stage progDlg = new Stage();
        progDlg.initModality(Modality.APPLICATION_MODAL);
        progDlg.initOwner(parentStage);
        progDlg.setTitle("Emitindo NF-e...");
        ProgressIndicator pi = new ProgressIndicator();
        Label lblProg = new Label("Enviando para o SEFAZ BA...");
        lblProg.setTextFill(Color.web("#e8eaf6"));
        VBox vb = new VBox(16, pi, lblProg);
        vb.setAlignment(Pos.CENTER);
        vb.setPadding(new Insets(32));
        vb.setStyle("-fx-background-color: #1e2027;");
        progDlg.setScene(new Scene(vb, 280, 160));
        progDlg.show();

        NFeService service = new NFeService();
        new Thread(() -> {
            NFeResultado resultado = service.emitir(venda);
            Platform.runLater(() -> {
                progDlg.close();
                if (resultado.isAutorizada()) {
                    Alerta.info("NF-e Autorizada! ✅",
                            "NF-e emitida com sucesso!\n\n" +
                            "Chave: " + resultado.getChaveAcesso() + "\n" +
                            "Protocolo: " + resultado.getProtocolo());
                } else {
                    Alerta.erro("Erro na emissão da NF-e", resultado.getMensagem());
                }
            });
        }, "nfe-emissao").start();
    }

    private void imprimirCupom(Stage stage) {
        String caminho = NotaFiscalUtil.gerar(venda);
        if (caminho == null) {
                Alerta.erro("Erro", "Erro ao gerar o cupom fiscal.");
                return;
            }
            try {
                File arquivo = new File(caminho);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().print(arquivo);
                } else {
                    Desktop.getDesktop().open(arquivo);
                }
            } catch (Exception ex) {
                try {
                    Desktop.getDesktop().open(new File(caminho));
                } catch (Exception ex2) {
                    Alerta.aviso("Aviso", "Não foi possível abrir o PDF: " + caminho);
                }
            }
    }

    private void salvarCupom() {
        String caminho = NotaFiscalUtil.gerar(venda);
        if (caminho == null) {
            Alerta.erro("Erro", "Erro ao gerar o cupom fiscal.");
            return;
        }
        try {
            Desktop.getDesktop().open(new File(caminho));
        } catch (Exception ex) { /* ignora */ }
        Alerta.info("Cupom gerado", "Cupom salvo em:\n" + caminho);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private HBox linhaResumo(String chave, String valor) {
        Label lChave = new Label(chave);
        lChave.setTextFill(Color.web("#a0a8c0"));
        lChave.setFont(Font.font(12));
        lChave.setMinWidth(110);

        Label lValor = new Label(valor != null ? valor : "-");
        lValor.setTextFill(Color.WHITE);
        lValor.setFont(Font.font("System", FontWeight.BOLD, 12));
        lValor.setWrapText(true);

        HBox linha = new HBox(8, lChave, lValor);
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    private void estilizarBotaoNFe(Button btn) {
        btn.setStyle("""
            -fx-background-color: #2e7d32;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 11;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);
    }

    private void estilizarBotaoPrimario(Button btn) {
        btn.setStyle("""
            -fx-background-color: #6c63ff;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 10;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);
    }

    private void estilizarBotaoSecundario(Button btn) {
        btn.setStyle("""
            -fx-background-color: #3a3d4a;
            -fx-text-fill: #a0a8c0;
            -fx-padding: 9;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);
    }

    private void estilizarBotaoPerigo(Button btn) {
        btn.setStyle("""
            -fx-background-color: #2a2d36;
            -fx-text-fill: #ff6b6b;
            -fx-padding: 9;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            """);
    }
}
