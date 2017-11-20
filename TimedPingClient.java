import java.io.*;
import java.net.*;
import java.util.*;

/* * Server to process ping requests over UDP. */
public class TimedPingClient { 
	private static final double LOSS_RATE = 0.3; 
	private static final int AVERAGE_DELAY = 100; // milliseconds
	
	private static InetAddress server_address;
	private static int server_port;
	private static long initial_time = System.currentTimeMillis();
	
	private static DatagramSocket socket;
	
	private Timer timer;
	
	public static void main(String[] args) throws Exception {
	// Get command line argument. 
		if (args.length != 2) { 
			System.out.println("Required arguments: server address and server port");
			return; 
		} 
		
		new TimedPingClient(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
		 
	}
	
	public TimedPingClient(InetAddress host, int port){
		server_address = host;
		server_port = port;
		
		try {
			socket = new DatagramSocket(8697);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		timer = new Timer();
		timer.schedule(new RemindTask(), 1000, 1000);
	}
	
	public class RemindTask extends TimerTask{
		
		// Especifica o numero de pacotes
		int numero_pacotes = 10;
		
		// Create random number generator for use in simulating packet loss and network delay. 
		Random random = new Random();

		int i = 0;

		// Variavel para calcular o Estimated RTT (valor medio), o RTT minimo e o RTT maximo - Questao extra
		double estimatedRtt = 0;
		double minRtt = Double.MAX_VALUE;
		double maxRtt = Double.MIN_VALUE;

		public void run(){
			// Loop para tentar enviar 10 requisicoes UDP e tentar receber as 10 respostas do servidor 
			//int i = 0;

			if(i < numero_pacotes) { 

				long sent_time = System.currentTimeMillis() - initial_time;
				//System.out.println(current_time);

				// Cria a mensagem que será enviada pelo UDP
				String pack = "PING " + i + " " + (int)sent_time + " "+ (char)13 + (char)10;
				byte[] message = pack.getBytes();
				
				// Cria o Datagrama que contem a mensagem e que será enviado para o servidor
				DatagramPacket request = new DatagramPacket(message, message.length, server_address, server_port);

				// Simulate network delay. 
				try {
					Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));
				} catch (InterruptedException e1) {
					System.out.println("Erro in simulate network delay.");
				}

				try {
					socket.send(request);
					System.out.println(" Request sent.");
				} catch (IOException e) {
					System.out.println(" Erro to send the request segment");
				}
				
				// Create a datagram packet to hold incomming UDP packet. 
				DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);

				try{
					//Estabelece o tempo limite (em milisegundos) que permanece bloqueado aguardando o pacote
					socket.setSoTimeout(1000);
					
					// Simular uma perda de pacote. 
					if (random.nextDouble() < LOSS_RATE) { 
						throw new SocketTimeoutException();
					} 
					// Block until the host receives a UDP packet and the time not exceeded the timeout. 
					socket.receive(reply);

					long receive_time = System.currentTimeMillis() - initial_time;
					long sampleRtt = receive_time - sent_time;

					// Calcula o RTT medio
					if(estimatedRtt == 0){ // Primeiro segmento
						estimatedRtt = sampleRtt;
					}else{
						estimatedRtt = 0.875 * estimatedRtt + 0.125 * sampleRtt;
					}

					// Calcula o Rtt minimo
					if(minRtt > sampleRtt){
						minRtt = sampleRtt;
					}

					// Calcula o Rtt maximo
					if(maxRtt < sampleRtt){
						maxRtt = sampleRtt;
					}

					// Print the recieved data. 
					TimedPingClient.printData(reply);

				}catch(SocketTimeoutException | SocketException ste){
					System.out.println(" packet lost.");
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

				i++;

			}else{
				System.out.println("\nEstimated Rtt: " + estimatedRtt);
				System.out.println("Minimum Rtt: " + minRtt);
				System.out.println("Maximum Rtt: " + maxRtt);
				
				timer.cancel();
			}
		}
	}

	

	/* * Print ping data to the standard output stream. */ 
	public static void printData(DatagramPacket request) throws Exception { 
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