package com.jordanglassman.xdl.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.common.net.MediaType;
import com.jordanglassman.xdl.PathsManager;
import com.jordanglassman.xdl.download.cs.CsDownloader;
import com.jordanglassman.xdl.exception.MailFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;

public class GmailFetcher implements MailFetcher {
	private static final Logger LOG = LoggerFactory.getLogger(GmailFetcher.class);
	private static final String ME = "me";
	private static final String CS_FILENAME = "cs%ty%tm%td.jpz";

	private NetHttpTransport httpTransport;
	private FileDataStoreFactory dataStoreFactory;
	private JsonFactory jsonFactory;

	private Credential credential;
	private Gmail gmail;

	private Date today;

	private PathsManager pathsManager;

	public static void main(String[] args) {
		//		CsDownloader csDownloader = new CsDownloader();
		//		csDownloader.authenticate();
		GmailFetcher fetcher = new GmailFetcher();
		if (fetcher.authenticate()) {
			Message message = fetcher.getMessage("");
			fetcher.getAttachment(message);
		}
	}

	public GmailFetcher() {
		this.today = new Date();
		this.pathsManager = new PathsManager();
		this.pathsManager.init();

		try {
			this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			LOG.error("{}", e);
		}

		try {
			this.dataStoreFactory = new FileDataStoreFactory(this.pathsManager.getCredsDir().toFile());
		} catch (IOException e) {
			LOG.error("{}", e);
		}

		this.jsonFactory = new JacksonFactory();
	}

	@Override public boolean authenticate() {
		try {
			// load client secrets
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(this.jsonFactory,
					new InputStreamReader(this.getClass().getResourceAsStream("/client_secrets.json")));

			// set up authorization code flow
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(this.httpTransport,
					this.jsonFactory, clientSecrets, Collections.singleton(GmailScopes.GMAIL_READONLY))
					.setDataStoreFactory(this.dataStoreFactory).build();

			// authorize
			AuthorizationCodeInstalledApp code = new AuthorizationCodeInstalledApp(flow,
					new PromptReceiver(clientSecrets.getInstalled().getRedirectUris().get(0)));

			this.credential = code.authorize(ME);
		} catch (IOException e) {
			LOG.error("error while authenticating with google, e={}", e);
			return false;
		}

		return Objects.nonNull(this.credential) && Objects.nonNull(this.credential.getAccessToken());
	}

	@Override public Message getMessage(String subject) {
		initGmail();

		final String searchString = String
				.format("subject:\"CrosSynergy XWord : %tY-%tm-%td\"", this.today, this.today, this.today);

		try {
			ListMessagesResponse messagesResponse = gmail.users().messages().list(ME).setQ(searchString).execute();
			if (messagesResponse.getMessages().size() != 1) {
				final String exceptionMessage = String
						.format("did not find expected single CS daily puzzle in mailbox, found %d instead",
								messagesResponse.size());
				throw new MailFetchException(exceptionMessage);
			}
			final String messageId = messagesResponse.getMessages().get(0).getId();
			return gmail.users().messages().get(ME, messageId).execute();
		} catch (IOException e) {
			return null;
		}
	}

	private void initGmail() {
		this.gmail = new Gmail.Builder(this.httpTransport, this.jsonFactory, credential).build();
	}

	@Override public byte[] getAttachment(final Object message) {
		try {
			if (Objects.isNull(message) || !(message instanceof Message)) {
				throw new MailFetchException("null or invalid email message, cannot download attachment");
			}

			Message gmailMessage = (Message) message;
			final String messageId = gmailMessage.getId();

			final String expectedFilename = String.format(CS_FILENAME, this.today, this.today, this.today);

			Optional<MessagePart> attachments = gmailMessage.getPayload().getParts().stream()
					.filter(p -> p.getMimeType().equals("multipart/mixed")).findFirst();

			if (attachments.isPresent()) {

				final Optional<MessagePart> jpzFile = attachments.get().getParts().stream()
						.filter(p -> p.getFilename().equals(expectedFilename)).findFirst();
				if (jpzFile.isPresent()) {
					final MessagePart part = jpzFile.get();
					String filename = part.getFilename();
					String attId = part.getBody().getAttachmentId();
					MessagePartBody attachPart = gmail.users().messages().attachments().get(ME, messageId, attId)
							.execute();
					byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
					for (final Path path : this.pathsManager.getPaths()) {
						final File outFile = path.resolve(filename).toFile();
						LOG.info(outFile.toString());
						FileOutputStream fos = new FileOutputStream(outFile);
						fos.write(fileByteArray);
						fos.close();
					}
					return fileByteArray;
				}
			}
			throw new MailFetchException("could not find or download expected xword puzzle as an email attachment");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}
}
