package ch.ethz.fgremper.rtca;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.SQLException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class OriginUpdater {

	private static final Logger log = LogManager.getLogger(OriginUpdater.class);
	
	public static void update(String repositoryAlias, String repositoryUrl) {
		DatabaseConnection db = null;
		try {
			db = new DatabaseConnection();
			
			String originStorageDirectory = ServerConfig.getInstance().originStorageDirectory;
			
			log.info("Cloning repository \"" + repositoryAlias + "\": " + repositoryUrl);

			int oldCount = db.getRepositoryCloneCount(repositoryAlias);
			String repositoryNewOriginDirectory = originStorageDirectory + "/" + repositoryAlias + "." + (oldCount + 1);
			String repositoryOldOriginDirectory = originStorageDirectory + "/" + repositoryAlias + "." + oldCount;
			
			// Create directory to clone repository in
			File userDir = new File(repositoryNewOriginDirectory);
			userDir.mkdir();
			FileUtils.cleanDirectory(userDir); 
			
			// Clone repository
			executeCommand("git clone " + repositoryUrl + " " + repositoryNewOriginDirectory);
			
			log.info("Reading repository \"" + repositoryAlias + "\"");
			// Read repository information like we would normally
			RepositoryReader repositoryReader = new RepositoryReader(repositoryNewOriginDirectory);
			JSONObject updateObject = repositoryReader.getUpdateObject();
			String inputJsonString = updateObject.toString();
			log.info("JSON string: " + inputJsonString);
			
			// Inserting into database
			log.info("Doing database stuff");
			db.startTransaction();
			
			// Create database user if it doesn't exist
			// This can throw an exception if the user already exists
			try { db.addUserToRepository("origin", repositoryAlias); }
			catch (Exception e) { }

			db.setEntireUserGitState(inputJsonString, "origin", repositoryAlias);
			db.commitTransaction();
			
			db.incRepositoryCloneCount(repositoryAlias);
			db.commitTransaction();
			
			FileUtils.forceDelete(new File(repositoryOldOriginDirectory)); 
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
	}
	
	public static void executeCommand(String consoleInput) throws Exception {
		log.info("Executing: " + consoleInput);
		Process p = Runtime.getRuntime().exec(consoleInput);
		p.waitFor();

		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line;
		while ((line = reader.readLine()) != null) {
			log.info("Console: " + line);
		}
	}
	
}
