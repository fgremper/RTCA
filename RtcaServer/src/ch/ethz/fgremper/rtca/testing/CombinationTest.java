package ch.ethz.fgremper.rtca.testing;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ch.ethz.fgremper.rtca.RepositoryReader;
import ch.ethz.fgremper.rtca.UpdateHttpHandler;

public class CombinationTest {


	@Test
	public void test() throws Exception {

		// setup scenario
		
		System.out.println("[Test] Setting up scenario in sandpit");
		
		TestGitHelper.clearSandpit();
		
		TestGitHelper.createOrigin();
		
		TestGitHelper.createUser("john");
		TestGitHelper.cloneOrigin("john");
		
		
		// run logic
		
		List<String> involvedUsers = new LinkedList<String>();
		involvedUsers.add("john");
		
		for (String user : involvedUsers) {
		
			System.out.println("[Test] Running plugin for user " + user);
			
			RepositoryReader repositoryReader = new RepositoryReader(TestSettings.SANDPIT_DIRECTORY_PATH + "/" + user);
			String jsonString = repositoryReader.getJsonString();
			
			System.out.println("[Test] JSON String: " + jsonString);

			System.out.println("[Test] Executing database update for " + user);
			
			UpdateHttpHandler updateHttpHandler = new UpdateHttpHandler();
			
			boolean success = updateHttpHandler.executeDataUpdate("testrepo", "user", jsonString);
			
			assertEquals(success, true);
			
		}
	
		
		// verify integrity and shit...
		
	}

}
