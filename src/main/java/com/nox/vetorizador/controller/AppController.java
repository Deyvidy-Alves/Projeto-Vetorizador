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
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
            // carrega a imagem em escala de cinza
            imagemOriginal = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);

            // dobra o tamanho da imagem pra dar mais margem pro potrace achar os traços finos
            org.opencv.core.Size novoTamanho = new org.opencv.core.Size(imagemOriginal.cols() * 2, imagemOriginal.rows() * 2);
            Imgproc.resize(imagemOriginal, imagemOriginal, novoTamanho, 0, 0, Imgproc.INTER_CUBIC);

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

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Salvar Vetor");
        saveChooser.setInitialFileName("resultado.svg");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vetor SVG", "*.svg"));

        Stage stage = (Stage) visualizadorImagem.getScene().getWindow();
        File fileDestino = saveChooser.showSaveDialog(stage);

        if (fileDestino != null) {
            try {
                // garante que o potrace.exe ta na raiz
                File potraceExe = new File("potrace.exe");
                if (!potraceExe.exists()) {
                    mostrarAlerta("Erro", "O potrace.exe não foi encontrado na raiz do projeto.", Alert.AlertType.ERROR);
                    return;
                }

                // cria os arquivos temporarios na raiz do projeto pra evitar bug de pasta do windows
                File tempBmp = new File("temp_input.bmp");
                File tempSvg = new File("temp_output.svg");

                boolean salvouBmp = Imgcodecs.imwrite(tempBmp.getAbsolutePath(), imagemProcessada);
                if (!salvouBmp) {
                    mostrarAlerta("Erro no OpenCV", "Falha ao criar a imagem temporária (temp_input.bmp) para o Potrace ler.", Alert.AlertType.ERROR);
                    return;
                }

                // passa as flags pro potrace desenhar com mais fidelidade e salvar localmente
                ProcessBuilder pb = new ProcessBuilder(
                        potraceExe.getAbsolutePath(),
                        tempBmp.getAbsolutePath(),
                        "-s",
                        "-t", "2",
                        "-a", "1.0",
                        "-o",
                        tempSvg.getAbsolutePath()
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                StringBuilder logPotrace = new java.lang.StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    logPotrace.append(line).append("\n");
                }

                process.waitFor();

                // se o svg temporario foi gerado, move ele pro destino final que o usuario escolheu
                if (process.exitValue() == 0 && tempSvg.exists()) {
                    Files.move(
                            tempSvg.toPath(),
                            fileDestino.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    mostrarAlerta("Sucesso", "Vetor gerado com sucesso em:\n" + fileDestino.getAbsolutePath(), Alert.AlertType.INFORMATION);
                } else {
                    mostrarAlerta("Falha Silenciosa", "O Potrace rodou, mas falhou em gerar o arquivo.\n\nLog Interno:\n" + logPotrace.toString(), Alert.AlertType.ERROR);
                }

                // limpa os arquivos temporarios da raiz
                if (tempBmp.exists()) tempBmp.delete();
                if (tempSvg.exists()) tempSvg.delete();

            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlerta("Erro Crítico", "Falha ao invocar o executável do Potrace.\n" + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }

}