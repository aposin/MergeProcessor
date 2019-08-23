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
package org.aposin.mergeprocessor.configuration;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.utils.FileUtils;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This preference page provides the possibility to configure the connection to
 * the database for merging renamed artifacts.
 * 
 * @author Stefan Weiser
 *
 */
public class RenamingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String RENAME_DATABASE_URL = "RENAME_DATABASE_URL"; //$NON-NLS-1$
	public static final String RENAME_DATABASE_USER = "RENAME_DATABASE_USER"; //$NON-NLS-1$
	public static final String RENAME_DATABASE_PASSWORD = "RENAME_DATABASE_PASSWORD"; //$NON-NLS-1$

	private Text textUrl;
	private Text textUser;
	private Text textPassword;
	private Label labelRenameDatabaseCopy;
	private Link linkRenameDatabaseCopy;
	private IConfiguration configuration;

	private final IPropertyChangeListener propertyChangeListener = this::internalPropertyChange;

	public RenamingPreferencePage() {
		super(GRID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(preferenceStore);
		setDescription(Messages.RenamingPreferencePage_description);
		preferenceStore.addPropertyChangeListener(propertyChangeListener);
		configuration = ((org.eclipse.e4.ui.workbench.IWorkbench) workbench).getApplication().getContext()
				.get(IConfiguration.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dispose() {
		super.dispose();
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		preferenceStore.removePropertyChangeListener(propertyChangeListener);
	}

	/**
	 * Fired when a property change on the preference store is executed.
	 * 
	 * @param event the event
	 */
	private void internalPropertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(RENAME_DATABASE_URL)) {
			setupNewDatabaseUrl(event.getNewValue().toString());
			showRenameCopyIfRequired();
		}
	}

	/**
	 * Shows the rename copy link, if a copy exists.
	 */
	private void showRenameCopyIfRequired() {
		getControl().getDisplay().syncExec(() -> {
			final Path pathToDelete = Paths.get("C:\\dev\\mp\\rename2.mv.db");
			if (pathToDelete.toFile().exists()) {
				labelRenameDatabaseCopy.setVisible(true);
				linkRenameDatabaseCopy.setVisible(true);
			} else {
				labelRenameDatabaseCopy.setVisible(false);
				linkRenameDatabaseCopy.setVisible(false);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		final StringFieldEditor urlField = new StringFieldEditor(RENAME_DATABASE_URL,
				Messages.RenamingPreferencePage_databaseUrl, getFieldEditorParent());
		textUrl = urlField.getTextControl(getFieldEditorParent());
		addField(urlField);

		final StringFieldEditor userField = new StringFieldEditor(RENAME_DATABASE_USER,
				Messages.RenamingPreferencePage_databaseUserId, getFieldEditorParent());
		addField(userField);
		textUser = userField.getTextControl(getFieldEditorParent());

		final StringFieldEditor passwordField = new StringFieldEditor(RENAME_DATABASE_PASSWORD,
				Messages.RenamingPreferencePage_databasePassword, getFieldEditorParent());
		passwordField.getTextControl(getFieldEditorParent()).setEchoChar('*');
		textPassword = passwordField.getTextControl(getFieldEditorParent());
		addField(passwordField);

		final Button buttonCheckDbConnection = new Button(getFieldEditorParent(), SWT.NONE);
		GridDataFactory.swtDefaults().span(2, 1).align(SWT.RIGHT, SWT.CENTER).applyTo(buttonCheckDbConnection);
		buttonCheckDbConnection.setText(Messages.RenamingPreferencePage_buttonValidateConnection);
		buttonCheckDbConnection.addListener(SWT.Selection, e -> checkDbConnection());

		labelRenameDatabaseCopy = new Label(getFieldEditorParent(), SWT.NONE);
		labelRenameDatabaseCopy.setText("H2 Database is copied to");

		linkRenameDatabaseCopy = new Link(getFieldEditorParent(), SWT.NONE);
		linkRenameDatabaseCopy.setText("<a>C:\\dev\\mp\\rename2.mv.db</a>");
		linkRenameDatabaseCopy.addListener(SWT.Selection, e -> openDirectoryForLocalH2DatabaseCopy());

		showRenameCopyIfRequired();
	}

	/**
	 * Checks the database connection. If the connection fails an error message is
	 * written for the preference page. Otherwise a message dialog appears to inform
	 * that the connection to the database was successful.
	 */
	private void checkDbConnection() {
		final String url = textUrl.getText();
		final String user = textUser.getText();
		final String password = textPassword.getText();
		try (final Connection connection = DriverManager.getConnection(url, user, password)) {
			if (connection.isValid(3000)) {
				setErrorMessage(null);
				setValid(true);
				new MessageDialog(textUrl.getShell(), Messages.RenamingPreferencePage_messageDialogTitleValidConnection,
						null, Messages.RenamingPreferencePage_messageDialogTextValidConnection,
						MessageDialog.INFORMATION, 0, IDialogConstants.OK_LABEL).open();
			} else {
				setErrorMessage(Messages.RenamingPreferencePage_errorMessageConnectionTimeout);
				setValid(false);
			}
		} catch (SQLException e1) {
			setErrorMessage(e1.getMessage());
			setValid(false);
		}
	}

	/**
	 * Open directory of the local H2 database.
	 */
	private void openDirectoryForLocalH2DatabaseCopy() {
		try {
			Desktop.getDesktop().open(Paths.get("C:\\dev\\mp").toFile());
		} catch (IOException e1) {
			LogUtil.throwing(e1);
		}
	}

	/**
	 * Setup when a new database URL was established.
	 * 
	 * @param newUrl the new database url
	 */
	private void setupNewDatabaseUrl(final String newUrl) {
		// Delete potential old local db
		final Path localH2RenameDatabase = configuration.getLocalH2RenameDatabase();
		try {
			Files.deleteIfExists(localH2RenameDatabase);
		} catch (IOException e) {
			Logger.getLogger(RenamingPreferencePage.class.getName()).log(Level.SEVERE,
					String.format("Could not delete local database '%s'", localH2RenameDatabase.toString()), e);
		}
		if (newUrl.startsWith("jdbc:h2:")) {
			final int urlEnd = newUrl.indexOf(';');
			final String stringPath;
			if (urlEnd == -1) {
				stringPath = newUrl.substring("jdbc:h2:".length());
			} else {
				stringPath = newUrl.substring("jdbc:h2:".length(), urlEnd);
			}
			final Path newPath = Paths.get(stringPath + ".mv.db");

			if (newPath.toFile().exists()) {
				final ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
				try {
					dialog.run(true, true, new IRunnableWithProgress() {

						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException, InterruptedException {
							final boolean success = FileUtils.copyFiles(newPath, localH2RenameDatabase, monitor);
							if (!success) {
								getPreferenceStore().setValue(RENAME_DATABASE_URL, "");
							} else {
								if (!localH2RenameDatabase.toFile().exists()) {
									Logger.getLogger(RenamingPreferencePage.class.getName()).severe(String.format(
											"H2 database was not copied successfully to '%s'", localH2RenameDatabase));
								}
							}
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					Logger.getLogger(RenamingPreferencePage.class.getName()).log(Level.SEVERE,
							String.format("Could not delete local database '%s'", localH2RenameDatabase.toString()), e);
					for (int i = 0; i < 10; i++) {
						try {
							Files.deleteIfExists(localH2RenameDatabase);
							break; // Success
						} catch (IOException e1) {
							LogUtil.throwing(e1);
							try {
								Thread.sleep(250);
							} catch (InterruptedException e2) {
								LogUtil.throwing(e2);
							}
						}
					}
				}
			} else {
				setErrorMessage(Messages.RenamingPreferencePage_errorMessageConnectionTimeout);
			}

		}
		showRenameCopyIfRequired();
	}

}
