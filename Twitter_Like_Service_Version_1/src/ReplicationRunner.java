import java.util.List;


public class ReplicationRunner implements Runnable {
	private DataStore datastore;
	private String tweet;
	private List<String> hashtags;
	
	public ReplicationRunner(DataStore datastore, String tweet, List<String> hashtags) {
		this.datastore = datastore;
		this.tweet = tweet;
		this.hashtags = hashtags;
	}	
	
	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		System.out.println("Replication runner: starts running");
		Flag flag = new Flag();
		//flag will be set to true if "REPLICATE COMPLETED" message has been received. 
		Thread receiverThread = new Thread(new ReplicationMessageReceiver(flag));
		receiverThread.start();
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		receiverThread.stop();
		System.out.println("Replication runner: resume after 10s");
		if (flag.getFlag()) {
			System.out.println("Replication runner: save tweet to data store");
			for (String hashtag : hashtags) {
				datastore.add(hashtag, tweet);
			}
		}
	}

}
