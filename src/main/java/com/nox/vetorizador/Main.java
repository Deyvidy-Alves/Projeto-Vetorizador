package com.nox.vetorizador;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import org.opencv.core.Core;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Inicializa o motor do OpenCV
        OpenCV.loadLocally();
        System.out.println("✅ OpenCV carregado com sucesso! Versão: " + Core.VERSION);

        // Cria uma tela básica em branco
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);

        // Configura e exibe a janela
        primaryStage.setTitle("Tattu Vector - Inicializado");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}