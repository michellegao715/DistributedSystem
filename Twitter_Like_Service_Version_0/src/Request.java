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
	public final String body;

	public Request(HTTPConstants.HTTPMethod method, 
			String host,
			int port,
			String path,
			Map<String, String> params, 
			String body) {
		this.method = method;
		this.host = host;
		this.port = port;
		this.path = path;
		this.params = params;
		this.body = body;
	}

	/*There are two(or three if there is body) parts of request required for construct related response: method,path(params),(body)*/
	/*Dubugging:POST /tweets?q=this&v=that HTTP/1.1\n{"text":"#this is #tweet"}*/
	public static Request parse(String request) {
		//check if there is \n which means there should be body follows it.
		String header = "";
		String body = "";
		if(request.contains("\n")){
			header = request.substring(0,request.indexOf("\n"));
			body = request.substring(request.indexOf("\n")+1);
		}else {
			header = request;
		}
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
		String HTTPVersion = parsedRequest[2];
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
				if(s.split("=").length !=2){
					return null;
				}else {
					String key = s.split("=")[0]; 
					String value = s.split("=")[1];
					if(!key.isEmpty() && !value.isEmpty()){
						params.put(key,value);
					}
				}
			}
		} else {  //if there is no parameters, path is everything follows "/"
			path = pathAndParams;
		}		
		/* Get the body from the request:
		 * starts from the 3th string in the array parsedRequest: 
		 * POST
			/tweets?q=this&v=that
			HTTP/1.1
			{"text":"#this
			is
			#tweet"}*/
	
		Request res = new Request(method,host,port,path,params,body);
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
