package ch.ethz.fgremper.rtca;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.JSONArray;
import org.json.JSONObject;

public class GitReader {

	private String mySha = null;
	private String theirSha = null;
	private String ancestorSha = null;

	String fileStorageDirectory = ServerConfig.getInstance().fileStorageDirectory;
	
	public GitReader(String repositoryAlias, String branch, String filename, String sessionUsername, String compareToBranch, String theirUsername, boolean showUncommitted) {


		DatabaseConnection db = null;
		try {
			// Get database connection
			db = new DatabaseConnection();
			
			mySha = db.getFileSha(repositoryAlias, sessionUsername, branch, filename, showUncommitted);
		 	theirSha = db.getFileSha(repositoryAlias, theirUsername, compareToBranch, filename, showUncommitted);
			
			
			
			String commit1 = db.getCommitForBranchAndFile(repositoryAlias, sessionUsername, branch, filename);
			System.out.println("[RequestHttpHandler] C1: " + commit1);
			String myFileContent = FileUtils.readFileToString(new File(fileStorageDirectory + "/" + db.getFileSha(repositoryAlias, sessionUsername, branch, filename, showUncommitted)), "UTF-8");
			System.out.println("Content: " + myFileContent);
			String commit2 = db.getCommitForBranchAndFile(repositoryAlias, theirUsername, compareToBranch, filename);
			System.out.println("[RequestHttpHandler] C2: " + commit2);
			String theirFileContent = FileUtils.readFileToString(new File(fileStorageDirectory + "/" + db.getFileSha(repositoryAlias, theirUsername, compareToBranch, filename, showUncommitted)), "UTF-8");
			System.out.println("Content: " + theirFileContent);
			String mergeBaseCommitId = db.getMergeBaseCommitId(repositoryAlias, commit1, commit2);
			System.out.println("[RequestHttpHandler] MC: " + mergeBaseCommitId);
			
			if (mergeBaseCommitId != null) {
				// get origin storage dir
				String originStorageDirectory = ServerConfig.getInstance().originStorageDirectory;
				String repositoryOriginDirectory = originStorageDirectory + "/" + repositoryAlias + "." + db.getRepositoryCloneCount(repositoryAlias);
				
				// Open the repository in JGit
				System.out.println("[RequestHttpHandler] Reading: " + repositoryOriginDirectory);
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				builder.setGitDir(new File(repositoryOriginDirectory + "/.git"));
				builder.setMustExist(true);
				builder.readEnvironment(); // scan environment GIT_* variables
				builder.findGitDir(); // scan up the file system tree
				Repository repository = builder.build();
		
				// Check, is this an actual git repository?
				if (repository.getDirectory() == null) {
					throw new Exception("Not a git repository: " + repositoryOriginDirectory);
				}
		        System.out.println("[RequestHttpHandler] Opened in jgit: " + repository.getDirectory());
		
		        // Walk
				ObjectId id = repository.resolve(mergeBaseCommitId);
				System.out.println("[RequestHttpHandler] Id: " + id);
				RevWalk walk = new RevWalk(repository);
				RevCommit commit = walk.parseCommit(id);
				System.out.println("[RequestHttpHandler] Commit: " + commit);
		        RevTree tree = commit.getTree();
		        
		        // Get a TreeWalk to walk through all the files of the commit
		        TreeWalk treeWalk = new TreeWalk(repository);
		        treeWalk.addTree(tree);
		        treeWalk.setRecursive(true);
		
		        // Iterate through the files (maybe some way to find it faster?)
		        while (treeWalk.next()) {
		        	if (treeWalk.getPathString().equals(filename)) {
		        		System.out.println("found file");
		        		
		            	ObjectId objectId = treeWalk.getObjectId(0);
		            	ObjectLoader loader = repository.open(objectId);
		            	InputStream fileInputStream = loader.openStream();
		            	String ancestorFileContent = IOUtils.toString(fileInputStream, "UTF-8");
		            	
		            	System.out.println("Content MC: " + ancestorFileContent);
		            	
		            	ancestorSha = DigestUtils.sha1Hex(ancestorFileContent).toString();
		    			FileUtils.writeStringToFile(new File(fileStorageDirectory + "/" + ancestorSha), ancestorFileContent);
		
		        	}
		        }
        
			}
			else {
				// no merge base found! -> empty file or what is the plan?!!?
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
		
		
	}
	
	public JSONArray diff() throws Exception {
		return SideBySideThreeWayDiff.diff(fileStorageDirectory + "/" + mySha, fileStorageDirectory + "/" + ancestorSha, fileStorageDirectory + "/" + theirSha);
			
	}
	
	public Integer countConflicts() throws Exception {

		return SideBySideThreeWayDiff.countConflicts(fileStorageDirectory + "/" + mySha, fileStorageDirectory + "/" + ancestorSha, fileStorageDirectory + "/" + theirSha);
		
	}
	
}
