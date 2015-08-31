import java.net.Socket;

public class WorkerFactory {
	//Overriden by BullyWorkerFactory, BEWorkerFactory,FEWorkerFactory.
	 
	public Worker createWorker(Socket clientSocket) {
		return new Worker(clientSocket);
	}
}
