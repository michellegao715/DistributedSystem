
public class ServerTest {
	public static void main(String [] args) {
		Server server = new Server(12345, new WorkerFactory());
		server.start();
	}
}
