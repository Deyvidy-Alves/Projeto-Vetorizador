package com.nox.vetorizador.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class AppController {

    @FXML private ImageView visualizadorImagem;
    @FXML private Slider sliderContraste;
    @FXML private Slider sliderDesfoque;
    @FXML private Slider sliderManchas;
    @FXML private Slider sliderCurvas;
    @FXML private Button btnGerarSvg;

    private Mat imagemOriginal;
    private Mat imagemProcessada;

    @FXML
    public void initialize() {
        // Atualiza a tela se o slider de contraste mudar
        sliderContraste.valueProperty().addListener((obs, oldV, newV) -> atualizarPreview());
        // Atualiza a tela se o slider de desfoque mudar
        sliderDesfoque.valueProperty().addListener((obs, oldV, newV) -> atualizarPreview());
    }

    @FXML
    public void carregarImagem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Escolher imagem para vetorizar");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) visualizadorImagem.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            imagemOriginal = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);

            Size novoTamanho = new Size(imagemOriginal.cols() * 2, imagemOriginal.rows() * 2);
            Imgproc.resize(imagemOriginal, imagemOriginal, novoTamanho, 0, 0, Imgproc.INTER_CUBIC);

            imagemProcessada = new Mat();
            atualizarPreview();
            btnGerarSvg.setDisable(false);
        }
    }

    private void atualizarPreview() {
        if (imagemOriginal == null) return;

        Mat imgTemp = new Mat();

        int blurSize = (int) sliderDesfoque.getValue();
        if (blurSize % 2 == 0) blurSize++; // O OpenCV exige que o valor do Blur seja ímpar (1, 3, 5, 7...)

        if (blurSize > 1) {
            Imgproc.GaussianBlur(imagemOriginal, imgTemp, new Size(blurSize, blurSize), 0);
        } else {
            imagemOriginal.copyTo(imgTemp);
        }

        Imgproc.threshold(imgTemp, imagemProcessada, sliderContraste.getValue(), 255, Imgproc.THRESH_BINARY);

        visualizadorImagem.setImage(matParaImage(imagemProcessada));
    }

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

    @FXML
    public void gerarSVG() {
        if (imagemProcessada == null) return;

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Salvar Vetor");
        saveChooser.setInitialFileName("resultado.svg");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vetor SVG", "*.svg"));

        Stage stage = (Stage) visualizadorImagem.getScene().getWindow();
        File fileDestino = saveChooser.showSaveDialog(stage);

        if (fileDestino != null) {
            try {
                File potraceExe = new File("potrace.exe");
                if (!potraceExe.exists()) {
                    mostrarAlerta("Erro", "O potrace.exe não foi encontrado na raiz do projeto.", Alert.AlertType.ERROR);
                    return;
                }

                File tempBmp = new File("temp_input.bmp");
                File tempSvg = new File("temp_output.svg");

                Imgcodecs.imwrite(tempBmp.getAbsolutePath(), imagemProcessada);

                String alphaStr = String.format(Locale.US, "%.2f", sliderCurvas.getValue());
                String turdStr = String.valueOf((int) sliderManchas.getValue());

                ProcessBuilder pb = new ProcessBuilder(
                        potraceExe.getAbsolutePath(),
                        tempBmp.getAbsolutePath(),
                        "-s",
                        "-t", turdStr,   // Usa o valor do slider de Manchas
                        "-a", alphaStr,  // Usa o valor do slider de Curvas
                        "-o", tempSvg.getAbsolutePath()
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();

                if (process.exitValue() == 0 && tempSvg.exists()) {
                    Files.move(tempSvg.toPath(), fileDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    mostrarAlerta("Sucesso", "Vetor gerado com sucesso!", Alert.AlertType.INFORMATION);
                } else {
                    mostrarAlerta("Falha", "Erro ao processar o vetor.", Alert.AlertType.ERROR);
                }

                if (tempBmp.exists()) tempBmp.delete();
                if (tempSvg.exists()) tempSvg.delete();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}