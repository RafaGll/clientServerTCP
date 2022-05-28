import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.plaf.synth.SynthOptionPaneUI;

public class client {
    static Scanner teclado = new Scanner(System.in);
	
	static DataOutputStream dataOutput;
	static DataInputStream dataInput = null;
	
	static boolean cliente = false;

	
	public static void main(String[] args) {
		HashMap<String, String> values = auxiliar.comprobarParametros(args, cliente);
		
		String carpeta = values.get("carpeta_cliente");
		String host = values.get("host");
		int puerto = Integer.parseInt(values.get("puerto"));
		String modo = values.get("modo");
		
		
		
		String command;
		String[] arguments;
		
		
		try {
			Socket socket = new Socket(host, puerto);
			dataInput = new DataInputStream(socket.getInputStream());
			if(dataInput.readBoolean()){
				try{auxiliar.writeError(cliente, "Conexión rechazada (client limit)");}catch(Exception a){}
				System.out.println("Conexión rechazada -- (client limit)");
				socket.close();
				return;
			}
			dataOutput = new DataOutputStream(socket.getOutputStream());

			do{
				System.out.print(">");
				command = teclado.nextLine();
				arguments = command.split(" ");
				arguments[0] = arguments[0].toLowerCase();

				if(auxiliar.comandoIncorrecto(arguments)){
					try{auxiliar.writeError(cliente, "Command error: >" + command);}catch(Exception a){}
					System.out.println("GET <file> -- PUT <file> -- LIST -- EXIT");
					continue;
				}
				try{auxiliar.writeAction(cliente, command);}catch(Exception a){}


				dataOutput.writeUTF(String.join(" ", arguments));
				dataOutput.flush();
				
				handleConnection(carpeta, arguments, socket);
			}while( !arguments[0].equals("exit"));
			
		}catch(Exception e) {
			try{auxiliar.writeError(cliente, "Servidor no operativo");}catch(Exception a){}
			System.out.println("No connection with server");
		}
	}
	
	
	private static void handleConnection(String path, String[] arguments, Socket socket) {
		File file;
		try {
			switch(arguments[0]) {
				case "put":

					String sendName = path + "/" + arguments[1];
					file = new File(sendName);

					if(auxiliar.checkFile(sendName)){ 
						// le dice al servidor que el archivo existe
						dataOutput.writeBoolean(true);
						dataOutput.flush();

						if(dataInput.readBoolean()){
							// si el archivo existe en el servidor, decidimos si seguir
							if( !sobreescribir(file.length()) ){
								// si no seguimos, informamos y salimos
								dataOutput.writeBoolean(false);
								dataOutput.flush();
								break;
							}			
						}
						// si el archivo no existe en el servidor o queremos sobreescribir
						// informamos y lo enviamos
						dataOutput.writeBoolean(true);
						dataOutput.flush();
						sendFile(file);
					}else {
						try{auxiliar.writeError(cliente, "El archivo (en cliente) no existe o no se tienen permisos para su lectura");}catch(Exception a){}
						System.out.println("El archivo no existe o no se tienen los permisos suficientes");
						dataOutput.writeBoolean(false);
						dataOutput.flush();
					}
					break;

				case "get":
					// si el archivo no existe en el servidor, salimos
					if(dataInput.readBoolean()) {
						String receiveName = path + "/" + arguments[1];
						file = new File(receiveName);

						if(auxiliar.checkFile(receiveName)){
							// si el archivo también existe en cliente, decidimos si seguir
							if( !sobreescribir(file.length()) ){
								// si no seguimos, informamos y salimos
								dataOutput.writeBoolean(false);
								dataOutput.flush();
								break;
							}
						}
						// si el archivo no existe en cliente o queremos sobreescribir
						// informamos y recibimos el archivo
						dataOutput.writeBoolean(true);
						dataOutput.flush();
						receiveFile(receiveName, socket);

					}else {
						try{auxiliar.writeError(cliente, "El archivo (en servidor) no existe o no se tienen permisos para su lectura");}catch(Exception a){}
						System.out.println("El archivo no existe o no se tienen los permisos suficientes");
					}
					break;

				case "list":
					receiveDirectory();
					break;

				case "exit":
					socket.close();
					System.exit(0);
					break;

				default: break;		
				
			}
		}catch(Exception e) {
			System.out.println(e);
		}
	}

	private static void sendFile(File file) {
		try{
			int bytes = 0;

			dataOutput.writeLong(file.length());
			dataOutput.flush();

			FileInputStream fileInputStream = new FileInputStream(file);		 
			
			byte[] buffer = new byte[4*1024];
			while ((bytes=fileInputStream.read(buffer))!=-1){
				dataOutput.write(buffer,0,bytes);
				dataOutput.flush();
			}
			fileInputStream.close();
		}catch(Exception e){
			System.out.println(e);
		}

		
	}
	private static void receiveFile(String directory, Socket socket) {
		try{
			int bytes = 0;
			FileOutputStream fileOutputStream = new FileOutputStream(directory);
			
			long size = dataInput.readLong();   
			System.out.println("Bytes a recibir: " + size);

			byte[] buffer = new byte[4*1024];
			while (size > 0 && (bytes = dataInput.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
				fileOutputStream.write(buffer,0,bytes);
				size -= bytes;   
			}
			fileOutputStream.close();
			File archivoNuevo = new File(directory);
			System.out.println("Tamaño final: " + archivoNuevo.length() + " bytes");

		}catch(Exception e){
			System.out.println(e);
		}
	}

	private static void receiveDirectory() {
		String archivoImprimir;
		long sizeArchivo;
		try{
			while(true){
				archivoImprimir = dataInput.readUTF();
				if(archivoImprimir.equals("")){
					break;
				}else{
					sizeArchivo = dataInput.readLong();
					double size = (double)sizeArchivo/1024;
					System.out.printf("- %s -- %.2f KB\n", archivoImprimir, size);
				}
			}

		}catch(Exception e){
			System.out.println(e);
		}
	}
	private static boolean sobreescribir(long tam) throws IOException{
		
		if(dataInput.readLong() == tam){
			System.out.println("Se trata del mismo archivo");
			try{auxiliar.writeError(cliente, "Ya existe el mismo archivo en destino");}catch(Exception a){}
		}else{
			try{auxiliar.writeError(cliente, "Archivo con el mismo nombre en destino");}catch(Exception a){}

			System.out.print("Archivo existente, ¿sobreescribir? (S/N)\n>");
			String eleccion = teclado.nextLine();
			if( eleccion.equals("S") || eleccion.equals("s") ){
				try{auxiliar.writeAction(cliente, "Sobreescribir");}catch(Exception a){}
				return true;
			}else{
				try{auxiliar.writeAction(cliente, "No sobreescribir");}catch(Exception a){}
			}
		}
		return false;
	}
}
