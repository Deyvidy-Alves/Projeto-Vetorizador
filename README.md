# Vetorizador Nox

Ferramenta desktop para conversão de imagens rasterizadas (PNG, JPG) em vetores escaláveis (SVG) de alta fidelidade. O sistema utiliza OpenCV para processamento prévio da imagem (binarização, ajuste de contraste e upscaling) e o motor do Potrace para a geração das curvas matemáticas.

## Arquitetura

O projeto separa a responsabilidade visual e de processamento:
* **Interface Gráfica:** JavaFX com layout definido em FXML.
* **Visão Computacional:** OpenCV (`nu.pattern.OpenCV`) para leitura em tons de cinza, redimensionamento bicúbico para preservação de traços finos e aplicação de threshold dinâmico.
* **Vetorização:** Integração via `ProcessBuilder` com o binário escrito em C do Potrace, passando parâmetros de alta precisão (`-t 2`, `-a 1.0`) para manter detalhes sutis como respingos e linhas finas.

## Requisitos do Sistema

* Java Development Kit (JDK) 21
* Apache Maven
* Binário do Potrace (`potrace.exe` solto na raiz do projeto)

## Execução em Ambiente de Desenvolvimento

Para testar a aplicação diretamente pelo código-fonte, execute a classe `Launcher.java` pela sua IDE. O uso do Launcher é necessário para carregar corretamente os módulos do JavaFX na memória antes de instanciar a classe `Main`.

## Build e Geração de Executável (.exe)

O projeto está configurado com o `maven-shade-plugin` para empacotar todas as dependências, incluindo as bibliotecas nativas do OpenCV, em um Fat JAR único. Em seguida, utilizamos o `jpackage` para criar um executável independente do Windows.

Gere o artefato principal compilado:
```bash
mvn clean package
```
Gere a imagem do aplicativo com o runtime do Java embutido:
```bash
jpackage --type app-image --name VetorizadorNox --input target/ --main-jar VetorizadorImagens-1.0-SNAPSHOT.jar --main-class com.nox.vetorizador.Launcher --win-console
```
Após o término do comando, uma pasta VetorizadorNox será criada. Mova uma cópia do arquivo potrace.exe para dentro dessa pasta, colocando-o no mesmo diretório do arquivo .exe gerado. A pasta inteira agora pode ser distribuída e rodará nativamente no Windows sem a necessidade de instalar o Java.