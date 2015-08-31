public class TweetCreationReplicationRunner extends ReplicationRunner {
	private TweetsStore tweetStore;
	private String tweet;
	private String username;
	
	public TweetCreationReplicationRunner(TweetsStore tweetStore, String tweet, String username) {
		super();
		this.tweetStore = tweetStore;
		this.tweet = tweet;
		this.username = username;
	}	
	
	@Override
	public void performReplication() {
		tweetStore.add(tweet, username);
	}
}
