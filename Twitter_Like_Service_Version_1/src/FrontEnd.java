import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FrontEnd {
	private FEWorkerFactory factory;
	//!!!Similar to BE, put the server into a thread to run since there is while(true) loop in the Server and it won't stop once started.
	private Thread serverThread;
	private DiscoveryService ds;
	//FE's Bully service doesn't startElect(), it just listens for elected Msg sent from BE's bully service.
	private BullyService bs;
	
	public FrontEnd(int port, InetAddress primaryIP) {
		this.factory = new FEWorkerFactory(primaryIP.getHostAddress(), new Client(), new DataStore());
		this.serverThread = new Thread(new Server(port, factory));
		this.ds = createDS(primaryIP);
		this.bs = createBS();
	}
	
	public DiscoveryService createDS(InetAddress primaryIP) {
		return new DiscoveryService(primaryIP, DiscoveryService.ServerType.FRONTEND, null);
	}
	
	//  FE's bully service just listens and handles the elected Msg, but doesn't startElect(). 
	public BullyService createBS() {
		return new BullyService(null, this, DiscoveryService.ServerType.FRONTEND);
	}
	
	public DiscoveryService getDS() {
		return ds;
	}
	
	public void start() {
		System.out.println("Starting Frontend ....");
		serverThread.start();
		ds.start();
		bs.start();
	}
	
	public void setPrimary(InetAddress primaryIP) {
		factory.setPrimary(primaryIP.getHostAddress());
		ds.setPrimary(primaryIP);
	}
	
	public static void main(String [] args) {
		Path keys = Paths.get("keys.json");	
		String jsonText = KeysJsonParser.jsonFileTojsonText(keys);
		int portOfFE = Integer.parseInt(KeysJsonParser.getText(jsonText, "FrontEndPORT"));
		InetAddress primaryIP = null;
		try {
			primaryIP = InetAddress.getByName(KeysJsonParser.getText(jsonText, "primaryIP"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		FrontEnd fe = new FrontEnd(portOfFE, primaryIP);
		fe.start();
	}
}
