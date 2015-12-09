package com.jordanglassman.xdl.util;

public interface MailFetcher {
	boolean authenticate();
	Object getMessage(final String subject);
	byte[] getAttachment(Object message);
}
