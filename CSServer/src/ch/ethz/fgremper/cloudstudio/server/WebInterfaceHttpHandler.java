package ch.ethz.fgremper.cloudstudio.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
 * 
 * This HTTP handler returns static HTML files if the web interface is requested.
 * 
 * @author Fabian Gremper
 * 
 */
public class WebInterfaceHttpHandler implements HttpHandler {

	private static final Logger log = LogManager.getLogger(WebInterfaceHttpHandler.class);

	/**
	 * 
	 * Handle HTTP exchange
	 * 
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 * 
	 */
	public void handle(HttpExchange exchange) throws IOException {
		
		URI uri = exchange.getRequestURI();
		String requestMethod = exchange.getRequestMethod();
		
		log.info("Incoming request: " + requestMethod + " " + uri.getPath());

		String webInterfacePrefix = File.separator;
		String pathToWebInterfaceFiles = "web/";
		
		if (requestMethod.equalsIgnoreCase("GET")) {
			
			String requestedFile = uri.getPath().substring(webInterfacePrefix.length());
			
			// don't allow paths with ".." in them
			if (requestedFile.contains("..")) return;
			
			if (!requestedFile.startsWith("css/") && !requestedFile.startsWith("js/") && !requestedFile.startsWith("templates/") && !requestedFile.startsWith("img/")) requestedFile = "index.html";
			log.info("Looking for: " + requestedFile);
			
			File file = new File(pathToWebInterfaceFiles + requestedFile).getCanonicalFile();

			// Does the file exist?
			if (file.isFile()) {
				
				// Set content type
				String mime = "text/html"; 
				if (requestedFile.substring(requestedFile.length() - 3).equals(".js")) mime = "application/javascript";
				if (requestedFile.substring(requestedFile.length() - 4).equals(".css")) mime = "text/css";
				if (requestedFile.substring(requestedFile.length() - 4).equals(".jpg")) mime = "image/jpeg";
				if (requestedFile.substring(requestedFile.length() - 4).equals(".png")) mime = "image/png";
				Headers h = exchange.getResponseHeaders();
				h.set("Content-Type", mime);
				
				// Read and send file
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
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
