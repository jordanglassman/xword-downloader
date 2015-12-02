package com.jordanglassman.xdl;

public interface Downloader {
	void doDownload();

	boolean authenticate();

	byte[] download(String urlFormat);

	void write(byte[] rawData);

	void logout();
}
