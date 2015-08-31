import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/* BEServer will get its status(primary/secondary) from BullyService.
 * After each election, bullyservice will give primary IP to Discovery Service 
 * to let it check if primary IP stays alive.
 * The Client of BullyServie is to send message, Server is to open a serversocket and listen for request. 
 * */
public class BullyService {
	public static String startMsg = "START";
	public static String okMsg = "OK";   //send when received start elections message and have higher ID.
	public static String electedMsg = "ELECTED";  //finish election and have a new leader.
	public static int port = 7788;
	
	private BackEnd be;
	private FrontEnd fe;
	private Thread serverThread;
	private Client client;
	private boolean duringElection;

	public BullyService(BackEnd be, FrontEnd fe, DiscoveryService.ServerType serverType) {
		this.be = be;
		this.fe = fe;
		serverThread = new Thread(new Server(port, new BullyWorkerFactory(this, serverType)));
		client = new Client();
		this.duringElection = false;
		System.out.println("BullyService running on beckend");
	}
	
	public void start() {
		serverThread.start();//open a thread to run Server(of bully service). or it will not end in Server's start() .
	}

	/* Given live IPs(from Discovery Service), start to elect new leader.
	 * update primaryIP  */
	public void startElect() {
		//if no BackEnd passed to BullyService(which means this is FrontEnd's BullyService running) ,then don't startElect.
		if (be == null)
			return;
		System.out.println("Trying to start election");
		if (duringElection)
			return;
		System.out.println("Election started");
		duringElection = true;
		InetAddress localIP = null;
		try {
			localIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		Flag flag = new Flag();
		System.out.println("Get list of IPs from DiscoveryService");
		System.out.println(be.getDS().getBackendIPs().size());
		for(InetAddress addr : be.getDS().getBackendIPs()){
			System.out.println(addr.getHostAddress());
			//BullyService pass the flag to ElectionRunner, if r receives response, set the value of flag to be true; 
			ElectionRunner runner = new ElectionRunner(addr, flag);
			runner.run();
		}
		System.out.println("Sleep 10s before declaring winning election");
		/* set timer 10s ,and then check if flag is false which means there is no 
		 * response of election request.So,localhost is the new primary and inform front ends*/
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Wake up from sleep");
		//If nobody else sends me "ok", then I am the winner(after 10s after starting election).
		if(flag.getFlag() == false) { 
			System.out.println("I won!");
			setPrimary(localIP);
			sendElectedMsg();
		}
		duringElection = false;
	}

	//called when a new leader has been elected, and use Discovery Service to setPrimary. 
	public void setPrimary(InetAddress newleader) {
		if (be != null)
			be.setPrimary(newleader);
		else //inform FE the new elected primary, 
			//that's the reason to pass FE to bully service(kind of bad idea, try to fix later).
			fe.setPrimary(newleader);
	}

	//When elected a new primary, send elected Msg to BE and FE's bully service. (that's reason FE should have a BullyService running to listen for the elected Msg)/ 
	private void sendElectedMsg() {
		for(InetAddress addr : be.getDS().getBackendIPs()) {
			Request request = new Request(HTTPConstants.HTTPMethod.POST, addr.getHostAddress(), BullyService.port, "", new HashMap<String,String>(), new HashMap<String, String>(), BullyService.electedMsg);
			client.sendRequest(request);
		}
		for(InetAddress addr : be.getDS().getFrontendIPs()) {
			Request request = new Request(HTTPConstants.HTTPMethod.POST, addr.getHostAddress(), BullyService.port, "", new HashMap<String,String>(), new HashMap<String, String>(), BullyService.electedMsg);
			client.sendRequest(request);
		}
		System.out.println("BullyService sending out elected message");
	}
}
