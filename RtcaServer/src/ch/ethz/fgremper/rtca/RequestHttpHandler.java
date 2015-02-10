package ch.ethz.fgremper.rtca;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.fgremper.rtca.helper.JSONHelper;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RequestHttpHandler implements HttpHandler {
	
	public void handle(HttpExchange exchange) throws IOException {
		
		// Get HTTP exchange information
		URI uri = exchange.getRequestURI();
		String requestMethod = exchange.getRequestMethod();
		System.out.println("[RequestHttpHandler] Incoming request: " + requestMethod + " " + uri.getPath());
		String inputJsonString = IOUtils.toString(exchange.getRequestBody(), "UTF-8");
		System.out.println("[RequestHttpHandler] JSON string: " + inputJsonString);
		
		// Variables
		String response = null;
		String prefix = "/request";

		DatabaseConnection db = null;
		try {
			// Get database connection
			db = new DatabaseConnection();
			
			// POST request?
			if (requestMethod.equalsIgnoreCase("POST")) {
				
				// Login request
				if (uri.getPath().equals(prefix + "/login") || uri.getPath().equals(prefix + "/createUserAndLogin")) {
					System.out.println("[RequestHttpHandler] Incoming LOGIN.");
					
					// Read parameters
					JSONObject loginObject = new JSONObject(inputJsonString);
					String username = loginObject.getString("username");
					String password = loginObject.getString("password");
					
					// Do we need to create the user?
					if (uri.getPath().equals(prefix + "/createUserAndLogin")) {
						db.addUser(username, password);
					}
					
					// Request new session ID from database
					String sessionId = db.getNewSessionIdForCorrectLogin(username, password);
	
					// Initialize response object
					JSONObject responseObject = new JSONObject();
					
					if (sessionId != null) {
						// Persist session ID
						db.startTransaction();
						db.persistSessionIdForUser(sessionId, username);
						db.commitTransaction();
	
						// Create user object
						responseObject.put("isAdmin", db.isUserAdmin(sessionId));
						responseObject.put("isCreator", db.isUserCreator(sessionId));
						responseObject.put("sessionId", sessionId);
						responseObject.put("username", username);
					}
					
					response = responseObject.toString();
					
				}
			
				// Get repositories request
				if (uri.getPath().equals(prefix + "/getRepositories")) {
					System.out.println("[RequestHttpHandler] Incoming GET REPOSITORIES.");
					
					// Read parameters
					JSONObject getRepositoriesObject = new JSONObject(inputJsonString);
					String sessionId = getRepositoriesObject.getString("sessionId");
					
					// Read repository information from database
					db.startTransaction();
					response = db.getRepositories(sessionId).toString();
					db.commitTransaction();
					
				}
	
				// Get users request
				if (uri.getPath().equals(prefix + "/getUsers")) {
					System.out.println("[RequestHttpHandler] Incoming GET USERS.");
					
					// Read parameters
					JSONObject getRepositoriesObject = new JSONObject(inputJsonString);
					String sessionId = getRepositoriesObject.getString("sessionId");
					
					// We need to be admin to retrieve this information
					if (db.isUserAdmin(sessionId)) {
						response = db.getUsers().toString();
					}
				}

				// Get branch level awareness
				if (uri.getPath().equals(prefix + "/getBranchLevelAwareness")) {
					System.out.println("Incoming GET BRANCH LEVEL AWARENESS.");

					JSONObject getConflictsObject = new JSONObject(inputJsonString);
					String sessionId = getConflictsObject.getString("sessionId");
					String repositoryAlias = getConflictsObject.getString("repositoryAlias");
					
					if (db.isUserAdmin(sessionId) || db.doesUserHaveRepositoryAccess(sessionId, repositoryAlias)) {
						response = db.getBranchLevelAwareness(repositoryAlias).toString();
					}
				}

				// Get content level awareness
				if (uri.getPath().equals(prefix + "/getContentLevelAwareness")) {
					System.out.println("Incoming GET CONTENT LEVEL AWARENESS.");

					JSONObject getFileLevelAwareness = new JSONObject(inputJsonString);
					String sessionId = getFileLevelAwareness.getString("sessionId");
					String repositoryAlias = getFileLevelAwareness.getString("repositoryAlias");
					String theirUsername = getFileLevelAwareness.getString("username");
					String myUsername = db.getUsername(sessionId);
					String branch = getFileLevelAwareness.getString("branch");
					String filename = getFileLevelAwareness.getString("filename");
					
					if (db.isUserAdmin(sessionId) || db.doesUserHaveRepositoryAccess(sessionId, repositoryAlias)) {
						String mySha = db.getFileSha(repositoryAlias, myUsername, branch, filename);
						String theirSha = db.getFileSha(repositoryAlias, theirUsername, branch, filename);
						
						// TODO: check they are not null and stuff

						String fileStorageDirectory = ServerConfig.getInstance().fileStorageDirectory;

						JSONObject responseObject = new JSONObject();
						
						if (mySha != null) {
							File myFile = new File(fileStorageDirectory + "/" + mySha);
							responseObject.put("mine", FileUtils.readFileToString(myFile, "UTF-8"));
						}

						if (theirSha != null) {
							File theirFile = new File(fileStorageDirectory + "/" + theirSha);
							responseObject.put("theirs", FileUtils.readFileToString(theirFile, "UTF-8"));
						}
						
						
						// TODO: do a diff! :D
						
						
						
						response = responseObject.toString();
						
					}
				}

				// Get file awareness requests
				if (uri.getPath().equals(prefix + "/getFileLevelAwareness")) {
					System.out.println("Incoming GET FILE LEVEL CONFLICTS.");

					JSONObject getConflictsObject = new JSONObject(inputJsonString);
					String sessionId = getConflictsObject.getString("sessionId");
					String repositoryAlias = getConflictsObject.getString("repositoryAlias");
					
					if (db.isUserAdmin(sessionId) || db.doesUserHaveRepositoryAccess(sessionId, repositoryAlias)) {
						String username = db.getUsername(sessionId);
						response = db.getFileLevelAwareness(getConflictsObject, username);
					}
				}
				
				// Get repository information
				if (uri.getPath().equals(prefix + "/getRepositoryInformation")) {
					System.out.println("Incoming GET REPOSITORY INFORMATION.");

					JSONObject getConflictsObject = new JSONObject(inputJsonString);
					String sessionId = getConflictsObject.getString("sessionId");
					String repositoryAlias = getConflictsObject.getString("repositoryAlias");
					
					if (db.isUserAdmin(sessionId) || db.doesUserHaveRepositoryAccess(sessionId, repositoryAlias)) {
						String username = db.getUsername(sessionId);
						response = db.getRepositoryInformation(repositoryAlias);
					}
				}
			
				// Add repository request
				if (uri.getPath().equals(prefix + "/addRepository")) {
					System.out.println("Incoming ADD REPOSITORY.");
		
					JSONObject addRepositoryObject = new JSONObject(inputJsonString);
					String repositoryAlias = addRepositoryObject.getString("repositoryAlias");
					String repositoryUrl = addRepositoryObject.getString("repositoryUrl");
					String sessionId = addRepositoryObject.getString("sessionId");
	
					if (db.isUserAdmin(sessionId) || db.isUserCreator(sessionId)) {
						db = new DatabaseConnection();
						db.startTransaction();
						String repositoryOwner = db.getUsername(sessionId);
						db.addRepository(repositoryAlias, repositoryUrl, repositoryOwner);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Delete repository request
				if (uri.getPath().equals(prefix + "/deleteRepository")) {
					System.out.println("Incoming DELETE REPOSITORY.");

					JSONObject deleteRepositoryObject = new JSONObject(inputJsonString);
					String repositoryAlias = deleteRepositoryObject.getString("repositoryAlias");
					String sessionId = deleteRepositoryObject.getString("sessionId");
	
					if (db.isUserAdmin(sessionId) || db.isUserRepositoryOwner(sessionId, repositoryAlias)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.deleteRepository(repositoryAlias);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Add user to repository request
				if (uri.getPath().equals(prefix + "/addUserToRepository")) {
					System.out.println("Incoming ADD USER TO REPOSITORY.");

					JSONObject addUserToRepositoryObject = new JSONObject(inputJsonString);
					String username = addUserToRepositoryObject.getString("username");
					String repositoryAlias = addUserToRepositoryObject.getString("repositoryAlias");
					String sessionId = addUserToRepositoryObject.getString("sessionId");
	
					if (db.isUserAdmin(sessionId) || db.isUserRepositoryOwner(sessionId, repositoryAlias)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.addUserToRepository(username, repositoryAlias);
						db.commitTransaction();
						response = "{}";
					}
				}
				
				// Delete user from repository request
				if (uri.getPath().equals(prefix + "/deleteUserFromRepository")) {
					System.out.println("Incoming DELETE USER FROM REPOSITORY.");

					JSONObject removeUserToRepositoryObject = new JSONObject(inputJsonString);
					String username = removeUserToRepositoryObject.getString("username");
					String repositoryAlias = removeUserToRepositoryObject.getString("repositoryAlias");
					String sessionId = removeUserToRepositoryObject.getString("sessionId");
	
					if (db.isUserAdmin(sessionId) || db.isUserRepositoryOwner(sessionId, repositoryAlias)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.deleteUserFromRepository(username, repositoryAlias);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Modify repository owner
				if (uri.getPath().equals(prefix + "/modifyRepositoryOwner")) {
					System.out.println("Incoming MODIFY REPOSITORY OWNER.");

					JSONObject modifyRepositoryOwner = new JSONObject(inputJsonString);
					String repositoryAlias = modifyRepositoryOwner.getString("repositoryAlias");
					String username = modifyRepositoryOwner.getString("username");
					String sessionId = modifyRepositoryOwner.getString("sessionId");
	
					if (db.isUserAdmin(sessionId) || db.isUserRepositoryOwner(sessionId, repositoryAlias)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.modifyRepositoryOwner(repositoryAlias, username);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Delete user
				if (uri.getPath().equals(prefix + "/deleteUser")) {
					System.out.println("Incoming DELETE USER.");

					JSONObject modifyRepositoryOwner = new JSONObject(inputJsonString);
					String username = modifyRepositoryOwner.getString("username");
					String sessionId = modifyRepositoryOwner.getString("sessionId");
	
					if (db.isUserAdmin(sessionId)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.deleteUser(username);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Make user admin
				if (uri.getPath().equals(prefix + "/makeUserAdmin")) {
					System.out.println("Incoming MAKE USER ADMIN.");

					JSONObject modifyRepositoryOwner = new JSONObject(inputJsonString);
					String username = modifyRepositoryOwner.getString("username");
					String sessionId = modifyRepositoryOwner.getString("sessionId");
	
					if (db.isUserAdmin(sessionId)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.makeUserAdmin(username);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Revoke user admin
				if (uri.getPath().equals(prefix + "/revokeUserAdmin")) {
					System.out.println("Incoming REVOKE USER ADMIN.");

					JSONObject modifyRepositoryOwner = new JSONObject(inputJsonString);
					String username = modifyRepositoryOwner.getString("username");
					String sessionId = modifyRepositoryOwner.getString("sessionId");
	
					if (db.isUserAdmin(sessionId)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.revokeUserAdmin(username);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Make user creator
				if (uri.getPath().equals(prefix + "/makeUserCreator")) {
					System.out.println("Incoming MAKE USER CREATOR.");

					JSONObject modifyRepositoryOwner = new JSONObject(inputJsonString);
					String username = modifyRepositoryOwner.getString("username");
					String sessionId = modifyRepositoryOwner.getString("sessionId");
	
					if (db.isUserAdmin(sessionId)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.makeUserCreator(username);
						db.commitTransaction();
						response = "{}";
					}
				}

				// Revoke user creator
				if (uri.getPath().equals(prefix + "/revokeUserCreator")) {
					System.out.println("Incoming REVOKE USER CREATOR.");

					JSONObject modifyRepositoryOwner = new JSONObject(inputJsonString);
					String username = modifyRepositoryOwner.getString("username");
					String sessionId = modifyRepositoryOwner.getString("sessionId");
	
					if (db.isUserAdmin(sessionId)) {
						db = new DatabaseConnection();
						db.startTransaction();
						db.revokeUserCreator(username);
						db.commitTransaction();
						response = "{}";
					}
				}
				
				// Set local git state request
				if (uri.getPath().equals(prefix + "/setLocalGitState")) {
					System.out.println("Incoming SET LOCAL GIT STATE.");

					JSONObject setLocalGitStateObject = new JSONObject(inputJsonString);
					String sessionId = setLocalGitStateObject.getString("sessionId");
					String repositoryAlias = setLocalGitStateObject.getString("repositoryAlias");
					
					if (db.doesUserHaveRepositoryAccess(sessionId, repositoryAlias)) {
						String username = db.getUsername(sessionId);
						db.startTransaction();
						db.setEntireUserGitState(inputJsonString, username);
						db.commitTransaction();
						response = "{}";
					}
					
				}

			}
			
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// Close database connection
		try {
			if (db != null) {
				db.closeConnection();
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (response != null) {
			exchange.sendResponseHeaders(200, response.length());
		}
		else {
			response = "500 (Error)";
			exchange.sendResponseHeaders(500, response.length());
		}
		System.out.println("[RequestHttpHandler] Sending response: " + response);
		OutputStream os = exchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
		
	}
}
