import java.io.*;
import java.net.*;
import java.util.*;

/* * Server to process ping requests over UDP. */
public class ReliableUdpSender { 
	private static final double LOSS_RATE = 0.3; 
	private static final int AVERAGE_DELAY = 100; // milliseconds 

	private static Random random;
	private static DatagramSocket socket;

	//Buffer para tratar pacotes duplicados
	private static ArrayList<String> received_buffer;
	private static ArrayList<String> send_buffer;


	public static void main(String[] args) throws Exception {
	// Get command line argument. 
		if (args.length != 3) { 
			System.out.println("Required arguments: server address, server port and message");
			return; 
		} 
		int port = 8697;
		InetAddress server_address = InetAddress.getByName(args[0]);
		int server_port = Integer.parseInt(args[1]);
		String line_message = args[2];

		Scanner scan = new Scanner(System.in);
		// Numero de envios da mensagem
		System.out.print("Number of message submissions: ");
		int n = scan.nextInt();

		received_buffer = new ArrayList<String>();
		send_buffer = new ArrayList<String>();
		
		scan.close();
		
		// Create random number generator for use in simulating packet loss and network delay. 
		random = new Random();
		
		// Create a datagram socket for receiving and sending UDP packets through the port specified on the command line.
		socket = new DatagramSocket(port);

		// Inicia a contagem do tempo (timestamp)
		long initial_time = System.currentTimeMillis();

		// String criada para armazenar temporariamente o valor da mensagem recebida
		String data;

		// Loop para tentar enviar 10 requisicoes UDP e tentar receber as 10 respostas do servidor 
		for (int i = 0; i < n; i++) { 

			long sent_time = System.currentTimeMillis() - initial_time;
			//System.out.println(current_time);

			// Cria a mensagem que será enviada pelo UDP
			String pack = line_message + " " + i + " " + (int)sent_time + " "+ (char)13 + (char)10;
			byte[] message = pack.getBytes();
			send_buffer.add(pack);
			
			// Cria o Datagrama que contem a mensagem e que será enviado para o servidor
			DatagramPacket request = new DatagramPacket(message, message.length, server_address, server_port);

			try{
				// Simular uma perda de pacote. 
				if (random.nextDouble() < LOSS_RATE) { 
					throw new IOException();
				} 
				// Simulate network delay. 
				Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));
				socket.send(request);
				System.out.println(" Request sent.");

				// Create a datagram packet to hold incomming UDP packet. 
				DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
				//Estabelece o tempo limite (em milisegundos) que permanece bloqueado aguardando o pacote
				socket.setSoTimeout(1000);


				// Block until the host receives a UDP packet and the time not exceeded the timeout. 
				socket.receive(reply);

				data = new String(reply.getData());
				
				// Verifica se o pacote ja foi recebido/enviado
				if(send_buffer.contains(data) || received_buffer.contains(data)){
					//System.out.println("Contem o pacote");
				}else{
					// Print the recieved data. 
					received_buffer.add(data);
					printData(reply);
				}

			}catch(SocketTimeoutException ste){
				System.out.println(" packet lost.");
				resend_message(request);
			}catch(IOException ioe){
				System.out.println(" Erro to sent the packet");
				resend_message(request);
			}	

			// Cria a mensagem que será enviada como resposta ao ACK recebido
			String ack_reply_message;
			if(i == n-1){
				ack_reply_message = "FIN " + (char)13 + (char)10;
			}else{
				ack_reply_message = "Sequence " + (i+1) + (char)13 + (char)10;
				send_buffer.add(ack_reply_message);
			}
			byte[] sequence = ack_reply_message.getBytes();
			
			// Cria o Datagrama que contem a mensagem e que será enviado para o servidor
			DatagramPacket ack_reply = new DatagramPacket(sequence, sequence.length, server_address, server_port);

			try{
				// Simular uma perda de pacote. 
				if (random.nextDouble() < LOSS_RATE) { 
					throw new IOException();
				} 
				// Simulate network delay. 
				Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));
				socket.send(ack_reply);
				System.out.println(" Request sent.");

				// Create a datagram packet to hold incomming UDP packet. 
				DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
				//Estabelece o tempo limite (em milisegundos) que permanece bloqueado aguardando o pacote
				socket.setSoTimeout(1000);

				// Block until the host receives a UDP packet and the time not exceeded the timeout. 
				socket.receive(reply);

				data = new String(reply.getData());

				// Verifica se o pacote ja foi recebido/enviado
				if(send_buffer.contains(data) || received_buffer.contains(data)){
					//System.out.println("Contem o pacote");
				}else{
					// Print the recieved data. 
					received_buffer.add(data);
					printData(reply);
				}

			}catch(SocketTimeoutException ste){
				System.out.println(" packet lost.");
				resend_message(request);
			}catch(IOException ioe){
				System.out.println(" Erro to sent the packet");
				resend_message(ack_reply);
			}

		}

		System.out.println(send_buffer); 
	}

	public static void resend_message(DatagramPacket dp) throws Exception{
		boolean ack = false;
		String data;

		while(ack == false){

			try{
				// Simulate network delay. 
				Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));
				socket.send(dp);
				System.out.println(" Request resent.");

				data = new String(dp.getData());
				if(data.contains("FIN"))
					break;

				// Create a datagram packet to hold incomming UDP packet. 
				DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
				//Estabelece o tempo limite (em milisegundos) que permanece bloqueado aguardando o pacote
				socket.setSoTimeout(1000);		

				// Simular uma perda de pacote. 
				if (random.nextDouble() < LOSS_RATE) { 
					throw new SocketTimeoutException();
				} 
				// Block until the host receives a UDP packet and the time not exceeded the timeout. 
				socket.receive(reply);

				data = new String(reply.getData());

				// Verifica se o pacote ja foi recebido/enviado
				if(send_buffer.contains(data) || received_buffer.contains(data)){
					//System.out.println("Contem o pacote");
				}else{
					// Print the recieved data. 
					received_buffer.add(data);
					printData(reply);
				}
				ack = true;

			}catch(SocketTimeoutException ste){
				System.out.println(" packet lost.");
				ack = false;
				continue;
				//resend_message(request); //Pode ser feito com recursividade
			}catch(IOException ioe){
				System.out.println(" Erro to sent the packet");
				ack = false;
				continue;
			}
		}
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