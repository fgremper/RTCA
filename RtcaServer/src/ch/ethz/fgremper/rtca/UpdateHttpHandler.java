package ch.ethz.fgremper.rtca;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdateHttpHandler implements HttpHandler {

	public void handle(HttpExchange exchange) throws IOException {
		try {
			URI uri = exchange.getRequestURI();
			String requestMethod = exchange.getRequestMethod();
			System.out.println("[UpdateHttpHandler] Incoming request: " + requestMethod + " " + uri.getPath());
	
			String username = "testuser";
			String repositoryAlias = "testrepo";
	
			if (requestMethod.equalsIgnoreCase("PUT")) {
	
				String jsonString = IOUtils.toString(exchange.getRequestBody(), "UTF-8");
				System.out.println("[UpdateHttpHandler] JSON string: " + jsonString);
	
				DatabaseConnection db;
					
				JSONArray fileArray = new JSONArray(jsonString);
	
				// TODO: use a pool of database connections
				db = new DatabaseConnection();
				
				db.startTransaction();
				db.deleteAllFilesFromRepositoryAndUser(repositoryAlias, username);
	
				for (int i = 0; i < fileArray.length(); i++) {
					JSONObject fileObject = fileArray.getJSONObject(i);
	
					String filename = fileObject.getString("filename");
					String content = fileObject.getString("content");
					String sha = DigestUtils.sha1Hex(content).toString();
					String branch = fileObject.getString("branch");
					String commit = fileObject.getString("commit");
					List<String> downstreamCommits = JSONHelper.jsonArrayToArray(fileObject.getJSONArray("downstreamCommits"));
	
					System.out.println("[UpdateHttpHandler] Filename: " + filename);
					System.out.println("[UpdateHttpHandler] Content: " + content);
					System.out.println("[UpdateHttpHandler] SHA: " + sha);
	
					FileUtils.writeStringToFile(new File("/Users/novocaine/Documents/masterthesis/workspace/RtcaServer/filestorage/" + sha), content);
	
					db.storeFile(repositoryAlias, username, filename, sha, branch, commit, downstreamCommits);
				}
	
				db.commitTransaction();

				// successful
				String successResponse = "";
				exchange.sendResponseHeaders(200, successResponse.length());
				OutputStream os = exchange.getResponseBody();
				os.write(successResponse.getBytes());
				os.close();
				
				return;
			}
		}
		catch (Exception e) {
			System.out.println("[UpdateHttpHandler] Exception: " + e.getMessage());
		}
		
		// if we got all the way down here, something went wrong
		String errorResponse = "";
		exchange.sendResponseHeaders(400, errorResponse.length());
		OutputStream os = exchange.getResponseBody();
		os.write(errorResponse.getBytes());
		os.close();

	}
}
