package compiladorpl0;

public class AnalizadorSintactico {
    private AnalizadorLexico alex;
    
    public AnalizadorSintactico(AnalizadorLexico alex) {
        this.alex = alex;
    }
    
    public void programa (){
        
    }
    
    public void bloque (){
        
    }
    
    public void proposicion (){
        case "begin":
        simbolo = alex.escanear();
        proposicion();
        while(simbolo.equals(";")){
            simb = alex.escanear();
        } 
        proposicion();
        
        if(simbolo.equals("end")){
            simbolo = alex.escanear();
        } else indicarError();
    }
    
    public void condicion (){
        
    }
    
    public void expresion (){
        switch(simbolo){
            case "+": 
                alex.escanear();
                break;
            case "-": 
                break;            
        }
    }
    
    public void termino (){
        
    }
    
    public void factor (){
        
    }
    
    public void indicarError(){
        
    }
}
