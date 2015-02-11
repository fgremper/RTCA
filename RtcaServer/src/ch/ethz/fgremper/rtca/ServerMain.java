package ch.ethz.fgremper.rtca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;

import jlibdiff.Diff;
import jlibdiff.Diff3;
import jlibdiff.Hunk;
import jlibdiff.Hunk3;

import com.sun.net.httpserver.HttpServer;

import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffAlgorithm;
import difflib.DiffUtils;
import difflib.Patch;

public class ServerMain {

	static int port = 7330;
	
	public static List<String> fileToLines(String filename) {
        List<String> lines = new LinkedList<String>();
        String line = "";
        BufferedReader in = null;
        try {
                in = new BufferedReader(new FileReader(filename));
                while ((line = in.readLine()) != null) {
                        lines.add(line);
                }
        } catch (IOException e) {
                e.printStackTrace();
        } finally {
                if (in != null) {
                        try {
                                in.close();
                        } catch (IOException e) {
                                // ignore ... any errors should already have been
                                // reported via an IOException from the final flush.
                        }
                }
        }
        return lines;
}
	public static void main(String[] args) throws Exception {
		

        List<String> original = fileToLines("one.txt");
        List<String> revised  = fileToLines("two.txt");

        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
        Patch patch = DiffUtils.diff(original, revised);
        
        /*for (String line: DiffUtils.generateUnifiedDiff(null, null, original, patch, 100)) {
            System.out.println(line);
        }*/
        
        
        for (String line : original) {
        	
        }

        
        List<String> mine = fileToLines("one.txt");
        List<String> their = fileToLines("one.txt");
        
        int offset = 0;
        for (Delta delta : patch.getDeltas()) {
        	if (delta.getType() == TYPE.CHANGE) {
        		int pos = delta.getRevised().getPosition() + offset;
        		for (String line : (List<String>) delta.getRevised().getLines()) {
        			their.set(pos, "*" + line);
        			pos++;
        		}
        		pos = delta.getOriginal().getPosition() + offset;
        		for (String line : (List<String>) delta.getOriginal().getLines()) {
        			mine.set(pos, "*" + line);
        			pos++;
        		}
        	}
        	if (delta.getType() == TYPE.DELETE) {
        		int pos = delta.getOriginal().getPosition() + offset;
        		for (String line : (List<String>) delta.getOriginal().getLines()) {
        			//their.(pos, "+" + line);
        			mine.set(pos, "+" + line);
        			their.set(pos, "-------");
        			pos++;
        		}
        	}
        	if (delta.getType() == TYPE.INSERT) {
        		int pos = delta.getOriginal().getPosition() + offset;
        		//pos++;
        		for (String line : (List<String>) delta.getRevised().getLines()) {
	    			//their.(pos, "+" + line);
	    			their.add(pos, "++++" + line);
	    			mine.add(pos, "+--------------");
	    			pos++;
	    			offset++;
	    		}
	    	}
        	
        	//)
        	System.out.println(delta.toString());
        	//System.out.println(delta.toString());
        	System.out.println(delta.getType());
        	//System.out.println(delta.g)
        	
        	
        }
        
        System.out.println("mine:");
        
        for (String l : mine) {
        	System.out.println("> " + l);
        }

        System.out.println("theirs:");
        
        for (String l : their) {
        	System.out.println("> " + l);
        }
        
		/*
		// Periodically origin updater
		System.out.println("[Main] Starting periodical origin updater");
		
		PeriodicalAllOriginUpdater originUpdaterInterval = new PeriodicalAllOriginUpdater();
		new Thread(originUpdaterInterval).start();
		
		*/
		
		// HTTP server
		/*
		int port = ServerConfig.getInstance().serverPort;
		
		System.out.println("[Main] Starting HTTP server on port " + port + "...");

		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/webinterface", new WebInterfaceHttpHandler());
		server.createContext("/request", new RequestHttpHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		System.out.println("[Main] Server up!");
		*/
	}

}
