package com.erp.view;

import com.erp.dao.FinanceiroDAO;
import com.erp.dao.LojaDAO;
import com.erp.dao.ProdutoDAO;
import com.erp.dao.VendaDAO;
import com.erp.model.Produto;
import com.erp.util.Formatador;
import com.erp.util.Sessao;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;

public class DashboardView {

    private final VendaDAO vendaDAO    = new VendaDAO();
    private final ProdutoDAO prodDAO   = new ProdutoDAO();
    private final FinanceiroDAO finDAO = new FinanceiroDAO();
    private final LojaDAO lojaDAO      = new LojaDAO();

    private String getLojaNome() {
        try {
            return lojaDAO.buscarPorId(Sessao.getInstance().getLojaId())
                    .map(l -> l.getNome()).orElse("Loja Principal");
        } catch (Exception e) { return "Loja Principal"; }
    }

    public Region criar() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #1e2027; -fx-background: #1e2027;");

        VBox conteudo = new VBox(20);
        conteudo.setPadding(new Insets(24));
        conteudo.setStyle("-fx-background-color: #1e2027;");

        Label titulo    = new Label("📊 Dashboard — " + getLojaNome());
        titulo.getStyleClass().add("titulo-modulo");
        Label subtitulo = new Label("Resumo do dia e indicadores principais");
        subtitulo.getStyleClass().add("subtitulo-modulo");

        conteudo.getChildren().addAll(
                new VBox(4, titulo, subtitulo),
                criarKpiRow(),
                criarChartsRow(),
                criarTablesRow()
        );
        scroll.setContent(conteudo);
        return scroll;
    }

    // ===== ROW 1 — KPI CARDS =====

    private HBox criarKpiRow() {
        double totalHoje = 0, totalMes = 0;
        long qtdHoje = 0, clientesMes = 0;
        try {
            totalHoje   = vendaDAO.totalVendasHoje();
            qtdHoje     = vendaDAO.quantidadeVendasHoje();
            totalMes    = vendaDAO.totalVendasMes();
            clientesMes = vendaDAO.clientesAtendidosMes();
        } catch (Exception ignored) {}
        double ticket = qtdHoje > 0 ? totalHoje / qtdHoje : 0;

        HBox row = new HBox(12);
        row.getChildren().addAll(
            kpiCard("💰 Vendas Hoje",     Formatador.formatarMoeda(totalHoje),  "card-verde",   "Total do dia"),
            kpiCard("🛒 Pedidos Hoje",    String.valueOf(qtdHoje),               "card-azul",    "Vendas finalizadas"),
            kpiCard("🎯 Ticket Médio",    Formatador.formatarMoeda(ticket),     "card-roxo",    "Por venda"),
            kpiCard("📅 Vendas do Mês",   Formatador.formatarMoeda(totalMes),   "card-verde",   "Mês atual"),
            kpiCard("👥 Clientes Mês",    String.valueOf(clientesMes),           "card-azul",    "Atendidos")
        );
        for (var child : row.getChildren()) HBox.setHgrow(child, Priority.ALWAYS);
        return row;
    }

    private VBox kpiCard(String titulo, String valor, String estilo, String sub) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", estilo);
        card.setMinHeight(90);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        Label lTit = new Label(titulo);
        lTit.getStyleClass().add("card-titulo");
        Label lVal = new Label(valor);
        lVal.getStyleClass().add("card-valor");
        if (valor.length() > 10) lVal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lSub = new Label(sub);
        lSub.setStyle("-fx-text-fill: #5a5d6e; -fx-font-size: 11px;");
        card.getChildren().addAll(lTit, lVal, lSub);
        return card;
    }

    // ===== ROW 2 — CHARTS =====

    private GridPane criarChartsRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        ColumnConstraints cc60 = new ColumnConstraints(); cc60.setPercentWidth(60);
        ColumnConstraints cc40 = new ColumnConstraints(); cc40.setPercentWidth(40);
        grid.getColumnConstraints().addAll(cc60, cc40);

        grid.add(criarBarChart(), 0, 0);
        grid.add(criarPieChart(), 1, 0);
        return grid;
    }

    private VBox criarBarChart() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        Label title = new Label("📊 Vendas dos Últimos 7 Dias");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setStyle("-fx-tick-label-fill: #9e9e9e;");
        yAxis.setStyle("-fx-tick-label-fill: #9e9e9e;");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(Region.USE_COMPUTED_SIZE);
        chart.setMinHeight(220);
        chart.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setStyle("-fx-background-color: transparent;");
        chart.getStyleClass().add("chart-content");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        try {
            for (Object[] d : vendaDAO.vendasUltimos7Dias())
                series.getData().add(new XYChart.Data<>((String) d[0], (Double) d[1]));
        } catch (Exception ignored) {}
        chart.getData().add(series);

        box.getChildren().addAll(title, chart);
        return box;
    }

    private VBox criarPieChart() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        Label title = new Label("💳 Pagamentos do Mês");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setPrefHeight(Region.USE_COMPUTED_SIZE);
        chart.setMinHeight(220);
        chart.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setStyle("-fx-background-color: transparent;");
        chart.getStyleClass().add("chart-content");

        try {
            for (Object[] d : vendaDAO.vendasPorFormaMes()) {
                chart.getData().add(new PieChart.Data((String) d[0], ((Long) d[1]).doubleValue()));
            }
        } catch (Exception ignored) {}
        if (chart.getData().isEmpty())
            chart.getData().add(new PieChart.Data("Sem dados", 1));

        box.getChildren().addAll(title, chart);
        return box;
    }

    // ===== ROW 3 — TABLES =====

    private GridPane criarTablesRow() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        ColumnConstraints cc50a = new ColumnConstraints(); cc50a.setPercentWidth(50);
        ColumnConstraints cc50b = new ColumnConstraints(); cc50b.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc50a, cc50b);

        grid.add(criarTopProdutos(), 0, 0);
        grid.add(criarEstoqueBaixo(), 1, 0);
        return grid;
    }

    private VBox criarTopProdutos() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        Label title = new Label("🏆 Top 5 Produtos do Mês");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        List<Object[]> top;
        try { top = vendaDAO.topProdutosMes(); } catch (Exception e) { top = List.of(); }

        if (top.isEmpty()) {
            Label empty = new Label("Nenhuma venda registrada este mês.");
            empty.setStyle("-fx-text-fill: #5a5d6e;");
            box.getChildren().add(empty);
            return box;
        }

        String[] medals = {"🥇", "🥈", "🥉", "4°", "5°"};
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        ColumnConstraints cRank = new ColumnConstraints(36);
        ColumnConstraints cNome = new ColumnConstraints(); cNome.setHgrow(Priority.ALWAYS);
        ColumnConstraints cQtd  = new ColumnConstraints(80);
        grid.getColumnConstraints().addAll(cRank, cNome, cQtd);

        for (int i = 0; i < top.size(); i++) {
            String rankStyle = switch (i) {
                case 0 -> "-fx-font-size: 18px;";
                case 1 -> "-fx-font-size: 18px;";
                case 2 -> "-fx-font-size: 18px;";
                default -> "-fx-text-fill: #9e9e9e; -fx-font-size: 14px;";
            };
            Label rank = new Label(medals[i]);
            rank.setStyle(rankStyle);
            Label nome = new Label((String) top.get(i)[0]);
            nome.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;");
            Label qtd = new Label(Formatador.formatarQuantidade((Double) top.get(i)[1]));
            qtd.setStyle("-fx-text-fill: #4dabf7; -fx-font-weight: bold;");
            grid.add(rank, 0, i);
            grid.add(nome, 1, i);
            grid.add(qtd,  2, i);
        }
        box.getChildren().add(grid);
        return box;
    }

    private VBox criarEstoqueBaixo() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        Label title = new Label("⚠ Produtos com Estoque Baixo");
        title.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        List<Produto> baixo;
        try { baixo = prodDAO.listarEstoqueBaixo(); }
        catch (Exception e) {
            Label err = new Label("⚠ Verifique a conexão com o banco.");
            err.setStyle("-fx-text-fill: #fa5252;");
            box.getChildren().add(err);
            return box;
        }

        if (baixo.isEmpty()) {
            Label ok = new Label("✅ Todos os produtos estão com estoque adequado.");
            ok.setStyle("-fx-text-fill: #40c057;");
            box.getChildren().add(ok);
            return box;
        }

        GridPane tbl = new GridPane();
        tbl.setHgap(14); tbl.setVgap(6);
        String[] hdrs = {"Nome", "Atual", "Mínimo"};
        for (int i = 0; i < hdrs.length; i++) {
            Label h = new Label(hdrs[i]);
            h.setStyle("-fx-text-fill: #5a5d6e; -fx-font-weight: bold; -fx-font-size: 11px;");
            tbl.add(h, i, 0);
        }
        int row = 1;
        for (Produto p : baixo.subList(0, Math.min(8, baixo.size()))) {
            tbl.add(lbl(p.getNome()), 0, row);
            Label ea = lbl(Formatador.formatarQuantidade(p.getEstoqueAtual()));
            ea.setStyle("-fx-text-fill: #fa5252; -fx-font-weight: bold;");
            tbl.add(ea, 1, row);
            tbl.add(lbl(Formatador.formatarQuantidade(p.getEstoqueMinimo())), 2, row++);
        }
        ColumnConstraints cn = new ColumnConstraints(); cn.setHgrow(Priority.ALWAYS);
        tbl.getColumnConstraints().addAll(cn, new ColumnConstraints(70), new ColumnConstraints(70));
        box.getChildren().add(tbl);
        return box;
    }

    private Label lbl(String t) {
        Label l = new Label(t != null ? t : "");
        l.setStyle("-fx-text-fill: #e0e0e0;");
        return l;
    }
}
