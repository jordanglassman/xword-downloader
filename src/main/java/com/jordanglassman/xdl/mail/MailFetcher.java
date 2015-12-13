package com.jordanglassman.xdl.mail;

public interface MailFetcher {
	boolean authenticate();
	Object getMessage();
	byte[] getAttachment(Object message);
}
