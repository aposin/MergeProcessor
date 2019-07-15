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

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

abstract class MergeUnitLabelProvider extends ColumnLabelProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(Object element) {
        if (element instanceof IMergeUnit) {
            return getText((IMergeUnit) element);
        } else {
            return super.getText(element);
        }
    }

    /**
     * @param mergeUnit the {@link IMergeUnit}
     * @return the text
     * @see #getText(Object)
     */
    protected String getText(IMergeUnit mergeUnit) {
        return super.getText(mergeUnit);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IMergeUnit) {
            return getImage((IMergeUnit) element);
        } else {
            return super.getImage(element);
        }
    }

    /**
     * @param mergeUnit the {@link IMergeUnit}
     * @return the image
     * @see #getImage(Object)
     */
    protected Image getImage(IMergeUnit mergeUnit) {
        return super.getImage(mergeUnit);
    }

}
