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
package org.aposin.mergeprocessor.view.mergeunit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.UnsupportedVersionControlSystemSupportException;
import org.aposin.mergeprocessor.model.git.GITMergeUnit;
import org.aposin.mergeprocessor.model.git.UnsupportedGITSupportException;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnitFactory;
import org.aposin.mergeprocessor.renaming.RenameQueryExecutor;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.view.Messages;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import com.google.common.base.Objects;

/**
 *
 */
public class MergeScriptDialog extends Dialog {

	private static final Logger LOGGER = Logger.getLogger(MergeScriptDialog.class.getName());
	private static final String COLOR_WARNING = "COLOR_WARNING";
	private static final String COLOR_INFORMATION = "COLOR_INFORMATION";
	private static final String COLOR_COMMENT_COMPLETE = "COLOR_COMMENT_COMPLETE";
	private static final String COLOR_COMMENT = "COLOR_COMMENT";

	// private final View parentView;
	private final IMergeUnit mergeUnit;
	private final String contentScript;
	private final RenameQueryExecutor renameQueryExecutor;

	static {
		JFaceResources.getColorRegistry().put(COLOR_WARNING, new RGB(234, 0, 0));
		JFaceResources.getColorRegistry().put(COLOR_INFORMATION, new RGB(0, 0, 150));
		JFaceResources.getColorRegistry().put(COLOR_COMMENT_COMPLETE, new RGB(0, 150, 0));
		JFaceResources.getColorRegistry().put(COLOR_COMMENT, new RGB(63, 127, 95));

	}

	/**
	 * The shell for this dialog
	 */
	protected Shell shell;

	/**
	 * @param parentShell   a shell which will be the parent of the new instance
	 * @param mergeUnit     the mergeUnit
	 * @param contentScript the script of the mergeUnit
	 */
	public MergeScriptDialog(final Shell parentShell, IMergeUnit mergeUnit, String contentScript,
			final RenameQueryExecutor renameQueryExecutor) {
		super(parentShell);
		this.mergeUnit = mergeUnit;
		this.contentScript = contentScript;
		this.renameQueryExecutor = renameQueryExecutor;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.SHELL_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
		shell.setMinimumSize(new Point(320, 240));
		shell.setSize(620, 670);
		shell.setText(NLS.bind(Messages.MergeScriptDialog_ShellText, mergeUnit.getFileName()));
		shell.setLayout(new FillLayout());

		final MergeScriptView view = new MergeScriptView(shell, SWT.NONE);

		view.getTextMergeScriptPath().setText(mergeUnit.getFileName());
		view.getTextStatus().setText(mergeUnit.getStatus().toString());
		view.getTextDate().setText(mergeUnit.getDate().toString());
		view.getTextRevisionRange().setText(mergeUnit.getRevisionInfo()); // $NON-NLS-1$
		view.getTextSourceBranch().setText(mergeUnit.getBranchSource());

		// for (final String branch : mergeUnit.listBranches()) {
		// view.getTextTargetBranch().add(branch);
		// }
		view.getTextTargetBranch().setText(mergeUnit.getBranchTarget());

		final Listener listener = new TextTargetBranchListener(view);
		view.getTextTargetBranch().addListener(SWT.FocusOut, listener);
		view.getTextTargetBranch().addListener(SWT.Traverse, listener);

		final StringBuilder sbNeededFiles = new StringBuilder();
		for (String neededFile : mergeUnit.getAffectedTargetFiles()) {
			sbNeededFiles.append(neededFile);
			sbNeededFiles.append('\n');
		}
		if (sbNeededFiles.toString().isEmpty()) {
			view.excludeTextNeededFiles(true);
		} else {
			view.getTextNeededFiles().setText(sbNeededFiles.toString());
		}
		view.getButtonShowChanges().addListener(SWT.Selection, e -> mergeUnit.showChanges());
		view.getTextContent().setText(contentScript);
		view.getTextContent().setStyleRanges(getStyleRangesForScript(contentScript));
		view.getButtonClose().addListener(SWT.Selection, e -> shell.close());
		view.getButtonClose().setFocus();

		try {
			final RenamingView renamingView = view.getRenamingView();
			if (renameQueryExecutor.hasRenaming(mergeUnit).get()) {
				final TableViewer tableViewer = renamingView.getTableViewer();
				tableViewer.setContentProvider(new RenameTableViewerContentProvider());
				final TableViewerColumn fromColumn = renamingView.getTableViewerColumnFrom();
				fromColumn.setLabelProvider(new RenameTableColumnViewerLabelProvider(fromColumn.getColumn()));
				final TableViewerColumn toColumn = renamingView.getTableViewerColumnTo();
				toColumn.setLabelProvider(new RenameTableColumnViewerLabelProvider(toColumn.getColumn()));
				fromColumn.getColumn().addListener(SWT.Resize, e -> updateAllElements(tableViewer));
				toColumn.getColumn().addListener(SWT.Resize, e -> updateAllElements(tableViewer));
				tableViewer.setComparator(new ViewerComparator() {

					@Override
					public int compare(Viewer viewer, Object e1, Object e2) {
						if (e1 instanceof Entry && e2 instanceof Entry) {
							final Entry<?, ?> entry1 = (Entry<?, ?>) e1;
							final Entry<?, ?> entry2 = (Entry<?, ?>) e2;
							if (entry1.getKey() instanceof Path && entry2.getKey() instanceof Path) {
								final Path path1 = (Path) entry1.getKey();
								final Path path2 = (Path) entry2.getKey();
								return path1.compareTo(path2);
							}
						}
						return super.compare(tableViewer, e1, e2);
					}

				});
				tableViewer.setInput(mergeUnit.getRenameMapping());
			} else {
				renamingView.setEnabled(false);
			}
		} catch (InterruptedException | ExecutionException e) {
			LogUtil.throwing(e);
		}

		shell.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.ESC) { // on return, enter or escape key close the dialog
					shell.close();
				}
			}
		});
	}

	/**
	 * Updates all items of the given {@link TableViewer}.
	 * 
	 * @param tableViewer the {@link TableViewer}
	 * @see TableViewer#update(Object[], String[])
	 */
	private static void updateAllElements(final TableViewer tableViewer) {
		final int count = tableViewer.getTable().getItemCount();
		final Object[] elements = new Object[count];
		for (int i = 0; i < count; i++) {
			elements[i] = tableViewer.getElementAt(i);
		}
		tableViewer.update(elements, null);
	}

	private static StyleRange[] getStyleRangesForScript(String contentScript) {
		List<StyleRange> styleRanges = new ArrayList<>();

		boolean isCrNl = contentScript.indexOf("\r\n") != -1; //$NON-NLS-1$
		String lineEnding = isCrNl ? "\r\n" : "\n"; //$NON-NLS-1$ //$NON-NLS-2$

		int posLineStart = 0;
		int posLineEnd = 0;

		for (; posLineEnd < contentScript.length(); posLineEnd += lineEnding.length()) {
			StyleRange styleRange = null;
			posLineStart = posLineEnd;
			posLineEnd = contentScript.indexOf(lineEnding, posLineEnd);

			if (posLineEnd == -1) {
				break;
			}

			String line = contentScript.substring(posLineStart, posLineEnd);

			if (line.isEmpty() || posLineStart == posLineEnd) {
				continue;
			}

			if (line.startsWith(SVNMergeUnitFactory.SYMBOL_WARNING)) {
				styleRange = new StyleRange();
				styleRange.start = posLineStart;
				styleRange.length = posLineEnd - posLineStart;
				styleRange.fontStyle = SWT.BOLD;
				styleRange.background = JFaceResources.getColorRegistry().get(COLOR_WARNING);
			} else if (line.startsWith(SVNMergeUnitFactory.SYMBOL_INFORMATION)) {
				styleRange = new StyleRange();
				styleRange.start = posLineStart;
				styleRange.length = posLineEnd - posLineStart;
				styleRange.foreground = JFaceResources.getColorRegistry().get(COLOR_INFORMATION);
			} else if (line.startsWith(SVNMergeUnitFactory.SYMBOL_COMMENT_COMPLETED)) {
				styleRange = new StyleRange();
				styleRange.start = posLineStart;
				styleRange.length = posLineEnd - posLineStart;
				styleRange.fontStyle = SWT.BOLD;
				styleRange.foreground = JFaceResources.getColorRegistry().get(COLOR_COMMENT_COMPLETE);
			} else if (line.startsWith(SVNMergeUnitFactory.SYMBOL_COMMENT)) {
				styleRange = new StyleRange();
				styleRange.start = posLineStart;
				styleRange.length = posLineEnd - posLineStart;
				styleRange.foreground = JFaceResources.getColorRegistry().get(COLOR_COMMENT);
			} else {
				styleRange = new StyleRange();
				styleRange.start = posLineStart;
				styleRange.length = posLineEnd - posLineStart;
				styleRange.fontStyle = SWT.BOLD;
			}

			if (styleRange != null) {
				styleRanges.add(styleRange);
			}
		}

		return styleRanges.toArray(new StyleRange[0]);
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}

	/**
	 * Listener for watching changes of the target branch.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private class TextTargetBranchListener implements Listener {

		private final MergeScriptView view;
		private boolean onHandleEvent = false;

		private TextTargetBranchListener(final MergeScriptView view) {
			this.view = view;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void handleEvent(Event event) {
			if (!onHandleEvent) {
				onHandleEvent = true;
				if (event.type == SWT.FocusOut || (event.type == SWT.Traverse && event.detail == SWT.TRAVERSE_RETURN)) {
					changeTargetBranch(view);
				}
				onHandleEvent = false;
			}
		}

		/**
		 * Forces the change of the target branch for the given merge unit.
		 * 
		 * @param mergeScriptView the view
		 */
		private void changeTargetBranch(final MergeScriptView mergeScriptView) {
			// final Combo textControl = mergeScriptView.getTextTargetBranch();
			// final String viewText = textControl.getText();
			// if (!Objects.equals(viewText, mergeUnit.getBranchTarget())) {
			// final String dialogTitle =
			// Messages.MergeScriptDialog_changeTargetBranchDialogTitle;
			// final String dialogMessage =
			// Messages.MergeScriptDialog_changeTargetBranchDialogMessage;
			// final String doChangesText =
			// Messages.MergeScriptDialog_changeTargetBranchDialogYes;
			// final String revertChanges =
			// Messages.MergeScriptDialog_changeTargetBranchDialogNo;
			// final MessageDialog dialog = new MessageDialog(mergeScriptView.getShell(),
			// dialogTitle, null,
			// dialogMessage, MessageDialog.CONFIRM, new String[] { doChangesText,
			// revertChanges }, 1);
			// final int result = dialog.open();
			// if (result == 0) {
			// //We merge
			// boolean retryOnFail = true;
			// while (retryOnFail) {
			// try {
			// changeTargetBranch(viewText);
			// mergeUnit.setBranchTarget(viewText);
			// break;
			// } catch (IOException | SftpException e) {
			// LOGGER.log(Level.SEVERE, String.format("Exception occured while getting
			// content of '%s'", //$NON-NLS-1$
			// mergeUnit.getRemotePath()), e);
			// retryOnFail = MergeProcessorUtil.bugUserToFixProblem(e.getMessage(),
			// ExceptionUtils.getStackTrace(e));
			// if (!retryOnFail) {
			// textControl.setText(mergeUnit.getBranchTarget());
			// }
			// }
			// }
			// } else {
			// //We revert everything
			// textControl.setText(mergeUnit.getBranchTarget());
			// }
			// }
		}

		// /**
		// * Forces the change of the target branch for the given merge unit.
		// *
		// * @param targetBranch the target branch
		// * @throws SftpException
		// * @throws IOException
		// * @throws SftpUtilException
		// */
		// private void changeTargetBranch(final String targetBranch)
		// throws IOException, SftpException, SftpUtilException {
		// if (mergeUnit.getStatus() != MergeUnitStatus.TODO) {
		// MergeProcessorUtil.todo(mergeUnit);
		// }
		// final String remotePath = mergeUnit.getRemotePath();
		//
		// final List<String> lines;
		// try (final InputStream is =
		// SftpUtil.getInstance().createInputStream(remotePath)) {
		// lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
		// }
		// final StringBuilder sb = new StringBuilder();
		// for (String line : lines) {
		// if (line.startsWith("URL_BRANCH_TARGET")) { //$NON-NLS-1$
		// if (line.endsWith("trunk")) { //$NON-NLS-1$
		// sb.append(line.replace("trunk", "branches/" + targetBranch)).append('\n');
		// //$NON-NLS-1$ //$NON-NLS-2$
		// } else {
		// assert (line.contains("/branches/")); //$NON-NLS-1$
		// sb.append(line.replaceFirst("branches\\/.*", //$NON-NLS-1$
		// "trunk".equals(targetBranch) ? "trunk" : "branches/" +
		// targetBranch)).append('\n'); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// }
		// } else {
		// sb.append(line).append('\n');
		// }
		// }
		// SftpUtil.getInstance().writeToRemotePath(sb.toString(), remotePath);
		// MergeScriptDialog.this.parentView.refresh();
		// }

	}

	/**
	 * Content provider for the rename table viewer.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class RenameTableViewerContentProvider implements IStructuredContentProvider {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Map) {
				return ((Map<?, ?>) inputElement).entrySet().stream()
						.filter(entry -> !Objects.equal(entry.getKey(), entry.getValue())) // Filter all items where key/value are equal
						.toArray();
			}
			return new Object[0];
		}

	}

	/**
	 * The label provider for the columns of the rename table viewer.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class RenameTableColumnViewerLabelProvider extends ColumnLabelProvider {

		private final TableColumn tableColumn;

		private GC gc;

		public RenameTableColumnViewerLabelProvider(final TableColumn tableColumn) {
			this.tableColumn = tableColumn;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void update(ViewerCell cell) {
			gc = new GC(cell.getControl());
			try {
				super.update(cell);
			} finally {
				gc.dispose();
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getText(Object element) {
			if (element instanceof Entry) {
				final Entry<?, ?> entry = (Entry<?, ?>) element;
				if (tableColumn.getParent().indexOf(tableColumn) == 0) { // FROM
					return calculateStringToColumnWidth(entry.getKey().toString());
				} else if (tableColumn.getParent().indexOf(tableColumn) == 1) { // TO
					return calculateStringToColumnWidth(entry.getValue().toString());
				}

			}
			return super.getText(element);
		}

		/**
		 * Calculates the output {@link String} depending to the column width. The text
		 * is dotted at the beginning if the column is smaller than the given
		 * {@link String}.
		 * 
		 * @param string the original output {@link String}
		 * @return the output {@link String}, maybe dottet at the beginning
		 */
		private String calculateStringToColumnWidth(final String string) {
			final String dotDotDot = "...";
			String output = string;
			Point outputLength = gc.textExtent(output);
			if (tableColumn.getWidth() == 0) {
				return string;
			} else {
				while (outputLength.x > tableColumn.getWidth()) {
					final int index = output.indexOf('\\', output.indexOf('\\') + 1);
					if (index == -1) {
						output = dotDotDot;
						break;
					} else {
						outputLength = gc.textExtent(output);
						output = dotDotDot + output.substring(index);
					}
				}
				return output;
			}
		}

	}
}
