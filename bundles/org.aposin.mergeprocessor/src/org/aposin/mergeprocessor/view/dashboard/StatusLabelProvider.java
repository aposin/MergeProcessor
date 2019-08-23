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
package org.aposin.mergeprocessor.view.dashboard;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

/**
 * 
 * @author Stefan Weiser
 *
 */
class StatusLabelProvider extends MergeUnitLabelProvider {

	private static final Map<MergeUnitStatus, String> IMAGE_PATH_MAPPING = createImagePathMapping();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getText(IMergeUnit mergeUnit) {
		return mergeUnit.getStatus().toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Image getImage(IMergeUnit mergeUnit) {
		final MergeUnitStatus status = mergeUnit.getStatus();
		if (status != null) {
			return getImage(status);
		} else {
			return super.getImage(mergeUnit);
		}
	}

	/**
	 * @return the image paths mapped to the {@link MergeUnitStatus}
	 */
	private static Map<MergeUnitStatus, String> createImagePathMapping() {
		final Map<MergeUnitStatus, String> map = new HashMap<>();
		map.put(MergeUnitStatus.TODO, "icons/v_collection_png/16x16/plain/clock.png");
		map.put(MergeUnitStatus.CANCELLED, "icons/v_collection_png/16x16/plain/sign_warning.png");
		map.put(MergeUnitStatus.IGNORED, "icons/v_collection_png/16x16/plain/lock.png");
		map.put(MergeUnitStatus.DONE, "icons/v_collection_png/16x16/plain/ok.png");
		map.put(MergeUnitStatus.MANUAL, "icons/v_collection_png/16x16/plain/pencil.png");
		return map;
	}

	/**
	 * Returns the image for the given {@link MergeUnitStatus}.
	 * 
	 * @param status the status
	 * @return the image
	 */
	private static Image getImage(final MergeUnitStatus status) {
		final Image image = JFaceResources.getImage(status.toString());
		if (image == null) {
			final URL imageClockUrl = FileLocator.find(Activator.getDefault().getBundle(),
					new Path(IMAGE_PATH_MAPPING.get(status)));
			if (imageClockUrl != null) {
				JFaceResources.getImageRegistry().put(status.toString(), ImageDescriptor.createFromURL(imageClockUrl));
				return JFaceResources.getImage(status.toString());
			} else {
				return null;
			}
		} else {
			return image;
		}
	}

}
