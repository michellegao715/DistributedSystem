package src;
import java.util.HashMap;

/**
HTTPRequestLine is a data structure that stores a Java representation of the parsed Request-Line.
 **/
public class HTTPRequestLine {

	private static HTTPConstants.HTTPMethod method;
	private static String uripath;
	private static HashMap<String, String> parameters= new HashMap<>();
	private static String httpversion;
	
	public HTTPRequestLine(HTTPConstants.HTTPMethod method,String uripath, 
			HashMap<String,String> parameters, String httpversion){
		HTTPRequestLine.method = method;
		HTTPRequestLine.uripath = uripath;
		HTTPRequestLine.parameters = parameters;
		HTTPRequestLine.httpversion = httpversion;
	}
	public HTTPRequestLine() {
	
	}
	public static HTTPConstants.HTTPMethod getMethod() {
		return method;
	}
	public static String getURIPath() {
		return uripath;
	}
	public static HashMap<String,String> getParameters() {
		return parameters;
	}
	public static String httpVersion() {
		return httpversion;
	}
	/*Set method*/
	public void setMethod(HTTPConstants.HTTPMethod method) {
		HTTPRequestLine.method = method;
	}
	public void setURIPath(String URIpath) {
		HTTPRequestLine.uripath = URIpath;		
	}
	public void setParameters(HashMap<String, String> parameter) {
		HTTPRequestLine.parameters = parameter;	
	}
	public void setHttpversion(String httpVersion) {
		HTTPRequestLine.httpversion = httpVersion;	
		
	}
	public boolean equals(HTTPRequestLine target) {  
		if(method == null
				|| !method.equals(target.getMethod()) 
				|| !(uripath.equals(target.getURIPath())) 
				|| !(parameters.equals(target.getParameters())) 
				|| !(httpversion.equals(target.httpVersion()))){
			return false;
		}else {
			return true;
		}
	}
	
	
}
