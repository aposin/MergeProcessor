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
package org.aposin.mergeprocessor.configuration;

import java.nio.file.Path;
import java.util.List;

import org.aposin.mergeprocessor.view.Column;
import org.eclipse.swt.SWT;

/**
 * Interface defining configurable parameters for the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public interface IConfiguration {

	/**
	 * @return the configuration for SFTP settings
	 */
	public ISftpConfiguration getSftpConfiguration();

	/**
	 * @return {@code true} if merges should be done automatically
	 */
	boolean isAutomatic();

	/**
	 * @param automatically {@code true} if merges should be done automatically
	 */
	void setAutomatic(boolean automatically);

	/**
	 * @return {@code true} if done merge units should be shown
	 */
	boolean isDisplayDone();

	/**
	 * @param displayDone  {@code true} if done merge units should be shown
	 */
	void setDisplayDone(boolean displayDone);

	/**
	 * @return {@code true} if ignored merge units should be shown
	 */
	boolean isDisplayIgnored();

	/**
	 * @param displayIgnored {@code true} if ignored merge units should be shown
	 */
	void setDisplayIgnored(boolean displayIgnored);

	/**
	 * @return the column to sort by
	 */
	Column getSortColumn();

	/**
	 * @param column the column to sort by
	 */
	void setSortColumn(Column column);

	/**
	 * @return {@value SWT#DOWN} if to sort descending, {@value SWT#UP} if to sort ascending. 
	 * Other values are are unknown and not supported.
	 */
	int getSortDirection();

	/**
	 * @param sortDirection {@value SWT#DOWN} if to sort descending, {@value SWT#UP} if to 
	 * sort ascending. Other values are are unknown and not supported.
	 */
	void setSortDirection(int sortDirection);

	/**
	 * @return the folder where the GIT repositories are set up.
	 */
	String getGitRepositoryFolder();

	/**
	 * @return {@code true} if the GIT repository should be created automatically, if required
	 */
	boolean autoCreateGitRepository();

	/**
	 * @return the database URL for merging renamed artifacts
	 */
	String getRenameDatabaseUrl();

	/**
	 * @return the database user for merging renamed artifacts
	 */
	String getRenameDatabaseUser();

	/**
	 * @return the database password for merging renamed artifacts
	 */
	String getRenameDatabasePassword();

	/**
	 * @return {@code true} if a local H2 rename database is available 
	 */
	boolean hasLocalH2RenameDatabase();

	/**
	 * @return the path of the local H2 rename database
	 */
	Path getLocalH2RenameDatabase();

	/**
	 * @return the path where the eclipse application is available for start
	 */
	Path getEclipseApplicationPath();

	/**
	 * @return the parameters when starting the eclipse application
	 */
	String getEclipseApplicationParameters();

	/**
	 * @return the last used eclipse workspace path
	 */
	Path getLastEclipseWorkspacePath();

	/**
	 * @param path the last used eclipse workspace path
	 */
	void setLastEclipseWorkspacePath(final Path path);

	/**
	 * @return the last used repository path
	 */
	Path getLastRepositoryPath();

	/**
	 * @param path the last used repository path
	 */
	void setLastRepositoryPath(final Path path);

	/**
	 * @return the path which contains the user specific initial preference settings
	 */
	Path getUserPrefsPath();

	/**
	 * @return the paths pointing to files to identify the version of a software product
	 */
	List<Path> getVersionInfoPaths();

	/**
	 * @return the working folder path for the user
	 */
	Path getUserWorkingFolder();

	/**
	 * @return the user's id for who to merge to
	 */
	String getUser();

	/**
	 * @return the interval to automatically refresh
	 */
	int getRefreshInterval();

	/**
	 * @param username the user name for the SVN credentials
	 * @throws ConfigurationException
	 */
	void setSvnUsername(String username) throws ConfigurationException;

	/**
	 * @return the user name for the SVN credentials
	 * @throws ConfigurationException
	 */
	String getSvnUsername() throws ConfigurationException;

	/**
	 * @param password the password for the SVN credentials
	 * @throws ConfigurationException
	 */
	void setSvnPassword(String password) throws ConfigurationException;

	/**
	 * @return the password for the SVN credentials
	 * @throws ConfigurationException
	 */
	String getSvnPassword() throws ConfigurationException;

}
