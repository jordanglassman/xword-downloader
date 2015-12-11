package com.jordanglassman.xdl.download.lat;

import com.jordanglassman.xdl.util.LoginInfo;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.runner.RunWith;

import java.nio.file.Path;
import java.util.List;

@RunWith(JMockit.class)
public class LatDownloaderTest {
	@Tested
	private LatDownloader testDownloader;

	@Injectable
	private LoginInfo mockLoginInfo;

	@Injectable
	private List<Path> paths;
}