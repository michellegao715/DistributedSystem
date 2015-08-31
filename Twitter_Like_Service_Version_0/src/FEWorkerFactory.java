import java.net.Socket;

public class FEWorkerFactory extends WorkerFactory {
	private final Client client;
	private final DataStore cache;
	
	public FEWorkerFactory(Client client, DataStore cache) {
		this.client = client;
		this.cache = cache;
	}
	
	@Override
	public Worker createWorker(Socket clientSocket) {
		return new FEWorker(clientSocket, client, cache);
	}
}
