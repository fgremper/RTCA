package ch.ethz.fgremper.rtca;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;

/**
 * RTCA client main class.
 * @author Fabian Gremper
 */
public class ClientMain {

	private static final Logger log = LogManager.getLogger(ClientMain.class);
    
	/**
	 * Run the RTCA client.
	 * @param args Filename of config XML can be specified as first argument (default is "config.xml")
	 * @throws Exception
	 */
	public static void main(String[] args) {
		
		// Set system look and feel
		/*
		try {
	        // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
		catch (Exception e) {
			// Do nothing
		}
		*/
		
		// Load GUI
		ClientGUI.createGuiContents();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ClientGUI.createAndShowGUI();
            }
        });

		ClientConfig config;
		String sessionId;

		// Starting
		log.info("RTCA client starting...");
		
		// Read the config XML
		try {
			ClientGUI.setStatus("Reading config...");
			ClientGUI.addLogMessage("Reading config...");
			log.info("Reading config...");
			String configFileName = "config.xml";
			if (args.length >= 1) configFileName = args[0];
			config = new ClientConfigReader(configFileName).getConfig();
		}
		catch (Exception e) {
			ClientGUI.setStatusRed();
			ClientGUI.addLogMessage("Error while reading config file: " + e.getMessage());
			log.error("Error while reading config file: " + e.getMessage());
			return;
		}
		ClientGUI.setMonitoringText("Monitoring " + config.repositoriesList.size() + " local " + (config.repositoriesList.size() == 1 ? "repository" : "repositories") + ".");
		
		// Get our HTTP client
		HttpClient httpClient = new HttpClient();

		// Login and get a session ID
		ClientGUI.setStatus("Requesting session ID for login...");
		ClientGUI.addLogMessage("Requesting session ID for login...");
		log.info("Requesting session ID for login...");
		try {
			sessionId = httpClient.login(config.serverUrl, config.username, config.password);
		}
		catch (Exception e) {
			ClientGUI.setStatusRed();
			ClientGUI.addLogMessage("Error while requesting session ID: " + e.getMessage());
			log.error("Error while requesting session ID: " + e.getMessage());
			return;
		}
		ClientGUI.addLogMessage("Successfully retrieved session ID.");
		log.info("Retrieved session ID: " + sessionId);
		
		// Keep updating the RTCA server
		while (true) {

			// For all repositories we're going to read the local data and send some of it to the server
			for (RepositoryInfo repositoryInfo : config.repositoriesList) {	
				
				try {

					ClientGUI.setStatus("Reading and sending \"" + repositoryInfo.alias + "\"...");
					ClientGUI.addLogMessage("Reading and sending state for repository \"" + repositoryInfo.alias + "\"...");
					log.info("Reading and sending repository \"" + repositoryInfo.alias + "\" at " + repositoryInfo.localPath);

					// Read repository info
					RepositoryReader repositoryReader = new RepositoryReader(repositoryInfo.localPath);
					JSONObject updateObject = repositoryReader.getUpdateObject();
					
			        // Store user information
					updateObject.put("sessionId", sessionId);
					updateObject.put("repositoryAlias", repositoryInfo.alias);
					
					// Send it to to the server
					String jsonString = updateObject.toString();
					httpClient.sendGitState(config.serverUrl, jsonString);
				
					ClientGUI.setLastUpdate();
				}
				catch (Exception e) {
					ClientGUI.setStatusYellow();
					ClientGUI.addLogMessage("Error while reading/sending local git state for " + repositoryInfo.alias + ": " + e.getMessage());
					log.error("Error while reading/sending local git state for " + repositoryInfo.alias + ": " + e.getMessage());
				}
				
			}
			
			// If interval is 0, we only submit once, otherwise wait and repeat periodically
			if (config.resubmitInterval == 0) {
				break;
			}
			else {
				ClientGUI.setStatus("Idle");
				ClientGUI.addLogMessage("Waiting " + config.resubmitInterval + " seconds.");
				
				log.info("Waiting " + config.resubmitInterval + " seconds...");
				for (int i = 0; i < config.resubmitInterval * 10; i++) {
					ClientGUI.setTimeTillNextUpdate((int) (1000.0 / config.resubmitInterval * i));
					ClientGUI.refreshLastUpdate();
					try {
					    Thread.sleep(100);
					} catch (InterruptedException ex) {
					    Thread.currentThread().interrupt();
					}
				}
			}
		}

		log.info("RTCA client stopping...");
		
	}

}
