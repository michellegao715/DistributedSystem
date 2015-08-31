import java.net.Socket;

public class BullyWorkerFactory extends WorkerFactory {
	private BullyService service;
	private DiscoveryService.ServerType serverType;

	public BullyWorkerFactory(BullyService service, DiscoveryService.ServerType serverType) {
		this.service = service;
		this.serverType = serverType;
	}

	@Override
	public Worker createWorker(Socket clientSocket) {
		return new BullyWorker(clientSocket, service, serverType);
	}
}


