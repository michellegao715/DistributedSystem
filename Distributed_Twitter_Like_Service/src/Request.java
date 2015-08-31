import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Request {
	public final HTTPConstants.HTTPMethod method;
	public final String host;
	public final int port;
	public final String path;
	public final Map<String, String> params;
	public final Map<String, String> cookies;
	public final String body;

	public Request(HTTPConstants.HTTPMethod method, 
			String host,
			int port,
			String path,
			Map<String, String> params,
			Map<String, String> cookies,
			String body) {
		this.method = method;
		this.host = host;
		this.port = port;
		this.path = path;
		this.params = params;
		this.cookies = cookies;
		this.body = body;
	}

	/*There are two(or three if there is body) parts of request required for construct related response: method,path(params),(body)*/
	/*Dubugging:POST /tweets?q=this&v=that HTTP/1.1\n{"text":"#this is #tweet"}*/
	public static Request parse(String header, String cookieHeader, String body) {
		//check if there is \n which means there should be body follows it.
//		System.out.println("Body :"+body+".");
		String[] parsedRequest = header.split(" ");
		HTTPConstants.HTTPMethod method = null;
		try{
			method = HTTPConstants.HTTPMethod.valueOf(parsedRequest[0]);
		} catch (Exception e ){
			return null;  //if the method is null(not GET or POST) return null.
		}
		String host = "localhost";  //this info is useless(since "I" am the host)for constructing related response for received request
		String pathAndParams = parsedRequest[1];
//		System.out.println("HTTPVersion :"+HTTPVersion);
		int port = -1;//since I will never use the port for construct response.
		String path = "";		
		/*Check parameters*/
		HashMap<String,String> params = new HashMap<>();		
		if(pathAndParams.contains("?")){
			path = pathAndParams.split("\\?")[0];
			String p = pathAndParams.split("\\?")[1];	
			/*Since parameter can be key=value&, so you can ignore the & in the end*/
			if(p.endsWith("&")) {  
				p = p.substring(0,p.length()-1);
			}
			String parameterString = p;
			if(!parameterString.contains("=")){
				return null;
			}
			String[] parameters = parameterString.split("&");
			for(String s: parameters){
				if (s.split("=").length == 2) {
					String key = s.split("=")[0]; 
					String value = s.split("=")[1];
					if(!key.isEmpty() && !value.isEmpty()){
						params.put(key,value);
					}
				} else if (s.split("=").length == 1) {
					params.put(s, "");
				} else {
					return null;
				}
			}
		} else {  //if there is no parameters, path is everything follows "/"
			path = pathAndParams;
		}
		
		System.out.println("Parsing cookieHeader: " + cookieHeader);
		Map<String, String> cookies = new HashMap<String, String>();
		String[] headerAndCookies = cookieHeader.split(": ");
		if (headerAndCookies.length > 1) {
			cookieHeader = cookieHeader.split(": ")[1];
			String[] cookieEntries = cookieHeader.split("; ");
			for(int i = 0; i < cookieEntries.length; ++i) {
				int idx = cookieEntries[i].indexOf("=");
				cookies.put(cookieEntries[i].substring(0, idx), cookieEntries[i].substring(idx + 1));
			}
		}
		
		/* Get the body from the request:
		 * starts from the 3th string in the array parsedRequest: 
		 * POST
			/tweets?q=this&v=that
			HTTP/1.1
			{"text":"#this
			is
			#tweet"}*/
	
		Request res = new Request(method,host,port,path,params,cookies,body);
		return res;
	}

	public boolean isGETRequest() {
		return method == HTTPConstants.HTTPMethod.GET;
	}

	public boolean isPOSTRequest() {
		return method == HTTPConstants.HTTPMethod.POST;
	}
  
   /*Reference: http://stackoverflow.com/questions/1066589/java-iterate-through-hashmap
    * Called by Client to construct a request containing uri with parameters*/
	public String pathWithParams() throws UnsupportedEncodingException {
		String queries = "";
		Iterator<Entry<String, String>> it = params.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> pair = it.next();
			queries += (pair.getKey()+ "=" + URLEncoder.encode(pair.getValue(), "UTF-8")) + "&";
		}
		if (queries.isEmpty())
			return path;
		else
			/*queries.length()-1 since the last character is &. */
			return path + "?" + queries.substring(0, queries.length() - 1);
	}

	@Override
	public String toString() {
		return "Method: " + method + 
				", host: " + host +
				", port: " + port +
				", path: " + path +
				", params: " + 
				params.toString() + 
				", body: " + body;
	}
}
