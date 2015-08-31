public class SessionDeletionReplicationRunner extends ReplicationRunner {
	private SessionStore sessionStore;
	private String sessionId;
	
	public SessionDeletionReplicationRunner(SessionStore sessionStore, String sessionId) {
		super();
		this.sessionStore = sessionStore;
		this.sessionId = sessionId;
	}	
	
	@Override
	public void performReplication() {
		sessionStore.delete(sessionId);
	}
}
