package com.jordanglassman.xdl.download.cs;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.jordanglassman.xdl.LoginInfo;
import com.jordanglassman.xdl.XwordType;
import com.jordanglassman.xdl.download.BaseDownloader;
import com.jordanglassman.xdl.util.PromptReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class CsDownloader extends BaseDownloader {
	private static final Logger LOG = LoggerFactory.getLogger(CsDownloader.class);

	public CsDownloader(final LoginInfo loginInfo, final List<Path> paths) {
		super(loginInfo, paths);
	}

	@Override
	protected String getDownloadUrlFormat() {
		return "";
	}

	@Override
	protected String getFilenameFormat() {
		return "";
	}

	@Override
	public boolean authenticate() {
//		if(!super.authenticate()) {
//			LOG.error(String.format("blank username or password detected, no %s xword will be downloaded", this.getType()));
//			return false;
//		}


		// authorization
		Credential credential = null;
//		try {
//			credential = authorize();
//		} catch (IOException e) {
//			LOG.error("{}",e);
//		}



		return true;
	}



	@Override
	public void logout() {
	}

	@Override public XwordType getType() {
		return XwordType.CS;
	}
}
