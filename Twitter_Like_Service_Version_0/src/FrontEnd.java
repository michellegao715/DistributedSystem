import java.nio.file.Path;
import java.nio.file.Paths;


public class FrontEnd {
	private final Server server;
	
	public FrontEnd(int port) {
		server = new Server(port, new FEWorkerFactory(new Client(), new DataStore()));
	}
	
	public static void main(String [] args) {
		Path keys = Paths.get("keys.json");	
		String jsonText = KeysJsonParser.jsonFileTojsonText(keys);
		int portOfFE= Integer.parseInt(KeysJsonParser.getText(jsonText, "FrontEndPORT"));
		FrontEnd fe = new FrontEnd(portOfFE);
		//FrontEnd fe = new FrontEnd(12345);
		fe.server.start();
	}
}
