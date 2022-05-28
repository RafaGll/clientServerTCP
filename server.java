import java.net.*;
import java.io.*;
import java.util.HashMap;

public class server {
		
	static String carpeta;
	static int max_clientes;
	static int id = 0;

	static boolean servidor = true;

	public static void main(String[] args) {
		DataOutputStream dataOutput;
		Socket socket = null;
		ServerSocket sSocket = null;
		Clientes cliente;
		
		HashMap<String, String> values = auxiliar.comprobarParametros(args, true);
		
		carpeta = values.get("carpeta_servidor");
		int puerto = Integer.parseInt(values.get("puerto"));
		max_clientes = Integer.parseInt(values.get("max_clientes"));
		
	
		try {
			sSocket = new ServerSocket(puerto);
			System.out.println("Escuchando el puerto: " + puerto);
			
			while(true) {

				socket = sSocket.accept();
				try{
					dataOutput = new DataOutputStream(socket.getOutputStream());
					if(max_clientes < 1) {
						try{auxiliar.writeAction(servidor, "Cliente rechazado (client limit)");}catch(Exception a){}
						dataOutput.writeBoolean(true);
						dataOutput.flush();
						continue;
					}else{
						dataOutput.writeBoolean(false);
						dataOutput.flush();
					}				
				}catch(IOException e){
					System.out.println(e);
				}
				max_clientes--; id++;
				cliente = new Clientes(socket, id);
				cliente.start();
			}
			
		}catch(BindException e) {
			try{auxiliar.writeError(servidor, "Error al iniciar el servidor - Puerto en uso");}catch(Exception a){}

			System.out.println("Puerto " + puerto + " en uso");
			return;
		}catch(Exception e) {
			try{auxiliar.writeError(servidor, "Error al iniciar servidor");}catch(Exception a){}

			System.out.println(e);
			return;
		}
		
	}
	static class Clientes extends Thread {
		int ClientID;
		Socket sClient = null;

		DataInputStream entradaCliente = null;
        DataOutputStream salidaCliente = null;

		
		private Clientes(Socket socket, int id) {
			this.sClient = socket;
			this.ClientID = id;
		}

		public void run() {
			try{auxiliar.writeAction(servidor, "ClientID " + ClientID + " CONECTADO");}catch(Exception a){}

			System.out.println("ClientID " + ClientID + " CONNECTED: " + sClient);
			

			String arguments;
			String[] command;
			try {
				entradaCliente = new DataInputStream(sClient.getInputStream());
        		salidaCliente = new DataOutputStream(sClient.getOutputStream());

				do {
					arguments = entradaCliente.readUTF();
					command = arguments.split(" ");
					try{auxiliar.writeAction(servidor, arguments);}catch(Exception a){}
					handleConnection(command);
				}while( !command[0].equals("exit") );
				
			}catch(IOException e) {
				max_clientes++;
				try{auxiliar.writeError(servidor, "Conexión con ClientID " + ClientID + " perdida");}catch(Exception a){}
				System.out.println("Conexión con ClientID " + ClientID + " perdida");
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		public void handleConnection(String[] command) {
			String directorio;
			File file;
			try {
				switch(command[0]) {
					case "put":
						//recibe del cliente que si archivo existe y puede seguir
						if(entradaCliente.readBoolean()) {
							directorio = carpeta + "/" + command[1];
							file = new File(directorio);

							if( !auxiliar.checkFile(directorio)){
								// si el archivo NO existe en el servidor, informa y lo recibe
								salidaCliente.writeBoolean(false);
								salidaCliente.flush();
								receiveFile(directorio);
								break;
							}
							// si el archivo SI existe en el servidor, informa
							salidaCliente.writeBoolean(true);
							salidaCliente.flush();
							// envía la longitud del archivo
							salidaCliente.writeLong(file.length());
							salidaCliente.flush();
							// envía el archivo si el cliente lo pide
							if(entradaCliente.readBoolean()){
								receiveFile(directorio);
							}else{
								try{auxiliar.writeError(servidor, "Archivo no recibido - Ya existe en destino");}catch(Exception a){}
							}
						}else{
							try{auxiliar.writeError(servidor, "Archivo no recibido - no existe / no se puede leer");}catch(Exception a){}
						}
						break;

					case "get":

						directorio = carpeta + "/" + command[1];
						file = new File(directorio);
						
						if(!auxiliar.checkFile(directorio)) {
							try{auxiliar.writeError(servidor, "Archivo no enviado - no existe / no se puede leer");}catch(Exception a){}

							// si el archivo no existe, informamos y salimos
							salidaCliente.writeBoolean(false);
							salidaCliente.flush();
							break;
						}
						
						// si el archivo existe, informamos
						salidaCliente.writeBoolean(true);
						salidaCliente.flush();
						// enviamos longitud del archivo
						salidaCliente.writeLong(file.length());
						salidaCliente.flush();
						// esperamos para ver si hay que enviar
						if(entradaCliente.readBoolean()){
							sendFile(file);
						}else{
							try{auxiliar.writeError(servidor, "Archivo no enviado - Ya existe en destino");}catch(Exception a){}
						}
							
						break;

					case "list":
						sendDirectory();
						break;

					case "exit":
						System.out.println("ClientID " + ClientID + " DISCONNECTED");
						max_clientes++;
						break;

					default: break;
				}
			}catch(Exception e) {
				System.out.println(e);
			}
		}

		public void receiveFile(String directory) {
			try{
				int bytes = 0;

				long size = entradaCliente.readLong(); 

				FileOutputStream fileOutputStream = new FileOutputStream(directory);
					
				byte[] buffer = new byte[4*1024];
				while (size > 0 && (bytes = entradaCliente.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
					fileOutputStream.write(buffer,0,bytes);
					size -= bytes;   
				}
				fileOutputStream.close();

			}catch(Exception e){
				System.out.println(e);
			}
		}
		

		public void sendFile(File file) {
			try{
				int bytes = 0;
				FileInputStream fileInputStream = new FileInputStream(file);		

				salidaCliente.writeLong(file.length());  
				salidaCliente.flush();

				byte[] buffer = new byte[4*1024];
				while ((bytes=fileInputStream.read(buffer))!=-1){
					salidaCliente.write(buffer,0,bytes);
					salidaCliente.flush();
				}
				fileInputStream.close();
			}catch(Exception e){
				System.out.println(e);
			}

		}

		public void sendDirectory() {
			File path = new File(carpeta);
			File archivo;
			String[] fileList = path.list();	
			try{
				for (int i = 0; i < fileList.length; i++){
					archivo = new File(carpeta, fileList[i]);
					salidaCliente.writeUTF(fileList[i]);
					salidaCliente.flush();
					salidaCliente.writeLong(archivo.length());
					salidaCliente.flush();
				}
				salidaCliente.writeUTF("");
				salidaCliente.flush();
			}catch(Exception e){
				System.out.println(e);
			}
		}
		
	}
}
