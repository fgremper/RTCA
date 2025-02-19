package ch.ethz.fgremper.cloudstudio.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import ch.ethz.fgremper.cloudstudio.common.RepositoryReader;

/**
 * 
 * Clone the origin from the repository URL and write the information to the database.
 * 
 * @author Fabian Gremper
 *
 */
public class OriginUpdater {

	private static final Logger log = LogManager.getLogger(OriginUpdater.class);

	private static final String originStorageDirectory = ServerConfig.getInstance().originStorageDirectory;
	
	public static void update(String repositoryAlias, String repositoryUrl) {
		
		// Don't have a repository URL? Don't try to fetch it.
		if (repositoryUrl == null || repositoryUrl.equals("")) return;
		
		DatabaseConnection db = new DatabaseConnection();
		try {
			
			// Get connection.
			db.getConnection();
			
			log.info("Cloning repository \"" + repositoryAlias + "\": " + repositoryUrl);

			// Get directories of old a new origin location
			int oldCount = db.getRepositoryCloneCount(repositoryAlias);
			String repositoryNewOriginDirectory = originStorageDirectory + File.separator + DigestUtils.sha1Hex(repositoryAlias).toString() + "." + (oldCount + 1);
			String repositoryOldOriginDirectory = originStorageDirectory + File.separator + DigestUtils.sha1Hex(repositoryAlias).toString() + "." + oldCount;

			// Create directory to clone repository in
			File userDir = new File(repositoryNewOriginDirectory);
			userDir.mkdir();
			FileUtils.cleanDirectory(userDir); 
			
			// Clone repository
			executeCommand("git clone " + repositoryUrl + " " + repositoryNewOriginDirectory);

			// Read repository information like we would normally
			log.info("Reading repository \"" + repositoryAlias + "\"");
			RepositoryReader repositoryReader = new RepositoryReader(repositoryNewOriginDirectory);
			JSONObject updateObject = repositoryReader.getUpdateObject();
			String inputJsonString = updateObject.toString();
			// log.info("JSON string: " + inputJsonString); // don't log this, because it can be way too long
			
			// Inserting into database
			log.info("Doing database stuff");
			db.startTransaction();
			db.setEntireUserGitState(inputJsonString, "origin", repositoryAlias);
			db.commitTransaction();
			
			// Everything was successful, let's increment the counter
			db.startTransaction();
			db.incRepositoryCloneCount(repositoryAlias);
			db.commitTransaction();
			
			// And delete the old repository directory
			try {
				FileUtils.forceDelete(new File(repositoryOldOriginDirectory));
			}
			catch (Exception e) {
				// Old folder not deleted
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// Close database connection
		db.closeConnection();
		
	}
	
	/**
	 * 
	 * Execute console command.
	 * 
	 * @param consoleInput
	 * 
	 */
	private static void executeCommand(String consoleInput) throws Exception {
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
