import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BackEnd {
	public static final String NORMAL_PATH = "/";
	public static final int REPLICATE_ANNOUNCE_PORT = 9049;
	public static final String REPLICATE_PATH = "/replication";
	private TweetsStore tweetsStore;
	private UserStore userStore;
	private SessionStore sessionStore;
	private BEWorkerFactory factory;
	private Thread serverThread;
	private BullyService bs;
	private DiscoveryService ds;
	private InetAddress primaryIP;

	public BackEnd(int port, InetAddress primaryIP) {
		this.tweetsStore = new TweetsStore();
		this.userStore = new UserStore();
		this.sessionStore = new SessionStore();
		this.factory = new BEWorkerFactory(tweetsStore, userStore, sessionStore, this, portOfBE(), false);
		this.serverThread = new Thread(new Server(port, factory));
		this.primaryIP = primaryIP;
		this.bs = createBS();
		this.ds = createDS();
	}
	
	public int portOfBE() {
		Path keys = Paths.get("keys.json");	
		String jsonText = KeysJsonParser.jsonFileTojsonText(keys);
		return Integer.parseInt(KeysJsonParser.getText(jsonText, "BackEndPORT"));
	}

	//setter method, to create a bully service for BeckEnd server by passing current BeckEnd object to BullySerive
	public BullyService createBS() {
		return new BullyService(this, null, DiscoveryService.ServerType.BACKEND);
	}

	public DiscoveryService createDS() {
		return new DiscoveryService(primaryIP, DiscoveryService.ServerType.BACKEND, this);
	}

	//getter method, return BeckEnd's BullyService.
	public BullyService getBS() {
		return bs;
	}

	public DiscoveryService getDS(){
		return ds;
	}

	public void start() {
		System.out.println("Starting Backend ....");
		ds.start();
		bs.start();
		boolean isPrimary = ds.isPrimary();
		factory.setIsPrimary(isPrimary);
		if (!isPrimary) {   
			setUpDataStore();
		}
		serverThread.start();
	}
	// called when launching a new BE at runtime, and the BE will ask for whole datastore(tweetsStore) from current primaryBE.
	private void setUpDataStore() {
		System.out.println("Setting up data store");
		Client client = new Client();
		Response response = null;
		try { 
			Map<String, String> params = new HashMap<String, String>();
			params.put("type", "tweets");
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.GET, primaryIP.getHostAddress(), portOfBE(), BackEnd.NORMAL_PATH, params, new HashMap<String, String>(), ""));
			System.out.println("Response from primary backend(setUpDataStore): " + response.body);
			JSONObject body = stringToJSON(response.body);
			tweetsStore.replicateStore(body);
		} catch (Exception e) {
			System.out.println("Exception when setUpDataStore");
		}
	}
	
	private JSONObject stringToJSON(String s) {
		JSONParser parser=new JSONParser();
		JSONObject obj = new JSONObject();
		try {
			obj = (JSONObject) parser.parse(s);
		} catch (ParseException e) {
			System.out.println("Having problem parsing request body to JSON object.");
		}
		return obj;
	}
	
	public void setPrimary(InetAddress primaryIP) {
		ds.setPrimary(primaryIP);
		//BEWorkerFactory set the IP to primary.
		factory.setIsPrimary(ds.isPrimary());
	}
}
