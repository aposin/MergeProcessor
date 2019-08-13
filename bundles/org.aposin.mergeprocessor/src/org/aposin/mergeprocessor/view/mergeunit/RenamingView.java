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

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * 
 * @author Stefan Weiser
 *
 */
public class RenamingView extends Composite {

	private TableViewer viewer;
	private TableViewerColumn viewerColumnFrom;
	private TableViewerColumn viewerColumnTo;

	/**
	 * @param parent a widget which will be the parent of the new instance (cannot be null)
	 * @param style the style of widget to construct
	 */
	public RenamingView(Composite parent, int style) {
		super(parent, style);

		final TableColumnLayout layout = new TableColumnLayout();
		setLayout(layout);

		viewer = new TableViewer(this, SWT.FULL_SELECTION);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);

		viewerColumnFrom = new TableViewerColumn(viewer, SWT.NONE);
		viewerColumnFrom.getColumn().setText("From");
		layout.setColumnData(viewerColumnFrom.getColumn(), new ColumnWeightData(1));

		viewerColumnTo = new TableViewerColumn(viewer, SWT.NONE);
		viewerColumnTo.getColumn().setText("To");
		layout.setColumnData(viewerColumnTo.getColumn(), new ColumnWeightData(1));

	}

	/**
	 * @return the viewer
	 */
	public TableViewer getTableViewer() {
		return viewer;
	}

	/**
	 * @return the viewerColumnFrom
	 */
	public TableViewerColumn getTableViewerColumnFrom() {
		return viewerColumnFrom;
	}

	/**
	 * @return the viewerColumnTo
	 */
	public TableViewerColumn getTableViewerColumnTo() {
		return viewerColumnTo;
	}
}
