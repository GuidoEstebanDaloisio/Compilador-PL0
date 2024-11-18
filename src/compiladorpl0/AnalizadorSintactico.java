package compiladorpl0;

import static compiladorpl0.Errores.*;
import static compiladorpl0.IdentType.*;
import static compiladorpl0.TokenType.*;
import java.io.IOException;

public class AnalizadorSintactico {

    private final AnalizadorLexico lex;
    private final AnalizadorSemantico semantico;
    private final GeneradorDeCodigo genCod;
    private Token tokenActual;
    private int cantVariablesDeclaradas = 0;

    public AnalizadorSintactico(AnalizadorLexico lex, AnalizadorSemantico semantico, GeneradorDeCodigo genCod) throws IOException {
        this.lex = lex;
        this.semantico = semantico;
        this.genCod = genCod;
        avanzar(); // Para obtener el primer token

    }

    private void avanzar() throws IOException {
        tokenActual = lex.escanear();
        //System.out.println(tokenActual.getValor());
    }

    public void analizarPrograma(String nombreArchivo) throws IOException {
        genCod.cargarEDI();

        analizarBloque(0);
        if (tokenActual.getTipo() != TokenType.PUNTO) {
            System.out.println(ERR_SINT_FALTA_PUNTO_FINAL);
            System.exit(0);

        }
        System.out.println("\n--Programa valido--");
        //genCod.volcarMemoriaEnArchivo(nombreArchivo);

        genCod.finalDePrograma(cantVariablesDeclaradas);
        genCod.generarArchivoExe(nombreArchivo);
        System.out.println("\nArchivo generado en " + nombreArchivo + ".exe");
    }

    private void analizarBloque(int base) throws IOException {
        int desplazamiento = 0;

        genCod.jmp_dir();// Se reserva un JUMP para el bloque y se rellena antes de la proposición E9 _ _ _ _ (5 bytes)
        int comienzoDeBloque = genCod.getSize();

        // Verificar si el bloque comienza con una declaración de constantes
        if (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("const")) {
            desplazamiento = analizarDeclaracionConstantes(base, desplazamiento);
        }

        // Después de constantes, puede seguir una declaración de variables
        if (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("var")) {
            desplazamiento = analizarDeclaracionVariables(base, desplazamiento);
        }

        // Después de variables, puede seguir una declaración de procedimientos
        while (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("procedure")) {
            desplazamiento = analizarDeclaracionProcedimientos(base, desplazamiento);
        }

        int finalDeBloque = genCod.getSize();
        genCod.cargarIntEn(finalDeBloque - comienzoDeBloque, (comienzoDeBloque - 4)); // Se llena el JUMP reservado al comienzo del bloque calculando la distancia entre la posición actual y el comienzo del bloque. Se carga en la posición del comienzoBloque - 4 para quedar despues del E9

        // Finalmente, debe analizarse una proposición
        analizarProposicion(base, desplazamiento);
    }

    private int analizarDeclaracionConstantes(int base, int desplazamiento) throws IOException {
        avanzar(); // Saltar "const"
        analizarIdentificador(); // Debe seguir un identificador

        semantico.registrarIdentificador(tokenActual, CONST, base, desplazamiento);
        desplazamiento++;
        Token identReciente = tokenActual;
        avanzar(); // Saltar identificador

        if (tokenActual.getTipo() != TokenType.COMPARAR) {
            System.out.println(ERR_SINT_FALTA_IGUAL_EN_CONSTANTE);
            System.exit(0);
        }
        avanzar(); // Saltar "="
        analizarNumero(); // Debe seguir un número
        semantico.asignarValor(identReciente.getValor(), Integer.parseInt(tokenActual.getValor()), base, desplazamiento);
        avanzar(); // Saltar número

        while (tokenActual.getTipo() == TokenType.COMA) {
            avanzar(); // Saltar ","
            analizarIdentificador();

            semantico.registrarIdentificador(tokenActual, CONST, base, desplazamiento);
            desplazamiento++;
            identReciente = tokenActual;
            avanzar(); // Saltar identificador

            if (tokenActual.getTipo() != TokenType.COMPARAR) {
                System.out.println(ERR_SINT_FALTA_IGUAL_EN_CONSTANTE);
                System.exit(0);
            }
            avanzar(); // Saltar "="
            analizarNumero();
            semantico.asignarValor(identReciente.getValor(), Integer.parseInt(tokenActual.getValor()), base, desplazamiento);
            avanzar(); // Saltar número
        }
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println(ERR_SINT_FALTA_PUNTO_Y_COMA_EN_CONSTANTE);
            System.exit(0);
        }
        avanzar(); // Saltar ";"
        return desplazamiento;
    }

    private int analizarDeclaracionVariables(int base, int desplazamiento) throws IOException {
        avanzar(); // Saltar "var"
        analizarIdentificador();

        semantico.registrarIdentificador(tokenActual, VAR, base, desplazamiento);
        desplazamiento++;
        semantico.asignarValor(tokenActual.getValor(), cantVariablesDeclaradas, base, desplazamiento);
        cantVariablesDeclaradas++;

        avanzar(); // Saltar identificador

        while (tokenActual.getTipo() == TokenType.COMA) {
            avanzar(); // Saltar ","
            analizarIdentificador();

            semantico.registrarIdentificador(tokenActual, VAR, base, desplazamiento);
            desplazamiento++;
            semantico.asignarValor(tokenActual.getValor(), cantVariablesDeclaradas, base, desplazamiento);
            cantVariablesDeclaradas++;

            avanzar(); // Saltar identificador
        }
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println(ERR_SINT_FALTA_PUNTO_Y_COMA_EN_VARIABLE);
            System.exit(0);
        }
        avanzar(); // Saltar ";"
        return desplazamiento;
    }

    private int analizarDeclaracionProcedimientos(int base, int desplazamiento) throws IOException {
        avanzar(); // Saltar "procedure"
        analizarIdentificador();
        Token identReciente = tokenActual;

        semantico.registrarIdentificador(tokenActual, PROCEDURE, base, desplazamiento);
        desplazamiento++;
        semantico.asignarValor(identReciente.getValor(), genCod.getSize(), base, desplazamiento);

        avanzar(); // Saltar identificador

        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println(ERR_SINT_FALTA_PUNTO_Y_COMA_EN_PROCEDIMIENTO);
            System.exit(0);
        }
        avanzar(); // Saltar ";"
        analizarBloque(base + desplazamiento);
        genCod.ret(); // Al finalizar el bloque de este procedimiento se agrega un RET
        if (tokenActual.getTipo() != TokenType.PUNTO_Y_COMA) {
            System.out.println(ERR_SINT_FALTA_PUNTO_Y_COMA_FINAL_EN_PROCEDIMIENTO);
            System.exit(0);

        }
        avanzar(); // Saltar ";"
        return desplazamiento;
    }

    private void analizarProposicion(int base, int desplazamiento) throws IOException {
        Token identReciente;
        switch (tokenActual.getTipo()) {
            case IDENTIFICADOR:
                identReciente = tokenActual;

                //Para una asignacion de este tipo solo puede ser con un identificador var
                //Si lo es seguirá de largo y sino lanzara el error el analizador semantico              
                semantico.validarQueEsIdentificadorVarDeclarado(tokenActual.getValor(), base, desplazamiento);

                avanzar();

                switch (tokenActual.getTipo()) {
                    case TokenType.ASIGNACION:
                        avanzar(); // Saltar ":="
                        analizarExpresion(base, desplazamiento);

                        //semantico.asignarValor(identReciente.getValor(), Integer.parseInt(tokenActual.getValor()), base, desplazamiento);
                        //cantVariablesDeclaradas++;
                        //semantico.asignarValor(identReciente.getValor(), cantVariablesDeclaradas, base, desplazamiento);
                        Identificador identVar = semantico.buscarIdentificador(identReciente.getValor(), base, desplazamiento);
                        genCod.asignarAVariable(identVar.getValor() * 4);// Se multiplica por 4 porque cada variable ocupa 4 bytes
                        break;

                    case TokenType.SUMA:
                        avanzar(); //salto el primer "+"
                        if (tokenActual.getTipo() == TokenType.SUMA) {
                            avanzar(); // Saltar el segundo "+"
                        } else {
                            System.out.println(ERR_SINT_FALTA_MAS_EN_PROPOSICION);
                            System.exit(0);
                        }
                        break;

                    default:

                        System.out.println(ERR_SINT_FALTA_ADICION_O_DOS_PUNTOS_IGUAL_EN_PROPOSICION);
                        System.exit(0);
                }

                break;
            case PALABRA_RESERVADA:
                switch (tokenActual.getValor()) {
                    case "call":
                        avanzar(); // Saltar "call"
                        analizarIdentificador();

                        semantico.validarQueEsIdentificadorProcedureDeclarado(tokenActual.getValor(), base, desplazamiento);

                        identReciente = tokenActual;
                        Identificador identProcedure = semantico.buscarIdentificador(identReciente.getValor(), base, desplazamiento);
                        int posicionActual = genCod.getSize();
                        int puntoSalto = identProcedure.getValor() - (posicionActual + 5); // Valor del procedimiento - (posición actual + 5 bytes)
                        genCod.call(puntoSalto);

                        avanzar(); // Saltar identificador
                        break;
                    case "begin":
                        avanzar(); // Saltar "begin"
                        analizarProposicion(base, desplazamiento); // Análisis de la primera proposición
                        while (tokenActual.getTipo() == TokenType.PUNTO_Y_COMA) {
                            avanzar(); // Saltar ";"
                            analizarProposicion(base, desplazamiento); // Análisis de proposiciones adicionales
                        }
                        if (tokenActual.getTipo() != TokenType.PALABRA_RESERVADA || !tokenActual.getValor().equals("end")) {
                            System.out.println(ERR_SINT_FALTA_END_EN_BLOQUE);
                            System.exit(0);
                        }
                        avanzar(); // Saltar "end"
                        break;
                    case "if":
                        avanzar(); // Saltar "if"
                        analizarCondicion(base, desplazamiento);
                        if (tokenActual.getTipo() != TokenType.PALABRA_RESERVADA || !tokenActual.getValor().equals("then")) {
                            System.out.println(ERR_SINT_FALTA_THEN_EN_CONDICION);
                            System.exit(0);
                        }
                        avanzar(); // Saltar "then"

                        int inicioDeProposicion = genCod.getSize();
                        analizarProposicion(base, desplazamiento);

                        int finDeProposicion = genCod.getSize();
                        int distanciaDeSalto = finDeProposicion - inicioDeProposicion; // Se calcula la distancia entre el inicio de la proposición y el final de la proposición para luego cargarla en el JUMP
                        genCod.cargarIntEn(distanciaDeSalto, inicioDeProposicion - 4); // Se carga la distancia en el JUMP reservado al inicio de la proposición (el JUMP se encuentra en la condición) - 4 para quedar despues del JUMP E9

                        break;
                    case "while":
                        avanzar(); // Saltar "while"

                        int inicioCondicion = genCod.getSize(); // Aca se vuelve para volver a evaluar la condicion (happy path)
                        analizarCondicion(base, desplazamiento);// TERMINA CON UN E9 _ _ _ _ (5 bytes) que se salta a la proposición
                        int finCondicion = genCod.getSize(); // Esto me sirve para corregir el jump anterior (que se encuentra en el final de la condicion) Estoy E9 _ _ _ _  x <-

                        if (tokenActual.getTipo() != TokenType.PALABRA_RESERVADA || !tokenActual.getValor().equals("do")) {
                            System.out.println(ERR_SINT_FALTA_DO_EN_CONDICION);
                            System.exit(0);
                        }
                        avanzar(); // Saltar "do"
                        analizarProposicion(base, desplazamiento);

                        int finProposicion = genCod.getSize() + 5; // Se suma 5 porque el siguiente JUMP E9 _ _ _ _ ocupa 5 bytes
                        genCod.jmp_dir_a(inicioCondicion - finProposicion); // Vuelve  a la condición para evaluarla nuevamente (happy path)
                        int distanciaSalto = finProposicion - finCondicion;
                        genCod.cargarIntEn(distanciaSalto, finCondicion - 4); // Se carga la distancia en el JUMP reservado al final de la condición - 4 para quedar despues del JUMP E9

                        break;

                    case "readln":
                        avanzar(); // Saltar "readln"
                        if (tokenActual.getTipo() == TokenType.PARENTESIS_IZQ) {
                            avanzar(); // Saltar "("
                            analizarIdentificador(); // Leer identificador

                            semantico.validarQueEsIdentificadorVarDeclarado(tokenActual.getValor(), base, desplazamiento);

                            Identificador identVar = semantico.buscarIdentificador(tokenActual.getValor(), base, desplazamiento);
                            genCod.readLn(identVar.getValor() * 4);

                            avanzar(); // Saltar identificador
                            while (tokenActual.getTipo() == TokenType.COMA) {
                                avanzar(); // Saltar ","
                                analizarIdentificador(); // Leer identificador adicional

                                semantico.validarQueEsIdentificadorVarDeclarado(tokenActual.getValor(), base, desplazamiento);

                                identVar = semantico.buscarIdentificador(tokenActual.getValor(), base, desplazamiento);
                                genCod.readLn(identVar.getValor() * 4);

                                avanzar(); // Saltar identificador
                            }
                            if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                                System.out.println(ERR_SINT_FALTA_PARENTESIS_DER);
                                System.exit(0);
                            }
                            avanzar(); // Saltar ")"
                        } else {
                            System.out.println(ERR_SINT_FALTA_PARENTESIS_IZQ_EN_READLN);
                            System.exit(0);
                        }
                        break;

                    case "writeln":
                        avanzar(); // Saltar "writeln"
                        if (tokenActual.getTipo() == TokenType.PARENTESIS_IZQ) {
                            avanzar(); // Saltar "("
                            analizarCadenaOExpresion(base, desplazamiento); // Leer cadena o expresión
                            while (tokenActual.getTipo() == TokenType.COMA) {
                                avanzar(); // Saltar ","
                                analizarCadenaOExpresion(base, desplazamiento); // Leer cadena o expresión adicional
                            }
                            if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                                System.out.println(ERR_SINT_FALTA_PARENTESIS_DER);
                                System.exit(0);
                            }
                            avanzar(); // Saltar ")"
                        } else {
                            //System.out.println(ERR_SINT_FALTA_PARENTESIS_IZQ_EN_WRITELN);
                            //System.exit(0);
                        }
                        genCod.writeln();

                        break;

                    case "write":
                        avanzar(); // Saltar "write"
                        if (tokenActual.getTipo() == TokenType.PARENTESIS_IZQ) {
                            avanzar(); // Saltar "("
                            analizarCadenaOExpresion(base, desplazamiento); // Leer cadena o expresión
                            while (tokenActual.getTipo() == TokenType.COMA) {
                                avanzar(); // Saltar ","
                                analizarCadenaOExpresion(base, desplazamiento); // Leer cadena o expresión adicional
                            }
                            if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                                System.out.println(ERR_SINT_FALTA_PARENTESIS_DER);
                                System.exit(0);
                            }
                            avanzar(); // Saltar ")"
                        } else {
                            System.out.println(ERR_SINT_FALTA_PARENTESIS_IZQ_EN_WRITE);
                            System.exit(0);
                        }
                        break;

                    case "for":
                        avanzar();// Saltar el "for"
                        analizarIdentificador();

                        semantico.validarQueEsIdentificadorVarDeclarado(tokenActual.getValor(), base, desplazamiento);

                        avanzar();// Salto el identificador

                        if (tokenActual.getTipo() == TokenType.ASIGNACION) {
                            avanzar(); // Saltar ":="
                            analizarExpresion(base, desplazamiento);
                        }

                        if (!tokenActual.getValor().equals("to") && !tokenActual.getValor().equals("downto")) {
                            System.out.println(ERR_SINT_FALTA_TO_O_DOWNTO_EN_BUCLE_FOR);
                            System.exit(0);
                        }
                        avanzar(); // Saltar el "to" o "downto"

                        analizarExpresion(base, desplazamiento);

                        if (!tokenActual.getValor().equals("do")) {
                            System.out.println(ERR_SINT_FALTA_DO_EN_BUCLE_FOR);
                            System.exit(0);
                        }
                        avanzar(); // Saltar el "do"

                        analizarProposicion(base, desplazamiento);

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

    private void analizarCadenaOExpresion(int base, int desplazamiento) throws IOException {
        if (tokenActual.getTipo() == TokenType.CADENA) {
            genCod.writeCadena(tokenActual.getValor());
            avanzar(); // Saltar la cadena
        } else {
            analizarExpresion(base, desplazamiento); // Si no es cadena, debe ser una expresión
            genCod.writeEntero();
        }
    }

    private void analizarExpresion(int base, int desplazamiento) throws IOException {
        Token operador = null;
        // Manejar el primer signo opcional
        if (tokenActual.getTipo() == TokenType.SUMA || tokenActual.getTipo() == TokenType.RESTA) {
            operador = tokenActual;//Guardo si fue un "+" o "-"
            avanzar(); // Saltar el primer "+" o "-"
        }

        // Analizar el término inicial
        analizarTermino(base, desplazamiento);

        if (operador != null && operador.getTipo() == TokenType.RESTA) {
            genCod.negar();
        }

        // Seguir analizando más términos conectados por "+" o "-"
        while (tokenActual.getTipo() == TokenType.SUMA || tokenActual.getTipo() == TokenType.RESTA) {
            operador = tokenActual;//Guardo si fue un "+" o "-"

            avanzar(); // Saltar "+" o "-"
            analizarTermino(base, desplazamiento); // Analizar el siguiente término

            if (operador.getTipo() == TokenType.RESTA) {
                genCod.restar();
            } else {
                genCod.sumar();
            }
        }
    }

    private void analizarTermino(int base, int desplazamiento) throws IOException {
        analizarFactor(base, desplazamiento);
        TokenType operacion;
        while (tokenActual.getTipo() == TokenType.MULTIPLICACION || tokenActual.getTipo() == TokenType.DIVISION) {
            operacion = tokenActual.getTipo();

            avanzar(); // Saltar "*" o "/"
            analizarFactor(base, desplazamiento); // Analiza el siguiente factor
            if (operacion == TokenType.MULTIPLICACION) {
                genCod.multiplicar();
            } else {
                genCod.dividir();
            }
        }
    }

    private void analizarFactor(int base, int desplazamiento) throws IOException {
        switch (tokenActual.getTipo()) {
            case IDENTIFICADOR:
                analizarIdentificador();
                if (semantico.esVar(tokenActual.getValor(), base, desplazamiento)) {
                    genCod.mov_eax_edi(semantico.obtenerValorDelIdentificador(tokenActual.getValor(), base, desplazamiento) * 4);
                } else if (semantico.esConst(tokenActual.getValor(), base, desplazamiento)) {
                    genCod.mov_eax(semantico.obtenerValorDelIdentificador(tokenActual.getValor(), base, desplazamiento));
                }
                genCod.push_eax();
                avanzar(); // Saltar identificador
                break;
            case NUMERO:
                analizarNumero();

                genCod.mov_eax(Integer.parseInt(tokenActual.getValor()));
                genCod.push_eax();

                avanzar(); // Saltar número
                break;
            case PARENTESIS_IZQ:
                avanzar(); // Saltar "("
                analizarExpresion(base, desplazamiento);
                if (tokenActual.getTipo() != TokenType.PARENTESIS_DER) {
                    System.out.println(ERR_SINT_FALTA_PARENTESIS_DER);
                    System.exit(0);
                }
                avanzar(); // Saltar ")"
                break;
            default:
                System.out.println(ERR_SINT_FALTA_FACTOR);
                System.exit(0);
        }
    }

    private void analizarCondicion(int base, int desplazamiento) throws IOException {
        // Caso 1: "odd" -> expresion
        if (tokenActual.getTipo() == TokenType.PALABRA_RESERVADA && tokenActual.getValor().equals("odd")) {
            avanzar(); // Saltar "odd"
            analizarExpresion(base, desplazamiento); // Analizar la expresión que sigue
            genCod.odd();
        } else {
            // Caso 2: expresion -> = o <> o < o <= o > o >= -> expresion
            analizarExpresion(base, desplazamiento); // Analizar la primera expresión
            if (tokenActual.getTipo() == TokenType.COMPARAR
                    || tokenActual.getTipo() == TokenType.DISTINTO
                    || tokenActual.getTipo() == TokenType.MENOR
                    || tokenActual.getTipo() == TokenType.MENOR_O_IG
                    || tokenActual.getTipo() == TokenType.MAYOR
                    || tokenActual.getTipo() == TokenType.MAYOR_O_IG) {

                TokenType operador = tokenActual.getTipo();

                avanzar(); // Saltar el operador de comparación
                analizarExpresion(base, desplazamiento); // Analizar la segunda expresión
                genCod.expresionCondicional(operador);
            } else {
                System.out.println(ERR_SINT_FALTA_OPERADOR_DE_COMPARACION);
                System.exit(0);
            }
        }
    }

    private void analizarIdentificador() throws IOException {
        if (tokenActual.getTipo() != TokenType.IDENTIFICADOR) {
            System.out.println(ERR_SINT_FALTA_IDENTIFICADOR);
            System.exit(0);
        }
        //avanzar(); // Saltar identificador
    }

    private void analizarNumero() throws IOException {
        if (tokenActual.getTipo() != TokenType.NUMERO && tokenActual.getTipo() != TokenType.RESTA) {
            System.out.println(ERR_SINT_FALTA_NUMERO);
            System.exit(0);
        }
        /*if (tokenActual.getTipo() == TokenType.RESTA) {
            avanzar();//salta el -
        }*/

        //avanzar(); // Saltar número
    }
}
