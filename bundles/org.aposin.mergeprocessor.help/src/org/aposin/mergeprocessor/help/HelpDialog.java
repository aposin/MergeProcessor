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
package org.aposin.mergeprocessor.help;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog showing the help information for the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public class HelpDialog extends Dialog {

	private String url;

	/**
	 * @param parentShell the parent shell, or <code>null</code> to create a top-level shell
	 */
	public HelpDialog(Shell parentShell) {
		super(parentShell);
		setBlockOnOpen(false);
		setShellStyle(SWT.DIALOG_TRIM | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
	}

	/**
	 * Sets the URL to show in the browser part.
	 * 
	 * @param url the URL to show.
	 */
	public void setUrl(final String url) {
		this.url = url.replace('\\', '/');
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite parent2 = (Composite) super.createDialogArea(parent);
		parent2.setLayout(new FillLayout());
		final Browser browser = new Browser(parent2, SWT.NONE);
		browser.setUrl(url);
		return parent2;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(850, 770);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

}
