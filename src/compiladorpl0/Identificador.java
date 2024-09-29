package compiladorpl0;

public class Identificador {

    private String nombre;
    private IdentType tipo;
    private Integer valor;

    public Identificador(String nombre, IdentType tipo, Integer valor) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.valor = valor;
    }

    public String getNombre() {
        return nombre;
    }

    public IdentType getTipo() {
        return tipo;
    }

    public Integer getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }
    
    
}
