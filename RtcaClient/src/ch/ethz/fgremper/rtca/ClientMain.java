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

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("RTCA Client");
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
               System.exit(0);
            }
         });
        
        frame.setSize(600, 420);
        frame.setResizable(false);

        
        Container pane = frame.getContentPane();
        pane.setLayout(null);
        
        
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        statusPanel.setBounds(0, 0, 600, 150);
        //pane.add(statusPanel);
        
        
        
        DefaultListModel fruitsName = new DefaultListModel();

        fruitsName.addElement("Apple");
        fruitsName.addElement("Grapes");
        fruitsName.addElement("Mango");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Peer");
        fruitsName.addElement("Last");

        JList fruitList = new JList(fruitsName);

        fruitList.setFont(new Font("Helvetica", Font.PLAIN, 12));
        
        JScrollPane fruitListScrollPane = new JScrollPane(fruitList);
        fruitListScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        fruitListScrollPane.setBounds(5, 155, 590, 237);
        
        // Scroll down
        JScrollBar vertical = fruitListScrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
        
        pane.add(fruitListScrollPane);
        
        try {
	        BufferedImage myPicture = ImageIO.read(new File("TrafficGreen.png"));
	        JLabel picLabel = new JLabel(new ImageIcon(myPicture));
	        picLabel.setBounds(15, 15, 120, 120);
	        pane.add(picLabel);
        }
        catch (Exception e) {
        	// 
        }

        JLabel monitoringText = new JLabel("Monitoring 4 local repositories.");
        monitoringText.setFont(new Font("Helvetica", Font.PLAIN, 14));
        monitoringText.setBounds(160, 27, 400, 20);
        pane.add(monitoringText);
        
        JLabel statusCaption = new JLabel("Status:");
        statusCaption.setFont(new Font("Helvetica", Font.BOLD, 14));
        statusCaption.setBounds(160, 58, 100, 20);
        pane.add(statusCaption);
        
        JLabel lastUpdateCaption = new JLabel("Last update:");
        lastUpdateCaption.setFont(new Font("Helvetica", Font.BOLD, 14));
        lastUpdateCaption.setBounds(160, 78, 100, 20);
        pane.add(lastUpdateCaption);

        JLabel nextUpdateCaption = new JLabel("Next update:");
        nextUpdateCaption.setFont(new Font("Helvetica", Font.BOLD, 14));
        nextUpdateCaption.setBounds(160, 109, 100, 20);
        pane.add(nextUpdateCaption);
        

        JLabel statusText = new JLabel("Idle");
        statusText.setFont(new Font("Helvetica", Font.PLAIN, 14));
        statusText.setBounds(270, 58, 300, 20);
        pane.add(statusText);
        
        JLabel lastUpdateText = new JLabel("49 seconds ago");
        lastUpdateText.setFont(new Font("Helvetica", Font.PLAIN, 14));
        lastUpdateText.setBounds(270, 78, 300, 20);
        pane.add(lastUpdateText);
        

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(80);
        //progressBar.setStringPainted(true);
        //progressBar.setFont(new Font("Helvetica", Font.PLAIN, 14));
        progressBar.setBounds(270, 109, 190, 20);
        pane.add(progressBar);
        
        JButton forceUpdateButton = new JButton("Force update");
        forceUpdateButton.setFont(new Font("Helvetica", Font.PLAIN, 12));
        forceUpdateButton.setBounds(470, 109, 100, 20);
        pane.add(forceUpdateButton);
        
        //frame.add();
        
        //Add the ubiquitous "Hello World" label.
        //JLabel label = new JLabel("Hello World");
        //frame.getContentPane().add(label);

        //Display the window.
        //frame.pack();
        frame.setVisible(true);
    }
    
	/**
	 * Run the RTCA client.
	 * @param args Filename of config XML can be specified as first argument (default is "config.xml")
	 * @throws Exception
	 */
	public static void main(String[] args) {
		 try {
	            // Set System L&F
	        //UIManager.setLookAndFeel(
	        //    UIManager.getSystemLookAndFeelClassName());
	    } 
		 catch (Exception e) {
			 
		 }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
        
		/*
		
		ClientConfig config;
		String sessionId;

		// Starting
		log.info("RTCA client starting...");
		
		// Read the config XML
		try {
			log.info("Reading config...");
			String configFileName = "config.xml";
			if (args.length >= 1) configFileName = args[0];
			config = new ClientConfigReader(configFileName).getConfig();
		}
		catch (Exception e) {
			log.error("Error while reading config file: " + e.getMessage());
			return;
		}
		
		// Get our HTTP client
		HttpClient httpClient = new HttpClient();

		// Login and get a session ID
		log.info("Login and requesting session ID...");
		try {
			sessionId = httpClient.login(config.serverUrl, config.username, config.password);
		}
		catch (Exception e) {
			log.error("Error while requesting session ID: " + e.getMessage());
			return;
		}
		log.info("Retrieved session ID: " + sessionId);
		
		// Keep updating the RTCA server
		while (true) {

			// For all repositories we're going to read the local data and send some of it to the server
			for (RepositoryInfo repositoryInfo : config.repositoriesList) {	
				
				try {
					
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
				
				}
				catch (Exception e) {
					log.error("Error while reading/sending local git state for " + repositoryInfo.alias + ": " + e.getMessage());
				}
				
			}
			
			// If interval is 0, we only submit once, otherwise wait and repeat periodically
			if (config.resubmitInterval == 0) {
				break;
			}
			else {
				log.info("Waiting " + config.resubmitInterval + " seconds...");
				try {
				    Thread.sleep(config.resubmitInterval * 1000);
				} catch (InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
			}
		}

		log.info("RTCA client stopping...");
		
		*/
		
	}

}
