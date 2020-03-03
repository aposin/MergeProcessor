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
import java.util.Objects;
import java.util.logging.Level;

import javax.inject.Named;

import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

/**
 * Handler showing the an HTML dialog of the MergeProcessor.
 * 
 * @author Stefan Weiser
 *
 */
public class HandlerHtmlDialog {

	/**
	 * Opens and shows the dialog.
	 * 
	 * @param shell          the parent shell
	 * @param url            the url of the HTML file to show
	 * @param initializeSize the initial size of the dialog
	 */
	@Execute
	public void show(final Shell shell, //
			@Named("org.aposin.mergeprocessor.help.command.parameter.url") String url,
			@Named("org.aposin.mergeprocessor.help.command.parameter.size") String initializeSize) {
		LogUtil.entering(shell);
		final HtmlDialog dialog = new HtmlDialog(shell);
		dialog.setInitialSize(convert(initializeSize));
		dialog.setUrl(Objects.requireNonNullElse(getHtmlFile(url), "").toString());
		dialog.open();
		LogUtil.exiting();
	}

	/**
	 * @return the file containing the help information for the user.
	 */
	private static File getHtmlFile(final String url) {
		final URL helpUrl = FileLocator.find(Activator.getDefault().getBundle(), new Path(url));
		if (helpUrl == null) {
			LogUtil.getLogger().severe(String.format("File %s not found.", url)); //$NON-NLS-1$
		} else {
			try {
				return new File(FileLocator.resolve(helpUrl).toString());
			} catch (IOException e) {
				LogUtil.getLogger().log(Level.SEVERE,
						String.format("Exception occurred on accessing file %s.", helpUrl), e); //$NON-NLS-1$
			}
		}
		return null;
	}

	/**
	 * Converts a given {@link String} into a {@link Point}.
	 * 
	 * @param p the {@link String} to convert
	 * @return the {@link Point} or {@code null} if conversion failed
	 */
	private static Point convert(String p) {
		String[] split = p.split(",");
		if (split.length == 2) {
			try {
				int x = Integer.parseInt(split[0]);
				int y = Integer.parseInt(split[1]);
				if (x > 0 && y > 0) {
					return new Point(x, y);
				} else {
					LogUtil.getLogger().severe(String.format("The given String '%s' contains negative values.", p));
				}
			} catch (NumberFormatException e) {
				LogUtil.getLogger().log(Level.SEVERE, String.format("Exception on parsing '%s' to a point ", p), e);
			}
		} else {
			LogUtil.getLogger().severe(
					String.format("The given String '%s' does not represent a valid point format, e.g. '100,150'.", p));
		}
		return null;
	}

}