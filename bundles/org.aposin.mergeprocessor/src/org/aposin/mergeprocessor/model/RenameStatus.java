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
package org.aposin.mergeprocessor.model;

import java.net.URL;

import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

/**
 * Stati for possible renaming actions for 
 * 
 * @author Stefan Weiser
 *
 */
public enum RenameStatus {

	NOTHING("no renaming, no linking", "icons/v_collection_png/16x16/plain/nothing.png"), //
	RENAME("renamed artifacts existing", "icons/v_collection_png/16x16/plain/rename.png"), //
	LINK("linked artifacts existing", null);

	private final String text;
	private String icon;

	private RenameStatus(final String text, final String icon) {
		this.text = text;
		this.icon = icon;
	}

	/**
	 * @return the image representing the rename status.
	 */
	public Image getImage() {
		if (icon != null) {
			final Image image = JFaceResources.getImage(icon);
			if (image == null) {
				final URL imageUrl = FileLocator.find(Activator.getDefault().getBundle(), new Path(icon));
				if (imageUrl == null) {
					LogUtil.getLogger().warning("Image not found for " + icon);
					icon = null;
				} else {
					JFaceResources.getImageRegistry().put(icon, ImageDescriptor.createFromURL(imageUrl));
					return JFaceResources.getImage(icon);
				}
			} else {
				return image;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return text;
	}

}
