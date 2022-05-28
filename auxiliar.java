import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class auxiliar {
    static FileWriter archivoEscribir;

    public static void writeAction(boolean tipo, String aEscribir) throws IOException{
        try{
            if(tipo){archivoEscribir = new FileWriter("accionesServer.log", true);}
            else{archivoEscribir = new FileWriter("accionesClient.log", true);}
            archivoEscribir.write(aEscribir + "\n");
            archivoEscribir.close(); 
        }catch(IOException e){
            System.out.println(e);
        }

    }
    public static void writeError(boolean tipo, String aEscribir) throws IOException {
        try{
            if(tipo){archivoEscribir = new FileWriter("erroresServer.log", true);}
            else{archivoEscribir = new FileWriter("erroresClient.log", true);}
            archivoEscribir.write(aEscribir + "\n");
            archivoEscribir.close();
        }catch(IOException e){
            System.out.println(e);
        }
    }

    
    public static boolean comprobarDirectorio(String path){
		File directory = new File(path);
		if ( !directory.exists() ) {
			System.out.println("El directorio no existe");
			return false;
		}else if( !directory.isDirectory() ) {
			System.out.println("No es un directorio");
			return false;
		}
		return true;
	}

	public static boolean comandoIncorrecto(String[] args){
        switch(args[0]){
            case "put":
            case "get":
                if (args.length == 2){ return false;}
                break;
            case "list":
            case "exit":
                if (args.length == 1){ return false;}
                break;
        }
        return true;
    }

	public static boolean checkFile(String path) {
		File archivo = new File(path);
		if(archivo.exists() && archivo.canRead()) {
			return true;
		}
		return false;
	}
	
	public static HashMap<String, String> comprobarParametros(String[] args, boolean tipo) {
		HashMap<String, String> values = new HashMap<String, String>();
		
		String carpeta = tipo ? "carpeta_servidor" : "carpeta_cliente";
		
		values.put("puerto","4332");
        values.put(carpeta, "./");
        if(tipo){
            values.put("max_clientes","10");
        }else{
            values.put("modo","");
            values.put("host","localhost");
        }
        
        for(String arg:args){
            String[] argumento = arg.split("=");
            if(argumento.length != 2){
                System.out.println(arg + " not valid");
                continue;
            }
            if(notAnInt(argumento[0],argumento[1])){
                continue;
            }
            if(values.replace(argumento[0],argumento[1]) == null){
                System.out.println("Invalid argument for" +argumento[0]);
                continue;
            }
        }
        if( !comprobarDirectorio(values.get(carpeta)) ) {
            System.out.println("Usando directorio default");
            values.put(carpeta, "./");
        }
        
        System.out.println("argument=value -> " + values);
        return values;
	}
	
	private static boolean notAnInt(String key, String value){
        if(key.equals("puerto") || key.equals("max_clientes")){
            try{ 
                Integer.parseInt(value); 
            }catch (Exception e) { 
                System.out.println("Invalid value for argument -" + key + "- Using default"); 
                return true;
            }
        }
        return false;
    }
}
