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
package org.aposin.mergeprocessor.view;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.swt.widgets.Composite;

/**
 * Represents the status bar of the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public class StatusBar {

	/**
	 * Creates an instance of {@link IStatusLineManager} and sets it into the
	 * context.
	 * 
	 * @param context   the eclipse context where so set the
	 *                  {@link IStatusLineManager}
	 * @param composite the parent composite
	 */
	@PostConstruct
	public void createControl(final IEclipseContext context, @Optional final Composite composite) {
		final StatusLineManager statusLine = new StatusLineManager();
		statusLine.createControl(composite);
		context.set(IStatusLineManager.class, statusLine);
	}
}