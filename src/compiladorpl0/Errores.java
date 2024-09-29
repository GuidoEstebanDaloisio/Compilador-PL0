package compiladorpl0;

public class Errores {

    //ERRORES SINTACTICOS
    static String ERR_SINT_FALTA_PUNTO_FINAL = "Error sintactico: Se esperaba un '.' al final del programa";
    static String ERR_SINT_FALTA_IGUAL_EN_CONSTANTE = "Error sintactico: Se esperaba '=' en la declaracion de constante";
    static String ERR_SINT_FALTA_PUNTO_Y_COMA_EN_CONSTANTE = "Error sintactico: Se esperaba ';' al final de la declaracion de constantes";
    static String ERR_SINT_FALTA_PUNTO_Y_COMA_EN_VARIABLE = "Error sintactico: Se esperaba ';' al final de la declaracion de variables";
    static String ERR_SINT_FALTA_PUNTO_Y_COMA_EN_PROCEDIMIENTO = "Error sintactico: Se esperaba ';' después de la declaracion del procedimiento";
    static String ERR_SINT_FALTA_PUNTO_Y_COMA_FINAL_EN_PROCEDIMIENTO = "Error sintactico: Se esperaba ';' al final de la declaracion del procedimiento";
    static String ERR_SINT_FALTA_DOS_PUNTOS_IGUAL_EN_PROPOSICION = "Error sintactico: Se esperaba ':=' en la proposicion";
    static String ERR_SINT_FALTA_END_EN_BLOQUE = "Error sintactico: Se esperaba 'end' al final del bloque";
    static String ERR_SINT_FALTA_THEN_EN_CONDICION = "Error sintactico: Se esperaba 'then' después de la condicion";
    static String ERR_SINT_FALTA_DO_EN_CONDICION = "Error sintactico: Se esperaba 'do' después de la condicion";
    static String ERR_SINT_FALTA_PARENTESIS_DER = "Error sintactico: Se esperaba ')'";
    static String ERR_SINT_FALTA_PARENTESIS_IZQ_EN_READLN = "Error sintactico: Se esperaba '(' despues de 'readln'";
    static String ERR_SINT_FALTA_PARENTESIS_IZQ_EN_WRITELN = "Error sintactico: Se esperaba '(' despues de 'writeln''";
    static String ERR_SINT_FALTA_PARENTESIS_IZQ_EN_WRITE = "Error sintactico: Se esperaba '(' despues de 'write''";  
    static String ERR_SINT_FALTA_OPERADOR_DE_COMPARACION ="Error sintactico: Operador de comparacion esperado en la condicion";
    static String ERR_SINT_FALTA_IDENTIFICADOR = "Error sintactico: Se esperaba un identificador";
    static String ERR_SINT_FALTA_NUMERO = "Error sintactico: Se esperaba un numero";
    
    //ERRORES SEMANTICOS
    static String ERR_SEM_IDENTIFICADOR = "Error semantico: El identificador '";
    static String ERR_SEM_YA_DECLARADO_EN_EL_BLOQUE = "' ya ha sido declarado en este bloque";
    static String ERR_SEM_NO_FUE_DECLARADO = "' no ha sido declarado";
    static String ERR_SEM_NO_ES_DEL_TIPO_ESPERADO = "' no es del tipo esperado";
    static String ERR_SEM_CONST_ASIGNADA = "' es una constante a la que ya se le fue asignado un valor";    
    static String ERR_SEM_CAPACIDAD_MAXIMA = "Capacidad máxima de identificadores alcanzada";
   
}
