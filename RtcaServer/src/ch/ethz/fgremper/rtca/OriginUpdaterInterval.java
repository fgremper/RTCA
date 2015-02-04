package ch.ethz.fgremper.rtca;

import org.json.JSONArray;
import org.json.JSONObject;

public class OriginUpdaterInterval {
	public OriginUpdaterInterval() {
		
	}
	
	public void updateAll() {
		try {
			DatabaseConnection db = new DatabaseConnection();
			JSONArray repositoriesArray = db.getAllRepositories();
			for (int i = 0; i < repositoriesArray.length(); i++) {
				JSONObject repositoryObject = repositoriesArray.getJSONObject(i);
				String repositoryAlias = repositoryObject.getString("repositoryAlias");
				String repositoryUrl = repositoryObject.getString("repositoryUrl");
				OriginUpdater originUpdater = new OriginUpdater(repositoryAlias, repositoryUrl);
				new Thread(originUpdater).start();
			}
		}
		catch (Exception e) {
			
		}
	}
}
