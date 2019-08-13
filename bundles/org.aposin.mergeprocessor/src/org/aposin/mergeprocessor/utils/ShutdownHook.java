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
package org.aposin.mergeprocessor.utils;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Shuts down the workbench when the user logs of or the machine is shutting down.
 */
public class ShutdownHook extends Thread {

	private static final String THREAD_NAME = "MergeProcessorShutdownHook"; //$NON-NLS-1$

	private static ShutdownHook instance = null;
	private boolean isActivated = false;

	private ShutdownHook() {
		super(THREAD_NAME);
	}

	/**
	 * @return the instance
	 */
	public static ShutdownHook getInstance() {
		if (instance == null) {
			instance = new ShutdownHook();
		}
		return instance;
	}

	/**
	 * Activates the Hook
	 */
	public void activate() {
		if (!isActivated) {
			Runtime.getRuntime().addShutdownHook(this);
			isActivated = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		IWorkbench workbench = PlatformUI.getWorkbench();

		if (!workbench.isClosing()) {
			PlatformUI.getWorkbench().close();
		}
	}
}
