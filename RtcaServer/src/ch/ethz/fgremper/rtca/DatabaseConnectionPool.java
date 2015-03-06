package ch.ethz.fgremper.rtca;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DatabaseConnectionPool {

    private static DatabaseConnectionPool datasource;
    private ComboPooledDataSource cpds;

    private DatabaseConnectionPool() throws IOException, SQLException, PropertyVetoException {
    	
    	ServerConfig serverConfig = ServerConfig.getInstance();
    	
        cpds = new ComboPooledDataSource();
        cpds.setDriverClass(serverConfig.dbDriverClass);
        cpds.setJdbcUrl(serverConfig.dbJdbcUrl);
        cpds.setUser(serverConfig.dbUser);
        cpds.setPassword(serverConfig.dbPassword);
    	cpds.setAutoCommitOnClose(true);
    	
        cpds.setMinPoolSize(serverConfig.dbMinPoolSize);
        cpds.setAcquireIncrement(serverConfig.dbAcquireIncrement);
        cpds.setMaxPoolSize(serverConfig.dbMaxPoolSize);
        cpds.setMaxStatements(serverConfig.dbMaxStatements);

    }

    public static DatabaseConnectionPool getInstance() throws IOException, SQLException, PropertyVetoException {
    	
        if (datasource == null) {
            datasource = new DatabaseConnectionPool();
            return datasource;
        } else {
            return datasource;
        }
        
    }

    public Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }

}