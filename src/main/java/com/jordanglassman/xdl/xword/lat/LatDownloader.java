package com.jordanglassman.xdl.xword.lat;

import com.jordanglassman.xdl.XwordType;
import com.jordanglassman.xdl.download.BaseDownloader;
import com.jordanglassman.xdl.exception.LoginException;
import com.jordanglassman.xdl.exception.LogoutException;
import com.jordanglassman.xdl.util.LoginInfo;
import com.jordanglassman.xdl.util.PathsManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LatDownloader extends BaseDownloader {
	private static final Logger LOG = LoggerFactory.getLogger(LatDownloader.class);

	private static final String LAT_LOGIN_URL = "http://www.cruciverb.com/index.php?action=login";
	private static final String LAT_LOGIN2_URL = "http://www.cruciverb.com/index.php?action=login2";

	private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36";

	private static final String LAT_FILENAME = "lat%ty%tm%td.puz";
	private static final String LAT_CROSSWORD_URL = "http://www.cruciverb.com/download.php?f=" + LAT_FILENAME;

	private static final String LAT_LOGOUT_URL = "http://www.cruciverb.com/index.php?action=logout";
	private static final String REFERER = "http://www.cruciverb.com/index.php";

	private String hiddenName = "";
	private String hiddenValue = "";

	public LatDownloader(final LoginInfo nytLoginInfo, final PathsManager pathsManager) {
		super(nytLoginInfo, pathsManager);

		// override default httpclient for cruciverb

		// init httpclient configs
		final BasicCookieStore cookieStore = new BasicCookieStore();

		// cruciverb (SMF) sometimes returns 500 errors when certain headers are not set
		final Header userAgentHeader = new BasicHeader(HTTP.USER_AGENT, USER_AGENT);
		final Header refererHeader = new BasicHeader("Referer", REFERER);
		final Collection<Header> headers = new ArrayDeque<>();
		headers.add(userAgentHeader);
		headers.add(refererHeader);

		// init http client
		final HttpClientBuilder builder = HttpClients.custom();
		builder.setDefaultCookieStore(cookieStore);
		builder.setDefaultHeaders(headers);
		this.setHttpClient(builder.build());
	}

	@Override
	protected String getDownloadUrlFormat() {
		return LAT_CROSSWORD_URL;
	}

	@Override
	protected String getFilenameFormat() {
		return LAT_FILENAME;
	}

	@Override
	public boolean authenticate() {
		if (!super.authenticate()) {
			LOG.error(String.format("blank username or password detected, no %s xword will be downloaded",
					this.getType()));
			return false;
		}

		final HttpUriRequest loginGet = RequestBuilder.get().setUri(LAT_LOGIN_URL).build();

		// arbitrary valid user agent; the default httpclient seems to invalidate the session
		loginGet.setHeader("User-Agent", USER_AGENT);

		final String loginPage;
		try (final CloseableHttpResponse getResponse = this.getHttpClient().execute(loginGet)) {
			loginPage = EntityUtils.toString(getResponse.getEntity());
		} catch (final IOException e) {
			LOG.error("error while navigating to LAT login page", e);
			return false;
		}

		try {
			final TagNode node = this.getCleaner().clean(loginPage);

			final Object[] foundNodes = node.evaluateXPath("(//input)[3]");
			if (foundNodes.length != 1) {
				this.throwLoginException("unexpected login page, could not find hidden input element");
			}

			final TagNode loginForm = (TagNode) foundNodes[0];
			this.hiddenName = loginForm.getAttributeByName("name");
			LOG.debug("found hidden hiddenName={}", this.hiddenName);
			this.hiddenValue = loginForm.getAttributeByName("value");
			LOG.debug("found hidden hiddenValue={}", this.hiddenValue);

			if (StringUtils.isBlank(this.hiddenName) || StringUtils.isBlank(this.hiddenValue)) {
				LOG.error("could not find hidden input elements, continuing with login, logout will "
								+ "likely cause an error that can be ignored");
			}
		} catch (LoginException | XPatherException e) {
			LOG.error("error while getting LAT login tokens", e);
			return false;
		}

		final String username = this.getLoginInfo().getUsername();
		final String password = this.getLoginInfo().getPassword();

		// @formatter:off
		final HttpUriRequest loginPost = RequestBuilder.post().setUri(LAT_LOGIN2_URL)
				.addParameter("user",  username)
				.addParameter("passwrd", password)
				.addParameter("cookielength", "-1")
				.addParameter(this.hiddenName, this.hiddenValue)
				.build();
		// @formatter:on

		try (CloseableHttpResponse postResponse = this.getHttpClient().execute(loginPost)) {

			// successful LAT login should give 200 status
			final int responseStatus = postResponse.getStatusLine().getStatusCode();
			if (responseStatus != 302) {
				final String errorMessage = String
						.format("did not detect expected 302 redirect, got %d instead", responseStatus);
				throw new LoginException(errorMessage);
			}

			// successful LAT login redirects to the LAT homepage
			final Header location = postResponse.getFirstHeader("Location");
			final Pattern expectedRedirectLocation = Pattern.compile("http://www.cruciverb.com/index.php.*");
			final String actualRedirectLocation = location.getValue();
			if (!expectedRedirectLocation.matcher(actualRedirectLocation).matches()) {
				final String errorMessage = String
						.format("redirect to unexpected URL, expected %s, found Location=%s instead",
								expectedRedirectLocation, actualRedirectLocation);
				throw new LoginException(errorMessage);
			}

			// successful LAT login should set a few cookies
			final Header[] cookies = postResponse.getHeaders("Set-Cookie");
			if (cookies.length < 1) {
				throw new LoginException("no post login cookies set, login likely failed");
			}

		} catch (final IOException | LoginException e) {
			LOG.error("error while logging in, e={}", e.getMessage());
			return false;
		}

		LOG.info("successfully logged in to LAT");
		return true;
	}

	@Override
	public void logout() {
		final String logoutUrl = LAT_LOGOUT_URL + ";" + this.hiddenName + "=" + this.hiddenValue;

		// in order to verify proper logout and cookie deletion, need override the default redirect behavior since this is a GET
		final HttpGet logoutGet = new HttpGet(logoutUrl);
		final RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).build();
		logoutGet.setConfig(config);

		try (final CloseableHttpResponse getResponse = this.getHttpClient().execute(logoutGet)) {

			// successful LAT logout should give a 302 redirect
			final int responseStatus = getResponse.getStatusLine().getStatusCode();
			if (responseStatus != 302) {
				final String errorMessage = String
						.format("did not detect expected 302, got %d instead", responseStatus);
				throw new LogoutException(errorMessage);
			}

			// successful LAT logout redirects to the cruciverb index page
			final Header location = getResponse.getFirstHeader("Location");
			final String expectedRedirectLocation = "http://www.cruciverb.com/index.php";
			final String actualRedirectLocation = location.getValue();
			if (!expectedRedirectLocation.equals(actualRedirectLocation)) {
				final String errorMessage = String
						.format("redirect to unexpected URL, expected %s, found Location=%s instead",
								expectedRedirectLocation, actualRedirectLocation);
				throw new LogoutException(errorMessage);
			}

			// successful LAT logout should reset at least one of its cookies to a negative max-age
			final Header[] cookies = getResponse.getHeaders("Set-Cookie");

			if (cookies.length < 1) {
				throw new LogoutException("no cookie deletions detected, logout might have failed");
			}

			final Stream<Header> cookieStream = Arrays.stream(cookies);
			final Pattern maxAge = Pattern.compile(".*Max-Age=(-\\d+).*");

			final Predicate<Header> maxAgeCheck = c -> {
				final Matcher matcher = maxAge.matcher(c.getValue());
				final boolean foundMaxAge = matcher.find();
				if (foundMaxAge) {
					final String age = matcher.group(1);
					final int intAge = Integer.valueOf(age);
					if (intAge < 0)
						return true;
				}
				return false;
			};

			if (!cookieStream.anyMatch(maxAgeCheck)) {
				final List<Header> unexpectedCookies = cookieStream.filter(maxAgeCheck).collect(Collectors.toList());
				LOG.error("unexpected cookies={}", unexpectedCookies);
				throw new LogoutException("unexpected cookie(s) set, loguout might have failed");
			}

			LOG.info("successfully logged out of LAT");
		} catch (IOException | LogoutException e) {
			LOG.error("error while logging out of LAT, e={}", e);
		}
	}

	@Override
	public XwordType getType() {
		return XwordType.LAT;
	}
}
