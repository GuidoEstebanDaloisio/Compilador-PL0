package compiladorpl0;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        String filePath = "MAL-06.pl0"; // Ruta del archivo definida manualmente
        AnalizadorLexico alex = new AnalizadorLexico(filePath);
        AnalizadorSemantico semantico = new AnalizadorSemantico();
        GeneradorDeCodigo genCod = new GeneradorDeCodigo();

        try {

            AnalizadorSintactico parser = new AnalizadorSintactico(alex, semantico, genCod);

            parser.analizarPrograma(); // Inicia el análisis sintáctico
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        } finally {
            if (alex != null) {
                try {
                    alex.cerrar();
                } catch (IOException e) {
                    System.err.println("Error al cerrar el archivo: " + e.getMessage());
                }
            }
        }
    }
}
