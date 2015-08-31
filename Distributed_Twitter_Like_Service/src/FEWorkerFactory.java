import java.net.Socket;

public class FEWorkerFactory extends WorkerFactory {
	private String primaryHostname;
	private final Client client;
	private final TweetsStore cache;
	
	public FEWorkerFactory(String primaryHostname, Client client, TweetsStore cache) {
		this.primaryHostname = primaryHostname;
		this.client = client;
		this.cache = cache;
	}
	
	@Override
	public Worker createWorker(Socket clientSocket) {
		return new FEWorker(clientSocket, primaryHostname, client, cache);
	}
	
	public void setPrimary(String primaryHostname) {
		this.primaryHostname = primaryHostname;
	}
}
