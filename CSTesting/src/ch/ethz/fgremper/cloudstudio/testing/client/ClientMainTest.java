package ch.ethz.fgremper.cloudstudio.testing.client;

import static org.junit.Assert.*;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import ch.ethz.fgremper.cloudstudio.client.ClientMain;
import ch.ethz.fgremper.cloudstudio.common.ParameterFilter;
import ch.ethz.fgremper.cloudstudio.server.ApiHttpHandler;
import ch.ethz.fgremper.cloudstudio.server.PeriodicalAllOriginUpdater;
import ch.ethz.fgremper.cloudstudio.server.WebInterfaceHttpHandler;
import ch.ethz.fgremper.cloudstudio.testing.helper.TestGitHelper;

public class ClientMainTest {

	@Test
	public void test() throws Exception {
		

		// Setup test state
		TestGitHelper.setupTest();
		
		// Setup HTTP server
		HttpServer server = HttpServer.create(new InetSocketAddress(7331), 0);
		HttpContext context = server.createContext("/api", new ApiHttpHandler());
		context.getFilters().add(new ParameterFilter());
		server.createContext("/", new WebInterfaceHttpHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		String[] argsJohn = {"configJohn.xml"};
		
		ClientMain.main(argsJohn);

		server.stop(0);
	}

}
