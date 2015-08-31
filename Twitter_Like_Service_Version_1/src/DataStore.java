import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class DataStore {
	/*Reference:http://tutorials.jenkov.com/java-util-concurrent/concurrentmap.html*/
	private final  ConcurrentHashMap<String,ArrayList<String>> store;
	public DataStore() {
		this.store = new ConcurrentHashMap<>();
	}
	//{"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}
	public synchronized void add(String hashtag, String tweet) {
		if(store.containsKey(hashtag)){
			store.get(hashtag).add(tweet);
		}else {
			ArrayList<String> tweets = new ArrayList<String>();
			tweets.add(tweet);
			store.put(hashtag, tweets);
		}
	}
	
	public synchronized void updataCache(String hashtag, ArrayList<String> tweets) {
		this.store.put(hashtag, tweets);
	}
	
	/*Deal with FE request of searching tweets with specific hashtag.*/
	public synchronized ArrayList<String> get(String hashtag) {
		if(store.keySet().contains(hashtag)){
			return store.get(hashtag);
		}
		return new ArrayList<String>();
	}
	
	public synchronized Set<String> keySet() {
		return store.keySet();
	}
	
	public synchronized int getVersion(String hashtag) {
		return get(hashtag).size();
	}
	
	
	public void dump() {
		for (String hashtag : store.keySet()) {
			System.out.println("hashtag: " + hashtag);
			for (String tweet : store.get(hashtag)) {
				System.out.println("===>>> tweet: " + tweet);
			}
		}
	}
}
