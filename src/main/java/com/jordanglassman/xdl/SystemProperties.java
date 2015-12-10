package com.jordanglassman.xdl;

public class SystemProperties {
	public static final String NYT_USERNAME = "com.jordanglassman.xdl.nyt.username";
	public static final String NYT_PASSWORD = "com.jordanglassman.xdl.nyt.password";
	public static final String LAT_USERNAME = "com.jordanglassman.xdl.lat.username";
	public static final String LAT_PASSWORD = "com.jordanglassman.xdl.lat.password";
	public static final String CS_USERNAME = "com.jordanglassman.xdl.cs.username";
	public static final String CS_PASSWORD = "com.jordanglassman.xdl.cs.password";

	public static final String BASE_DOWNLOAD_DIR = "com.jordanglassman.xdl.basedir";

	public static final String DEV_ENVIRONMENT = "com.jordanglassman.xdl.dev";

	// helpful for debugging httpclient
	// -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
	// -dorg.apache.commons.logging.simplelog.showdatetime=true
	// -Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG
	// -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=DEBUG

	// helpful for cutting and pasting
	//	-Dcom.jordanglassman.xdl.nyt.username=
	//	-Dcom.jordanglassman.xdl.nyt.password=
	//	-Dcom.jordanglassman.xdl.lat.username=
	//	-Dcom.jordanglassman.xdl.lat.password=
	//	-Dcom.jordanglassman.xdl.cs.username=
	//	-Dcom.jordanglassman.xdl.cs.password=
	//	-Dcom.jordanglassman.xdl.basedir=
	//	-Dcom.jordanglassman.xdl.dev=true
	//	-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
}
