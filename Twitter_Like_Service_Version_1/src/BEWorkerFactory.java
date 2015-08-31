import java.net.Socket;

public class BEWorkerFactory extends WorkerFactory {
	private final DataStore store; //every BE worker share this only datastore.
	private BackEnd backend;
	private int portOfBE;
	private boolean isPrimary;
	private Client client;

	public BEWorkerFactory(DataStore dataStore, BackEnd backend, int portOfBE, boolean isPrimary) {
		this.store = dataStore;
		this.backend = backend;
		this.portOfBE = portOfBE;
		this.isPrimary = isPrimary;
		this.client = new Client();
	}

	@Override
	public Worker createWorker(Socket clientSocket) {
		return new BEWorker(clientSocket, store, backend, portOfBE, isPrimary, client);
	}
	
	public void setIsPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	
	/*
	public void dumpDataStore() {
		store.dump();
	} */
}
