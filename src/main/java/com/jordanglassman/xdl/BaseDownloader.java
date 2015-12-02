package com.jordanglassman.xdl;

import com.jordanglassman.xdl.exception.LoginException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.HtmlCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public abstract class BaseDownloader implements Downloader {
	private static final Logger LOG = LoggerFactory.getLogger(BaseDownloader.class);

	private CloseableHttpClient httpClient;
	private final HtmlCleaner cleaner;

	private final LoginInfo loginInfo;
	private final List<Path> paths;

	private final Date today;


	public BaseDownloader(final LoginInfo loginInfo, final List<Path> paths) {
		this.loginInfo = loginInfo;
		this.paths = paths;

		this.today = new Date();

		// init httpclient
		final BasicCookieStore cookieStore = new BasicCookieStore();
		final HttpClientBuilder builder = HttpClients.custom().setDefaultCookieStore(cookieStore);
		this.httpClient = builder.build();

		this.cleaner = new HtmlCleaner();
	}

	@Override
	public void doDownload() {
		if(this.authenticate()) {
			final byte[] rawCrossword = this.download(this.getDownloadUrlFormat());
			this.write(rawCrossword);
			this.logout();
		}
	}

	protected abstract String getDownloadUrlFormat();
	protected abstract String getFilenameFormat();

	@Override
	public byte[] download(final String urlFormat) {
		final String downloadPath = String.format(urlFormat, this.today, this.today, this.today);
		LOG.debug("downloading daily crossword at url={}", downloadPath);

		final HttpUriRequest downloadGet = RequestBuilder.get().setUri(downloadPath).build();

		try (CloseableHttpResponse crosswordResponse = this.httpClient.execute(downloadGet)) {
			final HttpEntity crosswordEntity = crosswordResponse.getEntity();
			return EntityUtils.toByteArray(crosswordEntity);
		} catch (final IOException e) {
			LOG.error("error while downloading puzzle from nyt, e={}", e.getMessage());
			return new byte[0];
		}
	}

	@Override
	public void write(final byte[] rawCrossword) {
		final String filename = String
				.format(this.getFilenameFormat(), this.getToday(), this.getToday(), this.getToday());

		for (final Path path : this.getPaths()) {
			final Path resolvedPath = path.resolve(filename);
			try {
				LOG.debug("writing {} byte daily crossword to path={}", rawCrossword.length, resolvedPath);
				FileUtils.writeByteArrayToFile(resolvedPath.toFile(), rawCrossword);
			} catch (final IOException e) {
				LOG.error("error while writing puzzle to disk, e={}", e.getMessage());
			}
		}
	}

	protected boolean throwLoginException(final String message, final Object... params) throws LoginException {
		final String errorMessage = String.format(message, params);
		throw new LoginException(errorMessage);
	}

	public LoginInfo getLoginInfo() {
		return this.loginInfo;
	}

	public List<Path> getPaths() {
		return this.paths;
	}

	public HtmlCleaner getCleaner() {
		return this.cleaner;
	}

	public CloseableHttpClient getHttpClient() {
		return this.httpClient;
	}

	public void setHttpClient(final CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public Date getToday() {
		return this.today;
	}
}
