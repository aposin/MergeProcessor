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
package org.aposin.mergeprocessor.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 *
 */
public class MessageDialogScrollable extends MessageDialog {

	private String dialogScrollableText = null;

	/**
	 * Create a message dialog like {@link MessageDialog} but with an additional
	 * uneditable, scrollable text field. Note that the dialog will have no visual
	 * representation (no widgets) until it is told to open.
	 * <p>
	 * The labels of the buttons to appear in the button bar are supplied in this
	 * constructor as an array. The <code>open</code> method will return the index
	 * of the label in this array corresponding to the button that was pressed to
	 * close the dialog.
	 * </p>
	 * <p>
	 * <strong>Note:</strong> If the dialog was dismissed without pressing a button
	 * (ESC key, close box, etc.) then {@link SWT#DEFAULT} is returned. Note that
	 * the <code>open</code> method blocks.
	 * </p>
	 *
	 * @param parentShell          the parent shell
	 * @param dialogTitle          the dialog title, or <code>null</code> if none
	 * @param dialogTitleImage     the dialog title image, or <code>null</code> if
	 *                             none
	 * @param dialogMessage        the dialog message
	 * @param dialogScrollableText the dialog scrollable text
	 * @param dialogImageType      one of the following values:
	 *                             <ul>
	 *                             <li><code>MessageDialog.NONE</code> for a dialog
	 *                             with no image</li>
	 *                             <li><code>MessageDialog.ERROR</code> for a dialog
	 *                             with an error image</li>
	 *                             <li><code>MessageDialog.INFORMATION</code> for a
	 *                             dialog with an information image</li>
	 *                             <li><code>MessageDialog.QUESTION </code> for a
	 *                             dialog with a question image</li>
	 *                             <li><code>MessageDialog.WARNING</code> for a
	 *                             dialog with a warning image</li>
	 *                             </ul>
	 * @param dialogButtonLabels   an array of labels for the buttons in the button
	 *                             bar
	 * @param defaultIndex         the index in the button label array of the
	 *                             default button
	 */
	public MessageDialogScrollable(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
			String dialogScrollableText, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels,
				defaultIndex);

		this.dialogScrollableText = dialogScrollableText;
	}

	/**
	 * Creates the area with the scrollable text element.
	 */
	@Override
	protected Control createCustomArea(Composite parent) {
		Composite c = null;

		if (dialogScrollableText != null) {
			parent.setLayout(new FillLayout());
			c = new Composite(parent, SWT.NONE);
			c.setLayout(new FillLayout());

			Text text = new Text(c, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			text.setEditable(false);
			text.setText(dialogScrollableText);
		}

		return c;
	}

	/**
	 * @param kind
	 * @return
	 */
	private static String[] getButtonLabels(int kind) {
		String[] dialogButtonLabels;
		switch (kind) {
		case ERROR: // fall through to WARNING
		case INFORMATION: // fall through to WARNING
		case WARNING: {
			dialogButtonLabels = new String[] { IDialogConstants.OK_LABEL };
			break;
		}
		case CONFIRM: {
			dialogButtonLabels = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
			break;
		}
		case QUESTION: {
			dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
			break;
		}
		case QUESTION_WITH_CANCEL: {
			dialogButtonLabels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
					IDialogConstants.CANCEL_LABEL };
			break;
		}
		default: {
			throw new IllegalArgumentException("Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
		}
		}
		return dialogButtonLabels;
	}

	/**
	 * Convenience method to open a simple dialog as specified by the
	 * <code>kind</code> flag.
	 * 
	 * @param kind                 the kind of dialog to open, one of
	 *                             {@link #ERROR}, {@link #INFORMATION},
	 *                             {@link #QUESTION}, {@link #WARNING},
	 *                             {@link #CONFIRM}, or
	 *                             {@link #QUESTION_WITH_CANCEL}.
	 * @param parent               the parent shell of the dialog, or
	 *                             <code>null</code> if none
	 * @param title                the dialog's title, or <code>null</code> if none
	 * @param message              the message
	 * @param dialogScrollableText the scrollable text {@link SWT#NONE} for a
	 *                             default dialog, or {@link SWT#SHEET} for a dialog
	 *                             with sheet behavior
	 * @return <code>true</code> if the user presses the OK or Yes button,
	 *         <code>false</code> otherwise
	 * @since 3.5
	 */
	public static boolean open(int kind, Shell parent, String title, String message, String dialogScrollableText) {
		MessageDialogScrollable dialog = new MessageDialogScrollable(parent, title, null, message, dialogScrollableText,
				kind, getButtonLabels(kind), 0);
		return dialog.open() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Convenience method to open a simple confirm (OK/Cancel) dialog.
	 * 
	 * @param parent               the parent shell of the dialog, or
	 *                             <code>null</code> if none
	 * @param title                the dialog's title, or <code>null</code> if none
	 * @param message              the message
	 * @param dialogScrollableText the scrollable text
	 * @return <code>true</code> if the user presses the OK button,
	 *         <code>false</code> otherwise
	 */
	public static boolean openConfirm(Shell parent, String title, String message, String dialogScrollableText) {
		return open(CONFIRM, parent, title, message, dialogScrollableText);
	}

	/**
	 * Convenience method to open a standard error dialog.
	 * 
	 * @param parent               the parent shell of the dialog, or
	 *                             <code>null</code> if none
	 * @param title                the dialog's title, or <code>null</code> if none
	 * @param message              the message
	 * @param dialogScrollableText the scrollable text
	 */
	public static void openError(Shell parent, String title, String message, String dialogScrollableText) {
		open(ERROR, parent, title, message, dialogScrollableText);
	}

	/**
	 * Convenience method to open a standard information dialog.
	 * 
	 * @param parent               the parent shell of the dialog, or
	 *                             <code>null</code> if none
	 * @param title                the dialog's title, or <code>null</code> if none
	 * @param message              the message
	 * @param dialogScrollableText the scrollable text
	 */
	public static void openInformation(Shell parent, String title, String message, String dialogScrollableText) {
		open(INFORMATION, parent, title, message, dialogScrollableText);
	}

	/**
	 * Convenience method to open a simple Yes/No question dialog.
	 * 
	 * @param parent               the parent shell of the dialog, or
	 *                             <code>null</code> if none
	 * @param title                the dialog's title, or <code>null</code> if none
	 * @param message              the message
	 * @param dialogScrollableText the scrollable text
	 * @return <code>true</code> if the user presses the Yes button,
	 *         <code>false</code> otherwise
	 */
	public static boolean openQuestion(Shell parent, String title, String message, String dialogScrollableText) {
		return open(QUESTION, parent, title, message, dialogScrollableText);
	}

	/**
	 * Convenience method to open a standard warning dialog.
	 * 
	 * @param parent               the parent shell of the dialog, or
	 *                             <code>null</code> if none
	 * @param title                the dialog's title, or <code>null</code> if none
	 * @param message              the message
	 * @param dialogScrollableText the scrollable text
	 */
	public static void openWarning(Shell parent, String title, String message, String dialogScrollableText) {
		open(WARNING, parent, title, message, dialogScrollableText);
	}
}
