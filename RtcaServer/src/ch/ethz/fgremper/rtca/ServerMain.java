package ch.ethz.fgremper.rtca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.Vector;
import java.util.concurrent.Executors;

import jlibdiff.Diff;
import jlibdiff.Diff3;
import jlibdiff.Hunk;
import jlibdiff.Hunk3;

import com.sun.net.httpserver.HttpServer;

public class ServerMain {

	static int port = 7330;

	public static void main(String[] args) throws Exception {
		/*
		try {
			DatabaseConnection db = new DatabaseConnection();
			db.startTransaction();
			db.addUser("admin", "1234");
			db.makeUserAdmin("admin");
			db.commitTransaction();
		}
		catch (Exception e) {
			//e.printStackTrace();
		}
		*/
		/*
		Diff3 diff3 = new Diff3();
		
		diff3.diffBuffer(new BufferedReader(new FileReader("one.txt")), new BufferedReader(new FileReader("two.txt")), new BufferedReader(new FileReader("three.txt")));
		
		Vector<Hunk3> vector = diff3.getHunk3();

		for (Hunk3 hunk3 : vector) {
		    System.out.println(">>>>>>>>>>>>>>> " + hunk3.toString());
		}*/
		
		/*
		Diff diff = new Diff();
		
		diff.diffBuffer(new BufferedReader(new FileReader("two.txt")), new BufferedReader(new FileReader("three.txt")));

		Vector<Hunk> vector = diff.getHunks();

		int i = 0;
		for (Hunk hunk : vector) {
			System.out.println(i++);
		    System.out.println("0 from: " + hunk.lowLine(0) + " to " + hunk.highLine(0));
		    System.out.println("1 from: " + hunk.lowLine(1) + " to " + hunk.highLine(1));
		    System.out.println(">>>>>" + hunk.convert());
		    //System.out.println(" > " + hunk.convert());
		    //hunk.
		}
		*/
		
		int port = ServerConfig.getInstance().serverPort;
		
		System.out.println("[Main] Starting HTTP server on port " + port + "...");

		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/webinterface", new WebInterfaceHttpHandler());
		server.createContext("/request", new RequestHttpHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		System.out.println("[Main] Server up!");
		
		
	}

}
