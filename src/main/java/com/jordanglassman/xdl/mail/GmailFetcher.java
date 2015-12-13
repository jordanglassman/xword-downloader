package com.jordanglassman.xdl.mail;

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
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.jordanglassman.xdl.exception.MailFetchException;
import com.jordanglassman.xdl.util.PathsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class GmailFetcher implements MailFetcher {
	private static final Logger LOG = LoggerFactory.getLogger(GmailFetcher.class);

	private static final String ME = "me";
	private static final String CS_FILENAME = "cs%ty%tm%td.jpz";

	private NetHttpTransport httpTransport;
	private FileDataStoreFactory dataStoreFactory;
	private JsonFactory jsonFactory;

	private Credential credential;
	private Gmail gmail;

	private PathsManager pathsManager;

	public GmailFetcher(final PathsManager pathsManager) {
		this.pathsManager = pathsManager;
		this.jsonFactory = new JacksonFactory();
	}

	@Override
	public boolean authenticate() {
		try {
			this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			this.dataStoreFactory = new FileDataStoreFactory(this.pathsManager.getCredsDir().toFile());
		} catch (final GeneralSecurityException | IOException e) {
			LOG.error("error while preparing for oauth gmail authentication", e);
			return false;
		}

		try {
			// load client secrets
			final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(this.jsonFactory,
					new InputStreamReader(this.getClass().getResourceAsStream("/client_secrets.json")));

			// set up authorization code flow
			final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(this.httpTransport,
					this.jsonFactory, clientSecrets, Collections.singleton(GmailScopes.GMAIL_READONLY))
					.setDataStoreFactory(this.dataStoreFactory).build();

			// authorize
			final AuthorizationCodeInstalledApp code = new AuthorizationCodeInstalledApp(flow,
					new PromptReceiver(clientSecrets.getInstalled().getRedirectUris().get(0)));

			this.credential = code.authorize(ME);
		} catch (final IOException e) {
			LOG.error("error while authenticating with google", e);
			return false;
		}

		return Objects.nonNull(this.credential) && Objects.nonNull(this.credential.getAccessToken());
	}

	@Override
	public Message getMessage() {
		this.initGmail();

		final String searchString = String
				.format("subject:\"CrosSynergy XWord : %tY-%tm-%td\"", this.pathsManager.getToday(),
						this.pathsManager.getToday(), this.pathsManager.getToday());

		try {
			final ListMessagesResponse messagesResponse = this.gmail.users().messages().list(ME).setQ(searchString)
					.execute();
			if (messagesResponse.getMessages().size() != 1) {
				final String exceptionMessage = String
						.format("did not find expected single CS daily puzzle in mailbox, found %d instead",
								messagesResponse.size());
				throw new MailFetchException(exceptionMessage);
			}
			final String messageId = messagesResponse.getMessages().get(0).getId();
			return this.gmail.users().messages().get(ME, messageId).execute();
		} catch (final IOException e) {
			return null;
		}
	}

	private void initGmail() {
		this.gmail = new Gmail.Builder(this.httpTransport, this.jsonFactory, this.credential).build();
	}

	@Override
	public byte[] getAttachment(final Object message) {
		try {
			if (Objects.isNull(message) || !(message instanceof Message)) {
				throw new MailFetchException("null or invalid email message, cannot download attachment");
			}

			final Message gmailMessage = (Message) message;
			final String messageId = gmailMessage.getId();

			final String expectedFilename = String
					.format(CS_FILENAME, this.pathsManager.getToday(), this.pathsManager.getToday(),
							this.pathsManager.getToday());

			final Optional<MessagePart> attachments = gmailMessage.getPayload().getParts().stream()
					.filter(p -> p.getMimeType().equals("multipart/mixed")).findFirst();

			if (attachments.isPresent()) {

				final Optional<MessagePart> jpzFile = attachments.get().getParts().stream()
						.filter(p -> p.getFilename().equals(expectedFilename)).findFirst();
				if (jpzFile.isPresent()) {
					final MessagePart part = jpzFile.get();
					final String filename = part.getFilename();
					final String attId = part.getBody().getAttachmentId();
					final MessagePartBody attachPart = this.gmail.users().messages().attachments()
							.get(ME, messageId, attId).execute();
					final byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
					for (final Path path : this.pathsManager.getPaths()) {
						final File outFile = path.resolve(filename).toFile();
						LOG.info(outFile.toString());
						final FileOutputStream fos = new FileOutputStream(outFile);
						fos.write(fileByteArray);
						fos.close();
					}
					return fileByteArray;
				}
			}
			throw new MailFetchException("could not find or download expected xword puzzle as an email attachment");
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

}
