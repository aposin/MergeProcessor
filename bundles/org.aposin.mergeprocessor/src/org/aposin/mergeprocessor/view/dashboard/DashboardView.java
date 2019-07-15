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

import org.aposin.mergeprocessor.view.Messages;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * View of the merge processor dashboard.
 * 
 * @author Stefan Weiser
 *
 */
public class DashboardView extends Composite {

    private final TableViewer tableViewer;
    private final TableViewerColumn statusViewerColumn;
    private final TableViewerColumn hostViewerColumn;
    private final TableViewerColumn repositoryViewerColumn;
    private final TableViewerColumn dateViewerColumn;
    private final TableViewerColumn revisionViewerColumn;
    private final TableViewerColumn sourceBranchViewerColumn;
    private final TableViewerColumn targetBranchViewerColumn;
    private final TableViewerColumn mergeScriptViewerColumn;
    private final TableViewerColumn renameStatusViewerColumn;

    private final Button btnMergeSelection;
    private final Button btnIgnoreSelection;

    /**
     * @param parent a widget which will be the parent of the new instance (cannot be null)
     * @param style the style of widget to construct
     */
    public DashboardView(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout());

        final Composite tableComposite = new Composite(this, SWT.NONE);
        final TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);
        tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tableViewer = new TableViewer(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        final Table table = tableViewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        statusViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn statusColumn = statusViewerColumn.getColumn();
        tableColumnLayout.setColumnData(statusColumn, new ColumnPixelData(70));
        statusColumn.setText(Messages.View_Column_Status);

        hostViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn hostColumn = hostViewerColumn.getColumn();
        tableColumnLayout.setColumnData(hostColumn, new ColumnPixelData(120));
        hostColumn.setText(Messages.View_Column_Host);

        repositoryViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn repositoryColumn = repositoryViewerColumn.getColumn();
        tableColumnLayout.setColumnData(repositoryColumn, new ColumnPixelData(110));
        repositoryColumn.setText(Messages.View_Column_Repository);

        dateViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn dateColumn = dateViewerColumn.getColumn();
        tableColumnLayout.setColumnData(dateColumn, new ColumnPixelData(130));
        dateColumn.setText(Messages.View_Column_Date);

        revisionViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn revisionColumn = revisionViewerColumn.getColumn();
        tableColumnLayout.setColumnData(revisionColumn, new ColumnPixelData(100));
        revisionColumn.setText(Messages.View_Column_Revision_Range);

        sourceBranchViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn sourceBranchColumn = sourceBranchViewerColumn.getColumn();
        tableColumnLayout.setColumnData(sourceBranchColumn, new ColumnPixelData(120));
        sourceBranchColumn.setText(Messages.View_Column_Source_Branch);

        targetBranchViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn targetBranchColumn = targetBranchViewerColumn.getColumn();
        tableColumnLayout.setColumnData(targetBranchColumn, new ColumnPixelData(120));
        targetBranchColumn.setText(Messages.View_Column_Target_Branch);

        mergeScriptViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn mergeScriptColumn = mergeScriptViewerColumn.getColumn();
        tableColumnLayout.setColumnData(mergeScriptColumn, new ColumnPixelData(0));
        mergeScriptColumn.setText(Messages.View_Column_Merge_Script);

        renameStatusViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn renameColumn = renameStatusViewerColumn.getColumn();
        tableColumnLayout.setColumnData(renameColumn, new ColumnPixelData(23));
        renameColumn.setText(Messages.View_Column_Renaming);

        final Composite buttonComposite = new Composite(this, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        buttonComposite.setLayout(new FillLayout());

        btnMergeSelection = new Button(buttonComposite, SWT.PUSH);
        btnMergeSelection.setText(Messages.View_Button_Merge_Selection);

        btnIgnoreSelection = new Button(buttonComposite, SWT.PUSH);
        btnIgnoreSelection.setText(Messages.View_Button_Ignore_Selection);

    }

    /**
     * @return the tableViewer
     */
    public TableViewer getTableViewer() {
        return tableViewer;
    }

    /**
     * @return the statusViewerColumn
     */
    public TableViewerColumn getStatusViewerColumn() {
        return statusViewerColumn;
    }

    /**
     * @return the hostViewerColumn
     */
    public TableViewerColumn getHostViewerColumn() {
        return hostViewerColumn;
    }

    /**
     * @return the repositoryViewerColumn
     */
    public TableViewerColumn getRepositoryViewerColumn() {
        return repositoryViewerColumn;
    }

    /**
     * @return the dateViewerColumn
     */
    public TableViewerColumn getDateViewerColumn() {
        return dateViewerColumn;
    }

    /**
     * @return the revisionViewerColumn
     */
    public TableViewerColumn getRevisionViewerColumn() {
        return revisionViewerColumn;
    }

    /**
     * @return the sourceBranchViewerColumn
     */
    public TableViewerColumn getSourceBranchViewerColumn() {
        return sourceBranchViewerColumn;
    }

    /**
     * @return the targetBranchViewerColumn
     */
    public TableViewerColumn getTargetBranchViewerColumn() {
        return targetBranchViewerColumn;
    }

    /**
     * @return the mergeScriptViewerColumn
     */
    public TableViewerColumn getMergeScriptViewerColumn() {
        return mergeScriptViewerColumn;
    }

    /**
     * @return the renameStatusViewerColumn
     */
    public TableViewerColumn getRenameStatusViewerColumn() {
        return renameStatusViewerColumn;
    }

    /**
     * @return the btnMergeSelection
     */
    public Button getBtnMergeSelection() {
        return btnMergeSelection;
    }

    /**
     * @return the btnIgnoreSelection
     */
    public Button getBtnIgnoreSelection() {
        return btnIgnoreSelection;
    }

}
