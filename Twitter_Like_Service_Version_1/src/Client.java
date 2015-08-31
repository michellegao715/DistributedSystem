import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/*For FE to work as a client and send request and get response back from BE.*/
public class Client {
	/*TODO multiple workers accessing the same method, think about if there is thread-safe issue.*/
	Response sendRequest(Request request) {
		System.out.println("Client sending request:");
		System.out.println(request.toString());

		int statusCode;
		String reason;
		StringBuffer body = null;
		try {
			/*Open connection to BE by parsing BE's hostname, port number and uri.*/
			URL url = new URL("http", request.host, request.port, request.pathWithParams());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			DataOutputStream wr = null;
			if (request.isPOSTRequest()) {
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(request.body);
				wr.flush();
			}
			/*Deal with reading through, \r\n, use getResponseCode,getResponseMessage and getContent to parse statusCode,reason and body.*/
			statusCode = connection.getResponseCode();
			reason = connection.getResponseMessage();
			BufferedReader in = new BufferedReader(new InputStreamReader((InputStream) connection.getContent()));

			String inputLine = "";
			body = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				body.append(inputLine);
			}
			if (request.isPOSTRequest()) {
				wr.close();
			}
			in.close();
		} catch (IOException e) {
			throw new RuntimeException("Error sending request / reading response", e);
		}

		Response response = new Response(statusCode, reason, body.toString()); 

		System.out.println("Client received response: " + response.toString());

		return response;
	}
}
