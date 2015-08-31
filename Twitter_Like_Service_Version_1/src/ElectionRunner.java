import java.net.InetAddress;
import java.util.HashMap;

public class ElectionRunner implements Runnable{
	private Client client;
	private Request request;
	private Flag flag;
	
	public ElectionRunner(InetAddress addr, Flag flag) {
		this.client = new Client();
		System.out.println("ElectionRunner: host = " + addr.getHostAddress());
		this.request = new Request(HTTPConstants.HTTPMethod.POST, addr.getHostAddress(), BullyService.port, "", new HashMap<String,String>(), BullyService.startMsg);
		this.flag = flag;
	}

	@Override
	public void run() {
		Response response = null;
		try {
			response = client.sendRequest(request);
			if(response.body.equals("OK")){
				flag.setFlag(true);
			}
		} catch (RuntimeException e) {	
		}
	}
}
