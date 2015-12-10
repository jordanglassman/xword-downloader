package com.jordanglassman.xdl;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jordanglassman.xdl.SystemProperties.BASE_DOWNLOAD_DIR;

public class PathsManager {
	private static final Logger LOG = LoggerFactory.getLogger(PathsManager.class);

	private static final String XWORDS = "xwords";

	private Path baseDir;
	private Path credsDir;

	private List<Path> paths = new ArrayList<>();
	private LocalDate today = DateTime.now().toLocalDate();

	public PathsManager() {
		this.baseDir = this.initBaseDir();
		this.credsDir = this.initCredsDir();
	}

	public void init() {
		final Path daily = this.initDailyDir(this.baseDir, this.today);
		final Path archive = this.initArchiveDir(this.baseDir, this.today);

		this.paths.add(daily);
		this.paths.add(archive);

		LOG.debug("PathsManager() - created download dirs={}", this.paths);

		// remove any null paths
		this.paths = this.paths.stream().filter(Objects::nonNull).collect(Collectors.toList());

		if (this.paths.size() < 1) {
			LOG.warn("PathsManager() - no valid paths identified, no puzzles will be written");
		}
	}

	/**
	 * Gets a list of existing paths to write each crossword to.
	 *
	 * @return
	 */
	public List<Path> getPaths() {
		return this.paths;
	}

	/**
	 * Development utility for cleaning all previously built dirs.
	 */
	public void cleanPaths() {
		final FileVisitor<Path> deleter = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				LOG.debug("deleting file={}", file);
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
				if(exc == null) {
					LOG.debug("deleting dir={}", dir);
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					throw exc;
				}
			}
		};

		try {
			Files.walkFileTree(this.baseDir, deleter);
		} catch (final IOException e) {
			LOG.error("cleanPaths() - error while deleting download dirs, continuing anyway");
		}
	}

	private Path initBaseDir() {
		// use user-defined base dir if available, otherwise default to /user/home/xword
		final String userDir = System.getProperty("user.home");
		final String baseDir = System.getProperty(BASE_DOWNLOAD_DIR, userDir);

		LOG.debug("using baseDir={}", baseDir);

		Path baseDirPath = Paths.get(baseDir);
		Path xwordsDir;

		if (!baseDirPath.isAbsolute()) {
			LOG.info("detected relative baseDir path setting={}, using user home directory={} as the parent path",
					baseDir, userDir);
			xwordsDir = Paths.get(userDir).resolve(baseDirPath).resolve(XWORDS);
		} else {
			xwordsDir = baseDirPath.resolve(XWORDS);
		}

		LOG.info("writing xwords to xwordsDir={}", xwordsDir);

		return xwordsDir;
	}

	private Path initCredsDir() {
		final Path credsDir = this.baseDir.resolve("creds");
		try {
			Files.createDirectories(credsDir);
		} catch (IOException e) {
			LOG.error("error while creating creds dir, downloaders that rely on creds written to disk will not function");
			return null;
		}
		return credsDir;
	}

	private Path initDailyDir(final Path baseDirPath, final LocalDate today) {
		final String dayOfWeek = DateTimeFormat.forPattern("EEE").print(today).toLowerCase();
		Path daily = null;

		try {
			daily = baseDirPath.resolve("daily").resolve(dayOfWeek);
			Files.createDirectories(daily);

			// delete whatever xwords were already there
			final Stream<Path> xwords = Files.list(daily);
			xwords.forEach((path) -> {
				try {
					Files.delete(path);
				} catch (final IOException e) {
					LOG.error("error while deleting daily xwords already present");
				}
			});

			LOG.debug("initDailyDir() - created/verified daily dir={}", daily);
			return daily;
		} catch (final IOException e) {
			LOG.error("error while creating daily directory={}", daily);
		}

		LOG.warn("initDailyDir() - returning null path, no daily puzzle will be written");
		return null;
	}

	private Path initArchiveDir(final Path baseDirPath, final LocalDate today) {
		Path archive = null;
		final String formattedToday = DateTimeFormat.forPattern("EEE-MM-dd-yy").print(today).toLowerCase();

		try {
			archive = baseDirPath.resolve("archive").resolve(formattedToday);
			Files.createDirectories(archive);
			LOG.debug("initArchiveDir() - created/verified archive dir={}", archive);
			return archive;
		} catch (final IOException e) {
			LOG.error("error while creating archive directory={}", archive);
		}

		LOG.warn("initArchiveDir() - returning null path, no archive puzzle will be written");
		return null;
	}

	public Path getBaseDir() {
		return baseDir;
	}

	public Path getCredsDir() {
		return credsDir;
	}
}
