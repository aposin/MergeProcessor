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

package org.aposin.mergeprocessor.command;

import java.net.URL;
import java.util.logging.Logger;

import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Handler deleting the password cache.
 * 
 * @author Stefan Weiser
 *
 */
@SuppressWarnings("restriction")
public class HandlerDeletePasswordCache {

	private static final Logger LOGGER = Logger.getLogger(HandlerDeletePasswordCache.class.getName());

	@Execute
	public void execute(Shell shell) {
		LogUtil.entering(shell);

		URL location = InternalExchangeUtils.defaultStorageLocation();
		if (location == null) {
			LOGGER.info("location is null."); //$NON-NLS-1$
			LogUtil.exiting();
			return;
		}

		MessageBox messageBox = new MessageBox(shell, SWT.YES | SWT.NO);
		messageBox.setText(SecUIMessages.generalDialogTitle);
		messageBox.setMessage(SecUIMessages.confirmDeleteMsg);
		if (messageBox.open() != SWT.YES) {
			LogUtil.exiting();
			return;
		}

		// clear the data structure itself in case somebody holds on to it
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		defaultStorage.clear();
		defaultStorage.removeNode();

		// clear it from the list of open storages, delete the file
		InternalExchangeUtils.defaultStorageDelete();

		// suggest restart in case somebody holds on to the deleted storage
		MessageBox postDeletionBox = new MessageBox(shell, SWT.YES | SWT.NO);
		postDeletionBox.setText(SecUIMessages.generalDialogTitle);
		postDeletionBox.setMessage(SecUIMessages.postDeleteMsg);
		int result = postDeletionBox.open();
		if (result == SWT.YES) {
			PlatformUI.getWorkbench().restart();
		}
		LogUtil.exiting();
	}

}