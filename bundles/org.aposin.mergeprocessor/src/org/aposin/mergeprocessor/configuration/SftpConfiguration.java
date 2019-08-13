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

import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Implementation for {@link ISftpConfiguration} for the Merge Processor.
 * 
 * @author Stefan Weiser
 *
 */
final class SftpConfiguration implements ISftpConfiguration {

	/** Name of the to do subfolder in the sftpMergeFolder on the server. */
	private static final String SFTP_SUBFOLDER_MERGE_TODO = "todo/"; //$NON-NLS-1$
	/** Name of the done subfolder in the sftpMergeFolder on the server. */
	private static final String SFTP_SUBFOLDER_MERGE_DONE = "done/"; //$NON-NLS-1$
	/** Name of the ignored subfolder in the sftpMergeFolder on the server. */
	private static final String SFTP_SUBFOLDER_MERGE_IGNORED = "ignored/"; //$NON-NLS-1$
	/** Name of the canceled subfolder in the sftpMergeFolder on the server. */
	private static final String SFTP_SUBFOLDER_MERGE_CANCELED = "canceled/"; //$NON-NLS-1$
	/** Name of the manual subfolder in the sftpMergeFolder on the sever. */
	private static final String SFTP_SUBFOLDER_MERGE_MANUAL = "manual/"; //$NON-NLS-1$

	private final IPreferenceStore preferenceStore;

	SftpConfiguration(IPreferenceStore preferenceStore) {
		this.preferenceStore = preferenceStore;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTodoFolder() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_MERGEFOLDER)
				+ preferenceStore.getString(WorkbenchPreferencePage.USER_ID).toLowerCase() + '/' // $NON-NLS-1$
				+ SFTP_SUBFOLDER_MERGE_TODO);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDoneFolder() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_MERGEFOLDER)
				+ preferenceStore.getString(WorkbenchPreferencePage.USER_ID).toLowerCase() + '/' // $NON-NLS-1$
				+ SFTP_SUBFOLDER_MERGE_DONE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIgnoredFolder() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_MERGEFOLDER)
				+ preferenceStore.getString(WorkbenchPreferencePage.USER_ID).toLowerCase() + '/' // $NON-NLS-1$
				+ SFTP_SUBFOLDER_MERGE_IGNORED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCanceledFolder() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_MERGEFOLDER)
				+ preferenceStore.getString(WorkbenchPreferencePage.USER_ID).toLowerCase() + '/' // $NON-NLS-1$
				+ SFTP_SUBFOLDER_MERGE_CANCELED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getManualFolder() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_MERGEFOLDER)
				+ preferenceStore.getString(WorkbenchPreferencePage.USER_ID).toLowerCase() + '/' // $NON-NLS-1$
				+ SFTP_SUBFOLDER_MERGE_MANUAL);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_HOST));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUser() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_USERNAME));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPassword() {
		return LogUtil.exiting(preferenceStore.getString(WorkbenchPreferencePage.SFTP_PASSWORD));
	}
}