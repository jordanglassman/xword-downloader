package com.jordanglassman.xdl;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Gets and prepares credentials and URLs for xword downloads.
 */
public class CredsManager {

	// checks system props
	// TODO:
	// then env variables
	// then some file
	// then maybe will prompt?

	enum Creds { USERNAME, PASSWORD, HOST, PORT }


	private static final String NYT_USERNAME = "com.jordanglassman.xdl.nyt.username";
	private static final String NYT_PASSWORD = "com.jordanglassman.xdl.nyt.password";

	private static final String LAT_USERNAME = "com.jordanglassman.xdl.lat.username";
	private static final String LAT_PASSWORD = "com.jordanglassman.xdl.lat.password";

	private static final String CS_USERNAME = "com.jordanglassman.xdl.cs.username";
	private static final String CS_PASSWORD = "com.jordanglassman.xdl.cs.password";
	private static final String CS_HOST = "com.jordanglassman.xdl.cs.imap.host";
	private static final String CS_PORT = "com.jordanglassman.xdl.cs.imap.port";

	private final Map<XwordType, EnumMap<Creds, String>> creds = new HashMap<>();

	public CredsManager() {
		final String nytUsername = System.getProperty(NYT_USERNAME);
		final String nytPassword = System.getProperty(NYT_PASSWORD);
		final EnumMap<Creds, String> nytCreds = new EnumMap<>(Creds.class);
		nytCreds.put(Creds.USERNAME, nytUsername);
		nytCreds.put(Creds.PASSWORD, nytPassword);
		this.creds.put(XwordType.NYT, nytCreds);

		final String latUsername = System.getProperty(LAT_USERNAME);
		final String latPassword = System.getProperty(LAT_PASSWORD);
		final EnumMap<Creds, String> latCreds = new EnumMap<>(Creds.class);
		latCreds.put(Creds.USERNAME, latUsername);
		latCreds.put(Creds.PASSWORD, latPassword);
		this.creds.put(XwordType.LAT, latCreds);

		final String csUsername = System.getProperty(CS_USERNAME);
		final String csPassword = System.getProperty(CS_PASSWORD);
		final String csHost = System.getProperty(CS_HOST);
		final String csPort = System.getProperty(CS_PORT);
		final EnumMap<Creds, String> csCreds = new EnumMap<>(Creds.class);
		csCreds.put(Creds.USERNAME, csUsername);
		csCreds.put(Creds.PASSWORD, csPassword);
		csCreds.put(Creds.HOST, csHost);
		csCreds.put(Creds.PORT, csPort);
		this.creds.put(XwordType.CS, csCreds);
	}

	public String getUsername(final XwordType type) {
		return this.creds.get(type).get(Creds.USERNAME);
	}

	public String getPassword(final XwordType type) {
		return this.creds.get(type).get(Creds.PASSWORD);
	}

	public String getHost(final XwordType type) { return this.creds.get(type).get(Creds.HOST); }

	public Integer getPort(final XwordType type) { return Integer.valueOf(this.creds.get(type).get(Creds.PORT)); }
}
