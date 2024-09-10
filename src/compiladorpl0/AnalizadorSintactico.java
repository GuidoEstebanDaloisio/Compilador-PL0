package compiladorpl0;

import static compiladorpl0.TokenType.*;
import java.io.IOException;

public class AnalizadorSintactico {

    private final AnalizadorLexico lex;
    private Token tokenActual;

    public AnalizadorSintactico(AnalizadorLexico lex) throws IOException {
        this.lex = lex;
        avanzar(); // Para obtener el primer token
    }

    private void avanzar() throws IOException {
        tokenActual = lex.escanear();
        System.out.println(tokenActual.getValor());
    }

    public void analizarPrograma() throws IOException {
        analizarBloque();
        if (tokenActual.getTipo() != TokenType.PUNTO) {
            throw new RuntimeException("Error sintactico: Se esperaba un '.' al final del programa");
        }
        System.out.println("--Programa valido--");
    }

    private void analizarBloque() throws IOException {
        // Verificar si el bloque comienza con una declaración de constantes
        if (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("const")) {
            analizarDeclaracionConstantes();
        }

        // Después de constantes, puede seguir una declaración de variables
        if (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("var")) {
            analizarDeclaracionVariables();
        }

        // Después de variables, puede seguir una declaración de procedimientos
        while (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("procedure")) {
            analizarDeclaracionProcedimientos();
        }

        // Finalmente, debe analizarse una proposición
        analizarProposicion();
    }

    private void analizarDeclaracionConstantes() throws IOException {
        avanzar(); // Saltar "const"
        analizarIdentificador(); // Debe seguir un identificador
        if (tokenActual.getTipo() != TokenType.COMPARAR) {
            System.out.println("Error sintactico: Se esperaba '=' en la declaracion de constante");
            System.exit(0);
        }
        avanzar(); // Saltar "="
        analizarNumero(); // Debe seguir un número
        while (tokenActual.getTipo() == TokenType.COMA) {
            avanzar(); // Saltar ","
            analizarIdentificador();
            if (tokenActual.getTipo() != TokenType.COMPARAR) {
                System.out.println("Error sintactico: Se esperaba '=' en la declaracion de constante");
                System.exit(0);
            }
            avanzar(); // Saltar "="
            analizarNumero();
        }
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println("Error sintactico: Se esperaba ';' al final de la declaracion de constantes");
            System.exit(0);
        }
        avanzar(); // Saltar ";"
    }

    private void analizarDeclaracionVariables() throws IOException {
        avanzar(); // Saltar "var"
        analizarIdentificador();
        while (tokenActual.getTipo() == TokenType.COMA) {
            avanzar(); // Saltar ","
            analizarIdentificador();
        }
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println("Error sintactico: Se esperaba ';' al final de la declaracion de variables");
            System.exit(0);
        }
        avanzar(); // Saltar ";"
    }

    private void analizarDeclaracionProcedimientos() throws IOException {
        avanzar(); // Saltar "procedure"
        analizarIdentificador();
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println("Error sintactico: Se esperaba ';' después de la declaracion del procedimiento");
            System.exit(0);
        }
        avanzar(); // Saltar ";"
        analizarBloque();
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println("Error sintactico: Se esperaba ';' al final de la declaracion del procedimiento");
            System.exit(0);

        }
        avanzar(); // Saltar ";"
    }

    private void analizarProposicion() throws IOException {
        switch (tokenActual.getTipo()) {
            case IDENTIFICADOR:
                avanzar();
                if (tokenActual.getTipo() == TokenType.ASIGNACION) {
                    avanzar(); // Saltar ":="
                    analizarExpresion();
                } else {
                    System.out.println("Error sintactico: Se esperaba ':=' en la proposicion");
                    System.exit(0);
                }
                break;
            case PALABRA_RESERVADA:
                switch (tokenActual.getValor()) {
                    case "call":
                        avanzar(); // Saltar "call"
                        analizarIdentificador();
                        break;
                    case "begin":
                        avanzar(); // Saltar "begin"
                        analizarProposicion(); // Análisis de la primera proposición
                        while (tokenActual.getTipo() == TokenType.PUNTO_Y_COMA) {
                            avanzar(); // Saltar ";"
                            analizarProposicion(); // Análisis de proposiciones adicionales
                        }
                        if (tokenActual.getTipo() != TokenType.PALABRA_RESERVADA || !tokenActual.getValor().equals("end")) {
                            System.out.println("Error sintactico: Se esperaba 'end' al final del bloque");
                            System.exit(0);
                        }
                        avanzar(); // Saltar "end"
                        break;
                    case "if":
                        avanzar(); // Saltar "if"
                        analizarCondicion();
                        if (tokenActual.getTipo() != TokenType.PALABRA_RESERVADA || !tokenActual.getValor().equals("then")) {
                            System.out.println("Error sintactico: Se esperaba 'then' después de la condicion");
                            System.exit(0);
                        }
                        avanzar(); // Saltar "then"
                        analizarProposicion();
                        break;
                    case "while":
                        avanzar(); // Saltar "while"
                        analizarCondicion();
                        if (tokenActual.getTipo() != TokenType.PALABRA_RESERVADA || !tokenActual.getValor().equals("do")) {
                            System.out.println("Error sintactico: Se esperaba 'do' después de la condicion");
                            System.exit(0);
                        }
                        avanzar(); // Saltar "do"
                        analizarProposicion();
                        break;

                    case "readln":
                        avanzar(); // Saltar "readln"
                        if (tokenActual.getTipo() == TokenType.PARENTESIS_IZQ) {
                            avanzar(); // Saltar "("
                            analizarIdentificador(); // Leer identificador
                            while (tokenActual.getTipo() == TokenType.COMA) {
                                avanzar(); // Saltar ","
                                analizarIdentificador(); // Leer identificador adicional
                            }
                            if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                                System.out.println("Error sintactico: Se esperaba ')'");
                                System.exit(0);
                            }
                            avanzar(); // Saltar ")"
                        } else {
                            System.out.println("Error sintactico: Se esperaba '(' después de 'readln'");
                            System.exit(0);
                        }
                        break;

                    case "writeln":
                        avanzar(); // Saltar "writeln"
                        if (tokenActual.getTipo() == TokenType.PARENTESIS_IZQ) {
                            avanzar(); // Saltar "("
                            analizarCadenaOExpresion(); // Leer cadena o expresión
                            while (tokenActual.getTipo() == TokenType.COMA) {
                                avanzar(); // Saltar ","
                                analizarCadenaOExpresion(); // Leer cadena o expresión adicional
                            }
                            if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                                System.out.println("Error sintactico: Se esperaba ')'");
                                System.exit(0);
                            }
                            avanzar(); // Saltar ")"
                        } else {
                            System.out.println("Error sintactico: Se esperaba '(' despues de 'writeln'");
                            System.exit(0);
                        }
                        break;

                    case "write":
                        avanzar(); // Saltar "write"
                        if (tokenActual.getTipo() == TokenType.PARENTESIS_IZQ) {
                            avanzar(); // Saltar "("
                            analizarCadenaOExpresion(); // Leer cadena o expresión
                            while (tokenActual.getTipo() == TokenType.COMA) {
                                avanzar(); // Saltar ","
                                analizarCadenaOExpresion(); // Leer cadena o expresión adicional
                            }
                            if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                                System.out.println("Error sintactico: Se esperaba ')'");
                                System.exit(0);
                            }
                            avanzar(); // Saltar ")"
                        } else {
                            System.out.println("Error sintactico: Se esperaba '(' despues de 'write'");
                            System.exit(0);
                        }
                        break;
                    default:
                        // Si el token no coincide con ningún caso, puede ser una proposición vacía
                        // En este caso, se considera una proposición válida si no hay más tokens
                        break;
                }
                break;
            default:
                // Si el token no coincide con ningún caso, puede ser una proposición vacía
                // En este caso, se considera una proposición válida si no hay más tokens
                break;
        }
    }

    private void analizarCadenaOExpresion() throws IOException {
        if (tokenActual.getTipo() == TokenType.CADENA) {
            avanzar(); // Saltar la cadena
        } else {
            analizarExpresion(); // Si no es cadena, debe ser una expresión
        }
    }

    private void analizarExpresion() throws IOException {
        // Manejar el primer signo opcional
        if (tokenActual.getTipo() == TokenType.SUMA || tokenActual.getTipo() == TokenType.RESTA) {
            avanzar(); // Saltar el primer "+" o "-"
        }

        // Analizar el término inicial
        analizarTermino();

        // Seguir analizando más términos conectados por "+" o "-"
        while (tokenActual.getTipo() == TokenType.SUMA || tokenActual.getTipo() == TokenType.RESTA) {
            avanzar(); // Saltar "+" o "-"
            analizarTermino(); // Analizar el siguiente término
        }
    }

    private void analizarTermino() throws IOException {
        analizarFactor();
        while (tokenActual.getTipo() == TokenType.MULTIPLICACION || tokenActual.getTipo() == TokenType.DIVISION) {
            avanzar(); // Saltar "*" o "/"
            analizarFactor();
        }
    }

    private void analizarFactor() throws IOException {
        switch (tokenActual.getTipo()) {
            case IDENTIFICADOR:
                analizarIdentificador();
                break;
            case NUMERO:
                analizarNumero();
                break;
            case PARENTESIS_IZQ:
                avanzar(); // Saltar "("
                analizarExpresion();
                if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                    System.out.println("Error sintactico: Se esperaba ')'");
                    System.exit(0);
                }
                avanzar(); // Saltar ")"
                break;
            default:
                System.out.println("Error sintactico: Factor esperado");
                System.exit(0);
        }
    }

    private void analizarCondicion() throws IOException {
        // Caso 1: "odd" -> expresion
        if (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("odd")) {
            avanzar(); // Saltar "odd"
            analizarExpresion(); // Analizar la expresión que sigue
        } else {
            // Caso 2: expresion -> = o <> o < o <= o > o >= -> expresion
            analizarExpresion(); // Analizar la primera expresión
            if (tokenActual.getTipo() == TokenType.COMPARAR
                    || tokenActual.getTipo() == TokenType.DISTINTO
                    || tokenActual.getTipo() == TokenType.MENOR
                    || tokenActual.getTipo() == TokenType.MENOR_O_IG
                    || tokenActual.getTipo() == TokenType.MAYOR
                    || tokenActual.getTipo() == TokenType.MAYOR_O_IG) {

                avanzar(); // Saltar el operador de comparación
                analizarExpresion(); // Analizar la segunda expresión
            } else {
                System.out.println("Error sintactico: Operador de comparación esperado en la condicion");
                System.exit(0);
            }
        }
    }

    private void analizarIdentificador() throws IOException {
        if (tokenActual.getTipo() != TokenType.IDENTIFICADOR) {
            System.out.println("Error sintactico: Se esperaba un identificador");
            System.exit(0);
        }
        avanzar(); // Saltar identificador
    }

    private void analizarNumero() throws IOException {
        if (tokenActual.getTipo() != TokenType.NUMERO) {
            System.out.println("Error sintactico: Se esperaba un número");
            System.exit(0);

        }
        avanzar(); // Saltar número
    }
}
