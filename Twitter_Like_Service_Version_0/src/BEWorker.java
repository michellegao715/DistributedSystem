import java.net.Socket;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class BEWorker extends Worker {
	private final DataStore store;
	
	public BEWorker(Socket clientSocket, DataStore store) {
		super(clientSocket);
		this.store = store;
	}

	@Override
	protected Response constructResponse(Request request) {
		if(!request.isPOSTRequest() && !request.isGETRequest()){
			return new Response(404, "Not Found", "");
		}
		if(!request.path.equals("/tweets")){
			return new Response(404, "Not Found", "");
		}
		
		if(request.method.equals(HTTPConstants.HTTPMethod.POST)) {
			saveToDataStore(request.body);
			System.out.println("BE send back response");
			return new Response(201, "Created", "");
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

	private void saveToDataStore(String body) {
		JSONObject bodyJson = stringToJSON(body);
		JSONArray hashtags = (JSONArray) bodyJson.get("hashtags");
		String tweet = (String) bodyJson.get("tweet");
		for (int i = 0;i < hashtags.size();++i) {
			String hashtag = (String) hashtags.get(i);
			store.add(hashtag, tweet);
		}
	}
}
