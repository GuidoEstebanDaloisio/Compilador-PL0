package compiladorpl0;

public class Token {
    private final TokenType tipo;
    private final String valor;

    public Token(TokenType tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    public TokenType getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }
    
    @Override
    public String toString() {
        return  "Token{" + "tipo=" + tipo + ", valor='" + valor + '\'' + '}';
    }
}