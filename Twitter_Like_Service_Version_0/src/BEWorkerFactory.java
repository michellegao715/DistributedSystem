import java.net.Socket;


public class BEWorkerFactory extends WorkerFactory {
	private final DataStore store; //every BE worker share this only datastore.

	public BEWorkerFactory(DataStore dataStore) {
		this.store = new DataStore();
	}

	@Override
	public Worker createWorker(Socket clientSocket) {
		return new BEWorker(clientSocket, store);
	}
}
