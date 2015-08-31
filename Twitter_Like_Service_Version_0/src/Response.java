
public class Response {
	public final int statusCode;
	public final String reason;
	public final String body;
	
	public Response(int statusCode, String reason, String body) {
		this.statusCode = statusCode;
		this.reason = reason;
		this.body = body;
	}
	
	@Override
	public String toString() {
		return "statusCode: " + statusCode + ", reason: " + reason + ", body: " + body;
	}
}
