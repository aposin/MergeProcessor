/**
 * Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aposin.mergeprocessor.renaming;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.utils.LogUtil;

/**
 * This service provides functionalities for renamed and linked artifacts. When merges are done
 * artifacts may be renamed or shifted in another directory. Then it is of interest to know the new name,
 * the location and when this renaming of shift took place (i.e. from which version on). 
 * 
 * @author Stefan Weiser
 *
 */
public class RenamingService implements Closeable {

	private final String repository;
	private final Version source;
	private final Version target;
	private final Optional<Connection> dbConnection;

	/**
	 * @param configuration the configuration for setup the database connection
	 * @param repository the repository on which merges are done
	 * @param source the version from which merges are done
	 * @param target the version where merges should be done
	 */
	public RenamingService(final IConfiguration configuration, final String repository, final String source,
			final String target) {
		this(configuration, repository, new Version(source), new Version(target));
	}

	/**
	 * @param configuration the configuration for setup the database connection
	 * @param repository the repository on which merges are done
	 * @param source the version from which merges are done
	 * @param target the version where merges should be done
	 */
	public RenamingService(final IConfiguration configuration, final String repository, final Version source,
			final Version target) {
		if (StringUtils.isEmpty(repository)) {
			throw new IllegalArgumentException("The repository must not be empty.");
		}
		this.repository = repository;
		this.source = source;
		this.target = target;
		dbConnection = setupDatabaseConnection(configuration);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		if (dbConnection.isPresent()) {
			try {
				dbConnection.get().close();
			} catch (SQLException e) {
				LogUtil.getLogger().log(Level.SEVERE, "Could not close database connection", e);
			}
		}
	}

	/**
	 * Checks if the given path is a renamed artifact, using the given source and target version.
	 * 
	 * @param path the path of the artifact
	 * @return  {@code true} if the given path is a renamed artifact
	 */
	public boolean isRenamedArtifact(final Path path) {
		return hasRenamedArtifacts(Collections.singletonList(path));
	}

	/**
	 * Checks if at least 1 of the given paths is a renamed artifact, using the given source and target version.
	 * 
	 * @param paths the paths of all artifacts
	 * @return {@code true} if at least 1 path is a renamed artifact
	 */
	public boolean hasRenamedArtifacts(final Collection<Path> paths) {
		return applyIfPresentDbConnection(false, connection -> hasRenamedArtifacts(paths, connection));
	}

	/**
	 * Checks if at least 1 of the given paths is a renamed artifact, using the given source and target version.
	 * 
	 * @param paths the paths of all artifacts
	 * @param connection the JDBC connection
	 * @return {@code true} if at least 1 path is a renamed artifact
	 */
	private boolean hasRenamedArtifacts(final Collection<Path> paths, final Connection connection) {
		final String sql = "SELECT VERSION, NEW_NAME, OLD_NAME FROM RENAME_MAPPING WHERE OLD_NAME = ? AND REPOSITORY = ?";
		try (final PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(2, repository);
			for (final Path path : paths) {
				final Path output = findRenamedArtifact(path, statement, source);
				if (output != null)
					// If anything found, no further search required
					return true;
			}
		} catch (SQLException e) {
			LogUtil.throwing(e);
		}
		return false;
	}

	/**
	 * Returns the renamed artifact path for the given path, using the given source and target version.
	 * 
	 * @param path the path of the artifact
	 * @param sourceVersion the source version
	 * @param targetVersion the target version
	 * @return the renamed artifact path
	 * @throws RenamingServiceException when an internal error occurs
	 */
	public Path getRenamedArtifact(final Path path) {
		return getRenamedArtifacts(Collections.singletonList(path)).iterator().next();
	}

	/**
	 * Returns an unmodifiable collection of the renamed artifact paths for the given paths, using the given source and target version.
	 * 
	 * @param paths the paths of the artifacts
	 * @return a list of renamed artifact paths
	 * @throws RenamingServiceException when an internal error occurs
	 */
	public List<Path> getRenamedArtifacts(final List<Path> paths) {
		return applyIfPresentDbConnection(paths, connection -> getRenamedArtifacts(paths, connection));
	}

	/**
	 * Returns an unmodifiable collection of the renamed artifact paths for the given paths, using the given source and target version.
	 * 
	 * @param paths the paths of the artifacts
	 * @param connection the JDBC connection
	 * @return a list of renamed artifact paths
	 * @throws RenamingServiceException when an internal error occurs
	 */
	private List<Path> getRenamedArtifacts(final List<Path> paths, final Connection connection) {
		final List<Path> renamed = new ArrayList<>();
		final String sql = "SELECT VERSION, NEW_NAME, OLD_NAME FROM RENAME_MAPPING WHERE OLD_NAME = ? AND REPOSITORY = ?";
		try (final PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(2, repository);
			for (final Path path : paths) {
				final Path output = findRenamedArtifact(path, statement, source);
				// Nothing found --> no renaming available
				renamed.add(output == null ? path : output);
			}
		} catch (SQLException e) {
			LogUtil.throwing(e);
		}
		return Collections.unmodifiableList(renamed);
	}

	/**
	 * Returns the renamed artifact path for the given path, using the given from {@link Version} and the 
	 * target {@link Version} of the service.
	 * 
	 * @param input the input path
	 * @param statement the SQL statement
	 * @param from the version from which to search for
	 * @return the renamed path or {@code null} if no renamed path could be found
	 * @throws SQLException
	 */
	private Path findRenamedArtifact(final Path input, final PreparedStatement statement, final Version from)
			throws SQLException {
		Path pathToRename = input;
		while (pathToRename != null) {
			statement.setString(1, pathToRename.toString().replace('\\', '/'));
			try (final ResultSet rs = statement.executeQuery()) {
				Path foundPath = null;
				Version renamePoint = null;
				while (rs.next()) {
					renamePoint = new Version(rs.getString(1));
					if (renamePoint.isBetween(from, target)) {
						foundPath = Paths.get(rs.getString(2));
						break;
					}
				}
				if (foundPath != null) {
					/*
					 * Have a look, if further renamings are available All renamings should be
					 * considered, so at the end the correct target should be returned, even if 10
					 * renamings happened in the meantime.
					 */

					final Path tmp = findRenamedArtifact(foundPath.resolve(pathToRename.relativize(input)), statement,
							renamePoint);
					if (tmp == null) {
						return foundPath.resolve(pathToRename.relativize(input));
					} else {
						return tmp;
					}
				} else {
					pathToRename = pathToRename.getParent();
				}
			}
		}
		return null;
	}

	/**
	 * Checks if the given path is a linked artifact, using the given source and target version.
	 * 
	 * @param path the path of the artifact
	 * @return  {@code true} if the given path is a linked artifact
	 */
	public boolean isLinkedArtifact(final Path path) {
		return hasLinkedArtifacts(Collections.singletonList(path));
	}

	/**
	 * Checks if at least 1 of the given paths is a linked artifact, using the given source and target version.
	 * 
	 * @param paths the paths of all artifacts
	 * @return {@code true} if at least 1 path is a linked artifact
	 */
	public boolean hasLinkedArtifacts(final List<Path> paths) {
		return applyIfPresentDbConnection(false, connection -> hasLinkedArtifacts(paths, connection));
	}

	/**
	 * Checks if at least 1 of the given paths is a linked artifact, using the given source and target version.
	 * 
	 * @param paths the paths of all artifacts
	 * @param connection the JDBC connection
	 * @return {@code true} if at least 1 path is a linked artifact
	 */
	private boolean hasLinkedArtifacts(final List<Path> paths, final Connection connection) {
		final String sql = "SELECT VERSION, NAME1, NAME2 FROM LINK_MAPPING WHERE (NAME1 = ? OR NAME2 = ?) AND REPOSITORY = ?";
		try (final PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(3, repository);
			for (final Path path : paths) {
				Path pathToLink = path;
				while (pathToLink != null) {
					statement.setString(1, pathToLink.toString().replace('\\', '/'));
					statement.setString(2, pathToLink.toString().replace('\\', '/'));
					try (final ResultSet rs = statement.executeQuery()) {
						Path foundPath = null;
						while (rs.next()) {
							final Version linkPoint = new Version(rs.getString(1));
							if (linkPoint.isOlderThan(target)) {
								foundPath = Paths.get(rs.getString(2));
								if (foundPath.equals(pathToLink)) {
									// Reverse as linked path is searched in Name1 AND Name2
									foundPath = Paths.get(rs.getString(3));
								}
								break;
							}
						}
						if (foundPath != null) {
							return true;
						} else {
							pathToLink = pathToLink.getParent();
						}
					}
				}
			}
		} catch (SQLException e) {
			LogUtil.throwing(e);
		}
		return false;
	}

	/**
	 * Returns the linked artifact path for the given path, using the given source and target version.
	 * 
	 * @param path the path of the artifact
	 * @return the linked artifact path
	 */
	public Path getLinkedArtifact(final Path path) {
		return getLinkedArtifacts(Collections.singletonList(path)).iterator().next();
	}

	/**
	 * Returns a collection of the linked artifact paths for the given paths, using the given source and target version.
	 * 
	 * @param paths the paths of the artifacts
	 * @return a list of linked artifact paths
	 */
	public List<Path> getLinkedArtifacts(final List<Path> paths) {
		return applyIfPresentDbConnection(paths, connection -> getLinkedArtifacts(paths, connection));
	}

	/**
	 * Returns a collection of the linked artifact paths for the given paths, using the given source and target version.
	 * 
	 * @param paths the paths of the artifacts
	 * @param connection the JDBC connection
	 * @return a list of linked artifact paths
	 */
	private List<Path> getLinkedArtifacts(final List<Path> paths, final Connection connection) {
		final List<Path> linked = new ArrayList<>();
		final String sql = "SELECT VERSION, NAME1, NAME2 FROM LINK_MAPPING WHERE (NAME1 = ? OR NAME2 = ?) AND REPOSITORY = ?";
		try (final PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(3, repository);
			for (final Path path : paths) {
				Path pathToLink = path;
				while (pathToLink != null) {
					statement.setString(1, pathToLink.toString().replace('\\', '/'));
					statement.setString(2, pathToLink.toString().replace('\\', '/'));
					try (final ResultSet rs = statement.executeQuery()) {
						Path foundPath = null;
						while (rs.next()) {
							final Version linkPoint = new Version(rs.getString(1));
							if (linkPoint.isOlderThan(target) || linkPoint.equals(target)) {
								foundPath = Paths.get(rs.getString(2));
								if (foundPath.equals(pathToLink)) {
									// Reverse as linked path is searched in Name1 AND Name2
									foundPath = Paths.get(rs.getString(3));
								}
								break;
							}
						}
						if (foundPath != null) {
							linked.add(foundPath.resolve(pathToLink.relativize(path)));
							break;
						} else {
							pathToLink = pathToLink.getParent();
						}
					}
				}
				if (pathToLink == null) {
					// Nothing found --> no linking available
					linked.add(path);
				}
			}
		} catch (SQLException e) {
			LogUtil.throwing(e);
		}
		return Collections.unmodifiableList(linked);
	}

	/**
	 * Returns a list of all observable SVN repositories which exist in the database for linked artifacts.
	 * If the database is not available or an exception is thrown during querying an empty list is returned. 
	 * 
	 * @return a list containing observable SVN repositories for linked artifacts
	 */
	public static List<URL> getObservableSvnRepositoriesForLinkedArtifacts(final IConfiguration configuration) {
		final Optional<Connection> dbConnection = setupDatabaseConnection(configuration);
		if (dbConnection.isPresent()) {
			final Connection connection = dbConnection.get();
			final String sql = "SELECT DISTINCT REPOSITORY FROM LINK_MAPPING";
			try (final PreparedStatement statement = connection.prepareStatement(sql);
					final ResultSet result = statement.executeQuery()) {
				final List<URL> list = new ArrayList<>();
				while (result.next()) {
					final String repo = result.getString(1);
					try {
						list.add(new URL(repo));
					} catch (MalformedURLException e) {
						LogUtil.getLogger()
								.warning(String.format("The result '%s' from the sql query is no URL.", repo));
					}
				}
				return list;
			} catch (SQLException e) {
				LogUtil.throwing(e);
			}
		} else {
			Logger.getLogger(RenamingService.class.getName()).log(Level.WARNING, "No renaming database available.");
		}
		return Collections.emptyList();
	}

	/**
	 * Utility method calling the function, if a database connection is available.
	 * 
	 * @param paths the paths to return if no db connection is available
	 * @param function the function to apply
	 * @return the return value of the function or the paths itself if no db connection is available
	 */
	private <T> List<T> applyIfPresentDbConnection(final List<T> paths, final Function<Connection, List<T>> function) {
		if (dbConnection.isPresent()) {
			return function.apply(dbConnection.get());
		} else {
			LogUtil.getLogger().log(Level.WARNING, "No renaming database available.");
			return Collections.unmodifiableList(paths);
		}
	}

	/**
	 * Utility method calling the functon, if a database connection is avilable.
	 * 
	 * @param value the value to return if no db connection is available
	 * @param function the function to apply
	 * @return the return value of the function or the value parameter if no db connection is available
	 */
	private boolean applyIfPresentDbConnection(final boolean value, final Function<Connection, Boolean> function) {
		if (dbConnection.isPresent()) {
			return function.apply(dbConnection.get());
		} else {
			LogUtil.getLogger().log(Level.WARNING, "No renaming database available.");
			return value;
		}
	}

	/**
	 * Creates a connection to the renaming database.
	 * 
	 * @return a new connection to the renaming database if connection succeeded
	 */
	private static Optional<Connection> setupDatabaseConnection(IConfiguration configuration) {
		try {
			final String jdbcUrl;
			if (configuration.getRenameDatabaseUrl().startsWith("jdbc:h2:file:")) {
				if (configuration.getLocalH2RenameDatabase() != null
						&& configuration.getLocalH2RenameDatabase().toFile().exists()) {
					jdbcUrl = "jdbc:h2:file:" + configuration.getLocalH2RenameDatabase().toString().replace(".mv.db",
							";IFEXISTS=TRUE;ACCESS_MODE_DATA=r");
				} else {
					jdbcUrl = configuration.getRenameDatabaseUrl();
				}
			} else {
				jdbcUrl = configuration.getRenameDatabaseUrl();
			}
			return Optional.of(DriverManager.getConnection(jdbcUrl, configuration.getRenameDatabaseUser(),
					configuration.getRenameDatabasePassword()));
		} catch (SQLException e) {
			Logger.getLogger(RenamingService.class.getName()).log(Level.SEVERE, "Could not setup database connection",
					e);
			return Optional.empty();
		}
	}

}
