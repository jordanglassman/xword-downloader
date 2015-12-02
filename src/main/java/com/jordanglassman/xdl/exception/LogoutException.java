package com.jordanglassman.xdl.exception;

import org.apache.http.HttpException;

public class LogoutException extends HttpException {
	public LogoutException() {
		super();
	}

	public LogoutException(String message) {
		super(message);
	}

	public LogoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
