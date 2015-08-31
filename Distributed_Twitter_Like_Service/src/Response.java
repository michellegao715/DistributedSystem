import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Response {
	public final int statusCode;
	public final String reason;
	public final String body;
	public final String redirect;
	public final Map<String, String> cookies;
	
	public Response(int statusCode, String reason, String body) {
		this.statusCode = statusCode;
		this.reason = reason;
		this.body = body;
		this.redirect = "";
		this.cookies = new HashMap<String, String>();
	}
	
	public Response(int statusCode, String reason, String body, Map<String, String> cookies) {
		this.statusCode = statusCode;
		this.reason = reason;
		this.body = body;
		this.redirect = "";
		this.cookies = cookies;
	}
	
	public Response(String redirect) {
		this.statusCode = 302;
		this.reason = "Found";
		this.body = "";
		this.redirect = redirect;
		this.cookies = new HashMap<String, String>();
	}
	
	public Response(String redirect, Map<String, String> cookies) {
		this.statusCode = 302;
		this.reason = "Found";
		this.body = "";
		this.redirect = redirect;
		this.cookies = cookies;
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("HTTP/1.1 " + statusCode + " " + reason + "\r\n");
		if (redirect.isEmpty()) {
			res.append("Content-Type: text/html; charset=UTF-8\r\n");
			res.append(cookieString());
			res.append("Content-Length: " + body.length() + "\r\n");
			res.append("\r\n"); 
			res.append(body + "\r\n");
		} else {
			res.append("Location: " + redirect + "\r\n");
			res.append(cookieString());
		}
		return res.toString();
	}
	
	private String cookieString() {
		StringBuilder res = new StringBuilder();
		for(String key : cookies.keySet()) {
			if (cookies.get(key).isEmpty())
				res.append("Set-Cookie: " + key + "=" + cookies.get(key) + "; Expires=Mon, 01 Jan 1990 00:00:00 GMT" + "\r\n");
			else
				res.append("Set-Cookie: " + key + "=" + cookies.get(key) + "\r\n");
		}
		return res.toString();
	}
}
