package compiladorpl0;

import static compiladorpl0.Errores.*;
import static compiladorpl0.IdentType.*;

public class AnalizadorSemantico {

    private Identificador[] identificadoresRegistrados;

    public AnalizadorSemantico() {
        this.identificadoresRegistrados = new Identificador[1024]; // Capacidad maxima de 1024 identificadores
    }

    public void mostrarIdentificadoresRegistrados() {
        System.out.println("\nLista de identificadores registrados:");
        for (int i = 0; i < identificadoresRegistrados.length; i++) {
            if (identificadoresRegistrados[i] != null) {
                Identificador ident = identificadoresRegistrados[i];
                System.out.println("Posicion: " + i
                        + " | Nombre: " + ident.getNombre()
                        + " | Tipo: " + ident.getTipo()
                        + " | Valor: " + (ident.getValor() != null ? ident.getValor() : "Sin asignar"));
            }
        }
    }

    public void registrarIdentificador(Token token, IdentType tipo, int base, int desplazamiento) {

        Identificador ident = convertirTokenEnIdentificador(token, tipo);

        if (estaIdentificadorDeclaradoEnBloque(ident.getNombre(), base, desplazamiento)) {
            System.out.println(ERR_SEM_IDENTIFICADOR + ident.getNombre() + ERR_SEM_YA_DECLARADO_EN_EL_BLOQUE);
            System.exit(0);
        }

        //Se verifica que no se llego al tope de idents
        varificarCantidadDeIdent(desplazamiento);

        identificadoresRegistrados[base + desplazamiento] = ident;

        //mostrarIdentificadoresRegistrados();

    }

    public void asignarValor(String nombre, int valor, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);

        if (ident.getTipo().equals(CONST) && ident.getValor() != null) {

            //Si el ident es una constante con un valor ya cargado no se le puede asignar otro valor
            System.out.println(ERR_SEM_IDENTIFICADOR + nombre + ERR_SEM_CONST_ASIGNADA);
            System.exit(0);
        }

        ident.setValor(valor);
    }

    private Identificador convertirTokenEnIdentificador(Token token, IdentType tipo) {
        Identificador ident = new Identificador(token.getValor(), tipo, null);
        return ident;
    }

    private boolean estaIdentificadorDeclaradoEnBloque(String nombre, int base, int desplazamiento) {
        for (int i = base; i < base + desplazamiento; i++) {
            if (identificadoresRegistrados[i].getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    private void varificarCantidadDeIdent(int cantidadIdentificadores) {
        if (cantidadIdentificadores >= 1023) { //son 1023 en lugar de 1024 porque se cuenta desde la posicion 0
            System.out.println(ERR_SEM_CAPACIDAD_MAXIMA);
            System.exit(0);
        }
    }

    // Buscar un identificador en toda la tabla desde BASE+DESPLAZAMIENTO-1 hasta 0
    public Identificador buscarIdentificador(String nombre, int base, int desplazamiento) {
        
       for (int i = base + desplazamiento - 1; i >= 0; i--) {

            //verificamos que la posición actual en el array no sea nula, para evitar un NullPointerException
            if (identificadoresRegistrados[i] != null && identificadoresRegistrados[i].getNombre().equals(nombre)) {
                return identificadoresRegistrados[i];
            }
        }
        System.out.println(ERR_SEM_IDENTIFICADOR + nombre + ERR_SEM_NO_FUE_DECLARADO);
        System.exit(0);
        return null;

    }

    //Compruebo si el identificador es del tipo correcto
    public void validarQueEsIdentificadorConstOVarDeclarado(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);

        if (!ident.getTipo().equals(CONST) && !ident.getTipo().equals(VAR)) {
            System.out.println(ERR_SEM_IDENTIFICADOR + ident.getNombre() + ERR_SEM_NO_ES_DEL_TIPO_ESPERADO);
            System.exit(0);
        }
    }

    public void validarQueEsIdentificadorProcedureDeclarado(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);

        if (!ident.getTipo().equals(PROCEDURE)) {
            System.out.println(ERR_SEM_IDENTIFICADOR + ident.getNombre() + ERR_SEM_NO_ES_DEL_TIPO_ESPERADO);
            System.exit(0);
        }
    }

    public void validarQueEsIdentificadorVarDeclarado(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);

        if (!ident.getTipo().equals(VAR)) {
            System.out.println(ERR_SEM_IDENTIFICADOR + ident.getNombre() + ERR_SEM_NO_ES_DEL_TIPO_ESPERADO);
            System.exit(0);

        }
    }

    public void validarQueEsIdentificadorConstDeclarado(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);
        if (!ident.getTipo().equals(CONST)) {
            System.out.println(ERR_SEM_IDENTIFICADOR + ident.getNombre() + ERR_SEM_NO_ES_DEL_TIPO_ESPERADO);
            System.exit(0);
        }
    }

    public boolean esConst(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);
        return ident.getTipo().equals(CONST);
    }

    public boolean esVar(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);
        return ident.getTipo().equals(VAR);
    }

    public Integer obtenerValorDelIdentificador(String nombre, int base, int desplazamiento) {
        Identificador ident = buscarIdentificador(nombre, base, desplazamiento);
        return ident.getValor();
    }

}
