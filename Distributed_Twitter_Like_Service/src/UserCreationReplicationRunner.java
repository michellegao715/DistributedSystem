public class UserCreationReplicationRunner extends ReplicationRunner {
	private UserStore userStore;
	private SessionStore sessionStore;
	private String username;
	private String password;
	
	public UserCreationReplicationRunner(UserStore userStore, SessionStore sessionStore, String username, String password) {
		super();
		this.userStore = userStore;
		this.sessionStore = sessionStore;
		this.username = username;
		this.password = password;
	}	
	
	@Override
	public void performReplication() {
		userStore.add(username, password);
		sessionStore.create(username);
	}
}
