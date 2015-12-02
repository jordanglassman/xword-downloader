package com.jordanglassman.xdl;

import com.jordanglassman.xdl.lat.LatDownloader;
import com.jordanglassman.xdl.nyt.NytDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class XwordDownloader {

	public static final Logger LOG = LoggerFactory.getLogger(XwordDownloader.class);

	private static final String DEV_ENVIRONMENT = "com.jordanglassman.xdl.dev";

	private final CredsManager credsManager;
	private final PathsManager pathsManager;

	public XwordDownloader() {
		this.credsManager = new CredsManager();
		this.pathsManager = new PathsManager();
	}

	public static void main(final String[] args) {

//				System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
//				System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
//				System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
//				System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");


		final XwordDownloader xdl = new XwordDownloader();

		// do certain things only if we are in dev
		final Boolean isDev = Boolean.getBoolean(DEV_ENVIRONMENT);
		if(isDev) {
			xdl.getPathsManager().cleanPaths();
		}

		xdl.getPathsManager().init();

		// create and return download paths for today
		final List<Path> paths  = xdl.getPathsManager().getPaths();

		// fetch and write the nyt puzzle
		final LoginInfo nytLoginInfo = xdl.getLoginInfo(XwordType.NYT);
		final NytDownloader nytDownloader = new NytDownloader(nytLoginInfo, paths);
		nytDownloader.doDownload();

		// fetch and write the nyt puzzle
		final LoginInfo latLoginInfo = xdl.getLoginInfo(XwordType.LAT);
		final LatDownloader latDownloader = new LatDownloader(latLoginInfo, paths);
		latDownloader.doDownload();
	}

	public PathsManager getPathsManager() {
		return this.pathsManager;
	}

	private LoginInfo getLoginInfo(final XwordType type) {
		final String username = this.credsManager.getUsername(type);
		final String password = this.credsManager.getPassword(type);

		final LoginInfo loginInfo = new LoginInfo();
		loginInfo.setUsername(username);
		loginInfo.setPassword(password);

		return loginInfo;
	}
}
