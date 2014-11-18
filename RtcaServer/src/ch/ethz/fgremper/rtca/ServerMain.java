package ch.ethz.fgremper.rtca;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class ServerMain {

	static int port = 8080;

	public static void main(String[] args) throws Exception {
		System.out.println("Starting HTTP server on port " + port + "...");

		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", new RtcaHttpHandler()); 
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		System.out.println("Server up!");
	}

}
