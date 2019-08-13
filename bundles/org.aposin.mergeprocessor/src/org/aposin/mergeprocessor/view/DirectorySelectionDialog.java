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
package org.aposin.mergeprocessor.view;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for selecting a directory.
 * 
 * @author Stefan Weiser
 *
 */
public class DirectorySelectionDialog extends TitleAreaDialog {

	private String title;
	private String message;

	private String directoryDialogTitle;
	private String directoryDialogMessage;
	private String directoryDialogFilterPath;

	private String mergeFrom;
	private String mergeTo;

	/** This variable is used to return the selected directory path,  when the dialog is already closed */
	private Path selectedPath;
	private Function<Path, Boolean> pathValidationFunction = path -> true;
	private Text textRepository;

	/**
	 * @param parentShell  the parent shell
	 * @param targetBranch the target branch
	 */
	public DirectorySelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void setTitle(String newTitle) {
		if (textRepository == null) {
			title = newTitle;
		} else {
			super.setTitle(newTitle);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMessage(String newMessage) {
		if (textRepository == null) {
			message = newMessage;
		} else {
			super.setMessage(newMessage);
		}
	}

	/**
	 * @param directoryDialogTitle the directoryDialogTitle to set
	 */
	public void setDirectoryDialogTitle(String directoryDialogTitle) {
		this.directoryDialogTitle = directoryDialogTitle;
	}

	/**
	 * @param directoryDialogMessage the directoryDialogMessage to set
	 */
	public void setDirectoryDialogMessage(String directoryDialogMessage) {
		this.directoryDialogMessage = directoryDialogMessage;
	}

	/**
	 * @param directoryDialogFilterPath the directoryDialogFilterPath to set
	 */
	public void setDirectoryDialogFilterPath(String directoryDialogFilterPath) {
		this.directoryDialogFilterPath = directoryDialogFilterPath;
	}

	/**
	 * @param pathValidationFunction the pathValidationFunction to set
	 */
	public void setPathValidationFunction(Function<Path, Boolean> pathValidationFunction) {
		if (pathValidationFunction == null) {
			this.pathValidationFunction = path -> true;
		} else {
			this.pathValidationFunction = pathValidationFunction;
		}
	}

	/**
	 * @param selectedPath the selected repository path
	 */
	public void setSelectedPath(final Path selectedPath) {
		if (selectedPath != null) {
			this.selectedPath = selectedPath;
			if (textRepository != null) {
				textRepository.setText(selectedPath.toString());
			}
		}
	}

	/**
	 * @return the selected repository path
	 */
	public Path getSelectedPath() {
		return selectedPath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void create() {
		super.create();
		if (title != null) {
			super.setTitle(title);
			title = null;
		}
		if (message != null) {
			super.setMessage(message);
			message = null;
		}
		getShell().layout(true, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite dialogComposite = (Composite) super.createDialogArea(parent);

		final Composite composite = new Composite(dialogComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		composite.setLayout(new GridLayout(2, false));

		if (StringUtils.isNotEmpty(mergeFrom) && StringUtils.isNotEmpty(mergeTo)) {
			final Composite compMergeFromTo = new Composite(composite, SWT.NONE);
			compMergeFromTo.setLayout(new RowLayout(SWT.HORIZONTAL));
			compMergeFromTo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

			new Label(compMergeFromTo, SWT.NONE).setText(Messages.DirectorySelectionDialog_mergeFrom);

			final Label labelFrom = new Label(compMergeFromTo, SWT.NONE);
			labelFrom.setFont(JFaceResources.getFontRegistry().getBold(""));
			labelFrom.setText(mergeFrom);

			new Label(compMergeFromTo, SWT.NONE).setText(Messages.DirectorySelectionDialog_mergeTo);

			final Label labelTo = new Label(compMergeFromTo, SWT.NONE);
			labelTo.setFont(JFaceResources.getFontRegistry().getBold(""));
			labelTo.setText(mergeTo);
		}

		textRepository = new Text(composite, SWT.BORDER);
		textRepository.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (selectedPath != null) {
			textRepository.setText(selectedPath.toString());
		}
		textRepository.addModifyListener(e -> setErrorMessage(null));

		final Button buttonBrowseRepository = new Button(composite, SWT.NONE);
		buttonBrowseRepository.setText(Messages.DirectorySelectionDialog_browse);
		buttonBrowseRepository.addListener(SWT.Selection, e -> openDirectoryDialog());

		return dialogComposite;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void okPressed() {
		if (!fileExists(textRepository.getText())) {
			setErrorMessage("The selected path does not exist!");
			return;
		}
		if (!pathValidationFunction.apply(Paths.get(textRepository.getText()))) {
			setErrorMessage("The selected path is not valid!");
			return;
		}

		selectedPath = Paths.get(textRepository.getText());
		super.okPressed();
	}

	/**
	 * Open the directory dialog and set the selected directory into {@link #textRepository}.
	 */
	private void openDirectoryDialog() {
		final DirectoryDialog dialog = new DirectoryDialog(textRepository.getShell());
		dialog.setText(directoryDialogTitle == null ? "Select directory" : directoryDialogTitle);
		final String currentText = textRepository.getText();
		if (fileExists(currentText)) {
			dialog.setFilterPath(currentText);
		} else if (fileExists(directoryDialogFilterPath)) {
			dialog.setFilterPath(directoryDialogFilterPath);
		}
		dialog.setMessage(directoryDialogMessage == null ? "Select a directory." : directoryDialogMessage);
		final String directory = dialog.open();
		if (directory != null) {
			textRepository.setText(directory);
			selectedPath = Paths.get(textRepository.getText());
		}
	}

	/**
	 * Check if the file for the given path exists.
	 * 
	 * @param text the path
	 * @return {@code true} if the file exists
	 */
	private static boolean fileExists(final String text) {
		try {
			final Path path = Paths.get(text);
			return path.toFile().exists();
		} catch (InvalidPathException e) {
			return false;
		}
	}

	/**
	 * @param from where to merge from
	 */
	public void setMergeFrom(final String from) {
		this.mergeFrom = from;
	}

	/**
	 * @param to where to merge to
	 */
	public void setMergeTo(final String to) {
		this.mergeTo = to;
	}
}
