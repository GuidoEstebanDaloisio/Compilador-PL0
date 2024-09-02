package compiladorpl0;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AnalizadorLexico {
    private final BufferedReader reader;
    private int currentChar;
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
        "const", "var", "procedure", "call", "begin", "end", "if", "then", "while", "do", "odd", "readln", "writeln", "write"
    ));
    private boolean eofReached = false;

    public AnalizadorLexico(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        avanzar();
    }

    private void avanzar() throws IOException {
        currentChar = reader.read();
        if (currentChar == -1) {
            eofReached = true;
        }
    }

    public Token escanear() throws IOException {
        while (Character.isWhitespace(currentChar)) {
            avanzar();
        }

        if (eofReached) {
            return new Token(TokenType.EOF, "EOF");
        }

        if (Character.isLetter((char) currentChar)) {
            StringBuilder identificador = new StringBuilder();
            while (Character.isLetterOrDigit((char) currentChar)) {
                identificador.append((char) currentChar);
                avanzar();
            }
            String lexema = identificador.toString();
            if (PALABRAS_RESERVADAS.contains(lexema)) {
                return new Token(TokenType.PALABRA_RESERVADA, lexema);
            } else {
                return new Token(TokenType.IDENTIFICADOR, lexema);
            }
        }

        if (Character.isDigit((char) currentChar)) {
            StringBuilder numero = new StringBuilder();
            while (Character.isDigit((char) currentChar)) {
                numero.append((char) currentChar);
                avanzar();
            }
            return new Token(TokenType.NUMERO, numero.toString());
        }

if (currentChar == '\'') {
    StringBuilder cadena = new StringBuilder();
    avanzar();
    while (currentChar != '\'' && currentChar != -1 && currentChar != '\n') {
        cadena.append((char) currentChar);
        avanzar();
    }
    if (currentChar == '\'') {
        avanzar();
        return new Token(TokenType.CADENA, cadena.toString());
    } else {
        cadena.append((char) currentChar); // Añadir el salto de línea si es necesario
        avanzar();
        while (currentChar != -1 && currentChar != '\n') { // Leer hasta el final de la línea
            cadena.append((char) currentChar);
            avanzar();
        }
        return new Token(TokenType.NUL, cadena.toString());
    }
}

        switch (currentChar) {
            case ':':
                avanzar();
                if (currentChar == '=') {
                    avanzar();
                    return new Token(TokenType.ASIGNACION, ":=");
                } else {
                    return new Token(TokenType.NUL, ":");
                }
            case '<':
                avanzar();
                if (currentChar == '=') {
                    avanzar();
                    return new Token(TokenType.MENOR_O_IG, "<=");
                } else if (currentChar == '>') {
                    avanzar();
                    return new Token(TokenType.DISTINTO, "<>");
                } else {
                    return new Token(TokenType.MENOR, "<");
                }
            case '>':
                avanzar();
                if (currentChar == '=') {
                    avanzar();
                    return new Token(TokenType.MAYOR_O_IG, ">=");
                } else {
                    return new Token(TokenType.MAYOR, ">");
                }
            case '=':
                avanzar();
                return new Token(TokenType.COMPARAR, "=");
            case '+':
                avanzar();
                return new Token(TokenType.SUMA, "+");
            case '-':
                avanzar();
                return new Token(TokenType.RESTA, "-");
            case '*':
                avanzar();
                return new Token(TokenType.MULTIPLICACION, "*");
            case '/':
                avanzar();
                return new Token(TokenType.DIVISION, "/");
            case '(':
                avanzar();
                return new Token(TokenType.PARENTESIS_IZQ, "(");
            case ')':
                avanzar();
                return new Token(TokenType.PARENTESIS_DER, ")");
            case ';':
                avanzar();
                return new Token(TokenType.PUNTO_Y_COMA, ";");
            case ',':
                avanzar();
                return new Token(TokenType.COMA, ",");
            case '.':
                avanzar();
                if (eofReached) {
                    return new Token(TokenType.PUNTO, ".");
                } else {
                    return new Token(TokenType.NUL, ".");
                }
            default:
                throw new RuntimeException("Carácter inesperado: " + (char) currentChar);
        }
    }

    public void cerrar() throws IOException {
        reader.close();
    }
}
