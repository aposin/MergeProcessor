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
package org.aposin.mergeprocessor.application;

import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 *
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	/**
	 * @param configurer
	 */
	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ActionBarAdvisor(configurer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(631, 218));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		final IConfiguration configuration = E4CompatibilityUtil.getApplicationContext().get(IConfiguration.class);
		configurer.setTitle("MergeProcessor " + Configuration.getVersion() + " <" + configuration.getUser() + "@" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ configuration.getSftpConfiguration().getHost() + ">"); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean preWindowShellClose() {
		// minimize shell
		getWindowConfigurer().getWindow().getShell().notifyListeners(SWT.Iconify, new Event());

		// don't allow close
		return false;
	}

}
