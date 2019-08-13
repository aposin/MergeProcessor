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

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.view.Column;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * {@link SelectionListener} for sorting the columns with the given comparator.
 * 
 * @author Stefan Weiser
 *
 */
class MergeProcessorColumnSelectionListener extends SelectionAdapter {

	private static final String COLUMN_VIEWER_KEY = Policy.JFACE + ".columnViewer";//$NON-NLS-1$

	private final MergeUnitViewerComparator comparator;
	private final IConfiguration configuration;

	/**
	 * @param comparator the comparator
	 * @param configuration the configuration
	 */
	MergeProcessorColumnSelectionListener(final MergeUnitViewerComparator comparator,
			final IConfiguration configuration) {
		this.comparator = comparator;
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		final TableColumn column = (TableColumn) e.widget;
		final TableViewerColumn viewerColumn = (TableViewerColumn) column.getData(COLUMN_VIEWER_KEY);
		final Table table = column.getParent();
		comparator.setColumn(table.indexOf(column));
		if (table.getSortColumn() == column) {
			table.setSortDirection(table.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP);
		} else {
			table.setSortColumn(column);
			table.setSortDirection(SWT.DOWN);
		}
		viewerColumn.getViewer().refresh();
		configuration.setSortColumn(Column.valueForIndex(table.indexOf(column)));
		configuration.setSortDirection(table.getSortDirection());
	}
}