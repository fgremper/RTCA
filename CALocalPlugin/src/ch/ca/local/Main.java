package ch.ca.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

	static LinkedList<String> files = new LinkedList<String>();
	static String jsonString;
	
	String username = "username";
	// my repo path
	// repo name!
	
	public static void main(String[] args) throws Exception {
		String gitRepository = "C:/gitrepos/test";
		
		files.clear();
		files.add("/Users/novocaine/Documents/masterthesis/testrepos/test1.txt");
		files.add("/Users/novocaine/Documents/masterthesis/testrepos/test2.txt");
		//findChangedFiles(gitRepository);
		createJsonString();
		uploadChangesToCloudStudio();
	}
	
	public static void findChangedFiles(String repositoryPath) throws Exception {
		findChangedFilesFromConsoleInput("git --git-dir=" + repositoryPath + "/.git" + " --work-tree=" + repositoryPath + " diff --name-only HEAD", repositoryPath);
		findChangedFilesFromConsoleInput("git --git-dir=" + repositoryPath + "/.git" + " --work-tree=" + repositoryPath + " ls-files --others --exclude-standard", repositoryPath);
	}
	
	public static void findChangedFilesFromConsoleInput(String consoleInput, String repositoryPath) throws Exception {
		Process p = Runtime.getRuntime().exec(consoleInput);
	    p.waitFor();

	    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

	    String line;
	    while ((line = reader.readLine()) != null) {
	    	String filename = new File(repositoryPath, line).getPath();
	    	files.add(filename);
	    }
	}

	public static void createJsonString() throws Exception {
		JSONArray fileArray = new JSONArray();
		
		while(!files.isEmpty()) {
			String file = files.pop();
			
			JSONObject fileObject = new JSONObject();
			byte[] encoded = Files.readAllBytes(Paths.get(file));
			fileObject.put("filename", file);
			fileObject.put("content", new String(encoded, StandardCharsets.UTF_8));
			
			fileArray.put(fileObject);
		}
		
		jsonString = fileArray.toString();
	}
	
	public static void uploadChangesToCloudStudio() throws Exception {
		String url = "http://127.0.0.1:8080/api/something";
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("PUT");
		// con.setRequestProperty("User-Agent", "CALocalPlugin");
		con.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
		out.write(jsonString);
		out.close();
		
		int responseCode = con.getResponseCode();
		
		/*
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
 
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
 
		//print result
		System.out.println(response.toString()); */
	}
	
}
