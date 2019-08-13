/**
 * Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aposin.mergeprocessor.renaming;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.utils.FileUtils;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtilException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This class setups the optional H2 database.
 * 
 * @author Stefan Weiser
 *
 */
public class H2DatabaseSetup {

	private final IConfiguration configuration;
	private final IShellProvider shellProvider;
	private final Display display;

	/**
	 * @param configuration the {@link IConfiguration} which must not be
	 *                      {@code null}
	 */
	public H2DatabaseSetup(final IConfiguration configuration) {
		this((Display) null, configuration);
	}

	/**
	 * @param display       the {@link Display}
	 * @param configuration the {@link IConfiguration} which must not be
	 *                      {@code null}
	 */
	public H2DatabaseSetup(final Display display, final IConfiguration configuration) {
		Objects.requireNonNull(configuration);
		this.display = display;
		this.shellProvider = null;
		this.configuration = configuration;
	}

	/**
	 * @param shellProvider the {@link IShellProvider}
	 * @param configuration the {@link IConfiguration} which must not be
	 *                      {@code null}
	 */
	public H2DatabaseSetup(final IShellProvider shellProvider, final IConfiguration configuration) {
		Objects.requireNonNull(configuration);
		this.display = shellProvider.getShell().getDisplay();
		this.shellProvider = shellProvider;
		this.configuration = configuration;
	}

	/**
	 * Copies the H2 database of the configured JDBC url, if required.
	 * 
	 * @throws MergeProcessorUtilException
	 */
	public void downloadH2FileDatabaseIfRequired() throws MergeProcessorUtilException {
		Objects.requireNonNull(configuration, "IConfiguration not injected.");
		if (isDownloadRequired()) {
			Objects.requireNonNull(display, "Display not injected.");
			download();
		}
	}

	/**
	 * Checks, if the configured JDBC String requires a new copy for a local H2
	 * database.
	 * 
	 * @return {@code true} if copying is required
	 */
	private boolean isDownloadRequired() {
		return !getListOfDownloadableDBs().isEmpty();
	}

	/**
	 * @return a list of available rename databases when one have to be copied
	 *         locally
	 */
	private List<Path> getListOfDownloadableDBs() {
		final String dbUrl = Objects.requireNonNull(configuration).getRenameDatabaseUrl();
		// Only copy referenced H2 file databases
		if (dbUrl != null && dbUrl.startsWith("jdbc:h2:file:")) {
			final String pathAsString = dbUrl.substring("jdbc:h2:file:".length());
			// First check for existing directory
			if (Files.isDirectory(Paths.get(pathAsString))) {
				final Path path = Paths.get(pathAsString);
				try {
					final List<Path> list = Files.walk(path) //
							.filter(p -> p.toString().endsWith(".mv.db")) // Find all H2 database files
							.filter(p -> getLastModifiedTime(p) > 0l) // Filter paths where the last modification time
																		// could not be evaluated
							.filter(p -> hasNewDbAYoungerModifiedTime(p)) // Filter paths only newer than the existing
																			// one
							.sorted(((o1, o2) -> -1 * Long.compare(getLastModifiedTime(o1), getLastModifiedTime(o2)))) // Sort
																														// paths
																														// against
																														// their
																														// modification
																														// time
							.collect(Collectors.toList());
					return list;
				} catch (IOException e) {
					LogUtil.getLogger().warning(String.format("Error occured on walking file tree '%s'", path));
				}
			} else if (Files.exists(Paths.get(pathAsString + ".mv.db"))) {
				final Path path = Paths.get(pathAsString + ".mv.db");
				// It's a file
				if (hasNewDbAYoungerModifiedTime(path)) {
					return Collections.singletonList(path);
				}
			} else {
				LogUtil.getLogger()
						.warning(String.format("The given rename H2 database path '%s' does not exist", dbUrl));
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Check if the new database has a younger modification time than the existing
	 * one (if existing). If new local database exists, the method always returns
	 * {@code true}.
	 * 
	 * @param the new database to check if it has a younger modification time
	 * @return {@code true} if the new database has a younger modification time or
	 *         if the no local database does exist, else {@code false}
	 */
	private boolean hasNewDbAYoungerModifiedTime(final Path newDb) {
		// Database file found
		final Path localH2Db = configuration.getLocalH2RenameDatabase();
		if (Files.exists(localH2Db)) {
			// Check time stamp
			final long oldTime = getLastModifiedTime(localH2Db);
			final long newTime = getLastModifiedTime(newDb);
			return newTime > oldTime; //
		} else {
			// No local H2 database available, so copy anyway
			return true;
		}
	}

	/**
	 * Copies the H2 database of the configured JDBC url.
	 * 
	 * @param newUrl the JDBC url.
	 * @throws MergeProcessorUtilException
	 */
	private void download() throws MergeProcessorUtilException {
		final List<Path> pathToCopy = getListOfDownloadableDBs();

		if (!pathToCopy.isEmpty()) {

			final IRunnableWithProgress runnable = new DownloadRunnable(pathToCopy, configuration);

			final ProgressMonitorDialog dialog;
			if (display == null) {
				dialog = null;
				try {
					runnable.run(new NullProgressMonitor());
				} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				final Shell parentShell = shellProvider == null ? new Shell(display) : shellProvider.getShell();
				try {
					dialog = new ProgressMonitorDialog(parentShell);
					dialog.run(true, true, runnable);
				} catch (InvocationTargetException | InterruptedException e) {
					LogUtil.throwing(e);
				} finally {
					if (shellProvider == null) {
						parentShell.dispose();
					}
				}
			}

		}
	}

	/**
	 * Returns the last modification time of the given path. If an error occurs
	 * {@code 0l} is returned.
	 * 
	 * @param path the path where to check the last modification time
	 * @return the last modification time or 0l if an error occurs
	 */
	private static long getLastModifiedTime(final Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			LogUtil.getLogger()
					.warning(String.format("Error occurred on accessing last modification time of %s.", path));
			return 0l;
		}
	}

	/**
	 * This runnable downloads the first valid entry of the paths to copy into the
	 * path defined by {@link IConfiguration#getLocalH2RenameDatabase()}.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static final class DownloadRunnable implements IRunnableWithProgress {

		private static final List<String> TABLE_NAMES = List.of("LINK_MAPPING", "RENAME_MAPPING");
		private static final List<String> LINK_MAPPING_COLUMN_NAMES = List.of("ID", "NAME1", "NAME2", "VERSION",
				"REPOSITORY");
		private static final List<String> RENAME_MAPPING_COLUMN_NAMES = List.of("ID", "OLD_NAME", "NEW_NAME", "VERSION",
				"REPOSITORY");

		private final List<Path> pathToCopy;
		private final IConfiguration configuration;

		private DownloadRunnable(List<Path> pathsToCopy, final IConfiguration configuration) {
			this.pathToCopy = pathsToCopy;
			this.configuration = configuration;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			final Path localH2RenameDatabase = configuration.getLocalH2RenameDatabase();
			for (final Path path : pathToCopy) {
				deleteExisting();
				final boolean success = FileUtils.copyFiles(path, localH2RenameDatabase, monitor);
				if (success) {
					if (path.toFile().exists()) {
						if (isDatabaseValid(localH2RenameDatabase)) {
							break;
						} else {
							LogUtil.getLogger()
									.warning(String.format("%s does not contain a valid database structure.", path));
							deleteExisting();
						}
					} else {
						LogUtil.getLogger()
								.warning(String.format("Copy from %s to %s failed.", path, localH2RenameDatabase));
					}
				} else {
					LogUtil.getLogger()
							.warning(String.format("Copy from %s to %s failed.", path, localH2RenameDatabase));
					deleteExisting();
				}
			}
		}

		/**
		 * Delete potential old local database.
		 */
		private void deleteExisting() {
			final Path localH2RenameDatabase = configuration.getLocalH2RenameDatabase();
			try {
				Files.deleteIfExists(localH2RenameDatabase);
			} catch (IOException e) {
				LogUtil.getLogger().log(Level.SEVERE,
						String.format("Could not delete local database '%s'", localH2RenameDatabase.toString()), e);
			}
		}

		/**
		 * Check if the given H2 database file is valid in it's structure.
		 * 
		 * @param path the path of the H2 database file
		 * @return {@code true} if the structure is valid
		 */
		private boolean isDatabaseValid(final Path path) {
			final String h2FileDbUrl = "jdbc:h2:file:"
					+ path.toString().replace(".mv.db", ";IFEXISTS=TRUE;ACCESS_MODE_DATA=r");
			try (final Connection connection = DriverManager.getConnection(h2FileDbUrl,
					configuration.getRenameDatabaseUser(), configuration.getRenameDatabasePassword());
					final Statement statement = connection.createStatement()) {
				if (isDatabaseValid(statement)) {
					// All conditions OK
					return true;
				} else {
					LogUtil.getLogger().info(String.format(
							"The given rename H2 database '%s' does not match the required H2 database structure.",
							h2FileDbUrl));
					return false;
				}
			} catch (SQLException e) {
				LogUtil.getLogger().warning(
						String.format("The given rename H2 database path '%s' is no H2 database.", h2FileDbUrl));
				return false;
			}
		}

		/**
		 * Check if the given H2 database is valid in it's structure.
		 * 
		 * @param statement the already open SQL {@link Statement}
		 * @return {@code true} if the structure is valid
		 * @throws SQLException
		 */
		private static boolean isDatabaseValid(final Statement statement) throws SQLException {
			return isTableValid(statement) && // Check table
					isLinkMappingColumnValid(statement) && // Check LINK_MAPPING column
					isRenameMappingColumnValid(statement); // Check RENAME_MAPPING column
		}

		/**
		 * Check if the given H2 database contains all expected tables.
		 * 
		 * @param statement the already open SQL {@link Statement}
		 * @return {@code true} if all expected tables exist
		 * @throws SQLException
		 */
		private static boolean isTableValid(final Statement statement) throws SQLException {
			try (final ResultSet resultSet = statement.executeQuery("SHOW TABLES")) {
				final List<String> availableTableNames = new ArrayList<>(2);
				while (resultSet.next()) {
					availableTableNames.add(resultSet.getString(1));
				}
				return availableTableNames.containsAll(TABLE_NAMES);
			}
		}

		/**
		 * Check if the given table LINK_MAPPING contains all expected columns.
		 * 
		 * @param statement the already open SQL {@link Statement}
		 * @return {@code true} if all expected columns exist
		 * @throws SQLException
		 */
		private static boolean isLinkMappingColumnValid(final Statement statement) throws SQLException {
			try (final ResultSet resultSet2 = statement.executeQuery("SHOW COLUMNS FROM LINK_MAPPING")) {
				final List<String> availableColumns = new ArrayList<>(5);
				while (resultSet2.next()) {
					availableColumns.add(resultSet2.getString(1));
				}
				return availableColumns.containsAll(LINK_MAPPING_COLUMN_NAMES);
			}
		}

		/**
		 * Check if the given table RENAME_MAPPING contains all expected columns.
		 * 
		 * @param statement the already open SQL {@link Statement}
		 * @return {@code true} if all expected columns exist
		 * @throws SQLException
		 */
		private static boolean isRenameMappingColumnValid(final Statement statement) throws SQLException {
			try (final ResultSet resultSet2 = statement.executeQuery("SHOW COLUMNS FROM RENAME_MAPPING")) {
				final List<String> availableColumns = new ArrayList<>(5);
				while (resultSet2.next()) {
					availableColumns.add(resultSet2.getString(1));
				}
				return availableColumns.containsAll(RENAME_MAPPING_COLUMN_NAMES);
			}
		}

	}

}
