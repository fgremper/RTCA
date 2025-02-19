package ch.ethz.fgremper.cloudstudio.testing.combination;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import ch.ethz.fgremper.cloudstudio.server.DatabaseConnection;
import ch.ethz.fgremper.cloudstudio.testing.helper.TestGitHelper;

/**
 * 
 * Combination test for branch awareness for a given scenario
 * 
 * @author Fabian Gremper
 *
 */
public class BranchLevelAwarenessTest {

	/**
	 * 
	 * Helper function to find an item in a JSONObject
	 * 
	 */
	public JSONObject findItem(JSONArray array, String key, String value) throws Exception {
		for (int i = 0; i < array.length(); i++) {
			if (array.getJSONObject(i).getString(key).equals(value)) return array.getJSONObject(i);
		}
		return null;
	}

	/**
	 * 
	 * Test the relationship with origin feature
	 *
	 */
	@Test
	public void testRelationWithOrigin() throws Exception {
		
		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		JSONObject responseObject;
		JSONArray branchesArray;
		JSONObject branchObject;
		
		// Setup default state
		TestGitHelper.setupTest();
		TestGitHelper.runPlugins();
		
		// Test default state
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(1, branchesArray.length());
		branchObject = branchesArray.getJSONObject(0);

		// Setup all equal
		assertEquals(3, branchObject.getJSONArray("activeUsers").length());
		assertEquals(3, branchObject.getJSONArray("users").length());
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));
		
		// Setup John 1 ahead
		TestGitHelper.createOrModifyFile("John", "default.txt");
		TestGitHelper.commit("John");
		TestGitHelper.runPlugins();

		// Test John 1 ahead
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(1, branchesArray.length());
		branchObject = branchesArray.getJSONObject(0);
		assertEquals("AHEAD", findItem(branchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals(1, findItem(branchObject.getJSONArray("users"), "username", "John").getInt("distanceFromOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		// Setup John 2 ahead
		TestGitHelper.createOrModifyFile("John", "default.txt");
		TestGitHelper.commit("John");
		TestGitHelper.runPlugins();

		// Test John 2 ahead
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(1, branchesArray.length());
		branchObject = branchesArray.getJSONObject(0);
		assertEquals("AHEAD", findItem(branchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals(2, findItem(branchObject.getJSONArray("users"), "username", "John").getInt("distanceFromOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));
		
		// Setup David 1 ahead
		TestGitHelper.createOrModifyFile("David", "default.txt");
		TestGitHelper.commit("David");
		TestGitHelper.runPlugins();

		// Test David 1 ahead
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(1, branchesArray.length());
		branchObject = branchesArray.getJSONObject(0);
		assertEquals("AHEAD", findItem(branchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals(2, findItem(branchObject.getJSONArray("users"), "username", "John").getInt("distanceFromOrigin"));
		assertEquals("AHEAD", findItem(branchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals(1, findItem(branchObject.getJSONArray("users"), "username", "David").getInt("distanceFromOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		// Setup David pushes
		TestGitHelper.push("David");
		TestGitHelper.runPlugins();

		// Test David pushes
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(1, branchesArray.length());
		branchObject = branchesArray.getJSONObject(0);
		assertEquals("FORK", findItem(branchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals(3, findItem(branchObject.getJSONArray("users"), "username", "John").getInt("distanceFromOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("BEHIND", findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));
		assertEquals(1, findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getInt("distanceFromOrigin"));
		
		// Setup Isabelle pulls
		TestGitHelper.pull("Isabelle");
		TestGitHelper.runPlugins();

		// Test Isabelle pulls
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(1, branchesArray.length());
		branchObject = branchesArray.getJSONObject(0);
		assertEquals("FORK", findItem(branchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals(3, findItem(branchObject.getJSONArray("users"), "username", "John").getInt("distanceFromOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("EQUAL", findItem(branchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		db.closeConnection();
		
	}
	

	/**
	 * 
	 * Tests for setting up new branches
	 *
	 */
	@Test
	public void testNewBranches() throws Exception {
		
		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		JSONObject responseObject;
		JSONArray branchesArray;
		JSONObject masterBranchObject;
		JSONObject newBranchObject;
		
		// Setup new branch state
		TestGitHelper.setupTest();
		TestGitHelper.createBranch("John", "new_branch");
		TestGitHelper.runPlugins();
		
		// Test new branch state
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(2, branchesArray.length());
		masterBranchObject = findItem(branchesArray, "branch", "master");
		newBranchObject = findItem(branchesArray, "branch", "new_branch");

		assertEquals(3, masterBranchObject.getJSONArray("activeUsers").length());
		assertEquals(0, newBranchObject.getJSONArray("activeUsers").length());
		assertEquals("LOCAL_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		// Setup checking out the branch
		TestGitHelper.checkoutBranch("John", "new_branch");
		TestGitHelper.runPlugins();
		
		// Test checking out the branch
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(2, branchesArray.length());
		masterBranchObject = findItem(branchesArray, "branch", "master");
		newBranchObject = findItem(branchesArray, "branch", "new_branch");

		assertEquals(2, masterBranchObject.getJSONArray("activeUsers").length());
		assertEquals(1, newBranchObject.getJSONArray("activeUsers").length());
		assertEquals("LOCAL_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		// Setup new commit
		TestGitHelper.push("John");
		TestGitHelper.runPlugins();
		
		// Test checking out the branch
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(2, branchesArray.length());
		masterBranchObject = findItem(branchesArray, "branch", "master");
		newBranchObject = findItem(branchesArray, "branch", "new_branch");

		assertEquals(2, masterBranchObject.getJSONArray("activeUsers").length());
		assertEquals(1, newBranchObject.getJSONArray("activeUsers").length());
		assertEquals("EQUAL", findItem(newBranchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		// Setup new commit
		TestGitHelper.createOrModifyFile("John", "default.txt");
		TestGitHelper.commit("John");
		TestGitHelper.runPlugins();
		
		// Test checking out the branch
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(2, branchesArray.length());
		masterBranchObject = findItem(branchesArray, "branch", "master");
		newBranchObject = findItem(branchesArray, "branch", "new_branch");

		assertEquals(2, masterBranchObject.getJSONArray("activeUsers").length());
		assertEquals(1, newBranchObject.getJSONArray("activeUsers").length());
		assertEquals("AHEAD", findItem(newBranchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		// Setup new commit
		TestGitHelper.pull("David");
		TestGitHelper.runPlugins();
		
		// Test checking out the branch
		responseObject = db.getBranchLevelAwareness("TestRepository");
		branchesArray = responseObject.getJSONArray("branches");
		assertEquals(2, branchesArray.length());
		masterBranchObject = findItem(branchesArray, "branch", "master");
		newBranchObject = findItem(branchesArray, "branch", "new_branch");

		assertEquals(2, masterBranchObject.getJSONArray("activeUsers").length());
		assertEquals(1, newBranchObject.getJSONArray("activeUsers").length());
		assertEquals("AHEAD", findItem(newBranchObject.getJSONArray("users"), "username", "John").getString("relationWithOrigin"));
		assertEquals("EQUAL", findItem(newBranchObject.getJSONArray("users"), "username", "David").getString("relationWithOrigin"));
		assertEquals("REMOTE_BRANCH", findItem(newBranchObject.getJSONArray("users"), "username", "Isabelle").getString("relationWithOrigin"));

		db.closeConnection();
		
	}

}
