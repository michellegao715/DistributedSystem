public class TweetDeletionReplicationRunner extends ReplicationRunner {
	private TweetsStore tweetStore;
	private long id;
	private String tweet;
	private String username;
	
	public TweetDeletionReplicationRunner(TweetsStore tweetStore, long id, String tweet, String username) {
		super();
		this.tweetStore = tweetStore;
		this.id = id;
		this.tweet = tweet;
		this.username = username;
	}	
	
	@Override
	public void performReplication() {
		tweetStore.delete(id, tweet, username);
	}
}
