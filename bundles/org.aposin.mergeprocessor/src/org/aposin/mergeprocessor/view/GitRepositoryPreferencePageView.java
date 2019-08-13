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
package org.aposin.mergeprocessor.view;

import org.aposin.mergeprocessor.configuration.GitRepositoriesPreferencePage;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * View for the {@link GitRepositoriesPreferencePage}.
 * 
 * @author Stefan Weiser
 *
 */
public class GitRepositoryPreferencePageView extends Composite {

	private final TableViewer tableViewer;
	private final TableViewerColumn columnRepository;
	private final TableViewerColumn columnLocalPath;
	private final TableViewerColumn columnMemory;
	private final MenuItem menuItemGoToRepository;

	/**
	 * @param parent a widget which will be the parent of the new instance (cannot be null)
	 * @param style the style of widget to construct
	 */
	public GitRepositoryPreferencePageView(Composite parent, int style) {
		super(parent, style);
		final TableColumnLayout tableColumnLayout = new TableColumnLayout();
		setLayout(tableColumnLayout);

		tableViewer = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		columnRepository = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn colRepository = columnRepository.getColumn();
		tableColumnLayout.setColumnData(colRepository, new ColumnWeightData(1));
		colRepository.setText("Repository");

		columnLocalPath = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn colLocalPath = columnLocalPath.getColumn();
		tableColumnLayout.setColumnData(colLocalPath, new ColumnWeightData(1));
		colLocalPath.setText("Local Path");

		columnMemory = new TableViewerColumn(tableViewer, SWT.RIGHT);
		TableColumn colMemory = columnMemory.getColumn();
		tableColumnLayout.setColumnData(colMemory, new ColumnPixelData(80, true, true));
		colMemory.setText("Memory");

		final Menu menu = new Menu(tableViewer.getTable());
		menuItemGoToRepository = new MenuItem(menu, SWT.NONE);
		menuItemGoToRepository.setText("Go to Repository");
		tableViewer.getTable().setMenu(menu);
	}

	/**
	 * @return the tableViewer
	 */
	public TableViewer getTableViewer() {
		return tableViewer;
	}

	/**
	 * @return the columnRepository
	 */
	public TableViewerColumn getColumnRepository() {
		return columnRepository;
	}

	/**
	 * @return the columnLocalPath
	 */
	public TableViewerColumn getColumnLocalPath() {
		return columnLocalPath;
	}

	/**
	 * @return the columnMemory
	 */
	public TableViewerColumn getColumnMemory() {
		return columnMemory;
	}

	/**
	 * @return the menuItemGoToRepository
	 */
	public MenuItem getMenuItemGoToRepository() {
		return menuItemGoToRepository;
	}

}
