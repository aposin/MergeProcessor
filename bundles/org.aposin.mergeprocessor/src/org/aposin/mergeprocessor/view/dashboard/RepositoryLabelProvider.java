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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.VersionControlSystem;
import org.aposin.mergeprocessor.model.git.GITMergeUnit;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
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
class RepositoryLabelProvider extends MergeUnitLabelProvider {

    private static final Map<VersionControlSystem, String> IMAGE_PATH_MAPPING = createImagePathMapping();

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getText(IMergeUnit mergeUnit) {
        final String repository = mergeUnit.getRepository();
        if (repository.indexOf('/') > -1) {
            return repository.substring(repository.lastIndexOf('/') + 1);
        } else {
            return repository;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Image getImage(IMergeUnit mergeUnit) {
        if (mergeUnit instanceof SVNMergeUnit) {
            return getImage(VersionControlSystem.SVN);
        } else if (mergeUnit instanceof GITMergeUnit) {
            return getImage(VersionControlSystem.GIT);
        } else {
            return super.getImage(mergeUnit);
        }
    }

    /**
     * @return the image paths mapped to the {@link VersionControlSystem}
     */
    private static Map<VersionControlSystem, String> createImagePathMapping() {
        final Map<VersionControlSystem, String> map = new HashMap<>();
        map.put(VersionControlSystem.SVN, "icons/v_collection_png/16x16/plain/svn.png");
        map.put(VersionControlSystem.GIT, "icons/v_collection_png/16x16/plain/git.png");
        return map;
    }

    /**
     * Returns the image for the given {@link VersionControlSystem}.
     * 
     * @param system the system
     * @return the image
     */
    private static Image getImage(final VersionControlSystem system) {
        final Image image = JFaceResources.getImage(system.toString());
        if (image == null) {
            final URL imageClockUrl = FileLocator.find(Activator.getDefault().getBundle(),
                    new Path(IMAGE_PATH_MAPPING.get(system)));
            if (imageClockUrl != null) {
                JFaceResources.getImageRegistry().put(system.toString(), ImageDescriptor.createFromURL(imageClockUrl));
                return JFaceResources.getImage(system.toString());
            } else {
                return null;
            }
        } else {
            return image;
        }
    }

}
