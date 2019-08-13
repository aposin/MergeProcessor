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
package org.aposin.mergeprocessor.help;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;

/**
 * Handler showing help information.
 * 
 * @author Stefan Weiser
 *
 */
public class HandlerHelp {

	private static final String HELP_HTML = "doc/html/help.html"; //$NON-NLS-1$

	private File helpFile;

	@Execute
	public void execute(Shell shell) {
		LogUtil.entering(shell);
		final HelpDialog dialog = new HelpDialog(shell);
		dialog.setUrl(getHelpFile().toString());
		dialog.open();
		LogUtil.exiting();
	}

	/**
	 * @return the file containing the help information for the user.
	 */
	private File getHelpFile() {
		if (helpFile == null) {
			final URL helpUrl = FileLocator.find(Activator.getDefault().getBundle(), new Path(HELP_HTML));
			if (helpUrl == null) {
				LogUtil.getLogger().severe(String.format("File %s not found.", HELP_HTML)); //$NON-NLS-1$
			} else {
				try {
					helpFile = new File(FileLocator.resolve(helpUrl).toString());
				} catch (IOException e) {
					LogUtil.getLogger().log(Level.SEVERE,
							String.format("Exception occurred on accessing file %s.", helpUrl), e); //$NON-NLS-1$
				}
			}
		}
		return helpFile;
	}

}