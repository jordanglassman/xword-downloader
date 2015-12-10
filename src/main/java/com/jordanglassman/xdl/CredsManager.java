package com.jordanglassman.xdl;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Gets and prepares credentials and URLs for xword downloads.
 */
public class CredsManager extends SystemProperties {
	// checks system props
	// then env variables
	// then some file
	// then maybe will prompt?

	private final Map<XwordType, Pair<String, String>> creds = new HashMap<>();

	public CredsManager() {
		final String nytUsername = System.getProperty(NYT_USERNAME);
		final String nytPassword = System.getProperty(NYT_PASSWORD);
		final ImmutablePair<String, String> nytCreds = new ImmutablePair<>(nytUsername, nytPassword);
		this.creds.put(XwordType.NYT, nytCreds);

		final String latUsername = System.getProperty(LAT_USERNAME);
		final String latPassword = System.getProperty(LAT_PASSWORD);
		final ImmutablePair<String, String> latCreds = new ImmutablePair<>(latUsername, latPassword);
		this.creds.put(XwordType.LAT, latCreds);

		final String csUsername = System.getProperty(CS_USERNAME);
		final String csPassword = System.getProperty(CS_PASSWORD);
		final ImmutablePair<String, String> csCreds = new ImmutablePair<>(csUsername, csPassword);
		this.creds.put(XwordType.CS, csCreds);
	}

	public String getUsername(final XwordType type) {
		return this.creds.get(type).getLeft();
	}

	public String getPassword(final XwordType type) {
		return this.creds.get(type).getRight();
	}
}
