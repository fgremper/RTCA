package ch.ethz.fgremper.test.combination;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import ch.ethz.fgremper.rtca.ContentConflictGitReader;
import ch.ethz.fgremper.rtca.DatabaseConnection;
import ch.ethz.fgremper.rtca.test.helper.TestGitHelper;

public class ContentLevelConflictTest {

	public JSONObject findItem(JSONArray array, String key, String value) throws Exception {
		for (int i = 0; i < array.length(); i++) {
			if (array.getJSONObject(i).getString(key).equals(value)) return array.getJSONObject(i);
		}
		return null;
	}

	
	@Test
	public void testCorrectFileContentAndConflictCount()  throws Exception {


		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		ContentConflictGitReader gitReader;
		JSONArray diffLineArray;
		
		// Setup default state
		TestGitHelper.setupTest();
		TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", false);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
        
        
        
        
        
        
        
        TestGitHelper.writeContentToFile("John", "default.txt", "john content");
        TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", false);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
        
        
        

        TestGitHelper.commit("John");
        TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", false);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
        
        
        
        

        
        
        
        TestGitHelper.writeContentToFile("David", "default.txt", "david content");
        TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 1, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("david content", diffLineArray.getJSONObject(0).getString("theirContent"));

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", false);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
        
        
        

        TestGitHelper.commit("David");
        TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 1, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("david content", diffLineArray.getJSONObject(0).getString("theirContent"));

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", false);
		
        assertEquals((Integer) 1, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("default content", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("david content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
        
        
        
        
		db.closeConnection();

	}
	
	

	@Test
	public void testCorrectConflictDetection()  throws Exception {


		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		ContentConflictGitReader gitReader;
		JSONArray diffLineArray;
		
		// Setup default state
		TestGitHelper.setupTest();

		TestGitHelper.writeContentToFile("John", "default.txt", "one\ntwo\nthree\nfour\nfive");
		TestGitHelper.commit("John");
		TestGitHelper.push("John");
		TestGitHelper.pull("David");
		TestGitHelper.writeContentToFile("John", "default.txt", "one\ntwo\nCHANGED BY JOHN\nfour\nfive");
		TestGitHelper.writeContentToFile("David", "default.txt", "one\ntwo\nCHANGED BY DAVID\nfour\nfive");
        TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 1, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(5, diffLineArray.length());
        assertEquals("MODIFIED", diffLineArray.getJSONObject(2).getString("myType"));
        assertEquals("MODIFIED", diffLineArray.getJSONObject(2).getString("theirType"));

        

		TestGitHelper.writeContentToFile("John", "default.txt", "one\nCHANGED BY JOHN\nthree\nfour\nfive");
		TestGitHelper.writeContentToFile("David", "default.txt", "one\ntwo\nthree\nCHANGED BY DAVID\nfive");
        TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "default.txt", "John", "master", "David", true);
		
        assertEquals((Integer) 0, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(5, diffLineArray.length());
        assertEquals("MODIFIED", diffLineArray.getJSONObject(1).getString("myType"));
        assertEquals("MODIFIED", diffLineArray.getJSONObject(3).getString("theirType"));

        
        
		db.closeConnection();

	}
	

	@Test
	public void testNoCommonAncestor()  throws Exception {

		// Database connection
		DatabaseConnection db = new DatabaseConnection();
		db.getConnection();

		// Objects
		ContentConflictGitReader gitReader;
		JSONArray diffLineArray;
		
		// Setup default state
		TestGitHelper.setupTest();

		TestGitHelper.writeContentToFile("John", "new.txt", "john content");
		TestGitHelper.commit("John");
		
		TestGitHelper.writeContentToFile("David", "new.txt", "david content");
		TestGitHelper.commit("David");

		TestGitHelper.runPlugins();

        gitReader = new ContentConflictGitReader("TestRepository", "master", "new.txt", "John", "master", "David", true);

        assertEquals((Integer) 1, gitReader.countConflicts());
        diffLineArray = gitReader.diff();
        assertEquals(1, diffLineArray.length());
        assertEquals("john content", diffLineArray.getJSONObject(0).getString("myContent"));
        assertEquals("", diffLineArray.getJSONObject(0).getString("baseContent"));
        assertEquals("david content", diffLineArray.getJSONObject(0).getString("theirContent"));
        
		db.closeConnection();

	}
	
	
	

}
