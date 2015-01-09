package ch.ethz.fgremper.rtca;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {
		
	public void send(String serverUrl, String repositoryAlias, String username, String jsonString) throws Exception {

		String url = serverUrl + "/update/" + repositoryAlias + "/" + username;

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("PUT");
		con.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
		out.write(jsonString);
		out.close();

		int responseCode = con.getResponseCode();

		System.out.println("[Http] Response code: " + responseCode);

	}
	
}
