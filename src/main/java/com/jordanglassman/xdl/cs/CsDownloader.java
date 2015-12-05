package com.jordanglassman.xdl.cs;

import com.jordanglassman.xdl.BaseDownloader;
import com.jordanglassman.xdl.CredsManager;
import com.jordanglassman.xdl.LoginInfo;
import com.jordanglassman.xdl.XwordType;

import javax.mail.*;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

public class CsDownloader extends BaseDownloader {

	public CsDownloader(LoginInfo loginInfo, List<Path> paths) {
		super(loginInfo, paths);
	}

	@Override
	protected String getDownloadUrlFormat() {
		return null;
	}

	@Override
	protected String getFilenameFormat() {
		return null;
	}

	@Override
	public boolean authenticate() {
		return false;
	}

	@Override
	public void logout() {

	}

	@Override
	public byte[] download(String urlFormat) {
		return super.download(urlFormat);
	}

	@Override
	public void write(byte[] rawCrossword) {
		super.write(rawCrossword);
	}

	public static void main(String[] args) {
		CredsManager credsManager = new CredsManager();

//		Properties properties = new Properties();

		String host = credsManager.getHost(XwordType.CS);
		Integer port = credsManager.getPort(XwordType.CS);
		String username = credsManager.getUsername(XwordType.CS);
		String password = credsManager.getPassword(XwordType.CS);
		String provider = "imaps";

		//		JavaMail spec appendix A properties
		//		mail.store.protocol
		//		mail.transport.protocol
		//		mail.host
		//		mail.user
		//		mail.protocol.host
		//		mail.protocol.user
		//		mail.user
		//		mail.from
		//		mail.debug

		// https://myaccount.google.com/security?pli=1
		// Allow less secure apps: ON

		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		try {
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imaps");
			store.connect("imap.gmail.com", username, password);
			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);
						// get a list of javamail messages as an array of messages
						Message[] messages = inbox.getMessages();

						TreeSet treeSet = new TreeSet();

						for (int i = 0; i < messages.length; i++) {
							String from = getFrom(messages[i]);
							if (from != null) {
								from = removeQuotes(from);
								treeSet.add(from);
							}
						}

						Iterator it = treeSet.iterator();
						while (it.hasNext()) {
							System.out.println("from: " + it.next());
						}
			inbox.close(false);
			store.close();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (MessagingException e) {
			e.printStackTrace();
			System.exit(2);
		}

		// set this session up to use SSL for IMAP connections
//		properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//		 don't fallback to normal IMAP connections on failure.
//		properties.setProperty("mail.imap.socketFactory.fallback", "false");
//		 use the simap port for imap/ssl connections.
//		properties.setProperty("mail.imap.socketFactory.port", "993");
//
//		properties.setProperty("mail.store.protocol", "imaps");
//		properties.setProperty("mail.imap.host", host);
//		properties.setProperty("mail.imap.user", username);
//		properties.setProperty("mail.debug", "true");

//		try {
//			//Connect to the server
//			Session session = Session.getDefaultInstance(properties, null);
//			Store store = session.getStore(provider);
//			store.connect(host, port, username, password);
//
//			//open the inbox folder
//			Folder inbox = store.getFolder("INBOX");
//			inbox.open(Folder.READ_ONLY);
//
//			// get a list of javamail messages as an array of messages
//			Message[] messages = inbox.getMessages();
//
//			TreeSet treeSet = new TreeSet();
//
//			for (int i = 0; i < messages.length; i++) {
//				String from = getFrom(messages[i]);
//				if (from != null) {
//					from = removeQuotes(from);
//					treeSet.add(from);
//				}
//			}
//
//			Iterator it = treeSet.iterator();
//			while (it.hasNext()) {
//				System.out.println("from: " + it.next());
//			}
//
//			//close the inbox folder but do not
//			//remove the messages from the server
//			inbox.close(false);
//			store.close();
//		} catch (NoSuchProviderException nspe) {
//			System.err.println("invalid provider name");
//		} catch (MessagingException me) {
//			System.err.println("messaging exception");
//			me.printStackTrace();
//		}
	}

	private static String getFrom(Message javaMailMessage) throws MessagingException {
		String from = "";
		Address a[] = javaMailMessage.getFrom();
		if (a == null)
			return null;
		for (int i = 0; i < a.length; i++) {
			Address address = a[i];
			from = from + address.toString();
		}

		return from;
	}

	private static String removeQuotes(String stringToModify) {
		int indexOfFind = stringToModify.indexOf(stringToModify);
		if (indexOfFind < 0)
			return stringToModify;

		StringBuffer oldStringBuffer = new StringBuffer(stringToModify);
		StringBuffer newStringBuffer = new StringBuffer();
		for (int i = 0, length = oldStringBuffer.length(); i < length; i++) {
			char c = oldStringBuffer.charAt(i);
			if (c == '"' || c == '\'') {
				// do nothing
			} else {
				newStringBuffer.append(c);
			}

		}
		return new String(newStringBuffer);
	}

}
