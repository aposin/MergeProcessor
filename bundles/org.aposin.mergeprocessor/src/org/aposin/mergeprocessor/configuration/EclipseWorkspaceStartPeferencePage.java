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
package org.aposin.mergeprocessor.configuration;

import org.aposin.mergeprocessor.application.Activator;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This preference page provides the possibility to configure the startup of an Eclipse application for reviewing
 * the automatic merge. 
 * 
 * @author Stefan Weiser
 *
 */
public class EclipseWorkspaceStartPeferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String ECLIPSE_APPLICATION_PATH = "ECLIPSE_APPLICATION_PATH"; //$NON-NLS-1$
	public static final String ECLIPSE_APPLICATION_PARAMETERS = "ECLIPSE_APPLICATION_PARAMETERS"; //$NON-NLS-1$

	public EclipseWorkspaceStartPeferencePage() {
		super(GRID);
	}

	/**<o
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.EclipseWorkspaceStartPeferencePage_description);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		addField(new FileFieldEditor(ECLIPSE_APPLICATION_PATH,
				Messages.EclipseWorkspaceStartPeferencePage_applicationPath, getFieldEditorParent()));
		final StringFieldEditor parameters = new StringFieldEditor(ECLIPSE_APPLICATION_PARAMETERS,
				Messages.EclipseWorkspaceStartPeferencePage_parameters, 44, getFieldEditorParent());
		addField(parameters);
	}

}
