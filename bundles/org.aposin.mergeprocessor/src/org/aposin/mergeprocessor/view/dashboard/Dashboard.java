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
package org.aposin.mergeprocessor.view.dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.configuration.WorkbenchPreferencePage;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeTask;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.renaming.RenameQueryExecutor;
import org.aposin.mergeprocessor.renaming.SvnPackageMergeUnitFactory;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtilException;
import org.aposin.mergeprocessor.utils.SftpUtil;
import org.aposin.mergeprocessor.utils.SftpUtilException;
import org.aposin.mergeprocessor.utils.SvnUtil;
import org.aposin.mergeprocessor.utils.SvnUtilException;
import org.aposin.mergeprocessor.view.Column;
import org.aposin.mergeprocessor.view.MessageDialogScrollable;
import org.aposin.mergeprocessor.view.Messages;
import org.aposin.mergeprocessor.view.mergeunit.MergeScriptDialog;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.ibm.icu.text.SimpleDateFormat;

/**
 * View part showing the main GUI of the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public class Dashboard implements IShellProvider {

	private static IShellProvider pmd = null;

	private final IStatusLineManager statusLineManager;
	private final Shell shell;
	private final IConfiguration configuration;
	private final IPropertyChangeListener propertyChangeListener = this::handlePropertyChange;
	private final List<IMergeUnit> mergeUnits = new ArrayList<>();
	private final MergeUnitViewerComparator comparator = new MergeUnitViewerComparator();
	private final SvnPackageMergeUnitFactory svnPackageMergeUnitFactory;
	private final RenameQueryExecutor renameQueryExecutor = new RenameQueryExecutor();

	private Runnable timer = null;
	private boolean isTimerActive = false;
	private DashboardView view;

	@Inject
	public Dashboard(final Shell shell, final IConfiguration configuration,
			final SvnPackageMergeUnitFactory svnPackageMergeUnitFactory, final IStatusLineManager statusLineManager) {
		this.statusLineManager = statusLineManager;
		this.shell = shell;
		this.configuration = configuration;
		this.svnPackageMergeUnitFactory = svnPackageMergeUnitFactory;
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize it.
	 * @param parent the parent control
	 */
	@PostConstruct
	public void createPartControl(final Composite parent) {
		LogUtil.entering(parent);

		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);

		shell.setMinimumSize(631, 218);

		// restore previous location
		Point location = Configuration.getWindowLocation();
		shell.setLocation(location);

		// restore previous size
		Point size = Configuration.getWindowSize();
		shell.setSize(size);

		view = new DashboardView(parent, SWT.NONE);
		TableViewer tableViewer = view.getTableViewer();
		final Table table = tableViewer.getTable();
		table.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				final TableItem item = (TableItem) e.item;
				final Object data = item.getData();
				if (data instanceof IMergeUnit) {
					final IMergeUnit mergeUnit = (IMergeUnit) data;
					LogUtil.getLogger()
							.fine(String.format("Double clicked tableItem %s with mergeUnit %s.", item, mergeUnit)); //$NON-NLS-1$
					showMergeScript(mergeUnit);
				} else {
					LogUtil.getLogger()
							.warning(String.format("Double clicked tableItem %s with unexpected data %s.", item, data)); //$NON-NLS-1$
				}
			}

		});

		view.getBtnMergeSelection().addListener(SWT.Selection, e -> mergeSelection());
		view.getBtnIgnoreSelection().addListener(SWT.Selection, e -> ignoreSelection());

		tableViewer.setComparator(comparator);
		final SelectionListener selectionListener = new MergeProcessorColumnSelectionListener(comparator,
				configuration);

		final TableViewerColumn statusViewerColumn = view.getStatusViewerColumn();
		statusViewerColumn.setLabelProvider(new StatusLabelProvider());
		statusViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn hostViewerColumn = view.getHostViewerColumn();
		hostViewerColumn.setLabelProvider(new HostLabelProvider());
		hostViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn repositoryViewerColumn = view.getRepositoryViewerColumn();
		repositoryViewerColumn.setLabelProvider(new RepositoryLabelProvider());
		repositoryViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn dateViewerColumn = view.getDateViewerColumn();
		dateViewerColumn.setLabelProvider(new DateLabelProvider());
		dateViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn revisionViewerColumn = view.getRevisionViewerColumn();
		revisionViewerColumn.setLabelProvider(new RevisionLabelProvider());
		revisionViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn sourceBranchViewerColumn = view.getSourceBranchViewerColumn();
		sourceBranchViewerColumn.setLabelProvider(new BranchSourceLabelProvider());
		sourceBranchViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn targetBranchViewerColumn = view.getTargetBranchViewerColumn();
		targetBranchViewerColumn.setLabelProvider(new BranchTargetLabelProvider());
		targetBranchViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn mergeScriptViewerColumn = view.getMergeScriptViewerColumn();
		mergeScriptViewerColumn.setLabelProvider(new MergeScriptLabelProvider());
		mergeScriptViewerColumn.getColumn().addSelectionListener(selectionListener);

		final TableViewerColumn renameViewerColumn = view.getRenameStatusViewerColumn();
		renameViewerColumn.setLabelProvider(new RenameLabelProvider(tableViewer, renameQueryExecutor));
		renameViewerColumn.getColumn().addSelectionListener(selectionListener);

		// restore sort column
		Column columnToSort = configuration.getSortColumn();
		comparator.setColumn(columnToSort.ordinal());
		if (columnToSort.ordinal() < table.getColumnCount()) {
			// Check potential not existing column index when e.g. a column gets removed in
			// the future
			table.setSortColumn(table.getColumn(columnToSort.ordinal()));
		}
		table.setSortDirection(configuration.getSortDirection());
		tableViewer.refresh();

		initDataBindings();

		statusLineManager.setMessage(Messages.View_Status_Successfully_Started);

		try {
			// Delete working copy initially
			SvnUtil.recreateEmptyWorkingCopy(null, null);
		} catch (SvnUtilException e1) {
			new MessageDialogScrollable(shell, "Locked local working copy", null,
					"Could not delete local working copy.", "Could not delete local working copy.",
					MessageDialogScrollable.ERROR, new String[] { "OK" }, 0).open();
		}
		activateRefreshTimer();
		refresh();

		LogUtil.exiting();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Focus
	public void setFocus() {
		view.getTableViewer().getTable().setFocus();
	}

	/**
	 * Removes the property change listener from the preference store.
	 */
	@PreDestroy
	public void removePropertyChangeListener() {
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(propertyChangeListener);
	}

	/**
	 * Timer which refreshes the list of mergeUnits
	 */
	private void activateRefreshTimer() {
		isTimerActive = true;
		if (timer == null) {
			timer = () -> {
				if (isTimerActive) {
					Dashboard.this.refresh();
				}
				// start a new timer
				shell.getDisplay().timerExec(configuration.getRefreshInterval(), timer);
			};
			shell.getDisplay().timerExec(configuration.getRefreshInterval(), timer);
		}
	}

	private IMergeUnit[] getSelectedMergeUnits() {
		final List<?> list = view.getTableViewer().getStructuredSelection().toList();
		return list.toArray(new IMergeUnit[list.size()]);
	}

	private void selectMergeUnits(IMergeUnit[] mergeUnitsToSelect) {
		final Table table = view.getTableViewer().getTable();
		TableItem[] tableItems = table.getItems();

		ArrayList<TableItem> tableItemsToSelect = new ArrayList<>();

		for (TableItem tableItem : tableItems) {
			Object data = tableItem.getData();

			if (data != null && data instanceof IMergeUnit) {
				IMergeUnit mergeUnit = (IMergeUnit) data;

				for (IMergeUnit mergeUnitToSelect : mergeUnitsToSelect) {
					if (mergeUnit.compareTo(mergeUnitToSelect) == 0) {
						tableItemsToSelect.add(tableItem);
						break;
					}
				}
			} else {
				String message = String.format("Error while reading selection.Unexpected tableItem data. data=[%s]", //$NON-NLS-1$
						data);
				throw LogUtil.throwing(new RuntimeException(message));
			}
		}

		table.setSelection(tableItemsToSelect.toArray(new TableItem[tableItemsToSelect.size()]));
	}

	public void mergeSelection() {
		LogUtil.entering();
		// stop timer calls
		boolean isTimerActivePrev = isTimerActive;
		isTimerActive = false;

		final IMergeUnit[] mergeUnitsSelection = getSelectedMergeUnits();
		Arrays.sort(mergeUnitsSelection);
		for (final IMergeUnit mergeUnit : mergeUnitsSelection) {
			new MergeTask(mergeUnit, configuration, this).merge();
		}

		refresh();
		// start timer calls
		isTimerActive = isTimerActivePrev;
		LogUtil.exiting();
	}

	/**
	 * Merges the selected mergeUnits
	 */
	public void ignoreSelection() {
		LogUtil.entering();

		// stop timer calls
		boolean isTimerActivePrev = isTimerActive;
		isTimerActive = false;

		IMergeUnit[] selectedMergeUnits = getSelectedMergeUnits();
		Arrays.stream(selectedMergeUnits).filter(mergeUnit -> mergeUnit.getStatus() != MergeUnitStatus.IGNORED)
				.forEach(this::ignoreMergeUnit);
		refresh();

		// start timer calls
		isTimerActive = isTimerActivePrev;
		LogUtil.exiting();
	}

	private void ignoreMergeUnit(IMergeUnit mergeUnit) {
		LogUtil.entering(mergeUnit);

		MergeUnitStatus status = mergeUnit.getStatus();
		if (status == MergeUnitStatus.DONE) {
			String dialogMessage = NLS.bind(Messages.View_IgnoreSelection_Done_Question, mergeUnit.getFileName(),
					status);
			MessageDialog dialog = new MessageDialog(shell, Messages.View_IgnoreSelection_Done_Title, null,
					dialogMessage, MessageDialog.QUESTION,
					new String[] { Messages.View_IgnoreSelection_Done_Yes, Messages.View_IgnoreSelection_Done_No }, 0);

			int choice = dialog.open();

			if (choice != 0) {
				// user didn't say 'yes' so we skip it...
				LogUtil.getLogger().fine(() -> String.format("User skipped mergeUnit=%s with status %s.", mergeUnit, //$NON-NLS-1$
						mergeUnit.getStatus()));
				return;
			}
		}

		LogUtil.getLogger().fine(() -> String.format("Ignoring mergeUnit=%s", mergeUnit));
		MergeProcessorUtil.ignore(mergeUnit);
		LogUtil.exiting();
	}

	private void showMergeScript(IMergeUnit mergeUnit) {
		LogUtil.entering(mergeUnit);
		try {
			String contentScript = SftpUtil.getInstance().getContent(mergeUnit);
			MergeScriptDialog msd = new MergeScriptDialog(view.getShell(), mergeUnit, contentScript,
					renameQueryExecutor);
			msd.open();
			view.getTableViewer().update(mergeUnit, null);
		} catch (SftpUtilException e) {
			LogUtil.getLogger().log(Level.SEVERE, "Caught exception while receiving merge script content.", e); //$NON-NLS-1$
			MessageDialogScrollable.openError(view.getShell(), Messages.View_ShowMergeScript_Error_Title,
					Messages.View_ShowMergeScript_Error_Description, e.getMessage());
		}
		LogUtil.exiting();
	}

	/**
	 * Refreshes the mergeUnits. May start automatic merges.
	 * @see #downloadMergeUnitsFromServer(boolean)
	 */
	public void refresh() {
		svnPackageMergeUnitFactory.checkAndCreateNewSvnPackageMergeUnit();
		downloadMergeUnitsFromServer(false);
	}

	/**
	 * Checks, if the list of new {@link IMergeUnit mergeUnits} contains TODOs which are not existing in the old list 
	 * of {@link IMergeUnit mergeUnits}.
	 * 
	 * @param oldMergeUnits the list of old merge units
	 * @param newMergeUnits the list of new merge units
	 * @return {@code true} if new TODOs of merge units exist in the new list
	 */
	private static boolean hasNewTodos(final List<IMergeUnit> oldMergeUnits, final List<IMergeUnit> newMergeUnits) {
		for (final IMergeUnit newUnit : newMergeUnits) {
			if (newUnit.getStatus() == MergeUnitStatus.TODO) {
				boolean hasNewTodo = true;
				for (IMergeUnit oldUnit : oldMergeUnits) {
					if (newUnit.equals(oldUnit)) {
						hasNewTodo = false;
						break;
					}
				}
				if (hasNewTodo) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Refreshes the mergeUnits. If <code>isAutomatic</code> is <code>true</code> and automatic merging is active this starts automatic merges.
	 */
	private void downloadMergeUnitsFromServer(boolean isAutomatic) {
		LogUtil.entering(isAutomatic);

		// stop timer calls
		boolean isTimerActivePrev = isTimerActive;
		isTimerActive = false;

		try {
			// remember selected mergeUnits
			IMergeUnit[] mergeUnitsSelected = getSelectedMergeUnits();

			LogUtil.getLogger().fine("Clearing mergeUnitGroup."); //$NON-NLS-1$

			List<IMergeUnit> mergeUnitsFound = MergeProcessorUtil.getMergeUnits(configuration.isDisplayDone(),
					configuration.isDisplayIgnored());
			if (mergeUnitsFound.isEmpty()) {
				LogUtil.getLogger().fine("No mergeUnits found."); //$NON-NLS-1$
				this.mergeUnits.clear();
			} else {
				LogUtil.getLogger().fine(() -> String.format("Adding %s found mergeUnits.", mergeUnitsFound.size())); //$NON-NLS-1$

				final List<IMergeUnit> oldMergeUnits = new ArrayList<>(mergeUnits);
				mergeUnits.clear();
				mergeUnits.addAll(mergeUnitsFound);
				boolean containsUnseenTodos = hasNewTodos(oldMergeUnits, mergeUnits);

				if (containsUnseenTodos) {
					// Popup so that the user is informed about the found unseen to do merge units.
					shell.setVisible(true);
					shell.setFocus();
					shell.setMinimized(false);

					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						LogUtil.getLogger().log(Level.WARNING, "Caught exception while sleep.", ex); //$NON-NLS-1$
					}

					// start up automatic merging if active
					if (configuration.isAutomatic() && !isAutomatic) {
						LogUtil.getLogger().info("Automatically merging mergeUnits."); //$NON-NLS-1$
						mergeOldestAutomatically();
						downloadMergeUnitsFromServer(true);
					}
				}
			}
			view.getTableViewer().refresh();
			selectMergeUnits(mergeUnitsSelected);
		} catch (MergeProcessorUtilException e) {
			LogUtil.getLogger().log(Level.SEVERE, "Caught exception while getting merge units.", e); //$NON-NLS-1$
			MessageDialogScrollable dialog = new MessageDialogScrollable(shell,
					Messages.View_RefreshMergeUnits_Error_Title, null,
					Messages.View_RefreshMergeUnits_Error_Description,
					Messages.View_RefreshMergeUnits_Error_MessagePrefix + e.getMessage(), MessageDialogScrollable.ERROR,
					new String[] { Messages.View_RefreshMergeUnits_Error_Ok }, 0);

			dialog.open();
		}

		// Set status line text to time of last refresh
		{
			String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); //$NON-NLS-1$
			statusLineManager.setMessage(NLS.bind(Messages.View_Status_LastRefresh, date));
		}

		renameQueryExecutor.cleanup(mergeUnits);

		// start timer calls
		isTimerActive = isTimerActivePrev;

		LogUtil.exiting();
	}

	private void mergeOldestAutomatically() {
		LogUtil.entering();

		// stop timer calls
		boolean isTimerActivePrev = isTimerActive;
		isTimerActive = false;

		mergeUnits.stream().filter(mergeUnit -> mergeUnit.getStatus() == MergeUnitStatus.TODO).sorted().findFirst()
				.ifPresent(mergeUnit -> {
					LogUtil.getLogger().fine(String.format("Automatically merging MergeUnit=%s", mergeUnit)); //$NON-NLS-1$
					new MergeTask(mergeUnit, configuration, Dashboard.this).merge();
					LogUtil.getLogger().fine("Continuing automatic merging by refreshing."); //$NON-NLS-1$
					refresh();
				});

		isTimerActive = isTimerActivePrev;
		LogUtil.exiting();
	}

	/**
	 * @return the DataBindingContext
	 */
	private DataBindingContext initDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();
		final TableViewer tableViewer = view.getTableViewer();
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(mergeUnits);

		return bindingContext;
	}

	/**
	 * Handles the given {@link PropertyChangeEvent}.
	 * 
	 * @param event the event to handle
	 */
	private void handlePropertyChange(PropertyChangeEvent event) {
		switch (event.getProperty()) {
		case WorkbenchPreferencePage.WINDOW_SIZE:
			LogUtil.getLogger().fine("Setting new window size."); //$NON-NLS-1$
			shell.setSize(Configuration.getWindowSize());
			break;
		case WorkbenchPreferencePage.WINDOW_LOCATION:
			LogUtil.getLogger().fine("Setting new window location."); //$NON-NLS-1$
			shell.setLocation(Configuration.getWindowLocation());
			break;
		case WorkbenchPreferencePage.SORT_COLUMN:
			LogUtil.getLogger().fine("Setting new sort column."); //$NON-NLS-1$
			final int columnIndex1 = Column.indexForValue(configuration.getSortColumn());
			final Table table1 = view.getTableViewer().getTable();
			comparator.setColumn(columnIndex1);
			table1.setSortColumn(table1.getColumn(columnIndex1));
			view.getTableViewer().refresh();
			break;
		case WorkbenchPreferencePage.SORT_DIRECTION:
			LogUtil.getLogger().fine("Setting new sort direction."); //$NON-NLS-1$
			final int columnIndex = comparator.getColumn();
			final Table table = view.getTableViewer().getTable();
			comparator.setColumn(columnIndex);
			table.setSortColumn(table.getColumn(columnIndex));
			table.setSortDirection(configuration.getSortDirection());
			view.getTableViewer().refresh();
			break;
		default:
			break;
		}
	}

	/**
	 * @return the shell of the ProgressMonitorDialog
	 */
	public static IShellProvider getShellProgressMonitorDialog() {
		return pmd;
	}

	public static void setShellProgressMonitorDialog(IShellProvider pmd) {
		Dashboard.pmd = pmd;
	}

	@Override
	public Shell getShell() {
		return shell;
	}

}
