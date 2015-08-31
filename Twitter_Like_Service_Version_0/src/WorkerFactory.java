import java.net.Socket;

public class WorkerFactory {
	//Overriden by BEWorkerFactory and FEWorkerFactory.
	public Worker createWorker(Socket clientSocket) {
		return new Worker(clientSocket);
	}
}
