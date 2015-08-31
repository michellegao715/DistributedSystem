import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class FEWorker extends Worker {
	private String primaryHostname;
	private final Client client;
	private final DataStore cache;

	public FEWorker(Socket clientSocket, String primaryHostname, Client client, DataStore cache) {
		super(clientSocket);
		this.primaryHostname = primaryHostname;
		this.client = client;
		this.cache = cache;
	}

	@Override
	/*Receive reqeust from FE, create a request and send to BE, and get the response from BE
	 * and then send back to client.*/
	protected Response constructResponse(Request request) {
		if(request == null) {
			return new Response(404,"Not Found", "");
		}
		if(!request.isPOSTRequest() && !request.isGETRequest()){
			return new Response(404,"Not Found", "");
		}
		if(!request.path.equals("/tweets")){
			return new Response(404,"Not Found", "");
		}
		/*Get port of BE and hostname of BE from configuration file */
		Path keys = Paths.get("keys.json");	
		String jsonText = KeysJsonParser.jsonFileTojsonText(keys);
		HTTPConstants.HTTPMethod method = null;
		int portOfBE= Integer.parseInt(KeysJsonParser.getText(jsonText, "BackEndPORT"));
		
		String path = request.path;
		
		/*Handle Add Tweet request from client : POST /tweets. 
construct add tweet request(sending to BE:POST /tweet  body:{"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}  */
		if(request.isPOSTRequest()){
			String requestbody = request.body;
			JSONObject requestbodyFromClient = stringToJSON(requestbody);
			String tweet = requestbodyFromClient.get("text").toString();
			if(!tweet.contains("#")){
				return new Response(400, "Bad Request", "");
			}
	
			//construct body {"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}
			JSONObject requestbodyToBE=new JSONObject();
			JSONArray hashtags = new JSONArray();
			for(String s: tweet.split(" ")){
				if(s.startsWith("#")) {
					if(s.equals("#"))
						return new Response(400, "Bad Request", "");
					else
						hashtags.add(s.substring(1));
				}
			}
			requestbodyToBE.put("tweet", tweet);
			requestbodyToBE.put("hashtags",hashtags);
			Response response = null;
			try {
				response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.POST, primaryHostname, portOfBE, path, new HashMap<String, String>(), requestbodyToBE.toString()));
				return response;
			} catch (Exception e) {
				//in case current primary BE dies, and BE hasn't(got enough) time to elect a new primary, 
				// at this moment if client send a request, there is exception.
				return new Response(500, "Internal Error", "");
			}
		}
		else {
			Map<String, String> params = request.params;
			String hashtag = params.get("q");
			params.put("v", String.valueOf(cache.getVersion(hashtag)));
			Response response = null;
			try {
				response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.GET, primaryHostname, portOfBE, path, params, ""));
			} catch (Exception e) {
				return new Response(500, "Internal Error", "");
			}
			
			/*Tweets in BE have been modified.*/
			if (response.statusCode == 200) {
				JSONObject body = stringToJSON(response.body);
				
				JSONArray tweets = (JSONArray) body.get("tweets");
				ArrayList<String> tweetsArray = new ArrayList<String>();
				for (int i = 0;i < tweets.size();++i) {
					tweetsArray.add((String) tweets.get(i));
				}
				cache.updataCache(hashtag, tweetsArray);
				
				body.remove("v");
				return new Response(200, "Ok", body.toString());
			} else {  /*304 Not Modified.*/
				JSONObject responseToClient = new JSONObject();
				responseToClient.put("q", hashtag);
				JSONArray tweets = new JSONArray();
				for(String tweet: cache.get(hashtag)){
					tweets.add(tweet);
				}
				responseToClient.put("tweets", tweets);
				return new Response(200, "Ok", responseToClient.toString());
			}
		}
	}
}
