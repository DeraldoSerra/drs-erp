package com.erp;

import com.erp.view.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("DRS ERP");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

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
