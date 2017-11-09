import java.io.*;
import java.net.*;
import java.util.*;

/* * Server to process ping requests over UDP. */
public class PingClient { 
	private static final double LOSS_RATE = 0.3; 
	private static final int AVERAGE_DELAY = 100; // milliseconds 
	
	public static void main(String[] args) throws Exception {
	// Get command line argument. 
		if (args.length != 2) { 
			System.out.println("Required arguments: host port, server address and server port");
			return; 
		} 
		int port = 8697;
		InetAddress server_address = InetAddress.getByName(args[0]);
		int server_port = Integer.parseInt(args[1]); 
		
		// Create random number generator for use in simulating packet loss and network delay. 
		Random random = new Random();
		
		// Create a datagram socket for receiving and sending UDP packets through the port specified on the command line.
		DatagramSocket socket = new DatagramSocket(port);

		// Inicia a contagem do tempo (timestamp)
		long initial_time = System.currentTimeMillis();

		// Loop para tentar enviar 10 requisicoes UDP e tentar receber as 10 respostas do servidor 
		for (int i  = 0; i < 10; i++) { 

			long current_time = System.currentTimeMillis() - initial_time;
			//System.out.println(current_time);

			String pack = "PING " + i + " " + (int)current_time + " "+ (char)13 + (char)10;
			byte[] message = pack.getBytes();
			
			// Cria o Datagrama que contem a mensagem e que será enviado para o servidor
			DatagramPacket request = new DatagramPacket(message, message.length, server_address, server_port);

			// Simulate network delay. 
			Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

			socket.send(request);
			System.out.println(" Request sent.");

			// Create a datagram packet to hold incomming UDP packet. 
			DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
			//Estabelece o tempo limite (em milisegundos) que permanece bloqueado aguardando o pacote
			socket.setSoTimeout(1000);

			try{
				// Simular uma perda de pacote. 
				if (random.nextDouble() < LOSS_RATE) { 
					throw new SocketTimeoutException();
				} 
				// Block until the host receives a UDP packet and the time not exceeded the timeout. 
				socket.receive(reply); 

				// Print the recieved data. 
				printData(reply);

			}catch(SocketTimeoutException ste){
				System.out.println(" packet lost.");
			}

		} 
	}


	/** Utilizado para converter um inteiro em um array de bytes **/
	private static byte[] intToByteArray(long n){
		String numero = Long.toString(n);
		char[] array = numero.toCharArray();
		byte[] byteArray = new byte[array.length];
		for(int i = 0; i < array.length; i++){
			byteArray[i] = (byte) array[i];
		}

		return byteArray;
	}

	/* * Print ping data to the standard output stream. */ 
	private static void printData(DatagramPacket request) throws Exception { 
		// Obtain references to the packet's array of bytes. 
		byte[] buf = request.getData(); 

		// Wrap the bytes in a byte array input stream, so that you can read the data as a stream of bytes. 
		ByteArrayInputStream bais = new ByteArrayInputStream(buf); 

		// Wrap the byte array output stream in an input stream reader, so you can read the data as a stream of characters. 
		InputStreamReader isr = new InputStreamReader(bais);

		// Wrap the input stream reader in a bufferred reader, so you can read the character data a line at a time. 
		// (A line is a sequence of chars terminated by any combination of \r and \n.) 
		BufferedReader br = new BufferedReader(isr); 

		// The message data is contained in a single line, so read this line. 
		String line = br.readLine(); 

		// Print host address and data received from it. 
		System.out.println( "Received from " + request.getAddress().getHostAddress() + ": " + new String(line) );
	} 
}