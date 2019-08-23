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

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.RowDataFactory;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This dialog represents gets shown when a merge task is done into a workspace.
 * 
 * @author Stefan Weiser
 *
 */
public class WorkspaceMergeDialog extends ProgressMonitorDialog {

	private static final int COMMIT_ID = 43;
	private static final int OPEN_ECLIPSE_ID = 44;
	private static final int OPEN_WORKING_COPY = 45;
	private static final int OPEN_TORTOISE_SVN = 46;
	private static final String CONSOLE_FONT = "CONSOLE_FONT";

	private Text commandLineText;
	private Text commitMessageText;
	private ListViewer warningsTableViewer;

	private Button commitButton;
	private Button openButton;
	private Button openWorkingCopyButton;
	private Button openTortoiseSvnButton;
	private Button closeButton;
	private Button buttonConfirmCommit;

	private boolean isConfirmCommit;

	static {
		final FontRegistry fontRegistry = JFaceResources.getFontRegistry();
		final int height = fontRegistry.get("").getFontData()[0].getHeight();
		fontRegistry.put(CONSOLE_FONT, new FontData[] { new FontData("Lucida Console", height, SWT.NONE) });
	}

	/**
	 * @param parent the parent shell
	 */
	public WorkspaceMergeDialog(final Shell parent) {
		super(parent);
		create();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite createDialogArea = (Composite) super.createDialogArea(parent);

		createCheckboxButtons(parent);
		commandLineText = createEstablishedCommandsText(parent);
		commitMessageText = createCommitMessageText(parent);
		warningsTableViewer = createWarningsTableViewer(parent);

		return createDialogArea;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		commitButton = createButton(parent, COMMIT_ID, "Commit", false);
		commitButton.setVisible(false);
		((GridData) commitButton.getLayoutData()).exclude = true;

		openButton = createButton(parent, OPEN_ECLIPSE_ID, "Open Eclipse", false);
		openButton.setVisible(false);
		((GridData) openButton.getLayoutData()).exclude = true;

		openWorkingCopyButton = createButton(parent, OPEN_WORKING_COPY, "Open Working Copy", false);
		openWorkingCopyButton.setVisible(false);
		((GridData) openWorkingCopyButton.getLayoutData()).exclude = true;

		openTortoiseSvnButton = createButton(parent, OPEN_TORTOISE_SVN, "Open Tortoise SVN", false);
		openTortoiseSvnButton.setVisible(false);
		((GridData) openTortoiseSvnButton.getLayoutData()).exclude = true;

		closeButton = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
		closeButton.setVisible(false);
		((GridData) closeButton.getLayoutData()).exclude = true;

		super.createButtonsForButtonBar(parent);

	}

	/**
	 * Closes the dialog.
	 */
	public void closeDialog() {
		super.finishedRun();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finishedRun() {
		clearCursors();

		if (isConfirmCommit) {
			((RowData) buttonConfirmCommit.getLayoutData()).exclude = false;
			buttonConfirmCommit.setVisible(true);
		}

		if (commitButton.getListeners(SWT.Selection).length > 1) {
			commitButton.setVisible(true);
			((GridData) commitButton.getLayoutData()).exclude = false;
			if (isConfirmCommit) {
				commitButton.setEnabled(false);
			}
		}

		if (openButton.getListeners(SWT.Selection).length > 1) {
			openButton.setVisible(true);
			((GridData) openButton.getLayoutData()).exclude = false;
		}

		if (openWorkingCopyButton.getListeners(SWT.Selection).length > 1) {
			openWorkingCopyButton.setVisible(true);
			((GridData) openWorkingCopyButton.getLayoutData()).exclude = false;
		}

		if (openTortoiseSvnButton.getListeners(SWT.Selection).length > 1) {
			openTortoiseSvnButton.setVisible(true);
			((GridData) openTortoiseSvnButton.getLayoutData()).exclude = false;
		}

		closeButton.setVisible(true);
		((GridData) closeButton.getLayoutData()).exclude = false;

		final Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
		cancelButton.setVisible(false);
		((GridData) cancelButton.getLayoutData()).exclude = true;

		if (warningsTableViewer.getList().getItemCount() > 0) {
			((GridData) warningsTableViewer.getList().getParent().getLayoutData()).exclude = false;
			warningsTableViewer.getList().setVisible(true);
		}

		final Point size = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		getShell().setSize(size);
		getShell().layout(true, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Creates the checkbox buttons.
	 * 
	 * @param parent the composite where to create them
	 */
	private void createCheckboxButtons(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.VERTICAL));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(composite);

		final Button buttonShowCommandLineText = new Button(composite, SWT.CHECK);
		buttonShowCommandLineText.setText("Show Command Line Text");
		buttonShowCommandLineText.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event e) {
				final boolean selection = buttonShowCommandLineText.getSelection();
				commandLineText.getParent().setVisible(selection);
				((GridData) commandLineText.getParent().getLayoutData()).exclude = !selection;
				final Point size = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
				getShell().setSize(size);
				getShell().layout(true, true);
			}

		});

		buttonConfirmCommit = new Button(composite, SWT.CHECK);
		RowDataFactory.swtDefaults().exclude(true).applyTo(buttonConfirmCommit);
		buttonConfirmCommit.setText("Confirm 'Commit'. Repository contained changes before merge.");
		buttonConfirmCommit.setForeground(buttonConfirmCommit.getDisplay().getSystemColor(SWT.COLOR_RED));
		buttonConfirmCommit.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				commitButton.setEnabled(buttonConfirmCommit.getSelection());
			}
		});
	}

	/**
	 * Creates the text field showing the established commands.
	 * 
	 * @param parent the parent composite of the text field
	 * @return the text field
	 */
	private static Text createEstablishedCommandsText(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().span(2, 1).exclude(true).applyTo(composite);
		final Text text = new Text(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).hint(0 /* Do not layout dependent to the content */, 100)
				.span(2, 1).applyTo(text);
		text.setEditable(false);
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		text.setFont(JFaceResources.getFont(CONSOLE_FONT));
		return text;
	}

	/**
	 * Creates the text field showing the commit message.
	 * 
	 * @param parent the parent composite of the text field
	 * @return the text field
	 */
	private static Text createCommitMessageText(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(composite);
		new Label(composite, SWT.NONE).setText("Commit Message: ");
		final Text text = new Text(composite, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setEditable(false);
		return text;
	}

	/**
	 * Create the table viewer for showing warnings.
	 * 
	 * @param parent the parent composite of the table viewer
	 * @return the table viewer
	 */
	private static ListViewer createWarningsTableViewer(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).exclude(true).applyTo(composite);
		final ListViewer tableViewer = new ListViewer(composite);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new LabelProvider());
		return tableViewer;
	}

	/**
	 * The dialog showed show that an error exist.
	 */
	public void setStatusError() {
		imageLabel.setImage(getErrorImage());
	}

	/**
	 * @return the commitButton
	 */
	public Button getCommitButton() {
		return commitButton;
	}

	/**
	 * @return the openButton
	 */
	public Button getOpenButton() {
		return openButton;
	}

	/**
	 * @return the openWorkingCopyButton
	 */
	public Button getOpenWorkingCopyButton() {
		return openWorkingCopyButton;
	}

	/**
	 * @return the openTortoiseSvnButton
	 */
	public Button getOpenTortoiseSvnButton() {
		return openTortoiseSvnButton;
	}

	/**
	 * @return the closeButton
	 */
	public Button getCloseButton() {
		return closeButton;
	}

	/**
	 * @return the isConfirmCommit
	 */
	public boolean isConfirmCommit() {
		return isConfirmCommit;
	}

	/**
	 * @param isConfirmCommit the isConfirmCommit to set
	 */
	public void setConfirmCommit(boolean isConfirmCommit) {
		this.isConfirmCommit = isConfirmCommit;
	}

	/**
	 * @return the commandLineText
	 */
	public Text getCommandLineText() {
		return commandLineText;
	}

	/**
	 * @return the commitMessageText
	 */
	public Text getCommitMessageText() {
		return commitMessageText;
	}

	/**
	 * Sets the list of warnings to the viewer.
	 * 
	 * @param warnings the warnings to set
	 */
	public void setWarnings(List<String> warnings) {
		warningsTableViewer.setInput(warnings);
	}

}
