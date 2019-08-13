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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.aposin.mergeprocessor.configuration.ConfigurationException;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.configuration.ISftpConfiguration;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.view.Column;

/**
 * Implementation of {@link IConfiguration} for JUnit tests.
 * 
 * @author Stefan Weiser
 *
 */
public class JUnitConfiguration implements IConfiguration {

	private boolean automatically = true;
	private boolean displayDone = false;
	private boolean displayIgnored = false;
	private Column sortColumn = Column.COLUMN_HOST;
	private int sortDirection = 1 << 10; // see SWT.DOWN

	private String renameDatabaseUrl = "jdbc:h2:C:\\dev\\eclipseworkspaces\\mergeprocessor\\repo\\tests\\org.aposin.mergeprocessor.test\\testH2\\test";

	private Path localH2RenameDatabase = null;

	private String svnUsername;
	private String svnPassword;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutomatic() {
		return automatically;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAutomatic(boolean automatically) {
		this.automatically = automatically;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDisplayDone() {
		return displayDone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisplayDone(boolean displayDone) {
		this.displayDone = displayDone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDisplayIgnored() {
		return displayIgnored;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisplayIgnored(boolean displayIgnored) {
		this.displayIgnored = displayIgnored;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Column getSortColumn() {
		return sortColumn;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSortColumn(Column sortColumn) {
		this.sortColumn = sortColumn;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSortDirection() {
		return sortDirection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSortDirection(int sortDirection) {
		this.sortDirection = sortDirection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getGitRepositoryFolder() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean autoCreateGitRepository() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRenameDatabaseUrl() {
		return renameDatabaseUrl;
	}

	public void setRenameDatabaseUrl(String renameDatabaseUrl) {
		this.renameDatabaseUrl = renameDatabaseUrl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRenameDatabaseUser() {
		return "sa";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRenameDatabasePassword() {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getEclipseApplicationPath() {
		return Paths.get("C:\\Program Files\\eclipse47\\eclipse.exe");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEclipseApplicationParameters() {
		return "-vmargs -Xms400M -Xmx1700M -XX:MaxPermSize=256M -Dorg.eclipse.ecf.provider.filetransfer.excludeContributors=org.eclipse.ecf.provider.filetransfer.httpclient4";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getLastEclipseWorkspacePath() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLastEclipseWorkspacePath(Path path) {
		// NOOP
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasLocalH2RenameDatabase() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getLocalH2RenameDatabase() {
		if (localH2RenameDatabase == null) {
			try {
				localH2RenameDatabase = Files.createTempFile("testRename", ".mv.db");
				localH2RenameDatabase.toFile().deleteOnExit();
			} catch (IOException e) {
				LogUtil.throwing(e);
			}
		}
		return localH2RenameDatabase;
	}

	public void setLocalH2RenameDatabase(Path localH2RenameDatabase) {
		this.localH2RenameDatabase = localH2RenameDatabase;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getLastRepositoryPath() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLastRepositoryPath(Path path) {
		// NOOP
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getUserPrefsPath() {
		return null;
	}

	@Override
	public ISftpConfiguration getSftpConfiguration() {
		return new ISftpConfiguration() {

			@Override
			public String getTodoFolder() {
				return "testMergeUnits/todo";
			}

			@Override
			public String getDoneFolder() {
				return "testMergeUnits/done";
			}

			@Override
			public String getIgnoredFolder() {
				return "testMergeUnits/ignore";
			}

			@Override
			public String getCanceledFolder() {
				return "testMergeUnits/canceled";
			}

			@Override
			public String getManualFolder() {
				return "testMergeUnits/manual";
			}

			@Override
			public String getHost() {
				return "localhost";
			}

			@Override
			public String getUser() {
				return "testuser";
			}

			@Override
			public String getPassword() {
				return null;
			}
		};
	}

	@Override
	public List<Path> getVersionInfoPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getUserWorkingFolder() {
		try {
			final Path tempDirectory = Files.createTempDirectory("userWorkingFolder");
			tempDirectory.toFile().deleteOnExit();
			return tempDirectory;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getUser() {
		return "testuser";
	}

	@Override
	public int getRefreshInterval() {
		return 0;
	}

	@Override
	public void setSvnUsername(String username) throws ConfigurationException {
		svnUsername = username;
	}

	@Override
	public String getSvnUsername() throws ConfigurationException {
		return svnUsername;
	}

	@Override
	public void setSvnPassword(String password) throws ConfigurationException {
		svnPassword = password;
	}

	@Override
	public String getSvnPassword() throws ConfigurationException {
		return svnPassword;
	}

}
