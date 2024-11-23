package compiladorpl0;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GeneradorDeCodigo {

    private ArrayList<Byte> memoria;
    private int EDI;
    private int finalDeCodigoCargado;

    public GeneradorDeCodigo() {
        cargarParteDeLongitudFija();
    }

    public int getSize() {
        return memoria.size();
    }

    public void finalDePrograma(int cantVariables) {
        System.out.println("---------------------- FINAL DE PROGRAMA ----------------------\n\n\n");
        this.finalDeCodigoCargado = getSize();

        actualizarHeader(); // 0 - Actualizar Header
        fixupEDI(); // 1 - Actualizar EDI
        reservarMemoriaParaVariables(cantVariables); // 2 - Reservar memoria para variables
        actualizarVirtualSize(); // 3 - Actualizar VirtualSize
        rellenarMultiploDeFileAlignment(); // 4 - Rellenar con 0 múltiplo de FileAlignment
        ajustarSizoOfCodeSection(); // 5 - Ajustar SizeOfCodeSection
        ajustarSizeOfRawData(); // 5 - Ajustar SizeOfRawData
        ajustarSizeOfImage(); // 6 - Ajustar SizeOfImage
        ajustarSizeOfData(); // 6 - Ajustar BaseOfData

        int programaFinalizado = getSize();

        System.out.println("\n--------- Programa finalizado en " + programaFinalizado + " bytes (" + toHexa(programaFinalizado) + ") ---------");
    }

    private void actualizarHeader() {
        System.out.println("\n0. Cargando tamaño de programa en HEADER " + finalDeCodigoCargado + " bytes (" + toHexa(finalDeCodigoCargado) + ")\n");
        int distancia = Constantes.FINALIZAR_PROGRAMA - (finalDeCodigoCargado + 5);
        jmp_dir_a(distancia);
    }

    private void fixupEDI() {
        /*
         * A continuación, se debe hacer un fix-up de la primera instrucción de
         * la parte de longitud variable de la sección text (MOV EDI, 00000000), ya
         * que el desplazamiento actual en el archivo ejecutable indica el comienzo
         * del área de almacenamiento de las variables.
         */

        System.out.println("\n1. Actualizando EDI\n");

        int posicionActual = getSize();
        int baseOfCodePosicion = buscarEnteroEn(Constantes.BASE_OF_CODE_POSICION);
        int imageBasePosicion = buscarEnteroEn(Constantes.IMAGE_BASE_POSICION);
        int tamanoHeader = buscarEnteroEn(Constantes.TAMANO_HEADER_POSICION);

        int posicion = baseOfCodePosicion + imageBasePosicion + posicionActual
                - tamanoHeader;

        System.out.println("\n\t=== Calculando posición de EDI ===");
        System.out.println("\tPosición actual     : " + toHexa(posicionActual) + " (" + posicionActual + ")");
        System.out.println("\tBaseOfCode          : " + toHexa(baseOfCodePosicion) + " (" + baseOfCodePosicion + ")");
        System.out.println("\tImageBase           : " + toHexa(imageBasePosicion) + " (" + imageBasePosicion + ")");
        System.out.println("\tTamaño del header   : " + toHexa(tamanoHeader) + " (" + tamanoHeader + ")");
        System.out.println("\tPosición de EDI     : " + toHexa(posicion) + " (" + posicion + ")\n");

        cargarIntEn(posicion, EDI);
        System.out.println("\nEDI actualizado a   : " + toHexa(posicion));

    }

    private void reservarMemoriaParaVariables(int cantVariables) {
        /*
         * Luego, deben grabarse ceros al final del archivo ejecutable, a razón
         * de cuatro bytes por cada variable (a esta altura de la compilación, el
         * número de variables que fueron declaradas ya es conocido).
         */

        int espacioReservado = 4 * cantVariables;
        System.out.println("\n2. Reservando espacio para variables: " + cantVariables + " variables\n");
        System.out.println("\nDesde --> " + toHexa(getSize() + 1)); // + 1 para que no se incluya el byte actual
        for (int i = 0; i < espacioReservado; i++) {
            cargarByte(0);
        }
        System.out.println("Hasta --> " + toHexa(getSize()));
    }

    private void actualizarVirtualSize() {
        /*
         * Ahora se debe realizar el ajuste del campo VirtualSize del encabezado
         * de la sección text (posiciones 416-419, o 01A0-01A3 en hexadecimal),
         * colocando allí el tamaño de la sección text (hasta el momento).
         */

        int sizeTextSection = getSize() - buscarEnteroEn(Constantes.TAMANO_HEADER_POSICION);
        System.out.println("\n3. Actualizando VirtualSize\n");
        cargarIntEn(sizeTextSection, Constantes.VIRTUAL_SIZE_POSICION);
    }

    private void rellenarMultiploDeFileAlignment() {
        /*
         * Después, debe rellenarse el archivo con ceros, para que su tamaño sea
         * múltiplo del campo FileAlignment del encabezado opcional específico para
         * Windows (posiciones 220-223, o 00DC-00DF en hexadecimal).
         */

        int fileAlignment = buscarEnteroEn(Constantes.FILE_ALIGNMENT_POSICION);
        System.out.println("\n4. Rellenando múltiplo de FileAlignment (" + fileAlignment + " bytes)\n");
        int posicionActual = getSize();
        int cantidadDeCeros = 0;

        System.out.println("Desde --> " + toHexa(getSize() + 1)); // + 1 para que no se incluya el byte actual
        while (posicionActual % fileAlignment != 0) {
            cargarByte();
            posicionActual++;
            cantidadDeCeros++;
        }
        System.out.println("Hasta --> " + toHexa(getSize()));
        System.out.println("\nSe rellenaron " + cantidadDeCeros + " bytes con 00");
    }

    private void ajustarSizoOfCodeSection() {
        // SizeOfCodeSection (posiciones 188-191, o 00BC-00BF en hexadecimal)

        System.out.println("\n5. Ajustando SizeOfCodeSection\n");
        int sizeTextSection = getSize() - buscarEnteroEn(Constantes.TAMANO_HEADER_POSICION);
        cargarIntEn(sizeTextSection, Constantes.SIZE_OF_CODE_SECTION_POSICION);
    }

    private void ajustarSizeOfRawData() {
        // SizeOfRawData (posiciones 424-427, o 01A8-01AB en hexadecimal)

        System.out.println("\n5. Ajustando SizeOfRawData\n");
        int sizeTextSection = getSize() - buscarEnteroEn(Constantes.TAMANO_HEADER_POSICION);
        cargarIntEn(sizeTextSection, Constantes.SIZE_OF_RAW_DATA_POSICION);
    }

    private void ajustarSizeOfImage() {
        // SizeOfImage (posiciones 240-243, o 00F0-00F3 en hexadecimal)

        System.out.println("\n6. Ajustando SizeOfImage\n");
        int sizeOfCodeSection = buscarEnteroEn(Constantes.SIZE_OF_CODE_SECTION_POSICION);
        int sectionAlignment = buscarEnteroEn(Constantes.SECTION_ALIGNMENT_POSICION);

        cargarIntEn((2 + sizeOfCodeSection / sectionAlignment) * sectionAlignment, Constantes.SIZE_OF_IMAGE_POSICION);
    }

    private void ajustarSizeOfData() {
        // SizeOfImage (posiciones 240-243, o 00F0-00F3 en hexadecimal)

        System.out.println("\n6. Ajustando BaseOfData\n");
        int sizeOfRawData = buscarEnteroEn(Constantes.SIZE_OF_RAW_DATA_POSICION);
        int sectionAlignment = buscarEnteroEn(Constantes.SECTION_ALIGNMENT_POSICION);

        cargarIntEn((2 + sizeOfRawData / sectionAlignment) * sectionAlignment, Constantes.BASE_OF_DATA_POSICION);
    }

    void cargarEDI() {
        // MOV EDI, abcdefgh = BF gh ef cd ab --> (COPIA EL SEGUNDO OPERANDO EN EL
        // PRIMERO)
        // Luego se asignará el valor de EDI a la variable correspondiente (SIEMPRE ES
        // 0040157A)
        System.out.println("---------------------- INICIO DE CARGA DE PROGRAMA (" + toHexa(getSize() + 1) + ") ----------------------");

        reservarEDI();

        this.EDI = getSize() - 4; // - 4 para quedar en BF x<- _ _ _ 
    }

    private void reservarEDI() {
        mostrarInicioDeInstruccion("Reservando EDI [ BF _ _ _ _ ]", 5);

        cargarByte(0xBF);
        cargarByte(0x00);
        cargarByte(0x00);
        cargarByte(0x00);
        cargarByte(0x00);
    }

    private void pop_eax() {// Código de instrucción para POP EAX
        mostrarInicioDeInstruccion("POP EAX [ 58 ]", 1);
        cargarByte(0x58);
    }

    private void pop_ebx() {// Código de instrucción para POP EBX
        mostrarInicioDeInstruccion("POP EBX [ 5B ]", 1);
        cargarByte(0x5B);
    }

    private void imul_ebx() {// Código de instrucción para IMUL EBX
        mostrarInicioDeInstruccion("IMUL EBX [ F7 EB ]", 2);
        cargarByte(0xF7);
        cargarByte(0xEB);

    }

    private void xchg_eax_ebx() {// Código de instrucción para XCHG EAX, EBX --> INTERCAMBIA LOS VALORES DE LOS OPERANDOS
        mostrarInicioDeInstruccion("EXCHANGE [ 93 ]", 1);
        cargarByte(0x93);
    }

    private void cdq() {// Código de instrucción para CDQ
        cargarByte(0x99);
    }

    private void idiv_ebx() {// Código de instrucción para IDIV EBX
        cargarByte(0xF7);
        cargarByte(0xFB);
    }

    private void sub_eax_ebx() {// Código de instrucción para SUB EAX, EBX
        mostrarInicioDeInstruccion("SUB [ 29 D8 ]", 2);
        cargarByte(0x29);
        cargarByte(0xD8);
    }

    private void neg_eax() { //Código de instruccion NEG EAX --> CAMBIA EL SIGNO DE EAX
        mostrarInicioDeInstruccion("CAMBIAR SIGNO [ F7 D8 ]", 2);
        cargarByte(0xF7);
        cargarByte(0xD8);
    }

    private void add_eax_ebx() {// Código de instrucción para ADD EAX, EBX
        mostrarInicioDeInstruccion("ADD [ 01 D8 ]", 2);
        cargarByte(0x01);
        cargarByte(0xD8);
    }

    public void multiplicar() { //MULTIPLICAR [ 58 5B F7 EB 50 ]
        mostrarInicioDeInstruccion("MULTIPLICAR [ 58 5B F7 EB 50 ]", 5);
        pop_eax();
        pop_ebx();
        imul_ebx();
        push_eax();
        mostrarFinDeInstruccion("MULTIPLICAR");        
    }

    public void dividir() { //DIVIDIR [ 58 5B 93 99 F7 FB 50 ]
        mostrarInicioDeInstruccion("DIVIDIR [ 58 5B 93 99 F7 FB 50 ]", 7);
        pop_eax();
        pop_ebx();
        xchg_eax_ebx();
        cdq();
        idiv_ebx();
        push_eax();
        mostrarFinDeInstruccion("DIVIDIR");
        
    }

    public void restar() { //RESTAR [ 58 5B 93 29 D8 50 ]
        mostrarInicioDeInstruccion("RESTAR [ 58 5B 93 D8 50 ]", 5);
        pop_eax();
        pop_ebx();
        xchg_eax_ebx();
        sub_eax_ebx();
        push_eax();
        mostrarFinDeInstruccion("RESTAR");

    }

    public void sumar() { //SUMAR [ 58 5B 01 D8 50 ]
        mostrarInicioDeInstruccion("SUMAR [ 58 5B 01 D8 50 ]", 5);
        pop_eax();
        pop_ebx();
        add_eax_ebx();
        push_eax();
        mostrarFinDeInstruccion("SUMAR");

    }

    public void negar() { //NEGAR [ 58 F7 D8 50 ]
        mostrarInicioDeInstruccion("NEGAR [ 58 F7 D8 50 ]", 4);
        pop_eax();
        neg_eax();
        push_eax();
        mostrarFinDeInstruccion("NEGAR");        
    }

    public void call(int distancia) { //CALL [ E8 56 FF FF FF ]
        System.out.println("-- Cargando CALL a " + toHexa(getSize() + (distancia + 5)) + " --");
        cargarByte(0xE8);
        cargarInt(distancia);
    }

    public void ret() { // RET = C3 --> RETORNA AL PUNTO DESDE DONDE SE LLAMÓ UNA SUBRUTINA
        mostrarInicioDeInstruccion("RET [ C3 ]", 1);
        cargarByte(0xC3);
    }

    private void test_al() { //Código de instrucción para TEST AL, ab
        cargarByte(0xA8);
    }

    private void jpo_dir() { //Código de instrucción para JPO dir
        cargarByte(0x7B);
    }

    public void jmp_dir() { //Código de instrucción para JMP dir
        // RESERVAR JUMP = E9 00 00 00 00 --> RESERVA ESPACIO PARA EL JUMP YA QUE NO SE CONOCE LA CANTIDAD DE BYTES
        mostrarInicioDeInstruccion("JUMP | E9 _ _ _ _ |", 5);
        cargarByte(0xE9);
        cargarByte(0x00);
        cargarByte(0x00);
        cargarByte(0x00);
        cargarByte(0x00);
    }

    public void jmp_dir_a(int valor) { //Código de instrucción para JMP dir
        // CUANDO SE CONOCE EL VALOR DEL JUMP
        System.out.println("-- Cargando JUMP con valor: " + valor + " (" + toHexa(valor) + ") --");
        cargarByte(0xE9);
        cargarInt(valor);
    }

    public void readLn(int valorVar) {
        // CALL, _ _ _ _ (función en el header)

        int posicionActual = getSize();
        int distanciaHaciaES = Constantes.LEER_ENTERO_Y_GUARDAR_EN_EAX - (posicionActual + 5); // + 5 porqué se tienen en cuenta los 5 bytes de la instrucción siguiente (CALL)
        call(distanciaHaciaES);

        mov_edi_eax(valorVar);
    }

    public void writeln() {
        int posicionActual = getSize();
        int distanciaHaciaES = Constantes.IMPRIMIR_SALTO_DE_LINEA - (posicionActual + 5);
        call(distanciaHaciaES);

    }

    public void writeEntero() {
        pop_eax();
        int posicionActual = getSize();
        int distanciaHaciaES = Constantes.IMPRIMIR_ENTERO_DE_EAX - (posicionActual + 5);
        call(distanciaHaciaES);
    }

    public int buscarEnteroEn(int pos) {
        return memoria.get(pos)
                + memoria.get(pos + 1) * 0x100
                + memoria.get(pos + 2) * 0x10000
                + memoria.get(pos + 3) * 0x1000000;
    }

    public void writeCadena(String cadena) {
        /*
         * 1. Se genera la inicialización de EAX con la ubicación absoluta que
         * tendrá la cadena (se conoce porque la longitud de los pasos 2 y 3
         * es fija), usando para calcularla los campos BaseOfCode (posiciones
         * 204-207, o 00CC-00CF en hexadecimal) e ImageBase (posiciones 212-
         * 215, o 00D4-00D7 en hexadecimal) del encabezado del archivo
         * ejecutable;
         */
        int baseOfCodePosicion = buscarEnteroEn(Constantes.BASE_OF_CODE_POSICION); // Inicio de la sección de código
        int imageBasePosicion = buscarEnteroEn(Constantes.IMAGE_BASE_POSICION); // Base de la imagen
        int posicionActual = getSize(); // Posición actual en la memoria
        int inicioDeCadena = 15; // Posición de inicio de la cadena (después de las siguientes 3 instrucciones de 5 bytes c/u)
        int tamanoHeader = buscarEnteroEn(Constantes.TAMANO_HEADER_POSICION); // Tamaño del header

        int ubiCadena = baseOfCodePosicion + imageBasePosicion + posicionActual + inicioDeCadena - tamanoHeader;

        mov_eax(ubiCadena);

        // 2. Se genera la invocación a la rutina de E/S que mostrará la cadena;
        int distanciaHaciaES = Constantes.IMPRIMIR_CADENA - (getSize() + 5); // + 5 porqué se tienen en cuenta los 5 bytes de la instrucción siguiente (CALL)
        call(distanciaHaciaES);

        // 3. Se genera un salto incondicional E9 00 00 00 00;
        // reservarJUMP();
        int tamanoCadena = cadena.length();
        int comillasSimples = 2; // La cadena tiene comillas simples al principio y al final (se las sacamos)
        int ceroFinal = 1; // Tiene un cero al final de la cadena (se lo agregamos)

        int tamanoFinalDeCadena = tamanoCadena + ceroFinal;

        // cargarByte(0xe9);
        // cargarInt(tamanoFinalDeCadena);
        jmp_dir_a(tamanoFinalDeCadena);
        // int jumpPosicion = getSize();

        // 4. Se generan los bytes de la cadena, seguidos de un cero;
        for (int i = 0; i < tamanoCadena; i++) {
            char c = cadena.charAt(i);
            // System.out.println(c);
            cargarByte(c);
        }
        cargarByte(0);

        // 5. Se realiza el fix-up del salto colocado en el paso 3. (No hace falta
        // porque se cargo bien de inicio)
        // int cadenaFinal = getSize();
        // cargarIntEn(cadenaFinal, jumpPosicion - 4);
    }

    private void cmp_ebx_eax() { //Código de instrucción para CMP EBX, EAX
        cargarByte(0x39);
        cargarByte(0xC3);
    }

    public void odd() { //ODD [ 58 A8 01 7B 05 E9 00 00 00 00 ]
        mostrarInicioDeInstruccion("ODD [ 58 A8 01 7B 05 E9 00 00 00 00 ]", 10);
        pop_eax();
        test_al();
        cargarByte(0x01);
        jpo_dir();
        cargarByte(0x05); //Saltar 5 (para saltear el jump en caso de que tenga que saltar toda la posicion
        jmp_dir();
        mostrarFinDeInstruccion("ODD");

    }

    public void expresionCondicional(TokenType condicional) { //EXPRESION CONDICIONAL [58 5B 39 C3 ...]
        mostrarInicioDeInstruccion("EXPRESION CONDICIONAL", 11);
        pop_eax();
        pop_ebx();
        cmp_ebx_eax();

        switch (condicional) {
            case TokenType.COMPARAR:
                cargarByte(0x74);
                break;
            case TokenType.DISTINTO:
                cargarByte(0x75);
                break;
            case TokenType.MENOR:
                cargarByte(0x7C);
                break;
            case TokenType.MENOR_O_IG:
                cargarByte(0x7E);
                break;
            case TokenType.MAYOR:
                cargarByte(0x7F);
                break;
            case TokenType.MAYOR_O_IG:
                cargarByte(0x7D);
                break;
            default:
                System.out.println("Error: expresión condicional no válida");
                break;
        }

        cargarByte(0x05);
        jmp_dir();
    }

    public void asignarAVariable(int valorVar) {
        pop_eax();
        mov_edi_eax(valorVar);
    }

    public void mov_eax(int valor) {// Código de instrucción para MOV EAX, abcdefgh
        mostrarInicioDeInstruccion("MOV EAX [ B8 _ _ _ _ ] - Valor cargado: "+valor, 5);
        cargarByte(0xB8);
        cargarInt(valor);
    }

    public void mov_eax_edi(int valor) {//Código de instrucción para MOV EAX, [EDI+abcdefgh]
        mostrarInicioDeInstruccion("MOV EAX, [EDI+abcdefgh] [ 8B 87 _ _ _ _ ] - Valor cargado: "+valor, 6);
        cargarByte(0x8B);
        cargarByte(0x87);
        cargarInt(valor);

    }

    private void mov_edi_eax(int valor) {//Código de instrucción para MOV [EDI+abcdefgh], EAX
        mostrarInicioDeInstruccion("MOV [EDI+abcdefgh], EAX  [ 89 87 _ _ _ _ ] - Valor cargado: "+valor, 6);
        cargarByte(0x89);
        cargarByte(0x87);
        cargarInt(valor);

    }

    public void push_eax() {//Código de instrucción para PUSH EAX
        mostrarInicioDeInstruccion("PUSH EAX [ 50 ]", 1);
        cargarByte(0x50);

    }

    private void crearArchivo(String nombreArchivo) {

        int puntoIndex = nombreArchivo.lastIndexOf(".");
        if (puntoIndex != -1) {
            // Obtengo solo el nombre sin la extensión .PL0
            nombreArchivo = nombreArchivo.substring(0, puntoIndex);
            nombreArchivo = nombreArchivo.substring(0, nombreArchivo.lastIndexOf("."));
        }
        File archivo = new File(nombreArchivo);
        try {
            if (archivo.createNewFile()) {
                System.out.println("Archivo creado: " + archivo.getName());
            } else {
                System.out.println("El archivo ya existe.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generarArchivoExe(String nombreArchivo) {
        try {
            FileOutputStream archivo = new FileOutputStream("./" + nombreArchivo + ".exe");
            for (byte b : memoria) {
                archivo.write(b);
            }
            archivo.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void cargarByte(int byteACargar) {
        byte valorByte = (byte) byteACargar; // Casteo a byte
        memoria.add(valorByte);
    }

    private void cargarByte() {
        memoria.add((byte) 0x00);
    }

    public void cargarInt(int enteroACargar) {
        for (int i = 0; i < 4; i++) {  // Se asume que el entero tiene 4 bytes
            int byteValue = (enteroACargar >> (i * 8)) & 0xFF; // Extrae el byte correspondiente
            cargarByte(byteValue); // Agrega el byte a la memoria
        }
    }

    public void cargarByteEn(int byteACargar, int ubicacion) {
        byte valorByte = (byte) byteACargar;
        memoria.set(ubicacion, valorByte);
    }

    public void cargarIntEn(int intACargar, int ubicacion) {
        for (int i = 0; i < 4; i++) {
            int byteValue = (intACargar >> (i * 8)) & 0xFF; // Extrae el byte correspondiente
            cargarByteEn(byteValue, ubicacion + i); // Carga cada byte en la posición específica
        }
    }

    //Se usaria con el punto final en el analizador sintactico
    public void volcarMemoriaEnArchivo(String nombreArchivo) {

        int puntoIndex = nombreArchivo.lastIndexOf(".");
        if (puntoIndex != -1) {
            nombreArchivo = nombreArchivo.substring(0, puntoIndex);
        }

        crearArchivo(nombreArchivo);

        try (FileOutputStream archivo = new FileOutputStream(nombreArchivo)) {
            for (Byte b : memoria) {
                archivo.write(b);
            }
            archivo.flush(); // Asegurarse de que todos los datos se escriban
            System.out.println("Memoria volcada al archivo: " + nombreArchivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String toHexa(int valor) {
        return "0x" + Integer.toHexString(valor).toUpperCase();
    }

    public void mostrarInicioDeProposicion(String instruccion) {
        System.out.println("\n\n------------ [Inicio] proposicion " + instruccion + " ------------\n");
    }

    public void mostrarFinalDeProposicion(String instruccion) {
        System.out.println("\n------------ [Fin] proposicion " + instruccion + " ------------\n");
    }
    
    private void mostrarInicioDeInstruccion(String instruccion, int tamano) {
        System.out.println("\n-- [Cargando] instruccion " + instruccion + " (" + tamano + " bytes):");
    }

    private void mostrarFinDeInstruccion(String instruccion) {
        System.out.println("\n-- [Finalizada] instruccion " + instruccion + " Finalizada");
    }

    private void cargarParteDeLongitudFija() {
        memoria = new ArrayList<>();
        memoria.add(((byte) 0x4D));
        memoria.add(((byte) 0x5A));
        memoria.add(((byte) 0x60));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x60));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xA0));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0E));
        memoria.add(((byte) 0x1F));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x0E));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xB4));
        memoria.add(((byte) 0x09));
        memoria.add(((byte) 0xCD));
        memoria.add(((byte) 0x21));
        memoria.add(((byte) 0xB8));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x4C));
        memoria.add(((byte) 0xCD));
        memoria.add(((byte) 0x21));
        memoria.add(((byte) 0x54));
        memoria.add(((byte) 0x68));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x70));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x67));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x6D));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x57));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x33));
        memoria.add(((byte) 0x32));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x63));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x70));
        memoria.add(((byte) 0x70));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x63));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x2E));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x49));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x63));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x62));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x4D));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0x2D));
        memoria.add(((byte) 0x44));
        memoria.add(((byte) 0x4F));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0x2E));
        memoria.add(((byte) 0x0D));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0x24));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0x45));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x4C));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0x4C));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xE0));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x0B));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x1C));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x28));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x1C));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x2E));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x78));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0C));
        memoria.add(((byte) 0x06));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xE0));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x98));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xA4));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xB6));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x44));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x4B));
        memoria.add(((byte) 0x45));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0x4E));
        memoria.add(((byte) 0x45));
        memoria.add(((byte) 0x4C));
        memoria.add(((byte) 0x33));
        memoria.add(((byte) 0x32));
        memoria.add(((byte) 0x2E));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x98));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xA4));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xB6));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x45));
        memoria.add(((byte) 0x78));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x63));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x47));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x48));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x46));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x57));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x46));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x47));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x43));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x4D));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x43));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6E));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x4D));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0xA2));
        memoria.add(((byte) 0x1C));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x31));
        memoria.add(((byte) 0xC0));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0x2C));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x0D));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0xF5));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xA3));
        memoria.add(((byte) 0x2C));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x68));
        memoria.add(((byte) 0x30));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x68));
        memoria.add(((byte) 0x1C));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x0C));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x09));
        memoria.add(((byte) 0xC0));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x30));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0xEC));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x57));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x69));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x60));
        memoria.add(((byte) 0x31));
        memoria.add(((byte) 0xC0));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x37));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0xF6));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xA3));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x68));
        memoria.add(((byte) 0xD0));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0x25));
        memoria.add(((byte) 0xD0));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x35));
        memoria.add(((byte) 0xD0));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x35));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x14));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xA1));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x68));
        memoria.add(((byte) 0xD4));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x68));
        memoria.add(((byte) 0xBE));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x09));
        memoria.add(((byte) 0xC0));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x90));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x6A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0xB6));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0xBE));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0xD4));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0xB8));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0x65));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x6F));
        memoria.add(((byte) 0x72));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x60));
        memoria.add(((byte) 0x89));
        memoria.add(((byte) 0xC6));
        memoria.add(((byte) 0x30));
        memoria.add(((byte) 0xC0));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x06));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x46));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xE1));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xEB));
        memoria.add(((byte) 0xF2));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x90));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x30));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xC9));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x0D));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xB9));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xB2));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x4E));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x2D));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xA2));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xCB));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xC4));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xBD));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x07));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xB6));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xAF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xA8));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xA1));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x06));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x9A));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x93));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7D));
        memoria.add(((byte) 0x0B));
        memoria.add(((byte) 0x50));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x2D));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x4C));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xD8));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0xEF));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0xD1));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x27));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x8C));
        memoria.add(((byte) 0x95));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0xA0));
        memoria.add(((byte) 0x86));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x7B));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x42));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x61));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0x96));
        memoria.add(((byte) 0x98));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x47));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xE1));
        memoria.add(((byte) 0xF5));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x2D));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xCA));
        memoria.add(((byte) 0x9A));
        memoria.add(((byte) 0x3B));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0x13));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xCA));
        memoria.add(((byte) 0x9A));
        memoria.add(((byte) 0x3B));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x18));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xE1));
        memoria.add(((byte) 0xF5));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x05));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0x96));
        memoria.add(((byte) 0x98));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xF2));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x42));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xDF));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0xA0));
        memoria.add(((byte) 0x86));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x27));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xB9));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xA6));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x64));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x93));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x52));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x58));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x7A));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x15));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x10));
        memoria.add(((byte) 0x40));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xB9));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x51));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xA2));
        memoria.add(((byte) 0xFD));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x5B));
        memoria.add(((byte) 0x59));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x0D));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0x34));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0x94));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x2D));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0x09));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x30));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0xDB));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x39));
        memoria.add(((byte) 0x7F));
        memoria.add(((byte) 0xD7));
        memoria.add(((byte) 0x2C));
        memoria.add(((byte) 0x30));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0xD0));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x0C));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0xBF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0x3C));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xEB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0xCC));
        memoria.add(((byte) 0x0C));
        memoria.add(((byte) 0x7F));
        memoria.add(((byte) 0xA8));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x34));
        memoria.add(((byte) 0x33));
        memoria.add(((byte) 0x33));
        memoria.add(((byte) 0xF3));
        memoria.add(((byte) 0x7C));
        memoria.add(((byte) 0xA0));
        memoria.add(((byte) 0x88));
        memoria.add(((byte) 0xC7));
        memoria.add(((byte) 0xB8));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0xF8));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x7F));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x13));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x07));
        memoria.add(((byte) 0x7E));
        memoria.add(((byte) 0x0E));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0x7F));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x8F));
        memoria.add(((byte) 0x76));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB9));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x88));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x74));
        memoria.add(((byte) 0x04));
        memoria.add(((byte) 0x01));
        memoria.add(((byte) 0xC1));
        memoria.add(((byte) 0xEB));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x29));
        memoria.add(((byte) 0xC8));
        memoria.add(((byte) 0x91));
        memoria.add(((byte) 0x88));
        memoria.add(((byte) 0xF8));
        memoria.add(((byte) 0x51));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xC3));
        memoria.add(((byte) 0xFD));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x5B));
        memoria.add(((byte) 0x59));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0x4A));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x51));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x7A));
        memoria.add(((byte) 0xFC));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x20));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x73));
        memoria.add(((byte) 0xFC));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x6C));
        memoria.add(((byte) 0xFC));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x5B));
        memoria.add(((byte) 0x59));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x07));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0x25));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x07));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0x11));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x89));
        memoria.add(((byte) 0xC8));
        memoria.add(((byte) 0xB9));
        memoria.add(((byte) 0x0A));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0xBA));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x3D));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x7D));
        memoria.add(((byte) 0x08));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xD8));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xD8));
        memoria.add(((byte) 0xEB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0xF7));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x89));
        memoria.add(((byte) 0xC1));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x85));
        memoria.add(((byte) 0xE6));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0xDD));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0xD6));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x85));
        memoria.add(((byte) 0xCD));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xB0));
        memoria.add(((byte) 0x2D));
        memoria.add(((byte) 0x51));
        memoria.add(((byte) 0x53));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0xFD));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x5B));
        memoria.add(((byte) 0x59));
        memoria.add(((byte) 0xB3));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0xE9));
        memoria.add(((byte) 0xBB));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x03));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0xB2));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x80));
        memoria.add(((byte) 0xFB));
        memoria.add(((byte) 0x02));
        memoria.add(((byte) 0x75));
        memoria.add(((byte) 0x0C));
        memoria.add(((byte) 0x81));
        memoria.add(((byte) 0xF9));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x00));
        memoria.add(((byte) 0x0F));
        memoria.add(((byte) 0x84));
        memoria.add(((byte) 0xA1));
        memoria.add(((byte) 0xFE));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x51));
        memoria.add(((byte) 0xE8));
        memoria.add(((byte) 0x14));
        memoria.add(((byte) 0xFD));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0xFF));
        memoria.add(((byte) 0x59));
        memoria.add(((byte) 0x89));
        memoria.add(((byte) 0xC8));
        memoria.add(((byte) 0xC3));
    }
}
