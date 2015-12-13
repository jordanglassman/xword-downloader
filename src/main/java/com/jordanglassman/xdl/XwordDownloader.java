package com.jordanglassman.xdl;

import com.jordanglassman.xdl.xword.cs.CsDownloader;
import com.jordanglassman.xdl.xword.lat.LatDownloader;
import com.jordanglassman.xdl.xword.nyt.NytDownloader;
import com.jordanglassman.xdl.util.CredsManager;
import com.jordanglassman.xdl.util.LoginInfo;
import com.jordanglassman.xdl.util.PathsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

import static com.jordanglassman.xdl.util.SystemProperties.*;

public class XwordDownloader {
	public static final Logger LOG = LoggerFactory.getLogger(XwordDownloader.class);

	private final CredsManager credsManager;
	private final PathsManager pathsManager;

	public XwordDownloader() {

		final Package pkg = Package.getPackage("com.jordanglassman.xdl");
		final String version = pkg.getSpecificationVersion();
		final String implVersion = pkg.getImplementationVersion();

		LOG.info("Welcome to Xword Downloader " + version);
		LOG.debug("Implementation-Version" + implVersion);

		this.credsManager = new CredsManager();
		this.pathsManager = new PathsManager();
	}

	public static void main(final String[] args) {
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
		final boolean getNyt = Boolean.getBoolean(GET_NYT);
		if (getNyt) {
			final LoginInfo nytLoginInfo = xdl.getLoginInfo(XwordType.NYT);
			final NytDownloader nytDownloader = new NytDownloader(nytLoginInfo, xdl.getPathsManager());
			nytDownloader.doDownload();
		}

		// fetch and write the nyt puzzle
		final boolean getLat = Boolean.getBoolean(GET_LAT);
		if (getLat) {
			final LoginInfo latLoginInfo = xdl.getLoginInfo(XwordType.LAT);
			final LatDownloader latDownloader = new LatDownloader(latLoginInfo, xdl.getPathsManager());
			latDownloader.doDownload();
		}

		// fetch and write the cs puzzle from gmail
		final boolean getCs = Boolean.getBoolean(GET_CS);
		if (getCs) {
			final CsDownloader csDownloader = new CsDownloader(xdl.getPathsManager());
			csDownloader.doDownload();
		}
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
