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
package org.aposin.mergeprocessor.model;

import javax.inject.Inject;

import org.aposin.mergeprocessor.configuration.ConfigurationException;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.view.Messages;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This credential provider asks the user in a shell for the user name and the password.
 * 
 * @author Stefan Weiser
 *
 */
public class InstantUserAuthentication implements ICredentialProvider {

	@Inject
	public UISynchronize uISynchronize;

	@Inject
	public Display display;

	@Inject
	public IConfiguration configuration;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] authenticate() throws AuthenticationException {
		final Exception[] throwable = new Exception[1];
		final String[] result = new String[2];
		uISynchronize.syncExec(() -> {
			try {
				final String username = configuration.getSvnUsername();
				final String password = configuration.getSvnPassword();
				final Shell activeShell = display.getActiveShell();
				final SvnCredentialsDialog dialog = new SvnCredentialsDialog(activeShell);
				dialog.setUsername(username);
				dialog.setPassword(password);
				if (dialog.open() == Window.OK) {
					result[0] = dialog.getUsername();
					result[1] = dialog.getPassword();
				} else {
					throwable[0] = new AuthenticationException("User canceled authentication.");
				}
			} catch (ConfigurationException e) {
				throwable[0] = LogUtil.throwing(e);
			}
		});
		if (throwable[0] != null) {
			throw new AuthenticationException(throwable[0]);
		}
		return result;
	}

	/**
	 * Dialog for SVN user authentication.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class SvnCredentialsDialog extends TitleAreaDialog {

		private String username;
		private String password;

		private Text textUsername;
		private Text textPassword;

		private SvnCredentialsDialog(final Shell parent) {
			super(parent);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Control createDialogArea(Composite parent) {
			final Composite dialogArea = (Composite) super.createDialogArea(parent);
			setTitle(Messages.DialogSvnCredentials_Description);

			Composite composite = new Composite(dialogArea, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

			new Label(composite, SWT.NONE).setText(Messages.DialogSvnCredentials_Username);

			textUsername = new Text(composite, SWT.BORDER);
			textUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			textUsername.setText(username);
			textUsername.addListener(SWT.FocusOut, event -> username = textUsername.getText());

			new Label(composite, SWT.NONE).setText(Messages.DialogSvnCredentials_Password);

			textPassword = new Text(composite, SWT.BORDER | SWT.PASSWORD);
			textPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			textPassword.setText(password);
			textPassword.addListener(SWT.FocusOut, event -> password = textPassword.getText());
			return dialogArea;
		}

		/**
		 * @return the username
		 */
		private String getUsername() {
			return username;
		}

		/**
		 * @param username the username to set
		 */
		private void setUsername(String username) {
			this.username = username;
			if (textUsername != null && !textUsername.isDisposed()) {
				textUsername.setText(username);
			}
		}

		/**
		 * @return the password
		 */
		private String getPassword() {
			return password;
		}

		/**
		 * @param password the password to set
		 */
		private void setPassword(String password) {
			this.password = password;
			if (textPassword != null && !textPassword.isDisposed()) {
				textPassword.setText(password);
			}
		}

	}

}
