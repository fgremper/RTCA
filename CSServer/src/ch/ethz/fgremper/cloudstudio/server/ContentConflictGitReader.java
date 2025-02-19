package ch.ethz.fgremper.cloudstudio.server;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.JSONArray;

/**
 * 
 * Makes a three-way file comparison by finding a common ancestor of two file versions.
 * 
 * @author Fabian Gremper
 *
 */
public class ContentConflictGitReader {

	private static final Logger log = LogManager.getLogger(ContentConflictGitReader.class);
	
	private final String emptySha = DigestUtils.sha1Hex("").toString();
	
	private final String fileStorageDirectory = ServerConfig.getInstance().fileStorageDirectory;
	private final String originStorageDirectory = ServerConfig.getInstance().originStorageDirectory;

	private String mySha = null;
	private String theirSha = null;
	private String ancestorSha = null;
	
	/**
	 * 
	 * Initalize ContentConflictGitReader. Sets mySha, theirSha and ancestorSha.
	 * 
	 * @param repositoryAlias
	 * @param branch
	 * @param filename
	 * @param myUsername
	 * @param compareToBranch
	 * @param theirUsername
	 * @param showUncommitted
	 * 
	 */
	public ContentConflictGitReader(String repositoryAlias, String branch, String filename, String myUsername, String compareToBranch, String theirUsername, boolean showUncommitted) {

		log.debug("Reading conflicts for: " + filename);
		DatabaseConnection db = new DatabaseConnection();
		try {
			
			// Get database connection
			db.getConnection();
			
			// Retrieve my SHA and their SHA
			mySha = db.getFileSha(repositoryAlias, myUsername, branch, filename, showUncommitted);
		 	theirSha = db.getFileSha(repositoryAlias, theirUsername, compareToBranch, filename, showUncommitted);

		 	// If we didn't find our file, we set it to be an empty file
		 	if (mySha == null) {
		 		mySha = emptySha;
		 		createEmptyFile();
		 	}

		 	// If we didn't find their file, we set it to be an empty file
		 	if (theirSha == null) {
		 		theirSha = emptySha;
		 		createEmptyFile();
		 	}
		 	
		 	// Get commit IDs of the files we just read
			String myCommitId = db.getCommitForBranchAndFile(repositoryAlias, myUsername, branch, filename);
			String theirCommitId = db.getCommitForBranchAndFile(repositoryAlias, theirUsername, compareToBranch, filename);

			// Find the merge base commit ID
			String mergeBaseCommitId = db.getMergeBaseCommitId(repositoryAlias, myCommitId, theirCommitId);
			
			if (mergeBaseCommitId != null) {

				String repositoryOriginDirectory = originStorageDirectory + File.separator + DigestUtils.sha1Hex(repositoryAlias).toString() + "." + db.getRepositoryCloneCount(repositoryAlias);

				// Open the repository in JGit
				log.debug("Reading: " + repositoryOriginDirectory);
				FileRepositoryBuilder builder = new FileRepositoryBuilder();
				builder.setGitDir(new File(repositoryOriginDirectory + File.separator + ".git"));
				builder.setMustExist(true);
				builder.readEnvironment(); // scan environment GIT_* variables
				builder.findGitDir(); // scan up the file system tree
				Repository repository = builder.build();
		
				// Check if this an actual git repository?
				if (repository.getDirectory() == null) {
					throw new Exception("Not a git repository: " + repositoryOriginDirectory);
				}
		        log.debug("Opened in JGit: " + repository.getDirectory());
		
		        // Walk
				ObjectId id = repository.resolve(mergeBaseCommitId);
				log.debug("Id: " + id);
				RevWalk walk = new RevWalk(repository);
				RevCommit commit = walk.parseCommit(id);
				log.debug("Commit: " + commit);
		        RevTree tree = commit.getTree();
		        
		        // Get a TreeWalk to walk through all the files of the commit
		        TreeWalk treeWalk = new TreeWalk(repository);
		        treeWalk.addTree(tree);
		        treeWalk.setRecursive(true);
		
		        // Iterate through the files (maybe some way to find it faster?)
		        boolean ancestorFound = false;
		        while (treeWalk.next() && !ancestorFound) {
		        	
		        	// Found common ancestor file
		        	if (treeWalk.getPathString().equals(filename)) {
		        		
		        		// Read contents
		            	ObjectId objectId = treeWalk.getObjectId(0);
		            	ObjectLoader loader = repository.open(objectId);
		            	InputStream fileInputStream = loader.openStream();
		            	String ancestorFileContent = IOUtils.toString(fileInputStream, "UTF-8");
		            	
		            	// Set ancestor SHA and write content to file
		            	ancestorSha = DigestUtils.sha1Hex(ancestorFileContent).toString();
		            	File ancestorFile = new File(fileStorageDirectory + File.separator + ancestorSha);
		            	if (!ancestorFile.exists()) {
		            		FileUtils.writeStringToFile(ancestorFile, ancestorFileContent);
		            	}
		            	
		    			ancestorFound = true;
		    			
		        	}
		        	
		        }
		        
		        if (!ancestorFound) {
		        	// No common ancestor file found
	            	ancestorSha = emptySha;
	            	createEmptyFile();
		        }
        
			}
			else {
				// No common merge found
            	ancestorSha = emptySha;
            	createEmptyFile();
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
	 * Return a line-by-line side-by-side comparison of all three files.
	 * 
	 * @return JSONArray of lines.
	 * 
	 */
	public JSONArray diff() throws Exception {

		log.debug("Doing diff...");
		
		// If there has been a jgit error, the ancestorSha is null
		// Just so we don't display anything at all, set the ancestor sha to an empty file
		// A case where jgit can fail is when the closest merge commit is in a commit that hasn't been
		// pushed to the origin
		if (ancestorSha == null) {
        	ancestorSha = emptySha;
        	createEmptyFile();
		}

		if (mySha != null && theirSha != null && ancestorSha != null) {
			return SideBySideThreeWayDiff.diff(fileStorageDirectory + File.separator + mySha, fileStorageDirectory + File.separator + ancestorSha, fileStorageDirectory + File.separator + theirSha);
		}
		else {
			throw new Exception("Not all file SHAs set");
		}
	}
	
	/**
	 * 
	 * Count number of conflicts in a three-way diff.
	 * 
	 * @return Number of conflicting blocks
	 * 
	 */
	public Integer countConflicts() throws Exception {
		if (mySha != null && theirSha != null && ancestorSha != null) {
			return SideBySideThreeWayDiff.countConflicts(fileStorageDirectory + File.separator + mySha, fileStorageDirectory + File.separator + ancestorSha, fileStorageDirectory + File.separator + theirSha);
		}
		else {
			// If in doubt, don't display a content conflict
			return 0;
		}
	}
	
	/**
	 * 
	 * Create an empty file for the SHA of "".
	 * 
	 */
	private void createEmptyFile() throws Exception {
		File emptyFile = new File(fileStorageDirectory + File.separator + emptySha);
		if (!emptyFile.exists()) {
			FileUtils.writeStringToFile(emptyFile, "");
		}
	}
	
}
