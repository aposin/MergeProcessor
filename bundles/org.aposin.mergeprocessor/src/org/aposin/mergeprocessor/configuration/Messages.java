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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.aposin.mergeprocessor.configuration.messages"; //$NON-NLS-1$
	public static String EclipseWorkspaceStartPeferencePage_applicationPath;
	public static String EclipseWorkspaceStartPeferencePage_description;
	public static String EclipseWorkspaceStartPeferencePage_parameters;
	public static String GitRepositoriesPreferencePage_description;
	public static String GitRepositoriesPreferencePage_title;
	public static String RenamingPreferencePage_buttonValidateConnection;
	public static String RenamingPreferencePage_databasePassword;
	public static String RenamingPreferencePage_databaseUrl;
	public static String RenamingPreferencePage_databaseUserId;
	public static String RenamingPreferencePage_description;
	public static String RenamingPreferencePage_errorMessageConnectionTimeout;
	public static String RenamingPreferencePage_messageDialogTextValidConnection;
	public static String RenamingPreferencePage_messageDialogTitleValidConnection;
	public static String WorkbenchPreferencePage_Description;
	public static String WorkbenchPreferencePage_SVN_Description;
	public static String WorkbenchPreferencePage_GIT_Description;
	public static String WorkbenchPreferencePage_LogLevel;
	public static String WorkbenchPreferencePage_RefreshInterval;
	public static String WorkbenchPreferencePage_SftpHost;
	public static String WorkbenchPreferencePage_SftpMergeFolder;
	public static String WorkbenchPreferencePage_SftpPassword;
	public static String WorkbenchPreferencePage_SftpUsername;
	public static String WorkbenchPreferencePage_SortDirection;
	public static String WorkbenchPreferencePage_SortDirection_Down;
	public static String WorkbenchPreferencePage_SortDirection_Up;
	public static String WorkbenchPreferencePage_SortedColumn;
	public static String WorkbenchPreferencePage_UserId;
	public static String WorkbenchPreferencePage_Validate_InvalidUserId;
	public static String WorkbenchPreferencePage_Validate_RefreshIntervalMustBeAtLeast5s;
	public static String WorkbenchPreferencePage_Validate_RefreshIntervalMustntBeOver1800s;
	public static String WorkbenchPreferencePage_Validate_RefreshInvervalMustBeANumber;
	public static String WorkbenchPreferencePage_Validate_SftpHostMustntBeEmpty;
	public static String WorkbenchPreferencePage_Validate_SftpMergeFolderMustntBeEmpty;
	public static String WorkbenchPreferencePage_Validate_SftpPasswordMustntBeEmpty;
	public static String WorkbenchPreferencePage_Validate_SftpUsernameMustntBeEmpty;
	public static String WorkbenchPreferencePage_Validate_WindowLocationSizeWrongFormat;
	public static String WorkbenchPreferencePage_Validate_WorkingFolderDoesntExist;
	public static String WorkbenchPreferencePage_Validate_WorkingFolderIsNotADirectory;
	public static String WorkbenchPreferencePage_WindowLocation;
	public static String WorkbenchPreferencePage_WindowSize;
	public static String WorkbenchPreferencePage_WorkingFolder;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
