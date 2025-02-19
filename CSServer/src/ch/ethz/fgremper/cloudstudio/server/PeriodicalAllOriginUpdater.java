package ch.ethz.fgremper.cloudstudio.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * Periodically calls the OriginUpdater.
 * 
 * @author Fabian Gremper
 * 
 */
public class PeriodicalAllOriginUpdater implements Runnable {

	private static final Logger log = LogManager.getLogger(PeriodicalAllOriginUpdater.class);
	
	private int originUpdateInterval = ServerConfig.getInstance().originUpdateInterval;
	
	/**
	 * 
	 * Periodically run updateAll()
	 * 
	 */
	public void run() {
		
		while (true) {
			
			// Update origins
			log.info("Updating all origins...");
			updateAll();
			
			// Sleep
			log.info("Waiting " + originUpdateInterval + " seconds before updating origins again...");
			try {
				Thread.sleep(originUpdateInterval * 1000);
			}
			catch (InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
		
	}
	
	/**
	 * 
	 * Go through all the repositories and call the OriginUpdater.
	 * 
	 */
	public void updateAll() {
		
		DatabaseConnection db = new DatabaseConnection();
		
		JSONArray repositoriesArray;
		
		// Reading all repositories from database
		try {
			db.getConnection();
			repositoriesArray = db.getAllRepositories();
		}
		catch (Exception e) {
			log.error("Error reading repositories from database...");
			e.printStackTrace();
			return;
		}

		// Close database connection
		db.closeConnection();
				
		// Go through all the repositories and clone them
		for (int i = 0; i < repositoriesArray.length(); i++) {
			try {
				JSONObject repositoryObject = repositoriesArray.getJSONObject(i);
				String repositoryAlias = repositoryObject.getString("repositoryAlias");
				String repositoryUrl = repositoryObject.getString("repositoryUrl");
				OriginUpdater.update(repositoryAlias, repositoryUrl);
			}
			catch (Exception e) {
				// Error reading a repository
			}
		}
	}
}
