import java.util.List;


public class ReplicationRunner implements Runnable {
	
	public ReplicationRunner() {
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
			performReplication();
		}
	}
	/*Overriden by SessionDeletionReplicationRunner,TweetCreationReplicationRunner,
	 * TweetDeletionReplicationRunner,UserCreationReplicationRunner*/
	public void performReplication() {
	}
}
