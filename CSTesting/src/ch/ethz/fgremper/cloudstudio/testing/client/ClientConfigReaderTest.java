package ch.ethz.fgremper.cloudstudio.testing.client;

import static org.junit.Assert.*;

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ch.ethz.fgremper.cloudstudio.client.ClientConfig;
import ch.ethz.fgremper.cloudstudio.client.ClientConfigReader;

public class ClientConfigReaderTest {

	@Test
	public void testAllInfo() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
		
		assertEquals(config.username, "mark");
		assertEquals(config.serverUrl, "http://my.server.com");
		assertEquals(config.repositoriesList.size(), 1);
		assertEquals(config.repositoriesList.get(0).alias, "foo");
		assertEquals(config.repositoriesList.get(0).localPath, "/path/to/foo");		
	}

	@Test
	public void testAllInfoTwoRepositories() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"<repository>" +
				"<alias>bar</alias>" +
				"<localPath>/path/to/bar</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
		
		assertEquals(config.username, "mark");
		assertEquals(config.serverUrl, "http://my.server.com");
		assertEquals(config.repositoriesList.size(), 2);
		assertEquals(config.repositoriesList.get(0).alias, "foo");
		assertEquals(config.repositoriesList.get(0).localPath, "/path/to/foo");		
		assertEquals(config.repositoriesList.get(1).alias, "bar");
		assertEquals(config.repositoriesList.get(1).localPath, "/path/to/bar");		
	}

	@Test (expected = Exception.class)
	public void testNoUsername() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}
	
	@Test (expected = Exception.class)
	public void testEmptyUsername() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username></username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

	@Test (expected = Exception.class)
	public void testNoServerUrl() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

	@Test (expected = Exception.class)
	public void testEmptyServerUrl() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl></serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

	@Test (expected = Exception.class)
	public void testNoRepository() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

	@Test (expected = Exception.class)
	public void testNoRepositoryAlias() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

	@Test (expected = Exception.class)
	public void testEmptyRepositoryAlias() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias></alias>" +
				"<localPath>/path/to/foo</localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

	@Test (expected = Exception.class)
	public void testNoRepositoryLocalPath() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}
	@Test (expected = Exception.class)
	public void testEmptyRepositoryLocalPath() throws Exception {
		FileUtils.writeStringToFile(new File("config.xml"),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<config>" +
				"<username>mark</username>" +
				"<serverUrl>http://my.server.com</serverUrl>" +
				"<repositories>" +
				"<repository>" +
				"<alias>foo</alias>" +
				"<localPath></localPath>" +
				"</repository>" +
				"</repositories>" +
				"</config>");
		
		ClientConfig config = new ClientConfigReader("config.xml").getConfig();
	}

}
