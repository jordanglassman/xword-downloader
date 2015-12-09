package com.jordanglassman.xdl.exception;

import org.apache.http.HttpException;

import java.io.IOException;

public class MailFetchException extends IOException {
	public MailFetchException() {
		super();
	}

	public MailFetchException(String message) {
		super(message);
	}

	public MailFetchException(String message, Throwable cause) {
		super(message, cause);
	}
}
