package ch.ethz.fgremper.rtca;

import org.apache.commons.configuration.XMLConfiguration;

/**
 * Server configuration.
 * @author Fabian Gremper
 */
public class ServerConfig {
	
	// Instance
	private static ServerConfig serverConfig;

	// DB config parameters
	public String dbDriverClass;
	public String dbJdbcUrl;
	public String dbUser;
	public String dbPassword;

	// DB pool parameters
	public int dbMinPoolSize;
	public int dbAcquireIncrement;
	public int dbMaxPoolSize;
	public int dbMaxStatements;
	
	// Server port parameters
	public int serverPort;

	// Directory parameters
	public String fileStorageDirectory;
	public String originStorageDirectory;

	// Hash salt
	public String passwordSalt;

	// Origin update interval
	public int originUpdateInterval;
	
	public ServerConfig() {
		
		try {
			
			// Open the configuration
			XMLConfiguration xmlConfig = new XMLConfiguration("serverConfig.xml");
		
			// Read db parameters
			dbDriverClass = xmlConfig.getString("dbDriverClass");
			dbJdbcUrl = xmlConfig.getString("dbJdbcUrl");
			dbUser = xmlConfig.getString("dbUser");
			dbPassword = xmlConfig.getString("dbPassword");

			// Read db pool parameters
			dbMinPoolSize = Integer.parseInt(xmlConfig.getString("dbMinPoolSize"));
			dbAcquireIncrement = Integer.parseInt(xmlConfig.getString("dbAcquireIncrement"));
			dbMaxPoolSize = Integer.parseInt(xmlConfig.getString("dbMaxPoolSize"));
			dbMaxStatements = Integer.parseInt(xmlConfig.getString("dbMaxStatements"));

			// Read server port
			serverPort = Integer.parseInt(xmlConfig.getString("serverPort"));
			
			// Read directory information
			fileStorageDirectory = xmlConfig.getString("fileStorageDirectory");
			originStorageDirectory = xmlConfig.getString("originStorageDirectory");

			// Read salt
			passwordSalt = xmlConfig.getString("passwordSalt");

			// Origin update interval
			originUpdateInterval = Integer.parseInt(xmlConfig.getString("originUpdateInterval"));
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Get the instance server config
	 * @return ServerConfig instance
	 */
	public static ServerConfig getInstance() {
		if (serverConfig == null) {
			serverConfig = new ServerConfig();
		}
		return serverConfig;
	}
	
}
