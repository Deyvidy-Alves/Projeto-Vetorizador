package com.nox.vetorizador;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // inicializa o motor do OpenCV
        OpenCV.loadLocally();
        System.out.println("OpenCV carregado com sucesso! Versão: " + Core.VERSION);

        // carrega o arquivo visual FXML
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/nox/vetorizador/tela.fxml")));

        Scene scene = new Scene(root, 800, 600);

        // Exibe o título na janela
        primaryStage.setTitle("Vetorizador");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}