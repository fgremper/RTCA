package ch.ethz.fgremper.cloudstudio.testing.client;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.junit.Test;

import ch.ethz.fgremper.cloudstudio.client.ClientMain;
import ch.ethz.fgremper.cloudstudio.common.ParameterFilter;
import ch.ethz.fgremper.cloudstudio.server.ApiHttpHandler;
import ch.ethz.fgremper.cloudstudio.testing.helper.TestGitHelper;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * Class test for ClientMain
 * 
 * @author Fabian Gremper
 *
 */
public class ClientMainTest {

	/**
	 * 
	 * Test whether the execution is successful
	 *
	 */
	@Test
	public void testExecution() throws Exception {
		
		// Setup test state
		TestGitHelper.setupTest();
		
		// Setup HTTP server
		HttpServer server = HttpServer.create(new InetSocketAddress(7331), 0);
		HttpContext context = server.createContext("/api", new ApiHttpHandler());
		context.getFilters().add(new ParameterFilter());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		// Run ClientMain
		String[] argsJohn = {"configJohn.xml"};
		ClientMain.main(argsJohn);

		// All good, stop the server
		server.stop(0);
		
	}

}
