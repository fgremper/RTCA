package ch.ethz.fgremper.rtca;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.JSONArray;
import org.json.JSONObject;

public class RepositoryReader {
	
	private String jsonString;
	
	public RepositoryReader(RepositoryInfo repositoryInfo, ClientConfig config, String sessionId) throws Exception {
		
		// Create the object that we're going to send to the server
		JSONObject updateObject = new JSONObject();
		
		// Add the file array and commit history to the object
		JSONArray fileArray = new JSONArray();
		JSONArray commitHistory = new JSONArray();
		JSONArray branches = new JSONArray();
        updateObject.put("files", fileArray);
        updateObject.put("commitHistory", commitHistory);
        updateObject.put("branches", branches);
        
        // Store user information
		updateObject.put("username", config.username);
		updateObject.put("sessionId", sessionId);
		updateObject.put("repositoryAlias", repositoryInfo.alias);
		
		// Open the repository in JGit
		System.out.println("[Repository] Reading: " + repositoryInfo.localPath);
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.setGitDir(new File(repositoryInfo.localPath + "/.git"));
		builder.setMustExist(true);
		builder.readEnvironment(); // scan environment GIT_* variables
		builder.findGitDir(); // scan up the file system tree
		Repository repository = builder.build();
		
		// Check, is this an actual git repository?
		if (repository.getDirectory() == null) {
			throw new Exception("Not a git repository: " + repositoryInfo.localPath);
		}
        System.out.println("[Repository] Opened in jgit: " + repository.getDirectory());
        
        // Get all references we have in this git repository
        Collection<Ref> refs = repository.getAllRefs().values();
        
        // List of all commits we came along
        List<String> usedCommitIds = new LinkedList<String>();
        
        // Loop through all references
        for (Ref ref : refs) {
        	System.out.println("[Repository] Ref: " + ref.getName());
        	
        	// Is this a reference to a local branch?
        	if (ref.getName().startsWith("refs/heads/")) {
        		
        		// Get branch name
        		String branchName = ref.getName().substring("ref/heads/".length() + 1);
        		boolean isActiveBranch = ref.getName().equals(repository.getFullBranch());
        		
        		// Get variables necessary to do the following actions
        		Ref branch = repository.getRef(ref.getName());
        		RevWalk walk = new RevWalk(repository);
        		RevCommit commit = walk.parseCommit(branch.getObjectId());
	            RevTree tree = commit.getTree();
	            String commitId = commit.getName();
	            System.out.println("[Repository] Commit: " + commit.getName());
	            
	            JSONObject branchObject = new JSONObject();
	            branchObject.put("branch", branchName);
	            branchObject.put("commit", commitId);
	            
	            // If we didn't already, add the current commit to the commit history
	            if (!usedCommitIds.contains(commitId)) {
	            	
	            	usedCommitIds.add(commitId);
	            	
	            	// New commit object
	            	JSONObject commitObject = new JSONObject();
	            	JSONArray downstreamCommitsObject = new JSONArray();
	            	

		            commitObject.put("commit", commitId);
		            commitObject.put("downstreamCommits", downstreamCommitsObject);
		            commitHistory.put(commitObject);
	            	

		            List<RevCommit> todoCommits = new LinkedList<RevCommit>();
		            List<String> downstreamCommits = new LinkedList<String>();
		            HashMap<RevCommit, Integer> distance = new HashMap<RevCommit, Integer>();
		            if (commit.getParents() != null) { 
		            	todoCommits.addAll(Arrays.asList(commit.getParents()));
		            	for (RevCommit c : Arrays.asList(commit.getParents())) {
		            		distance.put(c, 1);
		            		//System.out.println("Adding commit to todo: " + c.getName());
		            	}
		            }
		            while (!todoCommits.isEmpty()) {
		            	RevCommit pop = todoCommits.remove(0);
		            	RevCommit currentCommit = walk.parseCommit(pop.getId());

	            		//System.out.println("Looking for commit: " + pop.getName());
		            	Integer distanceToCurrentCommit = distance.get(pop);
	            		//System.out.println("Distance: " + distanceToCurrentCommit);
	            		
		            	if (!downstreamCommits.contains(currentCommit.getName())) {
			            	downstreamCommits.add(currentCommit.getName());
			            	JSONObject dc = new JSONObject();
			            	dc.put("commit", currentCommit.getName());
			            	dc.put("distance", distanceToCurrentCommit);
			            	downstreamCommitsObject.put(dc);
				            if (currentCommit.getParents() != null) {
				            	todoCommits.addAll(Arrays.asList(currentCommit.getParents()));
				            	for (RevCommit c : Arrays.asList(currentCommit.getParents())) {
				            		distance.put(c, distanceToCurrentCommit + 1);
				            		//System.out.println("SECOND Adding commit to todo: " + c.getName());
				            	}
				            }
		            	}
		            }
		            
		            System.out.println("COMMIT HISTORY: " + commitObject.toString());
		            
	            	/*
		            // Find downstream commits iteratively
		            List<String> downstreamCommits = new LinkedList<String>();
		            if (commit.getParents() != null) todoCommits.addAll(Arrays.asList(commit.getParents()));
		            while (!todoCommits.isEmpty()) {
		            	RevCommit pop = todoCommits.remove(0);
		            	RevCommit currentCommit = walk.parseCommit(pop.getId());
		            	if (!downstreamCommits.contains(currentCommit.getName())) {
			            	downstreamCommits.add(currentCommit.getName());
				            if (currentCommit.getParents() != null) todoCommits.addAll(Arrays.asList(currentCommit.getParents()));
		            	}
		            }
		            
		            // Log all the downstream commits
		            for (String u : downstreamCommits) {
		            	System.out.println("[Repository] Downstream commit: "  + u);
		            }*/
		            
		            // Add objects to objects
					
	            }
	            
	            // Get a TreeWalk to walk through all the files of the commit
	            TreeWalk treeWalk = new TreeWalk(repository);
	            treeWalk.addTree(tree);
	            treeWalk.setRecursive(true);
	            
	            // Iterate through the files and add them to the file array
	            while (treeWalk.next()) {
	            	
	            	// Get file name
	            	System.out.println("[Repository] File: " + treeWalk.getPathString());
					JSONObject fileObject = new JSONObject();
					fileObject.put("filename", treeWalk.getPathString());
					
					// Get file content
	            	ObjectId objectId = treeWalk.getObjectId(0);
	            	ObjectLoader loader = repository.open(objectId);
	            	InputStream fileInputStream = loader.openStream();
	            	String fileContent = IOUtils.toString(fileInputStream, "UTF-8");

	            	// Some other file properties
					fileObject.put("content", fileContent);
					fileObject.put("branch", branchName);
					fileObject.put("commit", commitId);
					
					// If we're in the active branch, there's separate info for uncommitted
					fileObject.put("committed", isActiveBranch ? "committed" : "both");
					
					// Store the file object in the file array
					fileArray.put(fileObject);
					
	            }
	            
	            if (isActiveBranch) {
	            	// Get a TreeWalk to get the file system content
	            	treeWalk = new TreeWalk(repository);
	            	treeWalk.setRecursive(true);
	            	treeWalk.addTree(new FileTreeIterator(repository));

		            // Iterate through the files and add them to the file array
		            while (treeWalk.next()) {
		            	
		            	// Get file name
		            	System.out.println("[Repository] Local File: " + treeWalk.getPathString());
						JSONObject fileObject = new JSONObject();
						fileObject.put("filename", treeWalk.getPathString());
						
						// Get file content
		            	ObjectId objectId = treeWalk.getObjectId(0);
		            	ObjectLoader loader = repository.open(objectId);
		            	InputStream fileInputStream = loader.openStream();
		            	String fileContent = IOUtils.toString(fileInputStream, "UTF-8");

		            	// Some other file properties
						fileObject.put("content", fileContent);
						fileObject.put("branch", branchName);
						fileObject.put("commit", commitId);
						fileObject.put("committed", "uncommitted");
						
						// Store the file object in the file array
						fileArray.put(fileObject);
						
		            }
	            }
        	}
            
        }

        // Create JSON string from object
        jsonString = updateObject.toString();
        System.out.println("[Repository] JSON String: " + jsonString);
        
	}
	
	public String getJsonString() {
		return jsonString;
	}
}
