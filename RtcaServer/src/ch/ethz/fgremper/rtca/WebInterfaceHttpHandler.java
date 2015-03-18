package ch.ethz.fgremper.rtca;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This HTTP handler returns static HTML files if the web interface is requested.
 * @author Fabian Gremper
 */

public class WebInterfaceHttpHandler implements HttpHandler {

	private static final Logger log = LogManager.getLogger(WebInterfaceHttpHandler.class);
	
	public void handle(HttpExchange exchange) throws IOException {
		URI uri = exchange.getRequestURI();
		String requestMethod = exchange.getRequestMethod();
		log.info("Incoming WEB INTERFACE request: " + requestMethod + " " + uri.getPath());

		// TODO: don't allow '..' and shit in path!
		// actually, there should be a library to serve static content, no?
		
		String webInterfacePrefix = "web/";
		
		if (requestMethod.equalsIgnoreCase("GET")) {
			String requestedFile = uri.getPath().substring(1);
			if (!requestedFile.startsWith(webInterfacePrefix + "css/") && !requestedFile.startsWith(webInterfacePrefix + "js/") && !requestedFile.startsWith(webInterfacePrefix + "templates/") && !requestedFile.startsWith(webInterfacePrefix + "img/")) requestedFile = "web/index.html";
			log.info("Looking for: " + requestedFile);
			
			String path = uri.getPath();
			File file = new File(requestedFile).getCanonicalFile();

			if (!file.isFile()) {
				String response = "404 (Not Found)\n";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} else {
				
				
				/*
				// Set content type
				String mime = "text/html";
				if (path.substring(path.length()-3).equals(".js")) mime = "application/javascript";
				if (path.substring(path.length()-4).equals(".css")) mime = "text/css";
				if (path.substring(path.length()-4).equals(".jpg")) mime = "image/jpeg";

				Headers h = exchange.getResponseHeaders();
				h.set("Content-Type", mime);
				*/
				
				// Read and send file
				
				exchange.sendResponseHeaders(200, 0);
				OutputStream os = exchange.getResponseBody();
				FileInputStream fs = new FileInputStream(file);
				final byte[] buffer = new byte[0x10000];
				int count = 0;
				while ((count = fs.read(buffer)) >= 0) {
				os.write(buffer,0,count);
				}
				fs.close();
				os.close();

			}
		}

	}
}
