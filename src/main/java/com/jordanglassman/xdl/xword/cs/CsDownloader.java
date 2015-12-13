package com.jordanglassman.xdl.xword.cs;

import com.google.api.services.gmail.model.Message;
import com.jordanglassman.xdl.XwordType;
import com.jordanglassman.xdl.mail.GmailFetcher;
import com.jordanglassman.xdl.util.PathsManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class CsDownloader {
	private static final Logger LOG = LoggerFactory.getLogger(CsDownloader.class);
	private static final String CS_FILENAME = "cs%ty%tm%td.jpz";

	private PathsManager pathsManager;

	private GmailFetcher gmailFetcher;

	public CsDownloader(final PathsManager pathsManager) {
		this.pathsManager = pathsManager;
		this.gmailFetcher = new GmailFetcher(this.pathsManager);
	}

	public void doDownload() {
		if (this.gmailFetcher.authenticate()) {
			final Message message = this.gmailFetcher.getMessage();
			final byte[] rawPuzzle = this.gmailFetcher.getAttachment(message);
			this.write(rawPuzzle);
		} else
			LOG.error(String.format("authentication failed, no %s xword downloaded", this.getType()));
	}

	protected String getFilenameFormat() {
		return CS_FILENAME;
	}

	/**
	 * CS puzzles do not vary in difficulty by day, only by constructor.  Might need to rewrite this to add paths by
	 * constructor or some other system.
	 *
	 * @param rawCrossword
	 */
	public void write(final byte[] rawCrossword) {
		final Date today = this.pathsManager.getToday();
		final String filename = String.format(this.getFilenameFormat(), today, today, today);

		final List<Path> paths = this.pathsManager.getPaths();

		for (final Path path : paths) {
			final Path resolvedPath = path.resolve(filename);
			try {
				LOG.debug("writing {} byte daily crossword to path={}", rawCrossword.length, resolvedPath);
				FileUtils.writeByteArrayToFile(resolvedPath.toFile(), rawCrossword);
			} catch (final IOException e) {
				LOG.error("error while writing puzzle to disk, e={}", e.getMessage());
			}
		}

	}

	public XwordType getType() {
		return XwordType.CS;
	}
}
