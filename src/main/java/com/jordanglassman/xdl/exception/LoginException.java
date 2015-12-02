package com.jordanglassman.xdl.exception;

import org.apache.http.HttpException;

public class LoginException extends HttpException {
	public LoginException() {
		super();
	}

	public LoginException(String message) {
		super(message);
	}

	public LoginException(String message, Throwable cause) {
		super(message, cause);
	}
}
