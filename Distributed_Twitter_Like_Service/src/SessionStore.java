import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
	private static final SecureRandom random = new SecureRandom();
	private final ConcurrentHashMap<String, String> store;	// token -> username

	public SessionStore() {
		this.store = new ConcurrentHashMap<String, String>();
	}

	public synchronized String create(String username) {
		String token = nextSessionId();
		store.put(token, username);
		return token;
	}

	public synchronized void delete(String token) {
		store.remove(token);
	}

	public synchronized String find(String token) {
		return store.get(token);
	}

	private static String nextSessionId() {
		return new BigInteger(130, random).toString(32);
	}
	
	public synchronized void dump() {
		System.out.println("Dumping session store");
		for(String key : store.keySet()) {
			System.out.println(key + " : " + store.get(key));
		}
	}
}
