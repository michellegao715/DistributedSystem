import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class TweetsStore {
	public class Tweet {
		long id;
		String username;
		String tweet;

		public Tweet(long id, String username, String tweet) {
			this.id = id;
			this.username = username;
			this.tweet = tweet;
		}
		
		public Tweet(JSONObject obj) {
			this.id = Long.parseLong((String) obj.get("id"));
			this.username = (String) obj.get("username");
			this.tweet = (String) obj.get("tweet");
		}
		
		public JSONObject toJSONObject() {
			JSONObject res = new JSONObject();
			res.put("id", String.valueOf(id));
			res.put("username", username);
			res.put("tweet", tweet);
			return res;
		}

		@Override 
		public boolean equals(Object aThat) {
			Tweet that = (Tweet) aThat;
			if(this.id == that.id && this.username.equals(that.username) && this.tweet.equals(that.tweet))
				return true;
			else
				return false;
		}

		@Override 
		public int hashCode() {
			return (int) this.id + 31 * this.username.hashCode() + 31 * 31 * this.tweet.hashCode();
		}

		@Override
		public String toString() {
			return "id: " + id + ", username: " + username + ", tweet: " + tweet;
		}
	}
	
	private static final SecureRandom random = new SecureRandom();
	private static int nextId = 1;
	private static Map<String, String> hashtagVersions = new HashMap<String, String>();
	private static Map<String, String> usernameVersions = new HashMap<String, String>();

	/*Reference:http://tutorials.jenkov.com/java-util-concurrent/concurrentmap.html*/
	private final ConcurrentHashMap<String, ArrayList<Tweet>> hashtagToTweets;
	private final ConcurrentHashMap<String, ArrayList<Tweet>> usernameToTweets;

	public TweetsStore() {
		this.hashtagToTweets = new ConcurrentHashMap<>();
		this.usernameToTweets = new ConcurrentHashMap<>();
	}
	
	//{"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}
	public synchronized void add(String tweet, String username) {
		add(tweet, username, nextId++);
	}
	
	public synchronized void add(String tweet, String username, long id) {
		List<String> hashtagList = new ArrayList<String>();
		for(String s: tweet.split(" ")){
			if(s.startsWith("#")) {
				hashtagList.add(s.substring(1));
			}
		}
		Tweet newTweet = new Tweet(id, username, tweet);
		for(String hashtag : hashtagList) {
			if(hashtagToTweets.containsKey(hashtag)){
				hashtagToTweets.get(hashtag).add(newTweet);
			} else {
				ArrayList<Tweet> tweets = new ArrayList<Tweet>();
				tweets.add(newTweet);
				hashtagToTweets.put(hashtag, tweets);
			}
			incrementHashtagVersion(hashtag);
		}
		if (usernameToTweets.containsKey(username)) {
			usernameToTweets.get(username).add(newTweet);
		} else {
			ArrayList<Tweet> tweets = new ArrayList<Tweet>();
			tweets.add(newTweet);
			usernameToTweets.put(username, tweets);
		}
		incrementUsernameVersion(username);
	}

	public synchronized void updateHashtagCache(ArrayList<Tweet> tweets, String hashtag, String version) {
		hashtagToTweets.put(hashtag, tweets);
		hashtagVersions.put(hashtag, version);
	}
	
	public synchronized void updateUsernameCache(ArrayList<Tweet> tweets, String username, String version) {
		usernameToTweets.put(username, tweets);
		usernameVersions.put(username, version);
	}
	
	/*When lauching a new BE, the new BE will ask for current primaryBE for data store.*/
	public synchronized void replicateStore(JSONObject obj) {
		JSONArray hashtagWithVersions = (JSONArray) obj.get("hashtagWithVersions");
		for(int i = 0;i < hashtagWithVersions.size();++i) {
			JSONObject hashtagWithVersion = (JSONObject) hashtagWithVersions.get(i);
			String hashtag = (String) hashtagWithVersion.get("hashtag");
			String version = (String) hashtagWithVersion.get("version");
			ArrayList<Tweet> tweetList = new ArrayList<Tweet>(); 
			JSONArray tweets = (JSONArray) hashtagWithVersion.get("tweets");
			for(int j = 0;j < tweets.size();++j) {
				Tweet tweet = (Tweet) tweets.get(j);
				tweetList.add(tweet);
			}
			updateHashtagCache(tweetList, hashtag, version);
		}
		
		JSONArray usernameWithVersions = (JSONArray) obj.get("usernameWithVersions");
		for(int i = 0;i < usernameWithVersions.size();++i) {
			JSONObject usernameWithVersion = (JSONObject) usernameWithVersions.get(i);
			String username = (String) usernameWithVersion.get("username");
			String version = (String) usernameWithVersion.get("version");
			ArrayList<Tweet> tweetList = new ArrayList<Tweet>(); 
			JSONArray tweets = (JSONArray) usernameWithVersion.get("tweets");
			for(int j = 0;j < tweets.size();++j) {
				Tweet tweet = (Tweet) tweets.get(j);
				tweetList.add(tweet);
			}
			updateUsernameCache(tweetList, username, version);
		}
	}

	/*Deal with FE request of searching tweets with specific hashtag.*/
	public synchronized ArrayList<Tweet> getByHashtag(String hashtag) {
		if(hashtagToTweets.keySet().contains(hashtag)){
			return hashtagToTweets.get(hashtag);
		}
		return new ArrayList<Tweet>();
	}
	
	public synchronized ArrayList<Tweet> getByUsername(String username) {
		if(usernameToTweets.keySet().contains(username)){
			return usernameToTweets.get(username);
		}
		return new ArrayList<Tweet>();
	}
	
	public synchronized void delete(long id, String tweet, String username) {
		Tweet t = new Tweet(id, username, tweet);
		List<String> hashtagList = new ArrayList<String>();
		for(String s: tweet.split(" ")){
			if(s.startsWith("#")) {
				hashtagList.add(s.substring(1));
			}
		}
		for(String hashtag : hashtagList) {
			hashtagToTweets.get(hashtag).remove(t);
			incrementHashtagVersion(hashtag);
		}
		usernameToTweets.get(username).remove(t);
		incrementUsernameVersion(username);
	}

	public synchronized String getHashtagVersion(String hashtag) {
		if (hashtagVersions.containsKey(hashtag))
			return hashtagVersions.get(hashtag);
		else
			return "";
	}
	
	public synchronized String getUsernameVersion(String username) {
		if (usernameVersions.containsKey(username))
			return usernameVersions.get(username);
		else
			return "";
	}
	
	public synchronized void incrementHashtagVersion(String hashtag) {
		if (hashtagVersions.containsKey(hashtag)) {
			long version = Long.parseLong(hashtagVersions.get(hashtag));
			hashtagVersions.put(hashtag, String.valueOf(version + 1));
		} else
			hashtagVersions.put(hashtag, "1");
	}
	
	public synchronized void incrementUsernameVersion(String username) {
		if (usernameVersions.containsKey(username)) {
			long version = Long.parseLong(usernameVersions.get(username));
			usernameVersions.put(username, String.valueOf(version + 1));
		} else
			usernameVersions.put(username, "1");
	}
	
	public synchronized JSONArray hashtagWithVersionsJSON() {
		JSONArray result = new JSONArray();
		for (String hashtag : hashtagVersions.keySet()) {
			JSONObject hashtagWithVersion = new JSONObject();
			hashtagWithVersion.put("hashtag", hashtag);
			hashtagWithVersion.put("version", hashtagVersions.get(hashtag));
			JSONArray tweets = new JSONArray();
			for (Tweet tweet : hashtagToTweets.get(hashtag))
				tweets.add(tweet.toJSONObject());
			hashtagWithVersion.put("tweets", tweets);
			result.add(hashtagWithVersion);
		}
		return result;
	}
	
	public synchronized JSONArray usernameWithVersionsJSON() {
		JSONArray result = new JSONArray();
		for (String username : usernameVersions.keySet()) {
			JSONObject usernameWithVersion = new JSONObject();
			usernameWithVersion.put("username", username);
			usernameWithVersion.put("version", usernameVersions.get(username));
			JSONArray tweets = new JSONArray();
			for (Tweet tweet : usernameToTweets.get(username))
				tweets.add(tweet.toJSONObject());
			usernameWithVersion.put("tweets", tweets);
			result.add(usernameWithVersion);
		}
		return result;
	}
 }
