import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*HTTP Request format: http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.3*/
/*Reference:
HTTP Request format: http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.3
Fetch reqeust line and body :http://stackoverflow.com/questions/13353592/while-reading-from-socket-how-to-detect-when-the-client-is-done-sending-the-requ*/

//General Worker
public class Worker implements Runnable {
	protected final Socket clientSocket;

	protected static JSONObject stringToJSON(String s) {
		JSONParser parser=new JSONParser();
		JSONObject obj = new JSONObject();
		try {
			obj = (JSONObject) parser.parse(s);
		} catch (ParseException e) {
			System.out.println("Having problem parsing request body to JSON object.");
		}
		return obj;
	}
	
	protected static Map<String, String> getFormParams(String data) {
		Map<String, String> params = new HashMap<String, String>();
		for(String param : data.split("&")) {
			int idx = param.indexOf("=");
			params.put(param.substring(0, idx), param.substring(idx + 1));
		}
		return params;
	}
	//called when choosing which tweet to delete
	protected static Map<String, String> getHiddenFormParams(String data) {
		System.out.println("getHiddenFormParams");
		Map<String, String> params = new HashMap<String, String>();
		for(String param : data.split("\r\n")) {
			System.out.println(param);
			int idx = param.indexOf("=");
			params.put(param.substring(0, idx), param.substring(idx + 1));
		}
		return params;
	}

	public Worker(Socket clientSocket){
		this.clientSocket = clientSocket;

	}

	/*Overriden by FEWorker and BEWorker.*/
	protected Response constructResponse(Request request) throws IOException {
		return new Response(200, "OK", "<body>Hello World</body>");
	}

	private void writeResponse(Response response) throws IOException {
		System.out.println("writing response: ");
		System.out.println(response.toString());


		PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
		out.print(response.toString());
		out.flush();
		out.close();
	}

	@Override
	public void run() {
		System.out.println("New Worker created");
		String firstHeader = "";
		String cookieHeader = "";
		StringBuffer request;
		try {
			System.out.println("Working reading request");
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String inputLine = null;
			request = new StringBuffer();
			boolean firstLine = true;
			boolean seenEmptyLineBeforeBody = false;
			int contentLength = 0;
			while (true) {
				if (!seenEmptyLineBeforeBody) {   /*Read through the request, when get to the empty line before body, use a char array to read exactly contentlength characters.*/
					inputLine = in.readLine();
					System.out.println(inputLine);
					/* Get the "Request-Line" ( Method SP Request-URI SP HTTP-Version CRLF ), and add it to request(followed by body)*/
					if (firstLine) { 
						firstHeader = inputLine;
						firstLine = false;
					}
				}
				else {
					if (contentLength == 0)
						break;
					char[] buf = new char[contentLength];
					contentLength -= in.read(buf, 0, contentLength);
					inputLine = new String(buf);
					// request.append("\n");/*Add body behind the request line.*/
					request.append(inputLine);
				}
				if(inputLine.startsWith("Content-Length")) {
					contentLength = Integer.parseInt(inputLine.substring(16));
				}
				if(inputLine.startsWith("Cookie")) {
					cookieHeader = inputLine;
				}
				if (inputLine.isEmpty()) {
					seenEmptyLineBeforeBody = true;
				}
			}

			System.out.println("Worker finished reading request: ");
			System.out.println("1st header: " + firstHeader);
			System.out.println("Cookie: " + cookieHeader);
			System.out.println("Body: " + request.toString());

			Response response = constructResponse(Request.parse(firstHeader, cookieHeader, request.toString()));
			writeResponse(response);
			//			Request test = Request.parse(request.toString());
			//			System.out.println("Server worker finished reading request: ");
			//			System.out.println(test.method);
			//			System.out.println(test.path);
			//			System.out.println(test.params);
			//			System.out.println(test.body);
			//			System.out.println("Server worker finished parsing request.");
		} catch (IOException e) {
			throw new RuntimeException("Error reading request", e);
		}
	}

}
