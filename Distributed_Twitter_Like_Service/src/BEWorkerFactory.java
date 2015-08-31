import java.net.Socket;

public class BEWorkerFactory extends WorkerFactory {
	private final TweetsStore tweetsStore; //every BE worker share this only datastore.
	private final UserStore userStore;
	private final SessionStore sessionStore;
	private BackEnd backend;
	private int portOfBE;
	private boolean isPrimary;
	private Client client;

	public BEWorkerFactory(TweetsStore tweetsStore, UserStore userStore, SessionStore sessionStore, BackEnd backend, int portOfBE, boolean isPrimary) {
		this.tweetsStore = tweetsStore;
		this.userStore = userStore;
		this.sessionStore = sessionStore;
		this.backend = backend;
		this.portOfBE = portOfBE;
		this.isPrimary = isPrimary;
		this.client = new Client();
	}

	@Override
	public Worker createWorker(Socket clientSocket) {
		return new BEWorker(clientSocket, tweetsStore, userStore, sessionStore, backend, portOfBE, isPrimary, client);
	}
	
	public void setIsPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	
	/*
	public void dumpDataStore() {
		store.dump();
	} */
}
