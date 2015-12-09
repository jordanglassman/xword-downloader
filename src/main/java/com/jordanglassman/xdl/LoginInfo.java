package com.jordanglassman.xdl;

import org.apache.commons.lang3.StringUtils;

public class LoginInfo {
	private String username;
	private String password;

	public String getPassword() {
		return this.password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public boolean hasBlank() {
		return StringUtils.isBlank(this.username) || StringUtils.isBlank(this.password);
	}
}
