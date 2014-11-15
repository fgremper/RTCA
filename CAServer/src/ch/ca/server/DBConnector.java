package ch.ca.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBConnector {

	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	static final String DB_URL = "jdbc:mysql://localhost/cloudstudio";
	
	//  Database credentials
	static final String USER = "dbadmin";
	static final String PASS = "";

	Connection conn = null;
	
	public DBConnector() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		
		System.out.println("Connecting to database...");
		conn = DriverManager.getConnection(DB_URL,USER,PASS);
		System.out.println("Connected to database.");
		
	}
	
	public void write(String user, String filename, String content) throws Exception {
		Statement stmt = null;
		
		System.out.println("Creating statement...");
		stmt = conn.createStatement();
		String sql;
		sql = "INSERT INTO files (user, filename, content) VALUES ('test1', 'test2', 'test3')";
		stmt.executeUpdate(sql);
		System.out.println("Done!");

	}
}
