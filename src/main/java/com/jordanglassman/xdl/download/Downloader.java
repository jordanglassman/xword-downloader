package com.jordanglassman.xdl.download;

import com.jordanglassman.xdl.XwordType;

public interface Downloader {
	void doDownload();

	boolean authenticate();

	byte[] download(String urlFormat);

	void write(byte[] rawData);

	void logout();

	XwordType getType();
}
