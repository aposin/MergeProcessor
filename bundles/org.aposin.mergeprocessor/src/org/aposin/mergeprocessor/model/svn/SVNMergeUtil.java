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
package org.aposin.mergeprocessor.model.svn;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aposin.mergeprocessor.application.ApplicationUtil;
import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtil;
import org.aposin.mergeprocessor.utils.Messages;
import org.aposin.mergeprocessor.utils.SftpUtil;
import org.aposin.mergeprocessor.utils.SftpUtilException;
import org.aposin.mergeprocessor.utils.SvnUtil;
import org.aposin.mergeprocessor.utils.SvnUtilException;
import org.aposin.mergeprocessor.view.MessageDialogScrollable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.osgi.util.NLS;

/**
 * Utility class for merging in SVN.
 * 
 * @author Stefan Weiser
 *
 */
public class SVNMergeUtil {

    private static final Logger LOGGER = Logger.getLogger(SVNMergeUtil.class.getName());

    private SVNMergeUtil() {
        //Only static access
    }

    /**
     * Lists all available branches for the repository of the given merge unit.
     * 
     * @param mergeUnit the merge unit
     * @return a list of all available branches
     */
    public static List<String> listBranches(SVNMergeUnit mergeUnit) {
        final String rootUrl = SvnUtil.getRepositoryRootOfUrl(mergeUnit.getUrlSource());
        try {
            final List<String> branches = SvnUtil.listDirectories(rootUrl + '/' + "branches");
            branches.add("trunk");
            Collections.reverse(branches);
            return branches;
        } catch (Exception e) {
            LogUtil.throwing(e);
            return Collections.emptyList();
        }
    }

    public static boolean merge(ProgressMonitorDialog pmd, IProgressMonitor monitor, SVNMergeUnit mergeUnit)
            throws SvnClientException {
        LogUtil.entering(pmd, monitor, mergeUnit);

        String taskName = String.format(Messages.MergeProcessorUtil_Process_TaskName, mergeUnit.getRepository(),
                mergeUnit.getRevisionInfo(), mergeUnit.getBranchSource(), mergeUnit.getBranchTarget());
        monitor.beginTask(taskName, 9);

        boolean cancel = false;
        final ISvnClient client = E4CompatibilityUtil.getApplicationContext().get(ISvnClient.class);

        if (!cancel) {
            monitor.subTask("copyRemoteToLocal"); //$NON-NLS-1$
            cancel = copyRemoteToLocal(mergeUnit);
            monitor.worked(1);
            if (monitor.isCanceled()) {
                LOGGER.fine("User cancelled after 'copyRemoteToLocal'."); //$NON-NLS-1$
                cancel = true;
            }
        }

        if (!cancel) {
            monitor.subTask("buildMinimalWorkingCopy"); //$NON-NLS-1$
            cancel = buildMinimalSVNWorkingCopy((SVNMergeUnit) mergeUnit, client);
            monitor.worked(1);
            if (monitor.isCanceled()) {
                LOGGER.fine("User cancelled after 'buildMinimalWorkingCopy'."); //$NON-NLS-1$
                cancel = true;
            }
        }

        if (!cancel) {
            monitor.subTask("mergeChangesIntoWorkingCopy"); //$NON-NLS-1$
            cancel = mergeChangesIntoWorkingCopy(mergeUnit, client);
            monitor.worked(1);
            if (monitor.isCanceled()) {
                LOGGER.fine("User cancelled after 'mergeChangesIntoWorkingCopy'."); //$NON-NLS-1$
                cancel = true;
            }
        }

        if (!cancel) {
            monitor.subTask("checkIsCommittable"); //$NON-NLS-1$
            cancel = checkIsCommittable(mergeUnit, client);
            monitor.worked(1);
            if (monitor.isCanceled()) {
                LOGGER.fine("User cancelled after 'checkIsCommittable'."); //$NON-NLS-1$
                cancel = true;
            }
        }

        if (!cancel) {
            monitor.subTask("commitChanges"); //$NON-NLS-1$

            // can't cancel after commit
            pmd.setCancelable(false);

            cancel = commitChanges(mergeUnit, client);
            monitor.worked(1);
        }

        if (!cancel) {
            monitor.subTask("copyLocalToDone"); //$NON-NLS-1$
            cancel = copyLocalToDone(mergeUnit);
            monitor.worked(1);
        }

        if (cancel) {
            mergeUnit.setStatus(MergeUnitStatus.CANCELLED);
        }

        //delete the local merge file, even if cancelled.
        monitor.subTask("deleteLocal"); //$NON-NLS-1$
        deleteLocal(mergeUnit);
        monitor.worked(1);
        return LogUtil.exiting(cancel);
    }

    private static boolean copyRemoteToLocal(SVNMergeUnit mergeUnit) {
        LogUtil.entering(mergeUnit);

        boolean cancel = false;
        while (!cancel) {
            try {
                SftpUtil.getInstance().copyMergeUnitToWork(mergeUnit);
            } catch (SftpUtilException e) {
                LOGGER.log(Level.WARNING, "Caught exception while copying to work.", e); //$NON-NLS-1$

                String message = Messages.MergeProcessorUtil_CopyRemoteToLocal_Error_Message;
                String messageScrollable = String.format(
                        Messages.MergeProcessorUtil_CopyRemoteToLocal_Error_MessageScrollable,
                        Configuration.getPathLocalMergeFile(mergeUnit), e.getMessage());
                if (MergeProcessorUtil.bugUserToFixProblem(message, messageScrollable)) {
                    //user wants to retry
                    LOGGER.fine(String.format("User wants to retry copying the mergeUnit=%s to working directory.", //$NON-NLS-1$
                            mergeUnit));
                    continue;
                } else {
                    //user didn't say 'retry' so we cancel the whole merge...
                    LOGGER.fine(String.format(
                            "User cancelled processing mergeUnit=%s when copying merge file to working directory.", //$NON-NLS-1$
                            mergeUnit));
                    cancel = true;
                }
            }
            break;
        }

        return LogUtil.exiting(cancel);
    }

    private static boolean buildMinimalSVNWorkingCopy(SVNMergeUnit mergeUnit, ISvnClient client) {
        LogUtil.entering(mergeUnit);

        boolean cancel = false;
        while (!cancel) {
            try {
                cancel = SvnUtil.buildMinimalWorkingCopy(mergeUnit, client);
                if (cancel) {
                    LOGGER.fine(() -> String.format("User cancelled building minimal working copy for mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                }
            } catch (SvnUtilException e) {
                LOGGER.log(Level.WARNING, "Caught exception while building minimal working copy.", e); //$NON-NLS-1$
                String message = Messages.MergeProcessorUtil_BuildMinimalWorkingCopy_Error_Message;
                String messageScrollable = String.format(
                        Messages.MergeProcessorUtil_BuildMinimalWorkingCopy_Error_MessageScrollable_Prefix,
                        ExceptionUtils.getStackTrace(e));
                if (MergeProcessorUtil.bugUserToFixProblem(message, messageScrollable)) {
                    //user wants to retry
                    LOGGER.fine(String.format("User wants to retry building the minimal working copy for mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                    continue;
                } else {
                    //user didn't say 'retry' so we cancel the whole merge...
                    LOGGER.fine(
                            String.format("User cancelled building minimal working copy for mergeUnit=%s.", mergeUnit)); //$NON-NLS-1$
                    cancel = true;
                }
            }
            break;
        }

        return LogUtil.exiting(cancel);
    }

    private static boolean mergeChangesIntoWorkingCopy(final SVNMergeUnit mergeUnit, final ISvnClient client) {
        LogUtil.entering(mergeUnit);

        boolean cancel = false;
        boolean success = false;
        while (!cancel && !success) {
            try {
                SvnUtil.mergeChanges(mergeUnit, client);
                success = true;
            } catch (SvnUtilException e) {
                LOGGER.log(Level.WARNING, "Caught exception while merging changes into working copy.", e); //$NON-NLS-1$
                String messageScrollable = NLS.bind(
                        Messages.MergeProcessorUtil_MergeChangesIntoWorkingCopy_Error_Message_Prefix, e.getMessage());
                if (MergeProcessorUtil.bugUserToFixProblem(
                        Messages.MergeProcessorUtil_MergeChangesIntoWorkingCopy_Error_Title, messageScrollable)) {
                    //user wants to retry
                    LOGGER.fine(String.format("User wants to retry merging changes into working copy for mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                    continue;
                } else {
                    //user didn't say 'retry' so we cancel the whole merge...
                    LOGGER.fine(String.format("User cancelled merging changes into working copy for mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                    cancel = true;
                }
            }
        }

        if (!success) {
            //if we haven't had success we cancel. we can't continue without success.
            cancel = true;
        }

        return LogUtil.exiting(cancel);
    }

    private static boolean commitChanges(SVNMergeUnit mergeUnit, final ISvnClient client) {
        LogUtil.entering(mergeUnit);
        boolean cancel = false;
        while (!cancel) {
            try {
                SvnUtil.commitChanges(mergeUnit, client);
                break;
            } catch (SvnUtilException e) {
                LOGGER.log(Level.FINE, "Caught exception while committing changes.", e); //$NON-NLS-1$

                String[] conflicts;
                try {
                    conflicts = SvnUtil.conflictsOfWorkingCopy(client);
                } catch (SvnUtilException e2) {
                    LOGGER.log(Level.FINE, "Caught exception checking working copy for conflicts.", e2); //$NON-NLS-1$
                    String messageScrollable = NLS.bind(
                            Messages.MergeProcessorUtil_MergeProcessorUtil_CommitChanges_Conflicts_Error_Message,
                            ExceptionUtils.getStackTrace(e2), ExceptionUtils.getStackTrace(e));
                    if (MergeProcessorUtil.bugUserToFixProblem(
                            Messages.MergeProcessorUtil_CommitChanges_Conflicts_Error_Title, messageScrollable)) {
                        //user wants to retry
                        LOGGER.fine(String.format(
                                "User wants to retry checking the working copy for conflicts. mergeUnit=%s.", //$NON-NLS-1$
                                mergeUnit));
                        continue;
                    } else {
                        //user didn't say 'retry' so we cancel the whole merge...
                        LOGGER.fine(
                                String.format("User cancelled checking the working copy for conflicts. mergeUnit=%s.", //$NON-NLS-1$
                                        mergeUnit));
                        cancel = true;
                        break;
                    }
                }

                boolean isUpdateRequired = false;
                {
                    String[] lines = ExceptionUtils.getStackTrace(e).split("\n"); //$NON-NLS-1$
                    for (String line : lines) {
                        line = line.trim();
                        if (line.endsWith("' is out of date") || line.endsWith("' is out of date; try updating") //$NON-NLS-1$ //$NON-NLS-2$
                                || line.endsWith("resource out of date; try updating")) { //$NON-NLS-1$
                            isUpdateRequired = true;
                            break;
                        }
                    }
                }

                if (isUpdateRequired) {
                    LOGGER.fine("Working copy requires an update to commit the changes."); //$NON-NLS-1$
                    try {
                        SvnUtil.update(client);
                    } catch (SvnUtilException e1) {
                        LOGGER.log(Level.FINE, "Caught exception while updating working copy.", e1); //$NON-NLS-1$

                        String messageScrollable = NLS.bind(
                                Messages.MergeProcessorUtil_MergeProcessorUtil_CommitChanges_Update_Error_Details,
                                ExceptionUtils.getStackTrace(e), ExceptionUtils.getStackTrace(e1));
                        if (MergeProcessorUtil.bugUserToFixProblem(
                                Messages.MergeProcessorUtil_CommitChanges_Update_Error_Title, messageScrollable)) {
                            //user wants to retry
                            LOGGER.fine(String.format(
                                    "User wants to retry updating the working copy before committing. mergeUnit=%s.", //$NON-NLS-1$
                                    mergeUnit));
                            continue;
                        } else {
                            //user didn't say 'retry' so we cancel the whole merge...
                            LOGGER.fine(String.format(
                                    "User cancelled updating the working copy before committing. mergeUnit=%s.", //$NON-NLS-1$
                                    mergeUnit));
                            cancel = true;
                        }
                    }
                } else if (conflicts.length > 0) {
                    LOGGER.fine("Working copy has conflicts."); //$NON-NLS-1$
                    cancel = checkIsCommittable(mergeUnit, client);
                } else {
                    String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_CommitChanges_Commit_Error_Message,
                            e.getMessage(), ExceptionUtils.getStackTrace(e));
                    if (MergeProcessorUtil.bugUserToFixProblem(
                            Messages.MergeProcessorUtil_CommitChanges_Commit_Error_Title, messageScrollable)) {
                        LOGGER.fine(String.format(
                                "User wants to retry committing changes from the working copy. mergeUnit=%s.", //$NON-NLS-1$
                                mergeUnit));
                        continue;
                    } else {
                        LOGGER.fine(
                                String.format("User cancelled committing changes from the working copy. mergeUnit=%s.", //$NON-NLS-1$
                                        mergeUnit));
                        cancel = true;
                    }
                }
            }
        }

        return LogUtil.exiting(cancel);
    }

    private static boolean checkIsCommittable(final SVNMergeUnit mergeUnit, final ISvnClient client) {
        LogUtil.entering(mergeUnit);

        boolean cancel = false;
        String[] conflicts = null;
        CONFLICTS: while (!cancel) {
            try {
                conflicts = SvnUtil.conflictsOfWorkingCopy(client);
            } catch (SvnUtilException e) {
                LOGGER.log(Level.WARNING, "Caught exception while checking if working copy is in a committable state.", //$NON-NLS-1$
                        e);

                String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_CheckIsCommittable_Error_Message_Prefix,
                        ExceptionUtils.getStackTrace(e));
                if (MergeProcessorUtil.bugUserToFixProblem(Messages.MergeProcessorUtil_CheckIsCommittable_Error_Title,
                        messageScrollable)) {
                    //user wants to retry
                    LOGGER.fine(
                            String.format("User wants to retry checking the working copy for problems. mergeUnit=%s.", //$NON-NLS-1$
                                    mergeUnit));
                    continue;
                } else {
                    //user didn't say 'retry' so we cancel the whole merge...
                    LOGGER.fine(String.format("User cancelled checking working copy for problems. mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                    cancel = true;
                }
            }

            if (conflicts == null || conflicts.length == 0) {
                LOGGER.fine("Workspace has no conflicts."); //$NON-NLS-1$
                break;
            } else {
                int choice = 1;
                while (choice == 1) {
                    final String stringConflicts;
                    {
                        StringBuilder sbConflicts = new StringBuilder(
                                Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_Prefix);
                        for (String conflict : conflicts) {
                            sbConflicts.append('\t' + conflict + '\n');
                        }
                        stringConflicts = sbConflicts.toString();
                    }

                    final String[] choices = new String[] { Choices.CANCEL.name, Choices.RETRY.name,
                            Choices.OPEN_WORKING_COPY.name, Choices.OPEN_TORTOISE_SVN.name };
                    {
                        final AtomicInteger retVal = new AtomicInteger();

                        String dialogTitle = Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_Title;
                        String dialogMessage = String.format(
                                Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_DialogMessage,
                                mergeUnit.getRevisionInfo(), mergeUnit.getBranchSource(), mergeUnit.getBranchTarget());

                        E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class).syncExec(() -> {
                            MessageDialogScrollable dialog = new MessageDialogScrollable(
                                    ApplicationUtil.getApplicationShell(), dialogTitle, null, dialogMessage,
                                    stringConflicts, MessageDialogScrollable.INFORMATION, choices, 0);

                            retVal.set(dialog.open());
                        });
                        choice = retVal.get();
                    }

                    switch (Choices.forName(choices[choice])) {
                        case CANCEL:
                            //cancel the whole merge
                            LOGGER.fine("User cancelled checking working copy for problems."); //$NON-NLS-1$
                            cancel = true;
                            break;
                        case OPEN_WORKING_COPY:
                            LOGGER.fine(() -> String.format("User wants to open the working copy=%s.", //$NON-NLS-1$
                                    Configuration.getPathSvnWorkingCopy()));
                            MergeProcessorUtil.openFile(Configuration.getPathSvnWorkingCopy());
                            break;
                        case OPEN_TORTOISE_SVN:
                            //Open TortoiseSVN
                            LOGGER.fine("User wants to open TortoiseSVN."); //$NON-NLS-1$
                            try {
                                SvnUtil.openTortoiseSVN(Configuration.getPathSvnWorkingCopy());
                            } catch (final SvnUtilException e) {
                                LOGGER.log(Level.SEVERE, "Caught exception while opening TortoiseSVN.", e); //$NON-NLS-1$
                                E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class).syncExec(() -> {
                                    String message = NLS.bind(
                                            Messages.MergeProcessorUtil_CheckIsCommittable_TortoiseSvn_Error_Message_Prefix,
                                            ExceptionUtils.getStackTrace(e));
                                    MessageDialogScrollable.openError(ApplicationUtil.getApplicationShell(),
                                            Messages.MergeProcessorUtil_CheckIsCommittable_TortoiseSvn_Error_Title,
                                            message);
                                });
                            }
                            break;
                        case RETRY:
                            LOGGER.fine("User wants to repeat checking working copy for problems."); //$NON-NLS-1$
                            //repeat check.
                            break CONFLICTS;
                        default:
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine(String.format("Unknown choice. Repeating. choices=%s", //$NON-NLS-1$
                                        choices[choice]));
                            }
                            //repeat check.
                            break CONFLICTS;
                    }
                }
            }
        }

        return LogUtil.exiting(cancel);
    }

    public static boolean copyLocalToDone(SVNMergeUnit mergeUnit) {
        LogUtil.entering(mergeUnit);

        boolean cancel = false;
        while (!cancel) {
            try {
                SftpUtil.getInstance().copyMergeUnitFromWorkToDoneAndDeleteInTodo(mergeUnit);
                mergeUnit.setStatus(MergeUnitStatus.DONE);
                break;
            } catch (SftpUtilException e) {
                String logMessage = String.format("Caught exception while copying from work to done. mergeUnit=[%s]", //$NON-NLS-1$
                        mergeUnit);
                LOGGER.log(Level.WARNING, logMessage, e);

                String message = NLS.bind(Messages.MergeProcessorUtil_CopyLocalToDone_Error_Message, Choices.CANCEL,
                        MergeUnitStatus.TODO);
                String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_CopyLocalToDone_Error_Details,
                        e.getMessage());
                if (MergeProcessorUtil.bugUserToFixProblem(message, messageScrollable)) {
                    //user wants to retry
                    LOGGER.fine(String.format("User wants to retry copying the merge file to the server. mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                    continue;
                } else {
                    //user didn't say 'retry' so we cancel the whole merge...
                    LOGGER.fine(String.format("User cancelled committing changes from the working copy. mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                    cancel = true;
                }
            }
        }

        return LogUtil.exiting(cancel);
    }

    private static void deleteLocal(SVNMergeUnit mergeUnit) {
        LogUtil.entering(mergeUnit);

        File file = new File(Configuration.getPathLocalMergeFile(mergeUnit));
        LOGGER.fine(() -> String.format("Deleting local merge file=%s.", file.getAbsolutePath())); //$NON-NLS-1$
        boolean success = !file.exists();
        while (!success) {
            success = file.delete();
            if (!success) {
                String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_DeleteLocal_Error_Message,
                        file.getAbsolutePath());
                if (MergeProcessorUtil.bugUserToFixProblem(Messages.MergeProcessorUtil_DeleteLocal_Error_Title,
                        messageScrollable)) {
                    LOGGER.fine(() -> String.format("User wants to retry deleting local merge file. mergeUnit=%s.", //$NON-NLS-1$
                            mergeUnit));
                } else {
                    LOGGER.fine(
                            () -> String.format("User cancelled deleting local merge file. mergeUnit=%s.", mergeUnit)); //$NON-NLS-1$
                    success = true;
                }
            }
        }
        LogUtil.exiting();
    }

    /**
     * Enum indicating the user choice when doing merge.
     */
    private enum Choices {
        CANCEL(Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_Cancel), RETRY(
                Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_Retry), OPEN_WORKING_COPY(
                        Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_OpenWorkingCopy), OPEN_TORTOISE_SVN(
                                Messages.MergeProcessorUtil_CheckIsCommittable_Conflicts_OpenTortoiseSvn);

        private final String name;

        private Choices(String name) {
            this.name = name;
        }

        private static Choices forName(String name) {
            return Arrays.stream(Choices.values()).filter((choice) -> choice.name.equals(name)).findFirst().orElseThrow(
                    () -> new RuntimeException(NLS.bind(Messages.MergeProcessorUtil_Choices_Invalid, name)));
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
