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
package org.aposin.mergeprocessor.view.mergeunit;

import org.aposin.mergeprocessor.view.Messages;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * View for the {@link MergeScriptDialog}.
 * 
 * @author Stefan Weiser
 *
 */
public class MergeScriptView extends Composite {

	private final Label labelNeededFiles;
	private final Text textNeededFiles;
	private final Text textDate;
	private final Text textRevisionRange;
	private final Button buttonShowChanges;
	private final Text textMergeScriptPath;
	private final Text textSourceBranch;
	private final Text textTargetBranch;// TODO
	private final Text textStatus;
	private final StyledText textContent;
	private final Button buttonClose;
	private TabItem tabRenaming;
	private RenamingView renamingView;

	/**
	 * @param parent a widget which will be the parent of the new instance (cannot be null)
	 * @param style the style of widget to construct
	 */
	public MergeScriptView(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		new Label(this, SWT.NONE).setText(Messages.MergeScriptDialog_Path);

		textMergeScriptPath = new Text(this, SWT.BORDER);
		textMergeScriptPath.setEditable(false);
		textMergeScriptPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		new Label(this, SWT.NONE).setText(Messages.MergeScriptDialog_Status);

		textStatus = new Text(this, SWT.BORDER);
		textStatus.setEditable(false);
		textStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		new Label(this, SWT.NONE).setText(Messages.MergeScriptDialog_Date);

		textDate = new Text(this, SWT.BORDER);
		textDate.setEditable(false);
		textDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		new Label(this, SWT.NONE).setText(Messages.MergeScriptDialog_RevisionRange);

		textRevisionRange = new Text(this, SWT.BORDER);
		textRevisionRange.setEditable(false);
		textRevisionRange.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		new Label(this, SWT.NONE).setText(Messages.MergeScriptDialog_SourceBranch);

		textSourceBranch = new Text(this, SWT.BORDER);
		textSourceBranch.setEditable(false);
		textSourceBranch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label label = new Label(this, SWT.NONE);
		label.setText(Messages.MergeScriptDialog_TargetBranch);

		textTargetBranch = new Text(this, SWT.NONE); // TODO Change To Combo back if target branch change works again
		textTargetBranch.setEditable(false);
		textTargetBranch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		labelNeededFiles = new Label(this, SWT.NONE);
		labelNeededFiles.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		labelNeededFiles.setText(Messages.MergeScriptDialog_NeededFiles);

		textNeededFiles = new Text(this, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		textNeededFiles.setEditable(false);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).hint(SWT.DEFAULT, 50)
				.applyTo(textNeededFiles);

		buttonShowChanges = new Button(this, SWT.NONE);
		buttonShowChanges.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		buttonShowChanges.setText(Messages.MergeScriptDialog_ShowChanges);

		final TabFolder tabFolder = new TabFolder(this, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		final TabItem tabContent = new TabItem(tabFolder, SWT.NONE);
		tabContent.setText(Messages.MergeScriptView_tbtmNewItem_text);

		textContent = new StyledText(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tabContent.setControl(textContent);
		textContent.setEditable(false);

		tabRenaming = new TabItem(tabFolder, SWT.NONE);
		tabRenaming.setText(Messages.MergeScriptView_tbtmNewItem_1_text);

		renamingView = new RenamingView(tabFolder, SWT.NONE);
		tabRenaming.setControl(renamingView);

		buttonClose = new Button(this, SWT.NONE);
		buttonClose.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		buttonClose.setText(Messages.MergeScriptDialog_Close);
	}

	/**
	 * @return the textNeededFiles
	 */
	public Text getTextNeededFiles() {
		return textNeededFiles;
	}

	/**
	 * @return the textDate
	 */
	public Text getTextDate() {
		return textDate;
	}

	/**
	 * @return the textRevisionRange
	 */
	public Text getTextRevisionRange() {
		return textRevisionRange;
	}

	/**
	 * @return the buttonShowChanges
	 */
	public Button getButtonShowChanges() {
		return buttonShowChanges;
	}

	/**
	 * @return the textMergeScriptPath
	 */
	public Text getTextMergeScriptPath() {
		return textMergeScriptPath;
	}

	/**
	 * @return the textSourceBranch
	 */
	public Text getTextSourceBranch() {
		return textSourceBranch;
	}

	/**
	 * @return the textStatus
	 */
	public Text getTextStatus() {
		return textStatus;
	}

	/**
	 * @return the textContent
	 */
	public StyledText getTextContent() {
		return textContent;
	}

	/**
	 * @return the textTargetBranch
	 */
	public Text getTextTargetBranch() {// TODO
		return textTargetBranch;
	}

	/**
	 * @return the buttonClose
	 */
	public Button getButtonClose() {
		return buttonClose;
	}

	/**
	 * @return the tabRenaming
	 */
	public TabItem getTabRenaming() {
		return tabRenaming;
	}

	/**
	 * @return the renamingView
	 */
	public RenamingView getRenamingView() {
		return renamingView;
	}

	/**
	 * In- or excludes the text area for needed files and its label.
	 * 
	 * @param exlude {@code true} if the text field for needed files should be exluded
	 */
	public void excludeTextNeededFiles(final boolean exlude) {
		((GridData) labelNeededFiles.getLayoutData()).exclude = exlude;
		((GridData) textNeededFiles.getLayoutData()).exclude = exlude;
	}
}
