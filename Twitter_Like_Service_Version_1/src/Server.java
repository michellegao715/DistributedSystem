import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Server implements Runnable {
	private final int port;
	private final WorkerFactory factory;
	private ServerSocket serverSocket;

	
	public Server(int port, WorkerFactory factory) {
		this.port = port;
		this.factory = factory;
		this.serverSocket = null;
	}
	
	public void start() {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + port, e);
		}
		
		System.out.println("Server started on port " + port);
		
		while(true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				throw new RuntimeException("Error accepting client connection", e);
			}
			new Thread(factory.createWorker(clientSocket)).start();
			
		}
	}

	@Override
	public void run() {
		start();
	}
}
