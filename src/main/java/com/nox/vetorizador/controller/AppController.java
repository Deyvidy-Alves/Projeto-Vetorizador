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

    private Mat imagemOriginal;
    private Mat imagemProcessada;

    @FXML
    public void initialize() {
        // escuta o slider e atualiza o preview na hora
        sliderContraste.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (imagemOriginal != null) {
                atualizarPreview(newValue.doubleValue());
            }
        });
    }

    @FXML
    public void carregarImagem() {
        // abre o seletor pra buscar a imagem no PC
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Escolher imagem para vetorizar");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) visualizadorImagem.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // carrega a imagem em escala de cinza pra facilitar a binarização
            imagemOriginal = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
            imagemProcessada = new Mat();

            atualizarPreview(sliderContraste.getValue());
            btnGerarSvg.setDisable(false);
        }
    }

    private void atualizarPreview(double valorLimiar) {
        // aplica o threshold do OpenCV pra separar o que é linha do que é fundo
        Imgproc.threshold(imagemOriginal, imagemProcessada, valorLimiar, 255, Imgproc.THRESH_BINARY);

        // manda o resultado pro ImageView da tela
        visualizadorImagem.setImage(matParaImage(imagemProcessada));
    }

    private Image matParaImage(Mat mat) {
        // transforma o buffer do OpenCV em uma imagem que o JavaFX entende
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

        // pergunta onde quer salvar a porra do SVG final
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Salvar Vetor");
        saveChooser.setInitialFileName("resultado.svg");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vetor SVG", "*.svg"));

        Stage stage = (Stage) visualizadorImagem.getScene().getWindow();
        File fileDestino = saveChooser.showSaveDialog(stage);

        if (fileDestino != null) {
            try {
                // cria um BMP temporário porque o Potrace precisa de um arquivo de entrada
                File tempBmp = new File("temp_input.bmp");
                Imgcodecs.imwrite(tempBmp.getAbsolutePath(), imagemProcessada);

                // monta o comando e chama o executável do Potrace
                String command = "potrace temp_input.bmp -s -o \"" + fileDestino.getAbsolutePath() + "\"";

                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();

                if (process.exitValue() == 0) {
                    System.out.println("Vetor gerado: " + fileDestino.getAbsolutePath());
                }

                // deleta o arquivo temporário pra não deixar lixo na pasta
                tempBmp.delete();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}