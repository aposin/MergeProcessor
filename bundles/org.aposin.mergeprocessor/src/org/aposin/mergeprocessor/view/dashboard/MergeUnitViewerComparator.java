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
/**
 * 
 */
package org.aposin.mergeprocessor.view.dashboard;

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.view.Column;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

/**
 * @author Stefan Weiser
 */
class MergeUnitViewerComparator extends ViewerComparator {

    private int propertyIndex;

    /**
     * 
     */
    MergeUnitViewerComparator() {
        this.propertyIndex = 0;
    }

    /**
     * @param column
     */
    void setColumn(int column) {
        this.propertyIndex = column;
    }

    int getColumn() {
        return propertyIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 == e2) {
            return 0;
        } else if (e1 instanceof IMergeUnit && e2 instanceof IMergeUnit) {
            final int result;
            final IMergeUnit mergeUnit1 = (IMergeUnit) e1;
            final IMergeUnit mergeUnit2 = (IMergeUnit) e2;

            final Column[] columns = Column.sortedValues();
            switch (columns[propertyIndex]) {
                case COLUMN_STATUS:
                    result = mergeUnit1.getStatus().compareTo(mergeUnit2.getStatus());
                    break;
                case COLUMN_REPOSITORY:
                    result = mergeUnit1.getRepository().compareTo(mergeUnit2.getRepository());
                    break;
                case COLUMN_DATE:
                    result = mergeUnit1.getDate().compareTo(mergeUnit2.getDate());
                    break;
                case COLUMN_REVISIONS:
                    result = mergeUnit1.getRevisionInfo().compareTo(mergeUnit2.getRevisionInfo());
                    break;
                case COLUMN_BRANCH_SOURCE:
                    result = mergeUnit1.getBranchSource().compareTo(mergeUnit2.getBranchSource());
                    break;
                case COLUMN_BRANCH_TARGET:
                    result = mergeUnit1.getBranchTarget().compareTo(mergeUnit2.getBranchTarget());
                    break;
                case COLUMN_MERGESCRIPT:
                    result = mergeUnit1.getFileName().compareTo(mergeUnit2.getFileName());
                    break;
                default:
                    result = 0;
                    break;
            }
            if (viewer instanceof TableViewer) {
                final int sortDirection = ((TableViewer) viewer).getTable().getSortDirection();
                return sortDirection == SWT.UP ? result : result * -1;
            } else {
                return result;
            }
        } else {
            return e1 != null ? e1.toString().compareTo(e2.toString()) : 1;
        }
    }
}
