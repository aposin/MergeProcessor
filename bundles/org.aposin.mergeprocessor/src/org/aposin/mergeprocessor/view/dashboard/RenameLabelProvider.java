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

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.model.RenameStatus;
import org.aposin.mergeprocessor.renaming.RenameQueryExecutor;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * This label provider is responsible to show the renaming status of the merge unit.
 * 
 * @author Stefan Weiser
 *
 */
class RenameLabelProvider extends MergeUnitLabelProvider {

    private final RenameQueryExecutor executor;
    private final Display display;
    private final TableViewer tableViewer;

    /**
     * @param tableViewer the {@link TableViewer}
     * @param executor the {@link RenameQueryExecutor} to check the renaming status concurrently
     */
    RenameLabelProvider(final TableViewer tableViewer, final RenameQueryExecutor executor) {
        this.tableViewer = tableViewer;
        this.display = tableViewer.getTable().getDisplay();
        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getText(IMergeUnit mergeUnit) {
        final Optional<RenameStatus> renameStatus = getRenameStatus(mergeUnit);
        if (renameStatus.isPresent()) {
            return renameStatus.get().toString();
        } else {
            return "...";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Image getImage(IMergeUnit mergeUnit) {
        final Optional<RenameStatus> renameStatus = getRenameStatus(mergeUnit);
        if (renameStatus.isPresent()) {
            return renameStatus.get().getImage();
        } else {
            return null;
        }
    }

    /**
     * Returns the rename status for the given {@link IMergeUnit}. The rename status is evaluated
     * concurrently, so if the status is not known at the moment {@link Optional#empty() empty} returned.
     * 
     * @param mergeUnit
     * @return
     */
    private Optional<RenameStatus> getRenameStatus(IMergeUnit mergeUnit) {
        if (executor.isResultAvailable(mergeUnit)) {
            try {
                return executor.hasRenaming(mergeUnit).get() ? Optional.of(RenameStatus.RENAME)
                        : Optional.of(RenameStatus.NOTHING);
            } catch (InterruptedException | ExecutionException e) {
                LogUtil.throwing(e);
            }
        } else {
            if (mergeUnit.getStatus() == MergeUnitStatus.TODO || mergeUnit.getStatus() == MergeUnitStatus.CANCELLED) {
                final Future<Boolean> future = executor.hasRenaming(mergeUnit);
                final Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            future.get();
                            display.asyncExec(() -> tableViewer.update(mergeUnit, null));
                        } catch (InterruptedException | ExecutionException e) {
                            LogUtil.throwing(e);
                        }
                    }
                });
                thread.start();
            }
        }
        return Optional.empty();
    }

}
