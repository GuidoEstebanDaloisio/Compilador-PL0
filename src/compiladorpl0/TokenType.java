package compiladorpl0;

public enum TokenType {
    IDENTIFICADOR, 
    NUMERO, 
    ASIGNACION, 
    NUL,
    MENOR_O_IG, 
    DISTINTO, 
    MENOR, 
    MAYOR_O_IG, 
    MAYOR,
    SUMA, 
    RESTA, 
    MULTIPLICACION, 
    DIVISION,
    PARENTESIS_IZQ, 
    PARENTESIS_DER, 
    PUNTO_Y_COMA, 
    COMA, 
    EOF,
    COMPARAR,
    CADENA,
    PUNTO,
    PALABRA_RESERVADA
}