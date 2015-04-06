package ch.ethz.fgremper.cloudstudio.testing.combination;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import ch.ethz.fgremper.cloudstudio.server.DatabaseConnection;
import ch.ethz.fgremper.cloudstudio.testing.helper.TestGitHelper;

public class FileLevelAwarenessTest {

	public JSONObject findItem(JSONArray array, String key, String value) throws Exception {
		for (int i = 0; i < array.length(); i++) {
			if (array.getJSONObject(i).getString(key).equals(value)) return array.getJSONObject(i);
		}
		return null;
	}

	
	@Test
	public void testBranchInternal() throws Exception {


		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		JSONObject responseObject;
		JSONArray filesArray;
		JSONObject fileObject;
		
		// Setup default state
		TestGitHelper.setupTest();
		TestGitHelper.runPlugins();

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// Setup default state
		TestGitHelper.createOrModifyFile("David", "default.txt");
		TestGitHelper.runPlugins();

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// Setup default state
		TestGitHelper.commit("David");
		TestGitHelper.runPlugins();

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// Setup default state
		TestGitHelper.push("David");
		TestGitHelper.runPlugins();

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));



		// Setup default state
		TestGitHelper.pull("John");
		TestGitHelper.runPlugins();

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "master", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		
		db.closeConnection();

	}

	
	

	@Test
	public void testBranchCompare() throws Exception {


		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		JSONObject responseObject;
		JSONArray filesArray;
		JSONObject fileObject;
		
		// Setup default state
		TestGitHelper.setupTest();
		TestGitHelper.createBranch("John", "new_branch");
		TestGitHelper.checkoutBranch("John", "new_branch");
		TestGitHelper.push("John");
		TestGitHelper.pull("David");
		TestGitHelper.pull("Isabelle");
		
		TestGitHelper.runPlugins();

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// Setup default state
		TestGitHelper.createOrModifyFile("David", "default.txt");
		TestGitHelper.runPlugins();

		//  No branch internal conflicts
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// But conflicts against master
		
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// Setup default state
		TestGitHelper.commit("David");
		TestGitHelper.runPlugins();



		//  No branch internal conflicts
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// But conflicts against master
		
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		
		// Setup default state
		TestGitHelper.push("David");
		TestGitHelper.runPlugins();



		//  No branch internal conflicts
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// But conflicts against master
		
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		
		
		


		// Setup default state
		TestGitHelper.checkoutBranch("John", "master");
		TestGitHelper.pull("John");
		TestGitHelper.checkoutBranch("John", "new_branch");
		TestGitHelper.merge("John", "master");
		TestGitHelper.runPlugins();

		




		//  No branch internal conflicts
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "new_branch", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));


		// But conflicts against master
		
		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", true, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		// Test default state
		responseObject = db.getFileLevelAwareness("TestRepository", "John", "new_branch", "master", false, false);
		filesArray = responseObject.getJSONArray("files");
		assertEquals(1, filesArray.length());
		fileObject = filesArray.getJSONObject(0);
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "John").getString("type"));
		assertEquals("NO_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "David").getString("type"));
		assertEquals("FILE_CONFLICT", findItem(fileObject.getJSONArray("users"), "username", "Isabelle").getString("type"));

		
		
		db.closeConnection();

	}

}
