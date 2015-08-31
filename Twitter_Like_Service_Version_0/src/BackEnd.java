import java.nio.file.Path;
import java.nio.file.Paths;


public class BackEnd {
	private final Server server;
	
	public BackEnd(int port) {
		server = new Server(port, new BEWorkerFactory(new DataStore()));
	}
	
	public static void main(String [] args) {
		Path keys = Paths.get("keys.json");	
		String jsonText = KeysJsonParser.jsonFileTojsonText(keys);
		int portOfFE= Integer.parseInt(KeysJsonParser.getText(jsonText, "BackEndPORT"));
		BackEnd be = new BackEnd(portOfFE);
		//BackEnd be = new BackEnd(12346);
		be.server.start();
	}
}
