package ch.ethz.fgremper.rtca;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Test;

public class ConfigReaderTest {

	@Test
	public void test() {
		//fail("Not yet implemented");
		RepositoryInfo fun = new RepositoryInfo("alias", "url");
		assertEquals(fun.alias, "alias");
	}

}
