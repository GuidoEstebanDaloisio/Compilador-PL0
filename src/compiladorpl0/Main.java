package compiladorpl0;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filePath = "MAL-03.pl0"; // Ruta del archivo definida manualmente
        AnalizadorLexico alex = null;
        AnalizadorSintactico parser = null;

        try {
            alex = new AnalizadorLexico(filePath);
            parser = new AnalizadorSintactico(alex);
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
