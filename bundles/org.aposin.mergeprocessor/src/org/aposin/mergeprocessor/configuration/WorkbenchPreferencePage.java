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
/**
 *
 */
package org.aposin.mergeprocessor.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.view.Column;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for configuring merge processor specific settings.
 * 
 * @author Stefan Weiser
 *
 */
public class WorkbenchPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Local working folder of the MergeProcessor
	 */
	public static final String WORKING_FOLDER = "WORKING_FOLDER"; //$NON-NLS-1$

	/**
	 * User to look on the sftp server for merge units
	 */
	public static final String USER_ID = "USER_ID"; //$NON-NLS-1$

	/**
	 * Refresh interval in which the sftp server is queried.
	 */
	public static final String REFRESH_INTERVAL = "REFRESH_INTERVAL"; //$NON-NLS-1$

	/**
	 * Log level for the MergeProcessor
	 */
	public static final String LOG_LEVEL = "LOG_LEVEL"; //$NON-NLS-1$

	/**
	 * location of the MergeProcessor window
	 */
	public static final String WINDOW_LOCATION = "WINDOW_LOCATION"; //$NON-NLS-1$

	/**
	 * size of the MergeProcessor window
	 */
	public static final String WINDOW_SIZE = "WINDOW_SIZE"; //$NON-NLS-1$

	/**
	 * column by which the table is sorted
	 */
	public static final String SORT_COLUMN = "SORT_COLUMN"; //$NON-NLS-1$

	/**
	 * direction in which the table is sorted
	 */
	public static final String SORT_DIRECTION = "SORT_DIRECTION"; //$NON-NLS-1$

	/**
	 * option to automatically merge all new merge units
	 */
	public static final String OPTION_AUTOMATIC = "OPTION_AUTOMATIC"; //$NON-NLS-1$

	/**
	 * option to show mergeunits with status DONE
	 */
	public static final String OPTION_DISPLAY_DONE = "OPTION_DISPLAY_DONE"; //$NON-NLS-1$

	/**
	 * option to show mergeunits with status IGNORED
	 */
	public static final String OPTION_DISPLAY_IGNORED = "OPTION_DISPLAY_IGNORED"; //$NON-NLS-1$

	/**
	 * button to clear the svn password caches
	 */
	public static final String CLEAR_SVN_PASSWORD_CACHES = "CLEAR_SVN_PASSWORD_CACHES"; //$NON-NLS-1$ NOSONAR, it's a
																						// constant, not a password

	/**
	 * sftp username to authenticate at the sftp server
	 */
	public static final String SFTP_USERNAME = "SFTP_USERNAME"; //$NON-NLS-1$

	/**
	 * sftp password to authenticate at the sftp server
	 */
	public static final String SFTP_PASSWORD = "SFTP_PASSWORD"; //$NON-NLS-1$ NOSONAR, it's a constant, not a password

	/**
	 * folder where to look for merges
	 */
	public static final String SFTP_MERGEFOLDER = "SFTP_MERGEFOLDER"; //$NON-NLS-1$

	/**
	 * sftp server name
	 */
	public static final String SFTP_HOST = "SFTP_HOST"; //$NON-NLS-1$

	/** Property listing files containing the version of the software product */
	public static final String VERSION_INFO_FILES = "VERSION_INFO_FILES"; //$NON-NLS-1$

	public WorkbenchPreferencePage() {
		super(GRID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.WorkbenchPreferencePage_Description);
	}

	private static List<java.util.logging.Level> getLogLevels() {
		final List<java.util.logging.Level> list = new ArrayList<>();
		list.add(java.util.logging.Level.OFF);
		list.add(java.util.logging.Level.SEVERE);
		list.add(java.util.logging.Level.WARNING);
		list.add(java.util.logging.Level.INFO);
		list.add(java.util.logging.Level.CONFIG);
		list.add(java.util.logging.Level.FINE);
		list.add(java.util.logging.Level.FINER);
		list.add(java.util.logging.Level.FINEST);
		list.add(java.util.logging.Level.ALL);
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		addField(createWorkingFolderFieldEditor());
		addField(createUserFieldEditor());
		addField(createRefreshIntervalFieldEditor());

		String[][] entryNamesAndValues = getLogLevels().stream()
				.map(level -> new String[] { level.getName(), level.getName() }).toArray(String[][]::new);
		ComboFieldEditor comboFieldEditorLogLevel = new ComboFieldEditor(LOG_LEVEL,
				Messages.WorkbenchPreferencePage_LogLevel, entryNamesAndValues, getFieldEditorParent());
		addField(comboFieldEditorLogLevel);

		addField(createWindowLocationFieldEditor());
		addField(createWindowSizeFieldEditor());

		String[][] entrySortColumnNamesAndValues = Arrays.stream(Column.sortedValues())
				.map(column -> new String[] { column.toString(), Integer.toString(column.ordinal()) })
				.toArray(size -> new String[size][]);
		ComboFieldEditor cfeSortColumn = new ComboFieldEditor(SORT_COLUMN,
				Messages.WorkbenchPreferencePage_SortedColumn, entrySortColumnNamesAndValues, getFieldEditorParent());
		addField(cfeSortColumn);

		String[][] entrySortDirectionNamesAndValues = new String[][] {
				{ Messages.WorkbenchPreferencePage_SortDirection_Up, String.valueOf(SWT.UP) },
				{ Messages.WorkbenchPreferencePage_SortDirection_Down, String.valueOf(SWT.DOWN) } };
		ComboFieldEditor cfeSortDirection = new ComboFieldEditor(SORT_DIRECTION,
				Messages.WorkbenchPreferencePage_SortDirection, entrySortDirectionNamesAndValues,
				getFieldEditorParent());
		addField(cfeSortDirection);

		final Label separator = new Label(getFieldEditorParent(), SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(separator);

		addField(new CheckedStringFieldEditor(SFTP_USERNAME, //
				Messages.WorkbenchPreferencePage_SftpUsername, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_SftpUsernameMustntBeEmpty));
		final CheckedStringFieldEditor sftpPassword = new CheckedStringFieldEditor(SFTP_PASSWORD,
				Messages.WorkbenchPreferencePage_SftpPassword, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_SftpPasswordMustntBeEmpty);
		sftpPassword.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(sftpPassword);
		addField(new CheckedStringFieldEditor(SFTP_HOST, //
				Messages.WorkbenchPreferencePage_SftpHost, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_SftpHostMustntBeEmpty));
		addField(new CheckedStringFieldEditor(SFTP_MERGEFOLDER, //
				Messages.WorkbenchPreferencePage_SftpMergeFolder, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_SftpMergeFolderMustntBeEmpty));

	}

	/**
	 * @return a new field editor for the working folder
	 */
	private FieldEditor createWorkingFolderFieldEditor() {
		return new DirectoryFieldEditor(WORKING_FOLDER, Messages.WorkbenchPreferencePage_WorkingFolder,
				getFieldEditorParent()) {

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean checkState() {
				String errorMessage = validateWorkingFolder(getTextControl().getText());

				if (errorMessage == null) {
					clearErrorMessage();
					return true;
				} else {
					showErrorMessage(errorMessage);
					return false;
				}
			}

			private String validateWorkingFolder(String value) {
				String errorMessage = null;
				File fWorkingFolder = new File(value);
				if (!fWorkingFolder.exists()) {
					errorMessage = Messages.WorkbenchPreferencePage_Validate_WorkingFolderDoesntExist;
				} else if (!fWorkingFolder.isDirectory()) {
					errorMessage = Messages.WorkbenchPreferencePage_Validate_WorkingFolderIsNotADirectory;
				}
				return errorMessage;
			}
		};
	}

	/**
	 * @return a new field editor for the user name
	 */
	private FieldEditor createUserFieldEditor() {
		return new CheckedStringFieldEditor(USER_ID, //
				Messages.WorkbenchPreferencePage_UserId, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_InvalidUserId) {

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean check() {
				return StringUtils.isNotEmpty(getTextControl().getText());
			}

		};
	}

	/**
	 * @return a new field editor for the refresh interval
	 */
	private FieldEditor createRefreshIntervalFieldEditor() {
		return new CheckedStringFieldEditor(REFRESH_INTERVAL, //
				Messages.WorkbenchPreferencePage_RefreshInterval, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_InvalidUserId) {

			/**
			 * Checks the given text is a number and its interval is between 5 seconds and
			 * 30 minutes.
			 * 
			 * {@inheritDoc}
			 */
			@Override
			protected boolean check() {
				final int number;
				try {
					number = Integer.parseInt(getTextControl().getText());
				} catch (NumberFormatException e) {
					setErrorMessage(Messages.WorkbenchPreferencePage_Validate_RefreshInvervalMustBeANumber);
					return false;
				}

				long refreshInterval = TimeUnit.MILLISECONDS.convert(number, TimeUnit.SECONDS);
				long min = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
				long max = TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES);
				if (refreshInterval < min) {
					setErrorMessage(Messages.WorkbenchPreferencePage_Validate_RefreshIntervalMustBeAtLeast5s);
					return false;
				} else if (refreshInterval > max) {
					setErrorMessage(Messages.WorkbenchPreferencePage_Validate_RefreshIntervalMustntBeOver1800s);
					return false;
				} else {
					return true;
				}
			}

		};
	}

	/**
	 * @return a new field editor for the window location
	 */
	private FieldEditor createWindowLocationFieldEditor() {
		return new CheckedStringFieldEditor(WINDOW_LOCATION, //
				Messages.WorkbenchPreferencePage_WindowLocation, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_WindowLocationSizeWrongFormat) {

			/**
			 * Checks the format defines coordinates, e.g. 120,160 or 200,350.
			 * 
			 * {@inheritDoc}
			 */
			@Override
			protected boolean check() {
				return getTextControl().getText().matches("^-?\\d+,-?\\d+$"); //$NON-NLS-1$
			}

		};
	}

	/**
	 * @return a new field editor for the window size
	 */
	private FieldEditor createWindowSizeFieldEditor() {
		return new CheckedStringFieldEditor(WINDOW_SIZE, //
				Messages.WorkbenchPreferencePage_WindowSize, //
				getFieldEditorParent(), //
				Messages.WorkbenchPreferencePage_Validate_WindowLocationSizeWrongFormat) {

			/**
			 * Checks the format defines coordinates, e.g. 120,160 or 200,350.
			 * 
			 * {@inheritDoc}
			 */
			@Override
			protected boolean check() {
				return getTextControl().getText().matches("^-?\\d+,-?\\d+$"); //$NON-NLS-1$
			}

		};
	}

}
