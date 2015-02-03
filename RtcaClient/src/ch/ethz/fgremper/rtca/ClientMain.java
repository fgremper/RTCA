package ch.ethz.fgremper.rtca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.JSONArray;
import org.json.JSONObject;

public class ClientMain {

	public static void main(String[] args) throws Exception {

		// Read the config
		String configFileName = "config.xml";
		if (args.length >= 1) configFileName = args[0];
		ClientConfig config = new ClientConfigReader(configFileName).getConfig();

		// Get our HTTP client
		HttpClient httpClient = new HttpClient();

		// Login and get a session ID
		String sessionId = httpClient.login(config.serverUrl, config.username, config.password);
		System.out.println("[Main] sessionId: " + sessionId);
		
		// For all repositories we're going to read the local data and send some of it to the server
		for (RepositoryInfo repositoryInfo : config.repositoriesList) {	
			
			// Read repository info
			RepositoryReader repositoryReader = new RepositoryReader(repositoryInfo.localPath);
			JSONObject updateObject = repositoryReader.getUpdateObject();
			
	        // Store user information
			updateObject.put("username", config.username);
			updateObject.put("sessionId", sessionId);
			updateObject.put("repositoryAlias", repositoryInfo.alias);
			
			// Send it to to the server
			String jsonString = updateObject.toString();
			httpClient.sendGitState(config.serverUrl, jsonString);
			
		}
		
	}

}
