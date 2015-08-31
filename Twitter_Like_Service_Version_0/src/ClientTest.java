import java.util.HashMap;


public class ClientTest {
	public static void main(String [] args) {
		System.out.println("Test client:");
		Client client = new Client();
		Request request = new Request(HTTPConstants.HTTPMethod.GET, "localhost", 12345, "/tweet", new HashMap<String, String>(), "");
		//Request request = new Request(HTTPConstants.HTTPMethod.GET, "cs682.cs.usfca.edu", 80, "/assignments/project-1---twitter-v1", new HashMap<String, String>(), "");
		client.sendRequest(request);
	}
}
