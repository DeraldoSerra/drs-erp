package com.erp;

import com.erp.view.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DRS ERP");

        // Detecta resolução da tela e define tamanho mínimo proporcional
        javafx.geometry.Rectangle2D tela = javafx.stage.Screen.getPrimary().getVisualBounds();
        double minW = Math.max(900, tela.getWidth()  * 0.5);
        double minH = Math.max(600, tela.getHeight() * 0.5);
        primaryStage.setMinWidth(minW);
        primaryStage.setMinHeight(minH);

        // Ícone da janela
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/icon.ico");
            if (iconStream == null) iconStream = getClass().getResourceAsStream("/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception ignored) {}

        // DPI scaling hint para monitores de alta resolução
        System.setProperty("prism.allowhidpi", "true");

        LoginView loginView = new LoginView(primaryStage);
        primaryStage.setScene(loginView.criarScene());
        primaryStage.show();
        primaryStage.centerOnScreen();
    }

    @Override
    public void stop() {
        com.erp.config.DatabaseConfig.fechar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
