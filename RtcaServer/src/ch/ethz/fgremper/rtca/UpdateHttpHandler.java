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

import ch.ethz.fgremper.rtca.helper.JSONHelper;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdateHttpHandler implements HttpHandler {

	static final String FILE_STORAGE_PATH = "filestorage/";

	public void handle(HttpExchange exchange) throws IOException {
		
		URI uri = exchange.getRequestURI();
		String requestMethod = exchange.getRequestMethod();

		// TODO: retrieve this from the uri
		String username = "john";
		String repositoryAlias = "testrepo";

		System.out.println("[UpdateHttpHandler] Incoming request. Username: " + username + ". RepositoryAlias: " + repositoryAlias);

		boolean success = false;
		
		if (requestMethod.equalsIgnoreCase("PUT")) {

			String jsonString = IOUtils.toString(exchange.getRequestBody(), "UTF-8");
			System.out.println("[UpdateHttpHandler] JSON string: " + jsonString);
			
			success = executeDataUpdate(repositoryAlias, username, jsonString);
			
		}
		
		String response = "";
		exchange.sendResponseHeaders(success ? 200 : 400, response.length());
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();

	}
	
	public boolean executeDataUpdate(String repositoryAlias, String username, String jsonString) {
		
		try {
			
			DatabaseConnection db;
			
			JSONObject updateObject = new JSONObject(jsonString);
			JSONArray fileArray = updateObject.getJSONArray("files");
			JSONArray commitHistory = updateObject.getJSONArray("commitHistory");
			String sessionId = updateObject.getString("sessionId");

			// TODO: use a pool of database connections
			db = new DatabaseConnection();
			
			// login check: TODO better error handling, not just yes or no
			if (!db.isUserAdmin(sessionId) && !db.isUserAuthorized(sessionId, repositoryAlias)) return false;
			
			db.startTransaction();
			db.deleteAllFilesFromRepositoryAndUser(repositoryAlias, username);

			for (int i = 0; i < fileArray.length(); i++) {
				JSONObject fileObject = fileArray.getJSONObject(i);

				String filename = fileObject.getString("filename");
				String content = fileObject.getString("content");
				String sha = DigestUtils.sha1Hex(content).toString();
				String branch = fileObject.getString("branch");
				String commit = fileObject.getString("commit");
				String committed = fileObject.getString("committed");
				
				System.out.println("[UpdateHttpHandler] File: " + filename + " (sha: " + sha + ")");

				FileUtils.writeStringToFile(new File(FILE_STORAGE_PATH + sha), content);

				db.storeFile(repositoryAlias, username, filename, sha, branch, commit, committed);
			}

			for (int i = 0; i < commitHistory.length(); i++) {
				JSONObject commitObject = commitHistory.getJSONObject(i);

				String commit = commitObject.getString("commit");
				List<String> downstreamCommits = JSONHelper.jsonArrayToArray(commitObject.getJSONArray("downstreamCommits"));
				
				System.out.println("[UpdateHttpHandler] Commit: " + commit);
				
				db.storeCommitHistory(repositoryAlias, username, commit, downstreamCommits);
			}
			
			db.commitTransaction();

			return true;
			
		}
		catch (Exception e) {
			System.out.println("[UpdateHttpHandler] Exception: " + e.getMessage());
			e.printStackTrace();
			
			return false;
		}
	
	}
}
