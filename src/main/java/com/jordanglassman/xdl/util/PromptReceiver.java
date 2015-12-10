package com.jordanglassman.xdl.util;

import com.google.api.client.extensions.java6.auth.oauth2.AbstractPromptReceiver;

import java.io.IOException;

public class PromptReceiver extends AbstractPromptReceiver {
	private final String redirectUri;

	public PromptReceiver(final String redirectUri) {
		this.redirectUri = redirectUri;
	}

	@Override public String getRedirectUri() throws IOException {
		return this.redirectUri;
	}
}
