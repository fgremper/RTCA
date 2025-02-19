package ch.ethz.fgremper.cloudstudio.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sun.net.www.protocol.http.HttpURLConnection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * 
 * Handle API requests over HTTP
 * 
 * @author Fabian Gremper
 * 
 */
public class ApiHttpHandler implements HttpHandler {

	private static final Logger log = LogManager.getLogger(ApiHttpHandler.class);
	
	String fileStorageDirectory = ServerConfig.getInstance().fileStorageDirectory;

	/**
	 * 
	 * Handle HTTP exchange
	 * 
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void handle(HttpExchange exchange) throws IOException {
		
		// Get HTTP exchange information
		URI uri = exchange.getRequestURI();
		
		// Request method
		String requestMethod = exchange.getRequestMethod();

		// API name
		String prefix = "/api/";
		String apiName = uri.getPath().substring(prefix.length(), uri.getPath().length());
		
		// Parameters
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");

        // Body
		String body = IOUtils.toString(exchange.getRequestBody(), "UTF-8");
        log.info("Incoming request: " + requestMethod + " " + uri.getPath());
        // log.info("Body: " + body); // This body can contain all the file contents and be really long

		// Response variables
		String response = null;
		String error = null;

		DatabaseConnection db = new DatabaseConnection();
		try {
			
			// Get database connection
			db.getConnection();

			// Get session ID and session username
			String sessionId = null;
			String sessionUsername = null;
			sessionId = (String) params.get("sessionId");
			if (sessionId != null) {
				sessionUsername = db.getUsername(sessionId);
			}
			
			/* GET REQUESTS */
			
			// Repositories request
			if (apiName.equals("repositories") && requestMethod.equalsIgnoreCase("get")) {
				
				// Read repository information from database
				if (sessionUsername != null) {
					response = db.getRepositories(sessionUsername).toString();
				}
				else {
					throw new Exception("No valid session ID given");
				}
				
			}

			// Get users request
			else if (apiName.equals("users") && requestMethod.equalsIgnoreCase("get")) {
				
				// We need to be admin to retrieve this information
				if (db.isUserAdmin(sessionUsername)) {
					response = db.getUsers().toString();
				}
				else {
					throw new Exception("No administrator privileges");
				}
				
			}

			// Get repository information
			else if (apiName.equals("repositoryInformation") && requestMethod.equalsIgnoreCase("get")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				
				if (db.isUserAdmin(sessionUsername) || db.doesUserHaveRepositoryAccess(sessionUsername, repositoryAlias) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {
					response = db.getRepositoryInformation(repositoryAlias).toString();
				}
				else {
					throw new Exception("No repository access");
				}
				
			}
			
			// Get branch level awareness
			else if (apiName.equals("branchAwareness") && requestMethod.equalsIgnoreCase("get")) {
				
				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				
				// Need to be admin or have repository access
				if (db.isUserAdmin(sessionUsername) || db.doesUserHaveRepositoryAccess(sessionUsername, repositoryAlias) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {
					response = db.getBranchLevelAwareness(repositoryAlias).toString();
				}
				else {
					throw new Exception("No repository access");
				}
				
			}
			
			// Get file awareness requests
			else if (apiName.equals("fileAwareness") && requestMethod.equalsIgnoreCase("get")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String branch = (String) params.get("branch");
				String compareToBranch = (String) params.get("compareToBranch");
				boolean showConflicts = ((String) params.get("showConflicts")).equalsIgnoreCase("true");
				boolean showUncommitted = ((String) params.get("showUncommitted")).equalsIgnoreCase("true");
				boolean viewAsOrigin = ((String) params.get("viewAsOrigin")).equalsIgnoreCase("true");
				
				// Need to be admin or have repository access
				if (db.isUserAdmin(sessionUsername) || db.doesUserHaveRepositoryAccess(sessionUsername, repositoryAlias) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {
					
					// View as origin?
					if (viewAsOrigin) sessionUsername = "origin";
					System.out.println("USER: " + sessionUsername);
					
					JSONObject responseObject = db.getFileLevelAwareness(repositoryAlias, sessionUsername, branch, compareToBranch, showUncommitted, showConflicts);
					
					// If we show conflicts, go through all of the items and check the ones with file conflicts for line conflicts
					if (showConflicts) {
						
						// Go through all the files
						JSONArray fileArray = responseObject.getJSONArray("files");
						
						for (int j = 0; j < fileArray.length(); j++) {
							
							JSONObject conflict = fileArray.getJSONObject(j);
							String filename = conflict.getString("filename");
							JSONArray users = conflict.getJSONArray("users");
							
							// Go through all the users
							for (int k = 0; k < users.length(); k++) {
								
								JSONObject user = users.getJSONObject(k);
								
								// File conflict?
								if (user.getString("type").equals("FILE_CONFLICT")) {
									
									String theirUsername = user.getString("username");
									
									// Look up the ancestor file in the git repository
						            ContentConflictGitReader gitReader = new ContentConflictGitReader(repositoryAlias, branch, filename, sessionUsername, compareToBranch, theirUsername, showUncommitted);

						            // If there are content conflicts, put "CONTENT_CONFLICT" instead
						            int lineConflicts = gitReader.countConflicts();
						            if (lineConflicts > 0) {
						            	user.put("type", "CONTENT_CONFLICT");
						            	user.put("conflictCount", lineConflicts);
						            }
									
								}
								
							}
							
						}

					}
					
					response = responseObject.toString();
					
				}
				else {
					throw new Exception("No repository access");
				}
				
			}
			
			// Get content level awareness
			else if (apiName.equals("contentAwareness") && requestMethod.equalsIgnoreCase("get")) {
				
				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String branch = (String) params.get("branch");
				String compareToBranch = (String) params.get("compareToBranch");
				String theirUsername = (String) params.get("theirUsername");
				String filename = (String) params.get("filename");
				boolean showUncommitted = ((String) params.get("showUncommitted")).equalsIgnoreCase("true");
				boolean viewAsOrigin = ((String) params.get("viewAsOrigin")).equalsIgnoreCase("true");
				
				// Need to be admin or have repository access
				if (db.isUserAdmin(sessionUsername) || db.doesUserHaveRepositoryAccess(sessionUsername, repositoryAlias) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {

					// View as origin?
					if (viewAsOrigin) sessionUsername = "origin";

					// Get file SHAs
					String mySha = db.getFileSha(repositoryAlias, sessionUsername, branch, filename, showUncommitted);
					String theirSha = db.getFileSha(repositoryAlias, theirUsername, compareToBranch, filename, showUncommitted);

					List<String> myContent;
					List<String> theirContent;
					
					// Get my file content
					if (mySha != null) {
				        myContent = SideBySideDiff.fileToLines(fileStorageDirectory + File.separator + mySha);
					}
					else {
						myContent = new LinkedList<String>();
					}
					
					// Get their file content
					if (theirSha != null) {
				        theirContent = SideBySideDiff.fileToLines(fileStorageDirectory + File.separator + theirSha);
					}
					else {
						theirContent = new LinkedList<String>();
					}

					// Set response
					JSONObject responseObject = new JSONObject();
					responseObject.put("content", SideBySideDiff.diff(myContent, theirContent));
					response = responseObject.toString();

				}
				else {
					throw new Exception("No repository access");
				}
				
			}
			
			// Get content level conflicts
			else if (apiName.equals("contentConflicts") && requestMethod.equalsIgnoreCase("get")) {
				
				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String branch = (String) params.get("branch");
				String compareToBranch = (String) params.get("compareToBranch");
				String theirUsername = (String) params.get("theirUsername");
				String filename = (String) params.get("filename");
				boolean showUncommitted = ((String) params.get("showUncommitted")).equalsIgnoreCase("true");
				boolean viewAsOrigin = ((String) params.get("viewAsOrigin")).equalsIgnoreCase("true");
				
				// Need to be admin or have repository access
				if (db.isUserAdmin(sessionUsername) || db.doesUserHaveRepositoryAccess(sessionUsername, repositoryAlias) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {

					// View as origin?
					if (viewAsOrigin) sessionUsername = "origin";
					System.out.println("USER: " + sessionUsername);

					// Get content conflicts
		            ContentConflictGitReader gitReader = new ContentConflictGitReader(repositoryAlias, branch, filename, sessionUsername, compareToBranch, theirUsername, showUncommitted);
					
		            // Set reponse
		            JSONObject responseObject = new JSONObject();
					responseObject.put("content", gitReader.diff());
					response = responseObject.toString();
		            
				}
				else {
					throw new Exception("No repository access");
				}
				
			}
			
			/* POST REQUESTS */

			// Auth request
			else if (apiName.equals("login") && requestMethod.equalsIgnoreCase("post")) {

				// Read parameters
				String username = (String) params.get("username");
				String password = (String) params.get("password");

				// Request new session ID from database
				String loginUsername = db.getUsernameForCorrectLogin(username, password);

				if (loginUsername != null) {
				
					// Initialize response object
					JSONObject responseObject = new JSONObject();
					
					// Persist session ID
					db.startTransaction();
					String newSessionId = db.createAndPersistSessionIdForUser(loginUsername);
					db.commitTransaction();

					// Create user object
					responseObject.put("isAdmin", db.isUserAdmin(username));
					responseObject.put("isCreator", db.isUserCreator(username));
					responseObject.put("sessionId", newSessionId);
					responseObject.put("username", loginUsername);
					
					// Set response
					response = responseObject.toString();
					
				}
				else {
					throw new Exception("Incorrect login credentials");
				}
				
			}
			
			// Create repository
			else if (apiName.equals("createRepository") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String repositoryUrl = (String) params.get("repositoryUrl");
				String repositoryDescription = (String) params.get("repositoryDescription");

				// Need to be administrator or creator
				if (db.isUserAdmin(sessionUsername) || db.isUserCreator(sessionUsername)) {

					// Execute database action
					db.startTransaction();
					String repositoryOwner = sessionUsername;
					db.createRepository(repositoryAlias, repositoryUrl, repositoryOwner, repositoryDescription);
					db.addUserToRepository(sessionUsername, repositoryAlias);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("Insufficient privileges");
				}
				
			}

			// Set repository information
			else if (apiName.equals("setRepositoryInformation") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String repositoryUrl = (String) params.get("repositoryUrl");
				String repositoryDescription = (String) params.get("repositoryDescription");

				// Need to be admin or repository owner
				if (db.isUserAdmin(sessionUsername) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {
					
					// Execute database action
					db.startTransaction();
					db.updateRepositoryInformation(repositoryAlias, repositoryDescription, repositoryUrl);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("Insufficient privileges");
				}
				
			}

			// Delete repository
			else if (apiName.equals("deleteRepository") && requestMethod.equalsIgnoreCase("post")) {
				
				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");

				// Need to be administrator or repository owner
				if (db.isUserAdmin(sessionUsername) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {

					// Execute database action
					db.startTransaction();
					db.deleteRepository(repositoryAlias);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("Insufficient privileges");
				}
				
			}

			// Change password
			else if (apiName.equals("changePassword") && requestMethod.equalsIgnoreCase("post")) {
				
				// Get parameters
				String newPassword = (String) params.get("newPassword");

				// Execute database action
				if (sessionUsername != null) {
					db.startTransaction();
					db.changePassword(sessionUsername, newPassword);
					db.commitTransaction();
				}
				else {
					throw new Exception("No valid session ID given");
				}
				
				// Set response
				response = "{}";
				
			}

			// Add user to repository
			else if (apiName.equals("addUserToRepository") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String username = (String) params.get("username");

				// Need to be administrator or repository owner
				if (db.isUserAdmin(sessionUsername) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {

					// Execute database action
					db.startTransaction();
					db.addUserToRepository(username, repositoryAlias);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("Insufficient privileges");
				}
				
			}
			
			// Remove user from repository
			else if (apiName.equals("removeUserFromRepository") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String username = (String) params.get("username");

				// Need to be administrator or repository owner
				if (db.isUserAdmin(sessionUsername) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {
					
					// Execute database action
					db.startTransaction();
					db.deleteUserFromRepository(username, repositoryAlias);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("Insufficient privileges");
				}
			}

			// Modify repository owner
			else if (apiName.equals("modifyRepositoryOwner") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				String username = (String) params.get("username");

				// Need to be administrator or repository owner
				if (db.isUserAdmin(sessionUsername) || db.isUserRepositoryOwner(sessionUsername, repositoryAlias)) {
					db.startTransaction();
					db.modifyRepositoryOwner(repositoryAlias, username);
					db.commitTransaction();
					response = "{}";
				}
				else {
					throw new Exception("Insufficient privileges");
				}
				
			}

			// Create user
			else if (apiName.equals("createUser") && requestMethod.equalsIgnoreCase("post")) {

				// Read parameters
				String username = (String) params.get("username");
				String password = (String) params.get("password");

				// Execute database action
				db.startTransaction();
				db.addUser(username, password);
				db.commitTransaction();

				// Set response
				response = "{}";
				
			}
			
			// Delete user
			else if (apiName.equals("deleteUser") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String username = (String) params.get("username");

				// Need to be administrator
				if (db.isUserAdmin(sessionUsername)) {
					
					// Execute database action
					db.startTransaction();
					db.deleteUser(username);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("No administrator privileges");
				}
				
			}

			// Give admin privileges
			else if (apiName.equals("giveAdminPrivileges") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String username = (String) params.get("username");

				// Need to be administrator
				if (db.isUserAdmin(sessionUsername)) {

					// Execute database action
					db.startTransaction();
					db.makeUserAdmin(username);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("No administrator privileges");
				}
				
			}

			// Revoke admin privileges
			else if (apiName.equals("revokeAdminPrivileges") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String username = (String) params.get("username");

				// Need to be administrator
				if (db.isUserAdmin(sessionUsername)) {

					// Execute database action
					db.startTransaction();
					db.revokeUserAdmin(username);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("No administrator privileges");
				}
				
			}

			// Give creator privileges
			else if (apiName.equals("giveCreatorPrivileges") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String username = (String) params.get("username");

				// Need to be administrator
				if (db.isUserAdmin(sessionUsername)) {

					// Execute database action
					db.startTransaction();
					db.makeUserCreator(username);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("No administrator privileges");
				}
				
			}

			// Revoke creator privileges
			else if (apiName.equals("revokeCreatorPrivileges") && requestMethod.equalsIgnoreCase("post")) {

				// Get parameters
				String username = (String) params.get("username");

				// Need to be administrator
				if (db.isUserAdmin(sessionUsername)) {

					// Execute database action
					db.startTransaction();
					db.revokeUserCreator(username);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("No administrator privileges");
				}
				
			}
			
			// Set local git state request
			else if (apiName.equals("localState") && requestMethod.equalsIgnoreCase("post")) {
				
				// Get parameters
				String repositoryAlias = (String) params.get("repositoryAlias");
				
				if (db.doesUserHaveRepositoryAccess(sessionUsername, repositoryAlias)) {
					
					// Execute database action
					db.startTransaction();
					db.setEntireUserGitState(body, sessionUsername, repositoryAlias);
					db.commitTransaction();
					
					// Set response
					response = "{}";
					
				}
				else {
					throw new Exception("No access to repository");
				}
				
			}
			
			// Unknown API call
			else {
				throw new Exception("Unknown API call");
			}

		}
		catch (Exception e) {
			error = e.getMessage();
			log.error("Handling request error: " + e.getMessage());
			e.printStackTrace();
		}

		// Close database connection
		db.closeConnection();
		
		// Send response
		if (response != null) {
			
			// Successful call
			log.info("Sending response: " + response);
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
			
		}
		else {
			
			// Error
			response = "{}";
			try {
				JSONObject errorObject = new JSONObject();
				errorObject.put("error", (error == null ? "Unknown error" : error));
				response = errorObject.toString();
			}
			catch (JSONException e) {
				// Don't see how this can happen, we're just putting Strings into a JSON object
			}
			log.info("Sending error response: " + response);
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length());
			
		}
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
		
	}

}
