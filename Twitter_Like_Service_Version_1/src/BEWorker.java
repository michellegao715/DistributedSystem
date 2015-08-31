import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class BEWorker extends Worker {
	private final DataStore store;
	private BackEnd backend;
	private int portOfBE;
	private boolean isPrimary;
	private Client client;

	public BEWorker(Socket clientSocket, DataStore store, BackEnd backend, int portOfBE, boolean isPrimary, Client client) {
		super(clientSocket);
		this.store = store;
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

		if(request.method.equals(HTTPConstants.HTTPMethod.POST)) {
			if (isPrimary) {
				replicateData(request);
			}
			boolean isReplication = request.path.equals(BackEnd.REPLICATE_PATH);
			saveToDataStore(request.body, isReplication);
			System.out.println("BE send back response");
			return new Response(201, "Created", "");
		}
		else {
			if (!request.params.containsKey("q")) { // request from new backend to get entire datastore data
				System.out.println("Getting request from new backend to send the entire datastore data");
				JSONObject body = new JSONObject();
				JSONArray hashtagToTweets = new JSONArray();
				for (String hashtag : store.keySet()) {
					JSONObject obj = new JSONObject();
					obj.put("hashtag", hashtag);
					JSONArray tweets = new JSONArray();
					for (String tweet : store.get(hashtag)) {
						tweets.add(tweet);
					}
					obj.put("tweets", tweets);
					hashtagToTweets.add(obj);
				}
				body.put("hashtagToTweets", hashtagToTweets);
				return new Response(200, "Ok", body.toString());
			}
			else {
				String hashtag = request.params.get("q");
				int v = Integer.parseInt(request.params.get("v"));
				ArrayList<String> tweets = store.get(hashtag);
				if (tweets.size() == v) {
					return new Response(304, "Not modified", "");
				}
				else {
					JSONObject body = new JSONObject();
					body.put("q", hashtag);
					body.put("v", tweets.size());
					JSONArray tweetsArray = new JSONArray();
					for (String tweet : tweets) {
						tweetsArray.add(tweet);
					}
					body.put("tweets", tweetsArray);
					return new Response(200, "Ok", body.toString());
				}
			}
		}
	}

	//replicate data to secondary BEs and send replicated completion message to make sure everyone has received the replicate.
	private void replicateData(Request feRequest) {
		for(InetAddress addr : backend.getDS().getBackendIPs()) {
			Request request = new Request(HTTPConstants.HTTPMethod.POST, addr.getHostAddress(), portOfBE, BackEnd.REPLICATE_PATH, new HashMap<String,String>(), feRequest.body);
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

	private void saveToDataStore(String body, boolean isReplication) {
		System.out.println("Save to data store");
		JSONObject bodyJson = stringToJSON(body);
		JSONArray hashtags = (JSONArray) bodyJson.get("hashtags");
		String tweet = (String) bodyJson.get("tweet");
		List<String> hashtagList = new ArrayList<String>();
		for (int i = 0;i < hashtags.size();++i) {
			String hashtag = (String) hashtags.get(i);
			hashtagList.add(hashtag);
		}
		if (isReplication) {
			ReplicationRunner runner = new ReplicationRunner(store, tweet, hashtagList);
			new Thread(runner).start();
		} else {
			for (String hashtag : hashtagList) {
				store.add(hashtag, tweet);
			}
		}
	}
}
