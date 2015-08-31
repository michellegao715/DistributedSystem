package src;

import java.util.HashMap;

import src.HTTPConstants.HTTPMethod;
public class HTTPRequestLineParser {
	public static HTTPRequestLine parse(String line) throws Exception {
		String decodedline = java.net.URLDecoder.decode(line, "UTF-8");
		String[] request = decodedline.split(" ");
		if(request.length != 3) return null;
		String originalmethod = request[0]; // GET
		String uri = request[1];	/*restaurantmashup?city=abc&size=efg&pic=add&#fragment*/
		String httpversion = request[2];

		/*First check there are method,uripath and httpversion, these are required.*/
		if(originalmethod.isEmpty() || uri.isEmpty() || httpversion.isEmpty()){
			return null;
		}
		HTTPConstants.HTTPMethod method = HTTPRequestLine.getMethod();
		/*Check if method if valid.*/
		try {
			//method = HTTPRequestLine.getMethod();
			method=HTTPMethod.valueOf(originalmethod);
			if(method == null) {
				System.out.println("the method is null");
				return null;
			}
		} catch(Exception e) {
			System.out.println("Catch an exception,method is "+method);
			return null;
		}

		/*Check if httpversion if valid: HTTP/1.1 or HTTP/1.0*/
		try{
			if(!httpversion.equals("HTTP/1.1") && !httpversion.equals("HTTP/1.0")){
				return null;
			}
		}catch(Exception e){
			return null;
		}

		/*Remove # and what follows it and then check if parameters are valid */
		if(uri.contains("#")) {
			uri = uri.substring(0,uri.indexOf("#"));
		}
		/*uri shoud start with slash*/
		if(!uri.startsWith("/")) {
			return null;
		}
		/*Check parameters*/
		HashMap<String,String> parameters = HTTPRequestLine.getParameters();		
		String uripath = null;
		{
			if(uri.contains("?")){
				uripath = uri.split("\\?")[0];
				String p = uri.split("\\?")[1];	
				/*Since parameter can be key=value&, so you can ignore the & in the end*/
				if(p.endsWith("&")) {  
					p = p.substring(0,p.length()-1);
				}
				String parameterString = p;
				if(!parameterString.contains("=")){
					return null;
				}
				String[] params = parameterString.split("&");
				for(String s: params){
					if(s.split("=").length !=2){
						return null;
					}else {
						String key = s.split("=")[0]; 
						String value = s.split("=")[1];
						if(!key.isEmpty() && !value.isEmpty()){
							parameters.put(key,value);
						}
					}
				}
			} else {  //if there is no parameters, uripath is everything follows "/"
				uripath = uri;
			}
			/*return parsed http request*/
			//HTTPRequestLine result = new HTTPRequestLine(method,uripath,parameters,httpversion);
			HTTPRequestLine result = new HTTPRequestLine();
			result.setMethod(method);
			result.setURIPath(uripath);
			result.setParameters(parameters);
			result.setHttpversion(httpversion);
			return result;
		} 
	}
}
