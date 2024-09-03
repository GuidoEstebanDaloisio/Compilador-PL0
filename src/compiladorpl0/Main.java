package compiladorpl0;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filePath = "BIEN-01.PL0"; // Ruta del archivo definida manualmente
        AnalizadorLexico alex = null;

        try {
            alex = new AnalizadorLexico(filePath);
            Token token;

            do {
                token = alex.escanear();
                System.out.println(token);
            } while (token.getTipo() != TokenType.EOF);

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