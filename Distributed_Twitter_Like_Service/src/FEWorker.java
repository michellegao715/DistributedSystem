import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class FEWorker extends Worker {
	//username length between 3-15
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{3,15}$");
	
	private String primaryHostname;
	private final Client client;
	private final TweetsStore cache;
	private final int portOfBE;
	
	public FEWorker(Socket clientSocket, String primaryHostname, Client client, TweetsStore cache) {
		super(clientSocket);
		this.primaryHostname = primaryHostname;
		this.client = client;
		this.cache = cache;
		
		/*Get port of BE and hostname of BE from configuration file */
		Path keys = Paths.get("keys.json");	
		String jsonText = KeysJsonParser.jsonFileTojsonText(keys);
		HTTPConstants.HTTPMethod method = null;
		portOfBE= Integer.parseInt(KeysJsonParser.getText(jsonText, "BackEndPORT"));
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
		
		String path = request.path;
		if(!path.equals("/") && !path.equals("/signup") && !path.equals("/login") &&
				!path.equals("/create") && !path.equals("/search") && !path.equals("/delete") && !path.equals("/logout") &&
				!path.equals("/tweets")){
			return new Response(404,"Not Found", "");
		}
		
		if(path.equals("/")) {
			return handleIndex(request);
		} else if(path.equals("/signup")) {
			if(request.isPOSTRequest())
				return handleSignUpPost(request);
			else
				return handleSignUpGet(request);
		} else if (path.equals("/login")) {
			if(request.isPOSTRequest())
				return handleLogInPost(request);
			else
				return handleLogInGet(request);
		} else if (path.equals("/create")) {
			return handleCreate(request);
		} else if (path.equals("/search")) {
			return handleSearch(request);
		} else if (path.equals("/delete")) {
			if(request.isPOSTRequest())
				return handleDeletePost(request);
			else
				return handleDeleteGet(request);
		} else if (path.equals("/logout")) {
			return handleLogOut(request);
		}
		else { // handle /tweets
			/*Handle Add Tweet request from client : POST /tweets. 
			construct add tweet request(sending to BE:POST /tweet  body:{"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}  */
			if(request.isPOSTRequest())
				return handleTweetPost(request);
			else
				return handleTweetGet(request);
		}
	}
	
	/*Add new tweet*/
	private Response handleTweetPost(Request request) {
		String tweet = getFormParams(request.body).get("tweet");
		System.out.println(request.body);
		System.out.println(tweet);
		if(!tweet.contains("#")){
			return new Response(400, "Bad Request", "");
		}
		//construct body {"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}
		JSONObject requestbodyToBE=new JSONObject();
		for(String s: tweet.split(" ")){
			if(s.startsWith("#") && s.equals("#"))
				return new Response(400, "Bad Request", "");
		}
		requestbodyToBE.put("tweet", tweet);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "tweets");
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.POST, primaryHostname, portOfBE, "/", params, request.cookies, requestbodyToBE.toString()));
			return new Response(200,"OK",constructCreateSuccessfulResponseHTML());
		} catch (Exception e) {
			//in case current primary BE dies, and BE hasn't(got enough) time to elect a new primary, 
			// at this moment if client send a request, there is exception.
			return new Response(500, "Internal Error", "");
		}
	}
	
	/*Search tweet*/
	private Response handleTweetGet(Request request) {
		Map<String, String> params = request.params;
		params.put("type", "tweets");
		String hashtag = params.get("q");
		params.put("v", cache.getHashtagVersion(hashtag));
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.GET, primaryHostname, portOfBE, "/", params, request.cookies, ""));
		} catch (Exception e) {
			return new Response(500, "Internal Error", "");
		}
		/*Tweets in BE have been modified.*/
		if (response.statusCode == 200) {
			JSONObject body = stringToJSON(response.body);
			JSONArray tweets = (JSONArray) body.get("tweets");
			String v = (String) body.get("v");
			ArrayList<TweetsStore.Tweet> tweetsArray = new ArrayList<TweetsStore.Tweet>();
			for (int i = 0;i < tweets.size();++i) {
				tweetsArray.add(cache.new Tweet((JSONObject) tweets.get(i)));
			}
			cache.updateHashtagCache(tweetsArray, hashtag, v);
			//change to HTML
			return new Response(200,"OK",constructTweetResponseHTML(hashtag, tweetsArray));
			
		} else {  /*304 Not Modified.*/
			//change to HTML
			return new Response(200,"OK",constructTweetResponseHTML(hashtag, cache.getByHashtag(hashtag)));
		}
	}
	
	private boolean isLoggedIn(Request request) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "verify");
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.POST, primaryHostname, portOfBE, "/", params, request.cookies, request.body));
			if (response.body.equals("TRUE"))
				return true;
			else
				return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	private Response handleIndex(Request request) {
		if (isLoggedIn(request))
			return new Response(200,"OK",constructLoggedInIndexResponseHTML(request.cookies.get("username")));
		else
			//redirect to Welcome page if not successfully logged in 
			return new Response(200,"OK",constructLoggedOutIndexResponseHTML());
	}
	
	private Response handleSignUpGet(Request request) {
		return new Response(200,"OK",constructSignUpResponseHTML());
	}
	
	private Response handleSignUpPost(Request request) {
		Map<String, String> formParams = getFormParams(request.body);
		String username = formParams.get("username");
		String password = formParams.get("password");
		if (username.isEmpty() || password.isEmpty() || !USERNAME_PATTERN.matcher(username).matches())
			return new Response(200, "OK", constructUsernamePasswordInvalidHTML());
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "signup");
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.POST, primaryHostname, portOfBE, "/", params, request.cookies, request.body));
			if (response.statusCode == 201) {
				Map<String, String> cookies = new HashMap<String, String>();
				cookies.put("username", getFormParams(request.body).get("username"));
				cookies.put("sessionId", response.body);
				return new Response("/", cookies);
			} else if (response.statusCode == 406) {
				return new Response(200, "OK", constructUsernameExistHTML());
			} else {
				return new Response(500, "Internal Error", "");
			}
		} catch (Exception e) {
			return new Response(500, "Internal Error", "");
		}
	}
	
	private Response handleLogInGet(Request request) {
		return new Response(200,"OK",constructLogInResponseHTML());
	}
	
	private Response handleLogInPost(Request request) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "login");
		Map<String, String> formParams = getFormParams(request.body);
		String username = formParams.get("username");
		String password = formParams.get("password");
		params.put("username", username);
		params.put("password", password);
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.POST, primaryHostname, portOfBE, "/", params, new HashMap<String, String>(), request.body));
			if (response.statusCode == 200) {
				Map<String, String> cookies = new HashMap<String, String>();
				cookies.put("username", username);
				cookies.put("sessionId", response.body);
				return new Response("/", cookies);
			}
			else
				return new Response(200,"OK",constructLogInFailResponseHTML());
		} catch (Exception e) {
			return new Response(200,"OK",constructLogInFailResponseHTML());
		}
	}
	
	private Response handleCreate(Request request) {
		return new Response(200,"OK",constructCreateResponseHTML());
	}
	
	private Response handleSearch(Request request) {
		return new Response(200,"OK",constructSearchResponseHTML());
	}
	
	private Response handleDeleteGet(Request request) {
		String username = request.cookies.get("username");
		Map<String, String> params = new HashMap<String, String>();
		params.put("type", "tweets");
		params.put("v", cache.getUsernameVersion(username));
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.GET, primaryHostname, portOfBE, "/", params, request.cookies, ""));
		} catch (Exception e) {
			return new Response(500, "Internal Error", "");
		}
		/*Tweets in BE have been modified.*/
		if (response.statusCode == 200) {
			JSONObject body = stringToJSON(response.body);
			JSONArray tweets = (JSONArray) body.get("tweets");
			String v = (String) body.get("v");
			ArrayList<TweetsStore.Tweet> tweetsArray = new ArrayList<TweetsStore.Tweet>();
			for (int i = 0;i < tweets.size();++i) {
				tweetsArray.add(cache.new Tweet((JSONObject) tweets.get(i)));
			}
			cache.updateUsernameCache(tweetsArray, username, v);
			//change to HTML
			return new Response(200,"OK",constructDeleteResponseHTML(tweetsArray));
			
		} else {  /*304 Not Modified.*/
			//change to HTML
			return new Response(200,"OK",constructDeleteResponseHTML(cache.getByUsername(request.cookies.get("username"))));
		}
	}
	
	private Response handleDeletePost(Request request) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("type", "delete");
		params.put("id", getHiddenFormParams(request.body).get("id"));
		params.put("tweet", getHiddenFormParams(request.body).get("tweet"));
		Response response = null;
		try {
			//send delete request to Backend, and wait for response.
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.GET, primaryHostname, portOfBE, "/", params, request.cookies, ""));
		} catch (Exception e) {
			return new Response(500, "Internal Error", "");
		}
		if (response.statusCode == 200) {
			return new Response("/delete");
		} else {
			return new Response(500, "Internal Error", "");
		}
	}
	
	private Response handleLogOut(Request request) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", "logout");
		Response response = null;
		try {
			response = client.sendRequest(new Request(HTTPConstants.HTTPMethod.POST, primaryHostname, portOfBE, "/", params, request.cookies, request.body));
			if (response.statusCode == 200) {
				Map<String, String> cookies = new HashMap<String, String>();
				cookies.put("username", "");
				cookies.put("sessionId", "");
				return new Response("/", cookies);
			} else {
				return new Response(500, "Internal Error", "");
			}
		} catch (Exception e) {
			return new Response(500, "Internal Error", "");
		}
	}
	
	private String constructLoggedOutIndexResponseHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Welcome</h1>\n"+
				"<a href=\"/signup\">Sign Up</a>\n"+
				"<a href=\"/login\">Log In</a>\n"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructLoggedInIndexResponseHTML(String username) {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Welcome " + username + "</h1>\n"+
				"<a href=\"/create\">Create new tweet</a>\n"+
				"<br><br>"+
				"<a href=\"/search\">Search by hashtag</a>\n"+
				"<br><br>"+
				"<a href=\"/delete\">Delete my tweets</a>\n"+
				"<br><br>"+
				"<form action=\"/logout\" method=\"POST\">"+
				"<input type=\"submit\" value=\"Log Out\">"+
				"</form>"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructSignUpResponseHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Sign Up</h1>\n"+
				"<form action=\"/signup\" method=\"POST\">"+
				"Username: <br>"+
				"<input type=\"text\" name=\"username\">"+
				"<br>"+
				"Password: <br>"+
				"<input type=\"password\" name=\"password\">"+
				"<br><br>"+
				"<input type=\"submit\" value=\"Submit\">"+
				"</form>"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructUsernameExistHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"Username exists! Please sign up using a different username\n"+
				"<br>"+
				"<a href=\"/signup\">Back</a>\n"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructUsernamePasswordInvalidHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Invalid username / password</h1>\n"+
				"<br>"+
				"Username / password couldn not be empty\n"+
				"<br>"+
				"Length of username should be between 3 and 15\n"+
				"<br>"+
				"Username should start with english character\n"+
				"<br>"+
				"Username could only contain english character, digit, '_' and '-'\n"+
				"<br>"+
				"<a href=\"/signup\">Back</a>\n"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructLogInResponseHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Log In</h1>\n"+
				"<form action=\"/login\" method=\"POST\">"+
				"Username: <br>"+
				"<input type=\"text\" name=\"username\">"+
				"<br>"+
				"Password: <br>"+
				"<input type=\"password\" name=\"password\">"+
				"<br><br>"+
				"<input type=\"submit\" value=\"Submit\">"+
				"</form>"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructLogInFailResponseHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Log in failed</h1>\n"+
				"Username / password doesn't match our record"+
				"<br><br>"+
				"<a href=\"/\">Back</a>\n"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructCreateResponseHTML() { 
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Create new tweet</h1>\n"+
				"<form action=\"/tweets\" method=\"POST\" enctype=\"text/plain\">"+		//encode html to plain text (%23 -> #, + -> space)
				"Tweet: <br>"+
				"<input type=\"text\" name=\"tweet\">"+
				"<br><br>"+
				"<input type=\"submit\" value=\"Submit\">"+
				"</form>"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructCreateSuccessfulResponseHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Tweet created successfully!</h1>\n"+
				"<a href=\"/\">Back</a>\n"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructSearchResponseHTML() {
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Search tweet by hashtag</h1>\n"+
				"<form action=\"/tweets\" method=\"GET\">"+
				"Hashtag: <br>"+
				"<input type=\"text\" name=\"q\">"+
				"<br><br>"+
				"<input type=\"submit\" value=\"Submit\">"+
				"</form>"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructDeleteResponseHTML(List<TweetsStore.Tweet> tweets) {
		String tweetsHTML = "";
		for(TweetsStore.Tweet tweet : tweets)
			tweetsHTML += ("[Author: " + tweet.username + "] " + tweet.tweet + 
					"<form action=\"/delete\" method=\"POST\" enctype=\"text/plain\">" +
					"<input type=\"hidden\" name=\"id\" value=\"" + tweet.id + "\">"+
					"<input type=\"hidden\" name=\"tweet\" value=\"" + tweet.tweet + "\">"+
					"<input type=\"submit\" value=\"Delete\">" +
					"</form><br>\n");
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<body>\n"+
				"<h1>Choose a tweet to delete</h1>\n"+
				tweetsHTML +
				"<br>"+
 				"<a href=\"/\">Back</a>\n"+
				"</body>\n"+
				"</html>";
	}
	
	private String constructTweetResponseHTML(String hashtag, List<TweetsStore.Tweet> tweets) {
		String tweetsHTML = "";
		for(TweetsStore.Tweet tweet : tweets)
			tweetsHTML += ("<li>" + "[Author: " + tweet.username + "] " + tweet.tweet + "</li>\n");
		return "<!DOCTYPE html>\n"+
				"<html>\n"+
				"<head><title>Hello</title></head>\n"+
				"<body>\n"+
				"<h1>Search Result</h1>\n"+
				"<p>Hashtag:</p>\n"+
				hashtag+
				"<p>Searching Result:</p>\n"+
				"<ul>\n"+
				tweetsHTML +
				"</ul>\n"+
				"<br><br>"+
 				"<a href=\"/\">Back</a>\n"+
				"</body>\n"+
				"</html>";
	}
}
