package ch.ethz.fgremper.rtca;

import org.apache.commons.configuration.XMLConfiguration;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ServerConfig {
	private static ServerConfig serverConfig;

	public String dbDriverClass;
	public String dbJdbcUrl;
	public String dbUser;
	public String dbPassword;

	public int dbMinPoolSize;
	public int dbAcquireIncrement;
	public int dbMaxPoolSize;
	public int dbMaxStatements;
	
	public int serverPort;

	public String fileStorageDirectory;
	public String originStorageDirectory;
	
	public ServerConfig() {
		try {
			XMLConfiguration xmlConfig = new XMLConfiguration("serverConfig.xml");
		
			dbDriverClass = xmlConfig.getString("dbDriverClass");
			dbJdbcUrl = xmlConfig.getString("dbJdbcUrl");
			dbUser = xmlConfig.getString("dbUser");
			dbPassword = xmlConfig.getString("dbPassword");

			dbMinPoolSize = Integer.parseInt(xmlConfig.getString("dbMinPoolSize"));
			dbAcquireIncrement = Integer.parseInt(xmlConfig.getString("dbAcquireIncrement"));
			dbMaxPoolSize = Integer.parseInt(xmlConfig.getString("dbMaxPoolSize"));
			dbMaxStatements = Integer.parseInt(xmlConfig.getString("dbMaxStatements"));

			serverPort = Integer.parseInt(xmlConfig.getString("serverPort"));
			
			fileStorageDirectory = xmlConfig.getString("fileStorageDirectory");
			originStorageDirectory = xmlConfig.getString("originStorageDirectory");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ServerConfig getInstance() {
		if (serverConfig == null) {
			serverConfig = new ServerConfig();
		}
		return serverConfig;
	}
}
