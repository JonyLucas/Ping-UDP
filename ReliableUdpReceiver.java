import java.io.*;
import java.net.*;
import java.util.*;

/* * Server to process ping requests over UDP. */
public class ReliableUdpReceiver { 
	private static final double LOSS_RATE = 0.3; 
	private static final int AVERAGE_DELAY = 100; // milliseconds

	private static Random random = new Random();
	private static DatagramSocket socket; 
	
	public static void main(String[] args) throws Exception { 
	// Get command line argument. 
		if (args.length != 1) { 
			System.out.println("Required arguments: port");
			return; 
		} 
		int port = Integer.parseInt(args[0]);
		socket = new DatagramSocket(port);
		
		// Processing loop. 
		while (true) { 
			// Create a datagram packet to hold incomming UDP packet. 
			DatagramPacket request = new DatagramPacket(new byte[1024], 1024); 

			// Block until the host receives a UDP packet. 
			socket.receive(request); 

			// Print the recieved data. 
			printData(request);

			// Create a ACK packet. 
			InetAddress clientHost = request.getAddress(); 
			int clientPort = request.getPort();
			byte[] buf = request.getData();
			String ack_message = "ACK " + new String(buf);
			buf = ack_message.getBytes();
			DatagramPacket ack = new DatagramPacket(buf, buf.length, clientHost, clientPort);

			try{
				// Decide whether to reply, or simulate packet loss. 			
				if (random.nextDouble() < LOSS_RATE) { 
					throw new IOException();
				} 

				// Simulate network delay. 
				Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));
				socket.send(ack);
				System.out.println(" Reply sent.");
			}catch(IOException ioe){
				System.out.println(" Erro to sent the packet");
				resend_message(ack);
			}

			// Create a datagram packet to hold incomming UDP packet. 
			DatagramPacket reply_ack = new DatagramPacket(new byte[1024], 1024);
			//Estabelece o tempo limite (em milisegundos) que permanece bloqueado aguardando o pacote
			socket.setSoTimeout(1000);

			try{
				// Block until the host receives a UDP packet and the time not exceeded the timeout. 
				socket.receive(reply_ack);

				// Print the recieved data.
				printData(reply_ack);

			}catch(SocketTimeoutException ste){
				System.out.println(" packet lost.");
				resend_message(request);
			}
		}

	}

	public static void resend_message(DatagramPacket dp) throws Exception{
		boolean ack = false;

		while(ack == false){

			try{
				// Simulate network delay. 
				Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));
				socket.send(dp);
				System.out.println(" Request resent.");
			}catch(IOException ioe){
				System.out.println(" Erro to sent the packet");
				ack = false;
				continue;
			}

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
				ack = true;
			}catch(SocketTimeoutException ste){
				System.out.println(" packet lost.");
				ack = false;
				continue;
				//resend_message(request); //Pode ser feito com recursividade
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