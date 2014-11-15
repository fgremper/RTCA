package ch.ca.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.*;

public class CAHttpHandler implements HttpHandler {
   public void handle(HttpExchange exchange) throws IOException {
       URI uri = exchange.getRequestURI();
       String requestMethod = exchange.getRequestMethod();
       System.out.println("Incoming request: " + requestMethod + " " + uri.getPath());
       
       
       if (requestMethod.equalsIgnoreCase("PUT")) {
    	   System.out.println("Incoming PUT:");
    	   String jsonString = new Scanner(exchange.getRequestBody(), "UTF-8").useDelimiter("\\A").next();
    	   System.out.println("JSON string: " + jsonString);
    	   try {
    		   JSONArray fileArray = new JSONArray(jsonString);
    		   for (int i = 0; i < fileArray.length(); i++) {
    			   JSONObject fileObject = fileArray.getJSONObject(i);
    			   System.out.println("Filename: " + fileObject.getString("filename"));
    			   System.out.println("Contents: " + fileObject.getString("content"));
    			   DBConnector conneector = new DBConnector();
    			   conneector.write("", "", "");
    		   }
    	   } catch (Exception e) {
    		   // TODO Auto-generated catch block
    		   e.printStackTrace();
    	   }
    	   
       }
       
       String response = "Path: " + uri.getPath() + "\n";
       exchange.sendResponseHeaders(200, response.length());
       OutputStream os = exchange.getResponseBody();
       os.write(response.getBytes());
       os.close();
   }
}
