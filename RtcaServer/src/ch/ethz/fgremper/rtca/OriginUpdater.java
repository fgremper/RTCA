package ch.ethz.fgremper.rtca;

import org.json.JSONObject;

public class OriginUpdater {
	private String repositoryAlias;
	private String repositoryUrl;
	
	public OriginUpdater(String repositoryAlias, String repositoryUrl) {
		this.repositoryAlias = repositoryAlias;
		this.repositoryUrl = repositoryUrl;
	}
	
	public void update() throws Exception {

		
		// first clone the repository to local
		
		// Read repository info
		RepositoryReader repositoryReader = new RepositoryReader("/Users/novocaine/Documents/masterthesis/testsandpit/john");
		JSONObject updateObject = repositoryReader.getUpdateObject();
		updateObject.put("username", "origin");
		updateObject.put("repositoryAlias", repositoryAlias);
		String jsonString = updateObject.toString();
		
		// TODO: outsource the thing that parses the json string on the server and we're done here
		
		// TODO: this should all happen in a separate thread!
		
	}
}
