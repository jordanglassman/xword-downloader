package com.jordanglassman.xdl.download.nyt;

import com.jordanglassman.xdl.XwordType;
import com.jordanglassman.xdl.download.BaseDownloader;
import com.jordanglassman.xdl.exception.LoginException;
import com.jordanglassman.xdl.exception.LogoutException;
import com.jordanglassman.xdl.util.LoginInfo;
import com.jordanglassman.xdl.util.PathsManager;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NytDownloader extends BaseDownloader {
	private static final Logger LOG = LoggerFactory.getLogger(NytDownloader.class);

	private static final String NYT_LOGIN_URL = "https://myaccount.nytimes.com/auth/login";

	private static final String NYT_CROSSWORD_URL = "http://www.nytimes.com/svc/crosswords/v2/puzzle/daily-%tY-%tm-%td.puz";
	private static final String NYT_FILENAME = "%tb%td%ty.puz";

	private static final String NYT_LOGOUT_URL = "https://myaccount.nytimes.com/gst/signout?module=LogOut&action=Click&region=TopBar&WT.nav=shell&pgtype=Games";

	public NytDownloader(final LoginInfo nytLoginInfo, final PathsManager pathsManager) {
		super(nytLoginInfo, pathsManager);
	}

	@Override
	protected String getDownloadUrlFormat() {
		return NYT_CROSSWORD_URL;
	}

	@Override
	protected String getFilenameFormat() {
		return NYT_FILENAME;
	}

	@Override
	public boolean authenticate() {
		if(!super.authenticate()) {
			LOG.error(String.format("blank username or password detected, no %s xword will be downloaded", this.getType()));
			return false;
		}

		final HttpUriRequest loginGet = RequestBuilder.get().setUri(NYT_LOGIN_URL).build();

		final String loginPage;
		try (final CloseableHttpResponse getResponse = this.getHttpClient().execute(loginGet)) {
			loginPage = EntityUtils.toString(getResponse.getEntity());
		} catch (final IOException e) {
			LOG.error("error while navigating to NYT login page", e);
			return false;
		}

		final String token;
		final String expires;

		try {
			final TagNode node = this.getCleaner().clean(loginPage);

			final Object[] foundNodes = node.evaluateXPath("//input[@name='token']");
			if (foundNodes.length != 1) {
				this.throwLoginException("unexpected login page, found %d hidden token input elements, expected 1",
						foundNodes.length);
			}
			final TagNode hiddenTokenInput = (TagNode) foundNodes[0];
			token = hiddenTokenInput.getAttributeByName("value");
			LOG.debug("found hidden input token {}", token);

			final Object[] foundExpiresNodes = node.evaluateXPath("//input[@name='expires']");
			if (foundExpiresNodes.length != 1) {
				this.throwLoginException(
						"unexpected login page, found %d hidden token expiration input elements, expected 1",
						foundNodes.length);
			}
			final TagNode hiddenTokenExpiresInput = (TagNode) foundExpiresNodes[0];
			expires = hiddenTokenExpiresInput.getAttributeByName("value");
			LOG.debug("found hidden input token expiration {}", expires);
		} catch (LoginException | XPatherException e) {
			LOG.error("error while pulling login tokens from NYT login page", e);
			return false;
		}

		// @formatter:off
			final HttpUriRequest loginPost = RequestBuilder.post().setUri("https://myaccount.nytimes.com/auth/login")
					.addParameter("is_continue", Boolean.FALSE.toString())
					.addParameter("token", token)
					.addParameter("expires", expires)
					.addParameter("userid", this.getLoginInfo().getUsername())
					.addParameter("password", this.getLoginInfo().getPassword())
					.addParameter("remember", Boolean.TRUE.toString())
					.build();
			// @formatter:on

		try (CloseableHttpResponse postResponse = this.getHttpClient().execute(loginPost)) {

			// successful NYT login should give 302 status
			final int responseStatus = postResponse.getStatusLine().getStatusCode();
			if (responseStatus != 302) {
				final String errorMessage = String
						.format("did not detect expected 302 redirect, got %d instead", responseStatus);
				throw new LoginException(errorMessage);
			}

			// successful NYT login redirects to the NYT homepage
			final Header location = postResponse.getFirstHeader("Location");
			// have seen this redirect both with and without the final portion
			final Pattern expectedRedirectLocation = Pattern.compile("http://www.nytimes.com(\\?login=email)*");
			final String actualRedirectLocation = location.getValue();
			final Matcher matcher = expectedRedirectLocation.matcher(actualRedirectLocation);
			if (!matcher.matches()) {
				final String errorMessage = String
						.format("redirect to unexpected URL, expected %s, found Location=%s instead",
								expectedRedirectLocation, actualRedirectLocation);
				throw new LoginException(errorMessage);
			}

			// successful NYT login should set a few cookies
			final Header[] cookies = postResponse.getHeaders("Set-Cookie");
			if (cookies.length < 1) {
				throw new LoginException("no post login cookies set, login likely failed");
			}

		} catch (final IOException | LoginException e) {
			LOG.error("error while logging in, e={}", e.getMessage());
			return false;
		}

		LOG.info("successfully logged in to nyt");
		return true;
	}

	@Override
	public void logout() {
		final HttpUriRequest logoutGet = RequestBuilder.get().setUri(NYT_LOGOUT_URL).build();

		try (final CloseableHttpResponse getResponse = this.getHttpClient().execute(logoutGet)) {

			// successful NYT logout should give 200 status
			final int responseStatus = getResponse.getStatusLine().getStatusCode();
			if (responseStatus != 200) {
				final String errorMessage = String
						.format("did not detect expected 200, got %d instead", responseStatus);
				throw new LogoutException(errorMessage);
			}

			// successful NYT logout should delete a few cookies like this:
			// Set-Cookie: NYT-S=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; path=/; domain=.nytimes.com

			final Header[] cookies = getResponse.getHeaders("Set-Cookie");

			if (cookies.length < 1) {
				throw new LogoutException("no cookie deletions detected, logout might have failed");
			}

			final Stream<Header> cookieStream = Arrays.stream(cookies);
			final Predicate<Header> deletedCheck = c -> c.getValue().contains("deleted");
			if (!cookieStream.allMatch(deletedCheck)) {
				final List<Header> unexpectedCookies = cookieStream.filter(deletedCheck).collect(Collectors.toList());
				LOG.error("unexpected cookies={}", unexpectedCookies);
				throw new LogoutException("unexpected cookie(s) set, loguout might have failed");
			}

			LOG.info("successfully logged out of nyt");
		} catch (IOException | LogoutException e) {
			LOG.error("error while logging out of nyt, e={}", e);
		}
	}

	@Override public XwordType getType() {
		return XwordType.NYT;
	}
}
