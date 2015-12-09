package com.jordanglassman.xdl.download.lat;

import com.jordanglassman.xdl.LoginInfo;
import mockit.Deencapsulation;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class LatDownloaderTest {
	@Tested
	private LatDownloader testDownloader;

	@Injectable
	private LoginInfo mockLoginInfo;

	@Injectable
	private List<Path> paths;

	@Test
	public void testCruciverbSha1() throws Exception {
		// this example taken from a POST from the site
		assertEquals("345c02d9e74be06afc0586554f60c26f59f9c487",
				Deencapsulation.invoke(this.testDownloader, "cruciverbSha1", "jordanpg", "nukem42doofus", "24c9e7e8ba517cfe38096c638d2b29ba"));
	}
}