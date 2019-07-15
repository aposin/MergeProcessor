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
/**
 * 
 */
package org.aposin.mergeprocessor.configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.view.Column;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;

/**
 *
 */
public class Configuration implements IConfiguration {

    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    static final IPreferenceStore PREFERENCE_STORE = Activator.getDefault() == null ? null
            : Activator.getDefault().getPreferenceStore();

    private static final String PATH_USER_WORKING_FOLDER = System.getenv("APPDATA") + "\\MergeProcessor"; //$NON-NLS-1$ //$NON-NLS-2$
    private static String pathSvnkitFolder = PATH_USER_WORKING_FOLDER + "\\svnkit" + File.separator; //$NON-NLS-1$

    private final IPreferenceStore preferenceStore;
    private final ISftpConfiguration sftpConfiguration;
    private final CredentialStore credentialStore;

    @Inject
    public Configuration(final CredentialStore credentialStore) {
        this.preferenceStore = Objects
                .requireNonNull(Activator.getDefault(), "Plugin was not started. No Preference store available.")
                .getPreferenceStore();
        this.sftpConfiguration = new SftpConfiguration(this.preferenceStore);
        this.credentialStore = credentialStore;
    }

    /**
     * Full path of the log file folder. Log4j2 is configured in 'log4j2.yaml'.
     */
    private static final String PATH_LOG_FILE_FOLDER = PATH_USER_WORKING_FOLDER + "\\logs" + File.separator; //$NON-NLS-1$

    /**
     * File name without extension of the log file. Log4j2 is configured in 'log4j2.yaml'.
     */
    private static final String LOG_FILE_NAME = "mergeprocessor"; //$NON-NLS-1$

    /**
     * Extension of the log file. Log4j2 is configured in 'log4j2.yaml'.
     */
    private static final String LOG_FILE_EXTENSION = ".log"; //$NON-NLS-1$

    /**
     * Full path of the log file. Log4j2 is configured in 'log4j2.yaml'.
     */
    private static final String PATH_LOG_FILE = PATH_LOG_FILE_FOLDER + LOG_FILE_NAME + LOG_FILE_EXTENSION;

    /**
     * File extension of plain merge files.
     * @deprecated only for compatibility. Could be removed after next release.
     */
    @Deprecated
    public static final String EXTENSION_PLAINMERGE_FILE = ".merge"; //$NON-NLS-1$

    /**
     * File extension of svn merge files.
     */
    public static final String SVN_EXTENSION_FILE = ".svnmerge"; //$NON-NLS-1$
    /** File extension of svn merge files doing package merges */
    public static final String SVN_PACKAGE_MERGE_EXTENSION_FILE = ".svnmergepackage"; //$NON-NLS-1$

    /**
     * File extension of git merge files.
     */
    public static final String GIT_EXTENSION_FILE = ".gitmerge"; //$NON-NLS-1$

    /**
     * Name of the working copy subfolder in the MergeProcessor working folder.
     */
    public static final String SUBFOLDER_SVN_WORKINGCOPY = "wc" + File.separator; //$NON-NLS-1$
    /**
     * Name of the sftp subfolder in the MergeProcessor working folder.
     */
    public static final String SUBFOLDER_SFTP_WORKINGFOLDER = ".ssh" + File.separator; //$NON-NLS-1$

    private static final String LOCK_FILE = "mp_lock"; //$NON-NLS-1$

    private static final String LOCAL_MERGE_FILE_FOLDER = "work"; //$NON-NLS-1$

    private static final String LAST_ECLIPSE_WORKSPACE_PATH = "LAST_ECLIPSE_WORKSPACE_PATH";

    private static final String LAST_REPOSITORY_PATH = "LAST_REPOSITORY_PATH";

    private static final String LOCAL_H2_RENAME_DB = "rename2.mv.db";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutomatic() {
        return LogUtil.exiting(preferenceStore.getBoolean(WorkbenchPreferencePage.OPTION_AUTOMATIC));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAutomatic(boolean automatically) {
        LogUtil.entering(automatically);
        preferenceStore.setValue(WorkbenchPreferencePage.OPTION_AUTOMATIC, automatically);
        LogUtil.getLogger().config("Automatic merge " + (automatically ? "activated." : "deactivated"));
        LogUtil.exiting();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDisplayDone() {
        return LogUtil.exiting(preferenceStore.getBoolean(WorkbenchPreferencePage.OPTION_DISPLAY_DONE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayDone(boolean displayDone) {
        LogUtil.entering(displayDone);
        preferenceStore.setValue(WorkbenchPreferencePage.OPTION_DISPLAY_DONE, displayDone);
        LogUtil.getLogger().config("Show executed merge units " + (displayDone ? "activated." : "deactivated"));
        LogUtil.exiting();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDisplayIgnored() {
        return LogUtil.exiting(preferenceStore.getBoolean(WorkbenchPreferencePage.OPTION_DISPLAY_IGNORED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDisplayIgnored(boolean displayIgnored) {
        LogUtil.entering(displayIgnored);
        preferenceStore.setValue(WorkbenchPreferencePage.OPTION_DISPLAY_IGNORED, displayIgnored);
        LogUtil.getLogger().config("Show ignored merge units " + (displayIgnored ? "activated." : "deactivated"));
        LogUtil.exiting();
    }

    /**
     * @return the logLevel
     */
    public static Level getLogLevel() {
        return LogUtil.exiting(Level.parse(PREFERENCE_STORE.getString(WorkbenchPreferencePage.LOG_LEVEL)));
    }

    /**
     * @return the working folder on the local drive to work on
     */
    public static String getWorkingFolder() {
        return PREFERENCE_STORE.getString(WorkbenchPreferencePage.WORKING_FOLDER);
    }

    /**
     * @return the path to the svn working copy
     */
    public static String getPathSvnWorkingCopy() {
        return LogUtil.exiting(getWorkingFolder() + SUBFOLDER_SVN_WORKINGCOPY);
    }

    /**
     * @return the path to the SFTP working folder.
     */
    public static String getPathSftpWorkingFolder() {
        return LogUtil.exiting(getWorkingFolder() + SUBFOLDER_SFTP_WORKINGFOLDER);
    }

    /**
     * @return the pathLogFileFolder
     */
    public static String getPathLogFileFolder() {
        return LogUtil.exiting(PATH_LOG_FILE_FOLDER);

    }

    /**
     * @return the pathLocalMergeFileFolder
     */
    public static String getPathLocalMergeFileFolder() {
        return LogUtil.exiting(getWorkingFolder() + LOCAL_MERGE_FILE_FOLDER + File.separator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRefreshInterval() {
        int refreshInterval = PREFERENCE_STORE.getInt(WorkbenchPreferencePage.REFRESH_INTERVAL);
        return LogUtil.exiting((int) TimeUnit.MILLISECONDS.convert(refreshInterval, TimeUnit.SECONDS));
    }

    /**
     * @return the path of the Svnkit folder
     */
    public static String getPathSvnkitFolder() {
        return LogUtil.exiting(pathSvnkitFolder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUser() {
        return LogUtil.exiting(PREFERENCE_STORE.getString(WorkbenchPreferencePage.USER_ID));
    }

    /**
     * @return the version of this plugin
     */
    public static String getVersion() {
        String version = "-"; //$NON-NLS-1$
        try {
            Dictionary<String, String> directory = Platform.getBundle(Activator.PLUGIN_ID).getHeaders();
            version = directory.get("Bundle-Version"); //$NON-NLS-1$
        } catch (Exception ex) {
            String message = String.format("Couldn't find version of plugin [%s].", Activator.PLUGIN_ID); //$NON-NLS-1$
            LOGGER.log(Level.WARNING, message, ex);
        }
        return LogUtil.exiting(version);
    }

    /**
     * @return the path to the lock file
     */
    public static String getPathLockFile() {
        return LogUtil.exiting(getWorkingFolder() + LOCK_FILE);
    }

    /**
     * @return the path to the log file.
     */
    public static String getPathLogFile() {
        return LogUtil.exiting(PATH_LOG_FILE);
    }

    /**
     * @return the path to the log file.
     */
    public static String getLogFileName() {
        return LogUtil.exiting(LOG_FILE_NAME);
    }

    /**
     * @return the path to the log file.
     */
    public static String getLogFileExtension() {
        return LogUtil.exiting(LOG_FILE_EXTENSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSvnUsername() throws ConfigurationException {
        return LogUtil.exiting(credentialStore.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSvnUsername(String svnUsername) throws ConfigurationException {
        LogUtil.entering(svnUsername);
        credentialStore.setUsername(svnUsername);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSvnPassword() throws ConfigurationException {
        return LogUtil.exiting(credentialStore.getPassword());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSvnPassword(String svnPassword) throws ConfigurationException {
        LogUtil.entering(svnPassword);
        credentialStore.setPassword(svnPassword);
    }

    /**
     * Clears the credential store
     */
    public void clearCredentialStore() {
        LogUtil.entering();
        credentialStore.clearCredentials();
        LogUtil.exiting();
    }

    /**
     * @return a point representing the window location or null if there is no window size set.
     */
    public static Point getWindowLocation() {
        return LogUtil
                .exiting(parsePointFromString(PREFERENCE_STORE.getString(WorkbenchPreferencePage.WINDOW_LOCATION)));
    }

    /**
     * Sets the windowLocation to the given location.
     * @param location the new location
     */
    public static void setWindowLocation(Point location) {
        LogUtil.entering(location);
        String windowLocationNew = location.x + "," + location.y; //$NON-NLS-1$
        PREFERENCE_STORE.setValue(WorkbenchPreferencePage.WINDOW_LOCATION, windowLocationNew);
    }

    /**
     * @return a point representing the window size or null if there is no window size set.
     */
    public static Point getWindowSize() {
        return LogUtil.exiting(parsePointFromString(PREFERENCE_STORE.getString(WorkbenchPreferencePage.WINDOW_SIZE)));
    }

    /**
     * Sets the windowSize to the given size
     * @param size the new size
     */
    public static void setWindowSize(Point size) {
        LogUtil.entering(size);
        String windowSizeNew = size.x + "," + size.y; //$NON-NLS-1$
        PREFERENCE_STORE.setValue(WorkbenchPreferencePage.WINDOW_SIZE, windowSizeNew);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column getSortColumn() {
        return LogUtil.exiting(Column.valueForIndex(preferenceStore.getInt(WorkbenchPreferencePage.SORT_COLUMN)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSortColumn(Column column) {
        LogUtil.entering(column);
        preferenceStore.setValue(WorkbenchPreferencePage.SORT_COLUMN, column.ordinal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSortDirection() {
        return LogUtil.exiting(preferenceStore.getInt(WorkbenchPreferencePage.SORT_DIRECTION));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSortDirection(int sortDirection) {
        LogUtil.entering(sortDirection);
        preferenceStore.setValue(WorkbenchPreferencePage.SORT_DIRECTION, sortDirection);
    }

    public static Point parsePointFromString(String value) {
        LogUtil.entering(value);
        Point retVal = null;

        if (value != null) {
            String[] values = value.split(","); //$NON-NLS-1$

            if (values.length == 2) {
                try {
                    int x = Integer.parseInt(values[0]);
                    int y = Integer.parseInt(values[1]);
                    retVal = new Point(x, y);
                } catch (NumberFormatException e) {
                    String message = String.format("Couldn't parse Point from value=[%s].", value); //$NON-NLS-1$
                    LOGGER.log(Level.WARNING, message, e);
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(String.format("Couldn't parse Point from value=%s.", value)); //$NON-NLS-1$
                }
            }
        }
        return LogUtil.exiting(retVal);
    }

    /**
     * Returns the local path of the file for the given {@link IMergeUnit}.
     * 
     * @param mergeUnit the {@link IMergeUnit}
     * @return the local path
     */
    public static String getPathLocalMergeFile(final IMergeUnit mergeUnit) {
        return getPathLocalMergeFileFolder() + mergeUnit.getFileName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGitRepositoryFolder() {
        return LogUtil.exiting(preferenceStore.getString(GitRepositoriesPreferencePage.GIT_REPOSITORIES_FOLDER));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean autoCreateGitRepository() {
        return LogUtil
                .exiting(preferenceStore.getBoolean(GitRepositoriesPreferencePage.GIT_REPOSITORIES_AUTO_REPO_CREATE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRenameDatabaseUrl() {
        return LogUtil.exiting(preferenceStore.getString(RenamingPreferencePage.RENAME_DATABASE_URL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRenameDatabaseUser() {
        return LogUtil.exiting(preferenceStore.getString(RenamingPreferencePage.RENAME_DATABASE_USER));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRenameDatabasePassword() {
        return LogUtil.exiting(preferenceStore.getString(RenamingPreferencePage.RENAME_DATABASE_PASSWORD));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLocalH2RenameDatabase() {
        return getLocalH2RenameDatabase().toFile().exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getLocalH2RenameDatabase() {
        return Paths.get(preferenceStore.getString(WorkbenchPreferencePage.WORKING_FOLDER) + LOCAL_H2_RENAME_DB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getEclipseApplicationPath() {
        final String value = preferenceStore.getString(EclipseWorkspaceStartPeferencePage.ECLIPSE_APPLICATION_PATH);
        return LogUtil.exiting(Paths.get(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEclipseApplicationParameters() {
        return LogUtil
                .exiting(preferenceStore.getString(EclipseWorkspaceStartPeferencePage.ECLIPSE_APPLICATION_PARAMETERS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getLastEclipseWorkspacePath() {
        final String value = preferenceStore.getString(LAST_ECLIPSE_WORKSPACE_PATH);
        return LogUtil.exiting(Paths.get(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLastEclipseWorkspacePath(Path path) {
        LogUtil.entering(path);
        preferenceStore.setValue(LAST_ECLIPSE_WORKSPACE_PATH, path.toString());
        LogUtil.exiting();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getLastRepositoryPath() {
        final String value = preferenceStore.getString(LAST_REPOSITORY_PATH);
        return LogUtil.exiting(Paths.get(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLastRepositoryPath(Path path) {
        LogUtil.entering(path);
        preferenceStore.setValue(LAST_REPOSITORY_PATH, path.toString());
        LogUtil.exiting();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getUserPrefsPath() {
        /*
         * 1. Take prefs from Programdata
         * 2. Take prefs from user working folder, if not existing in ProgramData
         */
        final String programData = System.getenv("ProgramData");
        if (programData != null) {
            final Path programDatatPrefs = Paths.get(programData, "MergeProcessor", "org.aposin.mergeprocessor.prefs");
            if (Files.exists(programDatatPrefs)) {
                //Prefs file found in ProgramData
                return programDatatPrefs;
            }
        }
        //Prefs file not found in Program Data, expect it in user working folder
        return getUserWorkingFolder().resolve("org.aposin.mergeprocessor.prefs");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISftpConfiguration getSftpConfiguration() {
        return sftpConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Path> getVersionInfoPaths() {
        LogUtil.entering();
        final String value = preferenceStore.getString(WorkbenchPreferencePage.VERSION_INFO_FILES);
        if (value == null) {
            return LogUtil.exiting(Collections.emptyList());
        }
        final String[] paths = value.split(",");
        return LogUtil.exiting(Arrays.stream(paths).map(s -> Paths.get(s.trim())).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getUserWorkingFolder() {
        return Paths.get(PATH_USER_WORKING_FOLDER);
    }

}
