package ch.ethz.fgremper.rtca;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.fgremper.rtca.helper.JSONHelper;

public class DatabaseConnection {

	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	static final String DB_URL = "jdbc:mysql://localhost/cloudstudio";
	static final String USER = "dbadmin";
	static final String PASS = "";

	Connection con = null;

	public DatabaseConnection() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");

		System.out.println("[DatabaseConnection] Connecting to database...");
		con = DriverManager.getConnection(DB_URL, USER,PASS);
		System.out.println("[DatabaseConnection] Connected to database.");
	}

	public void startTransaction() throws SQLException {
		con.setAutoCommit(false);
	}

	public void commitTransaction() throws SQLException {
		con.commit();
	}

	public void rollbackTransaction() throws SQLException {
		con.rollback();
	}

	public void deleteAllFilesFromRepositoryAndUser(String repositoryAlias, String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM files WHERE repositoryalias = ? AND username = ?");

		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);

		int rowsAffected = stmt.executeUpdate();

	
		stmt = con.prepareStatement("DELETE FROM commithistory WHERE repositoryalias = ? AND username = ?");

		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);

		rowsAffected = stmt.executeUpdate();
	}

	public void storeFile(String repositoryAlias, String username, String filename, String sha, String branch, String commit) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO files (repositoryalias, username, filename, sha, branch, commit) VALUES (?, ?, ?, ?, ?, ?)");

		System.out.println("FILE: " + filename);
		stmt.setString(1, repositoryAlias);
		stmt.setString(2, username);
		stmt.setString(3, filename);
		stmt.setString(4, sha);
		stmt.setString(5, branch);
		stmt.setString(6, commit);

		int rowsAffected = stmt.executeUpdate();
	}
	
	public void storeCommitHistory(String repositoryAlias, String username, String commit, List<String> downstreamCommits) throws SQLException {

		PreparedStatement downstreamCommitInsertStatement = con.prepareStatement("INSERT INTO commithistory (repositoryalias, username, commit, downstreamcommit) VALUES (?, ?, ?, ?)");
		
		downstreamCommitInsertStatement.setString(1, repositoryAlias);
		downstreamCommitInsertStatement.setString(2, username);
		downstreamCommitInsertStatement.setString(3, commit);
		
		for (String downstreamCommit : downstreamCommits) {
			downstreamCommitInsertStatement.setString(4, downstreamCommit);
			downstreamCommitInsertStatement.executeUpdate();
		}
		
	}
	
	public JSONArray getRepositories(String sessionId) throws SQLException {
		JSONArray repositoriesArray = new JSONArray();
		
		PreparedStatement stmt = con.prepareStatement("SELECT DISTINCT repositories.repositoryalias, repositories.repositoryurl, useraccess.username FROM repositories LEFT OUTER JOIN useraccess ON repositories.repositoryalias = useraccess.repositoryalias WHERE EXISTS (SELECT users.username FROM users JOIN usersessions AS us1 ON users.username = us1.username WHERE users.isadmin = 'true' AND us1.sessionid = ?) OR EXISTS (SELECT useraccess.username FROM useraccess JOIN usersessions AS us2 ON useraccess.username = us2.username WHERE us2.sessionid = ? AND useraccess.repositoryalias = repositories.repositoryalias)");

		stmt.setString(1, sessionId);
		stmt.setString(2, sessionId);
		
		ResultSet rs = stmt.executeQuery();
		HashMap<String, JSONObject> index = new HashMap<String, JSONObject>();
		while (rs.next()) {
			String repositoryAlias = rs.getString("repositoryalias");
			String repositoryUrl = rs.getString("repositoryurl");
			String username = rs.getString("username");
			try {
				if (!index.containsKey(repositoryAlias)) {
					JSONObject repositoryObject = new JSONObject();
					index.put(repositoryAlias, repositoryObject);
					repositoryObject.put("repositoryAlias", repositoryAlias);
					repositoryObject.put("repositoryUrl", repositoryUrl);
					repositoryObject.put("users", new JSONArray());
					repositoriesArray.put(repositoryObject);
				}
				if (username != null) {
					index.get(repositoryAlias).getJSONArray("users").put(username);
				}
			} catch (JSONException e) {
				System.err.println("Error while creating JSON string.");
				e.printStackTrace();
			}
		}
		
		return repositoriesArray;
	}
	
	public JSONArray getFileConflicts() throws SQLException {
		JSONArray fileConflicts = new JSONArray();
		
		/*
		PreparedStatement stmt = con.prepareStatement("select f1.repositoryalias, f1.filename, f1.username as username1, f2.username as username2, f1.branch as branch1, f2.branch as branch2 from files as f1 " +
			"cross join files as f2 " +
			"where f1.repositoryalias = 'testrepo' " +
			"and f1.repositoryalias = f2.repositoryalias " +
			"and f1.filename = f2.filename " +
			"and f1.username < f2.username " +
			"and f1.sha <> f2.sha " +
			"and f1.branch = f2.branch");
		*/
		PreparedStatement stmt = con.prepareStatement("select f1.repositoryalias, f1.filename, f1.username as username1, f2.username as username2, f1.branch as branch1, f2.branch as branch2 from files as f1 " +
				"cross join files as f2 " +
				"where f1.repositoryalias = 'testrepo' " +
				"and f1.repositoryalias = f2.repositoryalias " +
				"and f1.filename = f2.filename " +
				"and f1.username < f2.username " +
				"and f1.sha <> f2.sha " +
				"and f1.branch = f2.branch " +
				"and f1.commit not in (SELECT h1.downstreamcommit FROM commithistory as h1 WHERE h1.commit = f2.commit) " +
				"and f2.commit not in (SELECT h2.downstreamcommit FROM commithistory as h2 WHERE h2.commit = f1.commit)");
		
		HashMap<List<String>, JSONObject> map = new HashMap<List<String>, JSONObject>();
		
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			JSONObject fileConflict = new JSONObject();
			String filename = rs.getString("filename");
			String branch = rs.getString("branch1");
			String username1 = rs.getString("username1");
			String username2 = rs.getString("username2");
			
			try {
				
				List<String> key = Arrays.asList(filename, branch);
				
				if (!map.containsKey(key)) {
					map.put(key, fileConflict);
					fileConflict.put("filename", filename);
					fileConflict.put("branch", branch);
					
					JSONArray involvedUsers = new JSONArray();
					
					fileConflict.put("involvedUsers", involvedUsers);
					involvedUsers.put(username1);
					involvedUsers.put(username2);

					fileConflicts.put(fileConflict);
				}
				else {
					JSONArray involvedUsers = map.get(key).getJSONArray("involvedUsers");
					if (!JSONHelper.contains(involvedUsers, username1)) involvedUsers.put(username1);
					if (!JSONHelper.contains(involvedUsers, username2)) involvedUsers.put(username2);
					
				}
			} catch (JSONException e) {
				System.err.println("Error while creating JSON string.");
				e.printStackTrace();
			}
		}
		
		return fileConflicts;
	}
	
	public void addRepository(String repositoryAlias, String repositoryUrl) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO repositories (repositoryalias, repositoryurl) VALUES (?, ?)");

		stmt.setString(1, repositoryAlias);
		stmt.setString(2, repositoryUrl);
		
		stmt.executeUpdate();
	}
	
	public void deleteRepository(String repositoryAlias) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM repositories WHERE repositoryalias = ?");

		stmt.setString(1, repositoryAlias);
		
		stmt.executeUpdate();
	}
	
	public String addUser(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO users (username, passwordhash, isadmin) VALUES (?, ?, 'false')");

		String password = getRandomHexString(4);
		
		stmt.setString(1, username);
		stmt.setString(2, password);
		
		stmt.executeUpdate();
		
		return password;
	}
	
	public void deleteUser(String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("DELETE FROM users WHERE username = ?");

		stmt.setString(1, username);
		
		stmt.executeUpdate();
	}
	

	public JSONArray getUsers() throws SQLException {
		JSONArray usersArray = new JSONArray();
		
		PreparedStatement stmt = con.prepareStatement("SELECT username, isadmin FROM users");
		
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String username = rs.getString("username");
			String isAdmin = rs.getString("isadmin");
			try {
				JSONObject userObject = new JSONObject();
				userObject.put("username", username);
				userObject.put("isAdmin", isAdmin.equals("true"));
				usersArray.put(userObject);
			} catch (JSONException e) {
				System.err.println("Error while creating JSON string.");
				e.printStackTrace();
			}
		}
		
		return usersArray;
	}
	
	public void resetDatabase() throws SQLException {
		PreparedStatement stmt = con.prepareStatement("TRUNCATE TABLE files");
		stmt.executeUpdate();
		stmt = con.prepareStatement("TRUNCATE TABLE repositories");
		stmt.executeUpdate();
		stmt = con.prepareStatement("TRUNCATE TABLE commithistory");
		stmt.executeUpdate();
	}
	
	public String getNewSessionIdForCorrectLogin(String username, String passwordHash) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("SELECT username FROM users WHERE username = ? AND passwordhash = ?");

		stmt.setString(1, username);
		stmt.setString(2, passwordHash);
		
		ResultSet rs = stmt.executeQuery();
		// username/password correct
		if (rs.next()) {
			// create a session ID
			return getRandomHexString(32);
		}
		else {
			// give no session back
			return null;
		}
	}
	
	public void persistSessionIdForUser(String sessionId, String username) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("INSERT INTO usersessions (sessionid, username, expires) VALUES (?, ?, NOW() + INTERVAL 1 DAY)");

		stmt.setString(1, sessionId);
		stmt.setString(2, username);
		
		stmt.executeUpdate();
	}
	
	public boolean isUserAuthorized(String sessionId, String repositoryAlias) throws SQLException {

		PreparedStatement stmt = con.prepareStatement("SELECT username FROM usersessions JOIN useraccess ON usersessions.username = useraccess.username WHERE usersessions.sessionid = ? AND useraccess.repositoryalias = ?");

		stmt.setString(1, sessionId);
		stmt.setString(2, repositoryAlias);
		
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		else {
			return false;
		}
		
	}

	public boolean isUserAdmin(String sessionId) throws SQLException {
		
		PreparedStatement stmt = con.prepareStatement("SELECT users.username FROM usersessions JOIN users ON usersessions.username = users.username WHERE usersessions.sessionid = ? and users.isadmin = 'true'");

		stmt.setString(1, sessionId);
		
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			return true;
		}
		else {
			return false;
		}
		
	}

	private String getRandomHexString(int numchars){
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars);
    }
}

