package com.nox.vetorizador.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class AppController {

    @FXML private ImageView visualizadorImagem;
    @FXML private Slider sliderContraste;
    @FXML private Button btnGerarSvg;

    private Mat imagemOriginal; // guardamos a original em memória
    private Mat imagemProcessada;

    @FXML
    public void initialize() {
        // toda vez que o slider mexer, ele chama o metodo de processamento
        sliderContraste.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (imagemOriginal != null) {
                atualizarPreview(newValue.doubleValue());
            }
        });
    }

    @FXML
    public void carregarImagem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione a arte da Tatuagem");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) visualizadorImagem.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // carrega a imagem em tons de cinza
            imagemOriginal = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
            imagemProcessada = new Mat();

            atualizarPreview(sliderContraste.getValue());
            btnGerarSvg.setDisable(false);
        }
    }

    private void atualizarPreview(double valorLimiar) {
        // binarização: o que for menor que o valor do slider vira preto, o que for maior vira branco
        Imgproc.threshold(imagemOriginal, imagemProcessada, valorLimiar, 255, Imgproc.THRESH_BINARY);

        // Converte de Mat para Image e joga na tela
        visualizadorImagem.setImage(matParaImage(imagemProcessada));
    }

    // Converte os formatos
    private Image matParaImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        byte[] buffer = new byte[width * height];
        mat.get(0, 0, buffer);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = buffer[y * width + x] & 0xFF;
                pw.setArgb(x, y, (0xFF << 24) | (pixel << 16) | (pixel << 8) | pixel);
            }
        }
        return image;
    }
}