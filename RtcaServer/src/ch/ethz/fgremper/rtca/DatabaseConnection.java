package ch.ethz.fgremper.rtca;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import ch.ethz.fgremper.rtca.helper.JSONHelper;

public class DatabaseConnection {

	Connection con = null;

	public DatabaseConnection() throws Exception {
		//con = DatabaseConnectionPool.getInstance().getConnection();
    	ServerConfig serverConfig = ServerConfig.getInstance();
        Class.forName(serverConfig.dbDriverClass);
        con = DriverManager.getConnection(serverConfig.dbJdbcUrl, serverConfig.dbUser, serverConfig.dbPassword);
	}

	/* UTILITY */
	
	public void startTransaction() throws SQLException {
		con.setAutoCommit(false);
	}

	public void commitTransaction() throws SQLException {
		con.commit();
	}

	public void rollbackTransaction() throws SQLException {
		con.rollback();
	}
	
	public void closeConnection() throws SQLException {
		if (con != null) {
			con.close();
		}
	}

	/* UPDATE FROM CLIENT CYCLE */
	
	public void setEntireUserGitState(String inputJsonString, String username) throws Exception {

		JSONObject setLocalGitStateObject = new JSONObject(inputJsonString);
		JSONArray fileArray = setLocalGitStateObject.getJSONArray("files");
		JSONArray commitHistory = setLocalGitStateObject.getJSONArray("commitHistory");
		JSONArray branchesArray = setLocalGitStateObject.getJSONArray("branches");
		String repositoryAlias = setLocalGitStateObject.getString("repositoryAlias");
		
		// Start transaction
		startTransaction();
		
		// We're replacing all we know about what we know about this users git state, so delete every thing first
		deleteAllRepositoryUserInformation(repositoryAlias, username);

		// Read information in and store files to database and filesystem
		for (int i = 0; i < fileArray.length(); i++) {
			JSONObject fileObject = fileArray.getJSONObject(i);

			String filename = fileObject.getString("filename");
			String content = fileObject.getString("content");
			String sha = DigestUtils.sha1Hex(content).toString();
			String branch = fileObject.getString("branch");
			String commit = fileObject.getString("commit");
			String committed = fileObject.getString("committed");
			
			System.out.println("[DatabaseConnection] File: " + filename + " (sha: " + sha + ")");

			FileUtils.writeStringToFile(new File(ServerConfig.getInstance().fileStorageDirectory + "/" + sha), content);

			storeFile(repositoryAlias, username, filename, sha, branch, commit, committed);
		}

		// Read in commit history and store it to database
		for (int i = 0; i < commitHistory.length(); i++) {
			JSONObject commitObject = commitHistory.getJSONObject(i);

			String commit = commitObject.getString("commit");
			JSONArray downstreamCommits = commitObject.getJSONArray("downstreamCommits");
			
			storeCommitHistory(repositoryAlias, username, commit, downstreamCommits);
		}

		// Read in the branches and store it to database
		for (int i = 0; i < branchesArray.length(); i++) {
			JSONObject branchObject = branchesArray.getJSONObject(i);

			String branch = branchObject.getString("branch");
			String commit = branchObject.getString("commit");
			String active = (branchObject.getBoolean("active") ? "true" : "false");
			
			storeBranches(repositoryAlias, username, branch, commit, active);
		}

	}
	
	public void deleteAllRepositoryUserInformation(String repositoryAlias, String username) throws SQLException {
		PreparedStatement stmt;
		
		// Delete files
		stmt = con.prepareStatement("DELETE FROM files WHERE repositoryalias = ? AND username = ?");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.executeUpdate();

		// Delete commit history
		stmt = con.prepareStatement("DELETE FROM commithistory WHERE repositoryalias = ? AND username = ?");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.executeUpdate();

		// Delete branches for user
		stmt = con.prepareStatement("DELETE FROM branches WHERE repositoryalias = ? AND username = ?");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.executeUpdate();
	}

	public void storeFile(String repositoryAlias, String username, String filename, String sha, String branch, String commit, String committed) throws SQLException {
		// Store file information
		PreparedStatement stmt = con.prepareStatement("INSERT INTO files (repositoryalias, username, filename, sha, branch, commit, committed) VALUES (?, ?, ?, ?, ?, ?, ?)");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.setString(3, filename);
		stmt.setString(4, sha);
		stmt.setString(5, branch);
		stmt.setString(6, commit);
		stmt.setString(7, committed);
		stmt.executeUpdate();
	}
	
	/*
	public void storeActiveBranch(String repositoryAlias, String username, String branch, String commit) throws Exception {
		// Set active branch for user
		PreparedStatement stmt = con.prepareStatement("INSERT INTO activebranch (repositoryalias, username, branch, commit) VALUES (?, ?, ?, ?)");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.setString(3, branch);
		stmt.setString(4, commit);
		stmt.executeUpdate();
	}
	*/

	public void storeBranches(String repositoryAlias, String username, String branch, String commit, String active) throws Exception {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO branches (repositoryalias, username, branch, commit, active) VALUES (?, ?, ?, ?, ?)");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.setString(3, branch);
		stmt.setString(4, commit);
		stmt.setString(5, active);
		stmt.executeUpdate();
	}

	public void storeCommitHistory(String repositoryAlias, String username, String commit, JSONArray downstreamCommits) throws Exception {
		// Statement to insert a single commit/downstreamcommit pair
		PreparedStatement stmt = con.prepareStatement("INSERT INTO commithistory (repositoryalias, username, commit, downstreamcommit, distance) VALUES (?, ?, ?, ?, ?)");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.setString(3, commit);
		
		// Store all commit/downstreamcommit pairs
		for (int i = 0; i < downstreamCommits.length(); i++) {
			JSONObject downstreamCommit = downstreamCommits.getJSONObject(i);
			stmt.setString(4, downstreamCommit.getString("commit"));
			stmt.setInt(5, downstreamCommit.getInt("distance"));
			stmt.executeUpdate();
		}
	}

	public void resetDatabase() throws SQLException {
		// Remove all columns from all tables
		PreparedStatement stmt;
		stmt = con.prepareStatement("DELETE FROM branches");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM activebranch");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM usersessions");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM useraccess");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM files");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM commithistory");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM repositories");
		stmt.executeUpdate();
		stmt = con.prepareStatement("DELETE FROM users");
		stmt.executeUpdate();
		// Add origin user
		stmt = con.prepareStatement("INSERT INTO users (username, passwordhash, isadmin, iscreator) VALUES ('origin', 'origin', 'false', 'false')");
		stmt.executeUpdate();
	}
	
	public JSONObject getBranchLevelAwareness(String repositoryAlias) throws Exception {
		JSONObject responseObject = new JSONObject();
		
		JSONArray branchesArray = new JSONArray();
		
		PreparedStatement stmt = con.prepareStatement(SqlQueryReader.getInstance().getQuery("BranchAwareness"));

		System.out.println("READ CONTENTS: " + SqlQueryReader.getInstance().getQuery("BranchAwareness"));
		
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, repositoryAlias);
		stmt.setString(3, repositoryAlias);
		stmt.setString(4, repositoryAlias);
		
		ResultSet rs = stmt.executeQuery();
		
		HashMap<String, JSONObject> index = new HashMap<String, JSONObject>();
		while (rs.next()) {
			String branch = rs.getString("branch");
			String username = rs.getString("username");
			String commit = rs.getString("commit");
			String origincommit = rs.getString("origincommit");
			String relation;
			
			Integer d = null;
			
			if (commit == null) {
				relation = "not checked out";
			}
			else if (origincommit == null) {
				relation = "local branch";
			}
			else if (commit.equals(origincommit)) {
				relation = "equal";
			}
			else if ((d = branchCommitIsInHistoryOfBranchCommit(repositoryAlias, commit, origincommit)) != null) {
				relation = d + " behind";
			}
			else if ((d = branchCommitIsInHistoryOfBranchCommit(repositoryAlias, origincommit, commit)) != null) {
				relation = d + " in front";
			}
			else if ((d = distanceForCommitsToSeeEachOther(repositoryAlias, origincommit, commit)) != null) {
				relation = d + " away";
			}
			else {
				relation = "unknown";
			}
			
			JSONObject branchObject;
			if (!index.containsKey(branch)) {
				branchObject = new JSONObject();
				branchesArray.put(branchObject);
				index.put(branch, branchObject);
				branchObject.put("branch", branch);
				branchObject.put("users", new JSONArray());
			}
			else {
				branchObject = index.get(branch);
			}
			
			JSONObject branchUserObject = new JSONObject();
			branchObject.getJSONArray("users").put(branchUserObject);
			branchUserObject.put("username", username);
			branchUserObject.put("relationWithOrigin", relation);
		}
		
		responseObject.put("branches", branchesArray);
		
		return responseObject;
	}
	
	public Integer branchCommitIsInHistoryOfBranchCommit(String repositoryAlias, String commit1, String commit2) throws Exception {
		PreparedStatement stmt = con.prepareStatement("SELECT distance FROM commithistory WHERE commit = ? AND downstreamcommit = ? AND repositoryalias = ?");

		stmt.setString(1, commit2);
		stmt.setString(2, commit1);
		stmt.setString(3, repositoryAlias);

		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return rs.getInt("distance");
		}
		else {
			return null;
		}
	}
	

	public Integer distanceForCommitsToSeeEachOther(String repositoryAlias, String commit1, String commit2) throws Exception {
		PreparedStatement stmt = con.prepareStatement("SELECT MIN(b1.distance + b2.distance) AS mindistance FROM commithistory AS b1 CROSS JOIN commithistory AS b2 WHERE b1.commit = ? AND b2.commit = ? AND b1.downstreamcommit = b2.downstreamcommit AND b1.repositoryalias = ? AND b2.repositoryalias = ?");

		stmt.setString(1, commit2);
		stmt.setString(2, commit1);
		stmt.setString(3, repositoryAlias);
		stmt.setString(4, repositoryAlias);

		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return rs.getInt("mindistance");
		}
		else {
			return null;
		}
	}
	

	public String getFileSha(String repositoryAlias, String username, String branch, String filename, boolean showUncommitted) throws Exception {
		String showUncommittedString = showUncommitted ? "uncommitted" : "committed";
		
		PreparedStatement stmt = con.prepareStatement("SELECT sha FROM files WHERE repositoryalias = ? AND username = ? AND branch = ? AND filename = ? AND (committed = ? OR committed = 'both')");

		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.setString(3, branch);
		stmt.setString(4, filename);
		stmt.setString(5, showUncommittedString);

		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return rs.getString("sha");
		}
		else {
			return null;
		}
	}

	public String getFileLevelAwareness(JSONObject getConflictsObject, String username) throws Exception {
		String repositoryAlias = getConflictsObject.getString("repositoryAlias");
		String branch = getConflictsObject.getString("branch");
		boolean showUncommitted = getConflictsObject.getBoolean("showUncommitted");
		String showUncommittedString = showUncommitted ? "uncommitted" : "committed";
		JSONArray selectedAdditionalBranches = getConflictsObject.getJSONArray("selectedAdditionalBranches");
		
		JSONObject responseObject = new JSONObject();
		JSONArray branchesArray = new JSONArray();
		responseObject.put("branches", branchesArray);
		
		PreparedStatement stmt;
		
		JSONObject branchObject = new JSONObject();
		branchesArray.put(branchObject);
		branchObject.put("branch", branch);
		
		stmt = con.prepareStatement(SqlQueryReader.getInstance().getQuery("FileAwareness"));
		
		/* 
		1 my user
		2 committed
		3 branch
		4 repositoryalias
		5 repositoryalias
		6 repository alias
		7 their branch <-- not anymore
		8 committed
		9 their branch
		10 repository
		*/
		
		stmt.setString(1, username);
		stmt.setString(2, showUncommittedString);
		stmt.setString(3, branch);
		stmt.setString(4, repositoryAlias);
		stmt.setString(5, repositoryAlias);
		stmt.setString(6, repositoryAlias);
		stmt.setString(7, showUncommittedString);
		stmt.setString(8, branch);
		stmt.setString(9, repositoryAlias);
		
		ResultSet rs = stmt.executeQuery();
		HashMap<String, JSONObject> fileMap = new HashMap<String, JSONObject>();
		JSONArray conflictList = new JSONArray();
		while (rs.next()) {
			String filename = rs.getString("filename");
			String mySha = rs.getString("mysha");
			String theirUsername = rs.getString("theirusername");
			String theirSha = rs.getString("theirsha");
			
			JSONObject conflict;
			if (!fileMap.containsKey(filename)) {
				conflict = new JSONObject();
				fileMap.put(filename, conflict);
				conflictList.put(conflict);
				conflict.put("filename", filename);
				conflict.put("users", new JSONArray());
			}
			else {
				conflict = fileMap.get(filename);
			}
			
			JSONArray conflictUsers = conflict.getJSONArray("users");
			JSONObject user = new JSONObject();
			conflictUsers.put(user);
			
			user.put("username", theirUsername);
			
			String conflictType;
			if (mySha == null && theirSha != null) conflictType = "new";
			else if (mySha != null && theirSha == null) conflictType = "delete";
			else if (mySha == null && theirSha == null) conflictType = "inexistent";
			else if (mySha.equals(theirSha)) conflictType = "equal";
			else conflictType = "file conflict";
			
			user.put("type", conflictType);
			
		}
		branchObject.put("files", conflictList);
		
		
		
		for (int i = 0; i < selectedAdditionalBranches.length(); i++) {
			String additionalBranch = selectedAdditionalBranches.getString(i);

			branchObject = new JSONObject();
			branchesArray.put(branchObject);
			branchObject.put("branch", additionalBranch);
			
			stmt = con.prepareStatement(SqlQueryReader.getInstance().getQuery("FileAwareness"));
			
			/* 
			1 my user
			2 committed
			3 branch
			4 repositoryalias
			5 repositoryalias
			6 repository alias
			7 their branch <-- not anymore
			8 committed
			9 their branch
			10 repository
			*/
			
			stmt.setString(1, username);
			stmt.setString(2, showUncommittedString);
			stmt.setString(3, branch);
			stmt.setString(4, repositoryAlias);
			stmt.setString(5, repositoryAlias);
			stmt.setString(6, repositoryAlias);
			stmt.setString(7, showUncommittedString);
			stmt.setString(8, additionalBranch);
			stmt.setString(9, repositoryAlias);
			
			rs = stmt.executeQuery();
			fileMap = new HashMap<String, JSONObject>();
			conflictList = new JSONArray();
			while (rs.next()) {
				String filename = rs.getString("filename");
				String mySha = rs.getString("mysha");
				String theirUsername = rs.getString("theirusername");
				String theirSha = rs.getString("theirsha");
				
				JSONObject conflict;
				if (!fileMap.containsKey(filename)) {
					conflict = new JSONObject();
					fileMap.put(filename, conflict);
					conflictList.put(conflict);
					conflict.put("filename", filename);
					conflict.put("users", new JSONArray());
				}
				else {
					conflict = fileMap.get(filename);
				}
				
				JSONArray conflictUsers = conflict.getJSONArray("users");
				JSONObject user = new JSONObject();
				conflictUsers.put(user);
				
				user.put("username", theirUsername);
				
				String conflictType;
				if (mySha == null && theirSha != null) conflictType = "new";
				else if (mySha != null && theirSha == null) conflictType = "delete";
				else if (mySha == null && theirSha == null) conflictType = "inexistent";
				else if (mySha.equals(theirSha)) conflictType = "equal";
				else conflictType = "file conflict";
				
				user.put("type", conflictType);
				
			}
			branchObject.put("files", conflictList);
		}
		
		return responseObject.toString();
	}
	
	/* REPOSITORY MANAGEMENT */
	
	public String getRepositoryInformation(String repositoryAlias) throws Exception {
		// Get users from repository
		JSONArray repositoryUsers = new JSONArray();
		ResultSet rs;		
		PreparedStatement getRepositoryUsersStmt = con.prepareStatement(
			"SELECT DISTINCT username FROM useraccess WHERE repositoryalias = ?"
		);
		getRepositoryUsersStmt.setString(1, repositoryAlias);
		rs = getRepositoryUsersStmt.executeQuery();
		while (rs.next()) {
			repositoryUsers.put(rs.getString("username"));
		}

		// Get branches from repository
		JSONArray repositoryBranches = new JSONArray();
		PreparedStatement getRepositoryBranchesStmt = con.prepareStatement(
			"SELECT DISTINCT branch FROM files WHERE repositoryalias = ?"
		);
		getRepositoryBranchesStmt.setString(1, repositoryAlias);
		rs = getRepositoryBranchesStmt.executeQuery();
		while (rs.next()) {
			repositoryBranches.put(rs.getString("branch"));
		}
		JSONObject responseObject = new JSONObject();
		
		// Add to and return object
		responseObject.put("repositoryUsers", repositoryUsers);
		responseObject.put("repositoryBranches", repositoryBranches);
		return responseObject.toString();
	}

	public JSONArray getAllRepositories() throws Exception {
		// Statement to get all the repositories
		PreparedStatement stmt = con.prepareStatement("SELECT DISTINCT repositoryalias, repositoryurl FROM repositories");
		
		// Put the results into a JSON array
		JSONArray repositoriesArray = new JSONArray();
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String repositoryAlias = rs.getString("repositoryalias");
			String repositoryUrl = rs.getString("repositoryurl");
			JSONObject repositoryObject = new JSONObject();
			repositoryObject.put("repositoryAlias", repositoryAlias);
			repositoryObject.put("repositoryUrl", repositoryUrl);
			repositoryObject.put("users", new JSONArray());
			repositoriesArray.put(repositoryObject);
		}
		
		return repositoriesArray;
	}
	
	public JSONArray getRepositories(String myUsername) throws Exception {
		// Statement to get all the repositories and users for your user permissions
		
		// TODO TODO TODO atm everyone can see everything
		PreparedStatement stmt = con.prepareStatement("SELECT DISTINCT repositories.repositoryalias, repositories.repositoryurl, repositories.repositoryowner, useraccess.username FROM repositories LEFT OUTER JOIN useraccess ON repositories.repositoryalias = useraccess.repositoryalias");
		//WHERE EXISTS (SELECT users.username FROM users JOIN usersessions AS us1 ON users.username = us1.username WHERE users.isadmin = 'true' AND us1.sessionid = ?) OR EXISTS (SELECT useraccess.username FROM useraccess JOIN usersessions AS us2 ON useraccess.username = us2.username WHERE us2.sessionid = ? AND useraccess.repositoryalias = repositories.repositoryalias) OR repositories.repositoryowner = (SELECT username FROM usersessions WHERE sessionid = ?)");
		/*stmt.setString(1, sessionId);
		stmt.setString(2, sessionId);
		stmt.setString(3, sessionId);
		*/
		// Put the results into a JSON array
		JSONArray repositoriesArray = new JSONArray();
		ResultSet rs = stmt.executeQuery();
		HashMap<String, JSONObject> index = new HashMap<String, JSONObject>();
		while (rs.next()) {
			String repositoryAlias = rs.getString("repositoryalias");
			String repositoryUrl = rs.getString("repositoryurl");
			String repositoryOwner = rs.getString("repositoryowner");
			String username = rs.getString("username");
			if (!index.containsKey(repositoryAlias)) {
				JSONObject repositoryObject = new JSONObject();
				index.put(repositoryAlias, repositoryObject);
				repositoryObject.put("repositoryAlias", repositoryAlias);
				repositoryObject.put("repositoryUrl", repositoryUrl);
				repositoryObject.put("repositoryOwner", repositoryOwner);
				repositoryObject.put("users", new JSONArray());
				repositoriesArray.put(repositoryObject);
			}
			if (username != null) {
				index.get(repositoryAlias).getJSONArray("users").put(username);
			}
		}
		
		return repositoriesArray;
	}
	
	public void addRepository(String repositoryAlias, String repositoryUrl, String repositoryOwner) throws SQLException {
		// Add repository
		PreparedStatement stmt = con.prepareStatement("INSERT INTO repositories (repositoryalias, repositoryurl, repositoryowner, clonecount) VALUES (?, ?, ?, 0)");
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, repositoryUrl);
		stmt.setString(3, repositoryOwner);
		stmt.executeUpdate();
	}
	
	public void deleteRepository(String repositoryAlias) throws SQLException {		
		// Delete repository
		PreparedStatement stmt = con.prepareStatement("DELETE FROM repositories WHERE repositoryalias = ?");
		stmt.setString(1, repositoryAlias);
		stmt.executeUpdate();
	}
	
	/* USER MANAGEMENT */

	public JSONArray getUsers() throws Exception {
		JSONArray usersArray = new JSONArray();
		
		PreparedStatement stmt = con.prepareStatement("SELECT username, isadmin, iscreator FROM users");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String username = rs.getString("username");
			String isAdmin = rs.getString("isadmin");
			String isCreator = rs.getString("iscreator");
			JSONObject userObject = new JSONObject();
			userObject.put("username", username);
			userObject.put("isAdmin", isAdmin.equals("true"));
			userObject.put("isCreator", isCreator.equals("true"));
			usersArray.put(userObject);
		}
		
		return usersArray;
	}
	
	public void addUser(String username, String password) throws SQLException {
		// Hash the password
		String passwordHash = DigestUtils.sha1Hex(ServerConfig.getInstance().passwordSalt + password).toString();

		PreparedStatement stmt = con.prepareStatement("INSERT INTO users (username, passwordhash, isadmin, iscreator) VALUES (?, ?, 'false', 'false')");
		stmt.setString(1, username);
		stmt.setString(2, passwordHash);
		stmt.executeUpdate();
	}
	
	public void deleteUser(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM users WHERE username = ?");
		stmt.setString(1, username);
		stmt.executeUpdate();
	}

	public void makeUserAdmin(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE users SET isadmin = 'true' WHERE username = ?");
		stmt.setString(1, username);
		stmt.executeUpdate();
	}

	public void revokeUserAdmin(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE users SET isadmin = 'false' WHERE username = ?");
		stmt.setString(1, username);
		stmt.executeUpdate();
	}

	public void makeUserCreator(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE users SET iscreator = 'true' WHERE username = ?");
		stmt.setString(1, username);
		stmt.executeUpdate();
	}

	public void revokeUserCreator(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE users SET iscreator = 'false' WHERE username = ?");
		stmt.setString(1, username);
		stmt.executeUpdate();
	}
	
	public void modifyRepositoryOwner(String repositoryAlias, String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE repositories SET repositoryowner = ? WHERE repositoryalias = ?");
		stmt.setString(1, username);
		stmt.setString(2, repositoryAlias);
		stmt.executeUpdate();
	}

	public void addUserToRepository(String username, String repositoryAlias) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO useraccess (username, repositoryalias) VALUES (?, ?)");
		stmt.setString(1, username);
		stmt.setString(2, repositoryAlias);
		stmt.executeUpdate();
	}

	public void deleteUserFromRepository(String username, String repositoryAlias) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM useraccess WHERE username = ? AND repositoryalias = ?");
		stmt.setString(1, username);
		stmt.setString(2, repositoryAlias);
		stmt.executeUpdate();
	}
		
	public String getNewSessionIdForCorrectLogin(String username, String password) throws SQLException {
		String passwordHash = DigestUtils.sha1Hex(ServerConfig.getInstance().passwordSalt + password).toString();

		PreparedStatement stmt = con.prepareStatement("SELECT username FROM users WHERE username = ? AND passwordhash = ?");		
		stmt.setString(1, username);
		stmt.setString(2, passwordHash);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			// User/pass correct -> create a new session
			return getRandomHexString(32);
		}
		else {
			// User/pass doesn't exist
			return null;
		}
	}
	
	public void persistSessionIdForUser(String sessionId, String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO usersessions (sessionid, username, expires) VALUES (?, ?, NOW() + INTERVAL 1 DAY)");
		stmt.setString(1, sessionId);
		stmt.setString(2, username);
		stmt.executeUpdate();
	}
	
	/* AUTHORIZATION CHECKS */
	
	public boolean doesUserHaveRepositoryAccess(String username, String repositoryAlias) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT username FROM useraccess WHERE username = ? AND repositoryalias = ?");
		stmt.setString(1, username);
		stmt.setString(2, repositoryAlias);
		ResultSet rs = stmt.executeQuery();
		return rs.next();
	}

	public boolean isUserAdmin(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT username FROM users WHERE username = ? and isadmin = 'true'");
		stmt.setString(1, username);
		ResultSet rs = stmt.executeQuery();
		return rs.next();
	}

	public boolean isUserCreator(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT username FROM users WHERE username = ? and iscreator = 'true'");
		stmt.setString(1, username);
		ResultSet rs = stmt.executeQuery();
		return rs.next();
	}

	public boolean isUserRepositoryOwner(String username, String repositoryAlias) throws SQLException {
		if (username == null) return false;
		PreparedStatement stmt = con.prepareStatement("SELECT repositoryowner FROM repositories WHERE repositoryowner = ? AND repositoryalias = ?");
		stmt.setString(1, username);
		stmt.setString(2, repositoryAlias);
		ResultSet rs = stmt.executeQuery();
		return rs.next();
	}

	public String getUsername(String sessionId) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT username FROM usersessions WHERE sessionid = ?");
		stmt.setString(1, sessionId);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return rs.getString("username");
		}
		else {
			return null;
		}
	}

	public int getRepositoryCloneCount(String repositoryAlias) throws Exception {
		PreparedStatement stmt = con.prepareStatement("SELECT clonecount FROM repositories WHERE repositoryalias = ?");
		stmt.setString(1, repositoryAlias);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return rs.getInt("clonecount");
		}
		else {
			throw new Exception("No Clone Count found");
		}
	}

	public void incRepositoryCloneCount(String repositoryAlias) throws Exception {
		PreparedStatement stmt = con.prepareStatement("UPDATE repositories SET clonecount = clonecount + 1 WHERE repositoryalias = ?");
		stmt.setString(1, repositoryAlias);
		stmt.executeUpdate();
	}
	
	/* THE RANDOM STRING FUNCTION FOR SESSION IDS */
	
	private String getRandomHexString(int numchars){
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars);
    }
	
}

