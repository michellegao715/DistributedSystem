import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/*   /signup --> handleSignUpPost()  + replication
 * 	 /login  --> handleLogInPost() 
 * 	 /logout --> handleLogOutPost()  + replication
 * 	 /tweets(POST) --> handleTweetPost() + replication 
 * 	 /tweets(GET)  --> handleTweetGET()  
 *   /delete       --> handleDeletePost()  + replication
 */ 
public class BEWorker extends Worker {
	private final TweetsStore tweetsStore;
	private final UserStore userStore;
	private final SessionStore sessionStore;
	private BackEnd backend;
	private int portOfBE;
	private boolean isPrimary;
	private Client client;

	public BEWorker(Socket clientSocket, TweetsStore tweetsStore, UserStore userStore, SessionStore sessionStore, BackEnd backend, int portOfBE, boolean isPrimary, Client client) {
		super(clientSocket);
		this.tweetsStore = tweetsStore;
		this.userStore = userStore;
		this.sessionStore = sessionStore;
		this.backend = backend;
		this.portOfBE = portOfBE;
		this.isPrimary = isPrimary;
		this.client = client;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Response constructResponse(Request request) {
		if(!request.isPOSTRequest() && !request.isGETRequest()){
			return new Response(404, "Not Found", "");
		}
		if(!request.path.equals(BackEnd.NORMAL_PATH) && !request.path.equals(BackEnd.REPLICATE_PATH)){
			return new Response(404, "Not Found", "");
		}
		
		if(!request.params.containsKey("type"))
			return new Response(404, "Not Found", ""); 
		
		if (isPrimary && request.isPOSTRequest())
			replicateData(request);
		boolean isReplication = request.path.equals(BackEnd.REPLICATE_PATH);

		if(request.params.get("type").equals("signup")) {
			return handleSignUpPost(request, isReplication);
		} else if(request.params.get("type").equals("login")) {
			return handleLogInPost(request);
		} else if(request.params.get("type").equals("verify")) {
			return handleVerifyPost(request);
		} else if(request.params.get("type").equals("logout")) {
			return handleLogOutPost(request, isReplication);
		} else if(request.params.get("type").equals("delete")) {
			return handleDeletePost(request, isReplication);
		} else if(request.params.get("type").equals("tweets")) {
			if (request.isPOSTRequest()) {
				return handleTweetPost(request, isReplication);
			} else {
				return handleTweetGet(request);
			}
		} else {
			return new Response(404, "Not Found", "");
		}
	}
	
	/*handle sign up post request, and replicate the sign up information to all BE servers.*/
	private Response handleSignUpPost(Request request, boolean isReplication) {
		Map<String, String> params = getFormParams(request.body);
		String username = params.get("username");
		String password = params.get("password");
		if (userStore.contains(username))
			return new Response(406, "Username exists", "");
		String token = "";
		if (isReplication) { //replicate new user info in UserStore and SessionStore
			ReplicationRunner runner = new UserCreationReplicationRunner(userStore, sessionStore, username, password);
			new Thread(runner).start();
		} else { 
			userStore.add(username, password);
			token = sessionStore.create(username);
		}
		return new Response(201, "Account created", token);
	}
	
	private Response handleLogInPost(Request request) {
		Map<String, String> params = getFormParams(request.body);
		String username = params.get("username");
		String password = params.get("password");
		if (userStore.matches(username, password)) {
			String token = sessionStore.create(username);
			return new Response(200, "Log in successful", token);
		} else
			return new Response(406, "Username/Password does not match", "");
	}
	
	private Response handleVerifyPost(Request request) {
		if(!request.cookies.containsKey("username") || !request.cookies.containsKey("sessionId"))
			return new Response(200, "OK", "FALSE");
		String username = request.cookies.get("username");
		String token = request.cookies.get("sessionId");
		if(sessionStore.find(token) == null || !sessionStore.find(token).equals(username))
			return new Response(200, "OK", "FALSE");
		return new Response(200, "OK", "TRUE");
	}
	
	private Response handleLogOutPost(Request request, boolean isReplication) {
		String sessionId = request.cookies.get("sessionId");
		if (isReplication) { //replicate new user info in UserStore and SessionStore
			ReplicationRunner runner = new SessionDeletionReplicationRunner(sessionStore, sessionId);
			new Thread(runner).start();
		} else {
			sessionStore.delete(sessionId);
		}
		return new Response(200, "OK", "log out successful");
	}
	
	private Response handleDeletePost(Request request, boolean isReplication) {
		String username = request.cookies.get("username");
		String tweet = "";
		try {
			tweet = java.net.URLDecoder.decode(request.params.get("tweet"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		long id = Long.parseLong(request.params.get("id"));
		if (isReplication) {
			ReplicationRunner runner = new TweetDeletionReplicationRunner(tweetsStore, id, tweet, username);
			new Thread(runner).start();
		} else {
			tweetsStore.delete(id, tweet, username);
		}
		return new Response(200, "OK", "Tweet deleted");
	}
	
	private Response handleTweetPost(Request request, boolean isReplication) {
		String username = request.cookies.get("username");
		JSONObject bodyJson = stringToJSON(request.body);
		String tweet = (String) bodyJson.get("tweet");
		if (isReplication) {
			ReplicationRunner runner = new TweetCreationReplicationRunner(tweetsStore, tweet, username);
			new Thread(runner).start();
		} else {
			tweetsStore.add(tweet, username);
		}
		return new Response(201, "Created", "");
	}
	
	private Response handleTweetGet(Request request) {
		if (!request.params.containsKey("q")) { 
			System.out.println("Getting request from new backend to send the entire datastore data");
			JSONObject body = new JSONObject();
			if (request.cookies.containsKey("username")) {  //request sent by FE to search tweets of username(for deleting tweet)  
				String username = request.cookies.get("username");
				body.put("v", tweetsStore.getUsernameVersion(username));
				JSONArray result = new JSONArray();
				List<TweetsStore.Tweet> tweets = tweetsStore.getByUsername(username);
				for (TweetsStore.Tweet tweet : tweets) {
					result.add(tweet.toJSONObject());
				}
				body.put("tweets", result);
			} else {  //request sent by other BE to replicate.  
				body.put("hashtagWithVersions", tweetsStore.hashtagWithVersionsJSON());
				body.put("usernameWithVersions", tweetsStore.usernameWithVersionsJSON());
			}
			return new Response(200, "Ok", body.toString());
		}
		else {//request sent by FE to search by hashtag. 
			String hashtag = request.params.get("q");
			String v = request.params.get("v");
			if (tweetsStore.getHashtagVersion(hashtag).equals(v)) {
				return new Response(304, "Not modified", "");
			} else {
				ArrayList<TweetsStore.Tweet> tweets = tweetsStore.getByHashtag(hashtag);
				JSONObject body = new JSONObject();
				body.put("q", hashtag);
				body.put("v", tweetsStore.getHashtagVersion(hashtag));
				JSONArray tweetsArray = new JSONArray();
				for (TweetsStore.Tweet tweet : tweets) {
					tweetsArray.add(tweet.toJSONObject());
				}
				body.put("tweets", tweetsArray);
				return new Response(200, "Ok", body.toString());
			}
		}
	}

	//replicate data to secondary BEs and send replicated completion message to make sure everyone has received the replicate.
	private void replicateData(Request feRequest) {
		for(InetAddress addr : backend.getDS().getBackendIPs()) {
			Request request = new Request(HTTPConstants.HTTPMethod.POST, addr.getHostAddress(), portOfBE, BackEnd.REPLICATE_PATH, feRequest.params, feRequest.cookies, feRequest.body);
			client.sendRequest(request);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		replicateCompleted();
	}
	
	private void replicateCompleted() {
		System.out.println("Send replication complete message");
		byte[] sendData = "REPLICATE COMPLETED".getBytes();
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setBroadcast(true);
			InetAddress broadcastIP = InetAddress.getByName("255.255.255.255");
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, broadcastIP, BackEnd.REPLICATE_ANNOUNCE_PORT);
			clientSocket.send(sendPacket);
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
