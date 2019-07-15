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
package org.aposin.mergeprocessor.model;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtil;
import org.aposin.mergeprocessor.utils.RuntimeUtil;
import org.aposin.mergeprocessor.utils.RuntimeUtil.CmdUtilException;
import org.aposin.mergeprocessor.utils.SftpUtil;
import org.aposin.mergeprocessor.utils.SftpUtilException;
import org.aposin.mergeprocessor.utils.SvnUtil;
import org.aposin.mergeprocessor.utils.SvnUtilException;
import org.aposin.mergeprocessor.view.DirectorySelectionDialog;
import org.aposin.mergeprocessor.view.Messages;
import org.aposin.mergeprocessor.view.WorkspaceMergeDialog;
import org.aposin.mergeprocessor.view.dashboard.Dashboard;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

/**
 * This class merges executes a merge task for a given merge unit.
 * 
 * @author Stefan Weiser
 *
 */
public class MergeTask {

    private static final Logger LOGGER = Logger.getLogger(MergeTask.class.getName());

    private final IMergeUnit mergeUnit;
    private final IConfiguration configuration;
    private final Dashboard shellProvider; //TODO should be IShellProvider
    private final ISvnClient svnClient;

    /**
     * @param mergeUnit the merge unit to execute
     * @param configuration the configuration
     * @param shellProvider provider providing the parent shell
     */
    public MergeTask(final IMergeUnit mergeUnit, final IConfiguration configuration, final Dashboard shellProvider) {
        this.mergeUnit = Objects.requireNonNull(mergeUnit);
        this.configuration = Objects.requireNonNull(configuration);
        this.shellProvider = Objects.requireNonNull(shellProvider);
        this.svnClient = E4CompatibilityUtil.getApplicationContext().get(ISvnClient.class);
    }

    /**
     * Executes the merge task.
     * @throws InterruptedException 
     * @throws InvocationTargetException 
     */
    public void merge() {
        final MergeUnitStatus status = mergeUnit.getStatus();
        if (status == MergeUnitStatus.DONE || status == MergeUnitStatus.IGNORED || status == MergeUnitStatus.MANUAL) {
            if (!askUserToMergeFinishedMergeUnit()) {
                return;
            }
            LOGGER.fine(() -> String.format("mergeUnit=%s will be moved from %s to %s before merge.", mergeUnit, //$NON-NLS-1$
                    status, MergeUnitStatus.TODO));
            MergeProcessorUtil.todo(mergeUnit);
        }
        try {
            if (mergeUnit instanceof SVNMergeUnit) {
                if (mergeUnit.hasRenaming()) {
                    //New Way
                    mergeIntoWorkspace((SVNMergeUnit) mergeUnit);
                } else {
                    //Old Way
                    mergeInMinimalWorkingCopy();
                }
            } else {
                //Old Way
                mergeInMinimalWorkingCopy();
            }
        } catch (InvocationTargetException | InterruptedException e) {
            LogUtil.throwing(e);
        }
    }

    /**
     * Executes the merge in a separate minimal working copy.
     * @throws InterruptedException 
     * @throws InvocationTargetException 
     * @throws SvnClientException 
     */
    private void mergeInMinimalWorkingCopy() throws InvocationTargetException, InterruptedException {
        LogUtil.entering();
        final ProgressMonitorDialog pmd = new ProgressMonitorDialog(shellProvider.getShell());
        Dashboard.setShellProgressMonitorDialog(pmd);
        pmd.run(true, true, monitor -> {
            try {
                boolean cancelled = MergeProcessorUtil.merge(pmd, monitor, configuration, mergeUnit);
                if (cancelled) {
                    mergeUnit.setStatus(MergeUnitStatus.CANCELLED);
                    MergeProcessorUtil.canceled(mergeUnit);
                } else {
                    mergeUnit.setStatus(MergeUnitStatus.DONE);
                }
            } catch (Throwable e) {
                pmd.getShell().getDisplay().syncExec(() -> {
                    MultiStatus status = createMultiStatus(e.getLocalizedMessage(), e);
                    ErrorDialog.openError(pmd.getShell(), "Error dusrching merge process",
                            "An Exception occured during the merge process. The merge didn't run successfully.",
                            status);
                });
            }

        });
        LogUtil.exiting();
    }

    private static MultiStatus createMultiStatus(String msg, Throwable t) {
        final List<Status> childStatuses = new ArrayList<>();
        final String[] stacks = ExceptionUtils.getStackFrames(t);
        for (int i = 0; i < stacks.length; i++) {
            //Filter the first stack, otherwise the Exception is listed twice in the UI.
            if (i > 0) {
                childStatuses.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, stacks[i].replace("\t", "   ")));
            }
        }
        final MultiStatus ms = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR,
                childStatuses.toArray(new Status[] {}), t.toString(), t);
        return ms;
    }

    /**
     * Asks the user if an already finished merge unit should really be executed.
     * 
     * @return {@code true} if an already finished merge unit should really be executed 
     */
    private boolean askUserToMergeFinishedMergeUnit() {
        final MergeUnitStatus status = mergeUnit.getStatus();
        String dialogMessage = NLS.bind(Messages.View_MergeSelection_Ignored_Todo_Question, mergeUnit.getFileName(),
                status);
        MessageDialog dialog = new MessageDialog(shellProvider.getShell(),
                Messages.View_MergeSelection_Ignored_Todo_Title, null, dialogMessage, MessageDialog.INFORMATION,
                new String[] { Messages.View_MergeSelection_Ignored_Todo_Yes,
                        Messages.View_MergeSelection_Ignored_Todo_No },
                0);

        int choice = dialog.open();

        if (choice != 0) {
            //user didn't say 'yes' so we skip it...
            LOGGER.fine(() -> String.format("User skipped mergeUnit=%s with status %s.", mergeUnit, status));
            return false;
        }
        return true;
    }

    /**
     * Executes the merge into user selected repository, where the commit is not done automatically.
     * Instead the user has the possibility to review the merge result before is gets commited.
     * 
     * @param mergeUnit
     */
    private void mergeIntoWorkspace(SVNMergeUnit mergeUnit) {
        final DirectorySelectionDialog dialog = createRepositorySelectionDialog(mergeUnit);
        switch (dialog.open()) {
            case Dialog.OK:
                final Path repositoryPath = dialog.getSelectedPath();
                configuration.setLastRepositoryPath(repositoryPath);
                final WorkspaceMergeDialog mergeDialog = createWorkspaceMergeDialog(repositoryPath);
                try {
                    mergeDialog.run(true, false, monitor -> {
                        boolean retry = true;
                        while (retry) {
                            final Display display = shellProvider.getShell().getDisplay();
                            try {
                                final SVNMergeUnit mergeUnit1 = mergeUnit;
                                final Consumer<String> commandConsumer = text -> display.syncExec(
                                        () -> mergeDialog.getCommandLineText().append(text + System.lineSeparator()));
                                final List<String> warnings = MergeProcessorUtil.merge(monitor, commandConsumer,
                                        mergeUnit1, repositoryPath);
                                monitor.beginTask("Checking repository", 1);
                                monitor.subTask("");
                                if (svnClient.hasConflicts(dialog.getSelectedPath())) {
                                    monitor.beginTask(
                                            "Conflicts identified. Commiting not possible till they are resolved.", 0);
                                    display.syncExec(mergeDialog::setStatusError);
                                } else {
                                    monitor.beginTask("Finished", 0);
                                }
                                display.syncExec(() -> mergeDialog.setWarnings(warnings));
                                retry = false;
                            } catch (SvnUtilException | SvnClientException e) {
                                LOGGER.log(Level.SEVERE, "Could not execute merge correctly.", e); //$NON-NLS-1$
                                retry = MergeProcessorUtil.bugUserToFixProblem("Could not execute merge correctly.",
                                        ExceptionUtils.getStackTrace(e));
                                if (!retry) {
                                    display.syncExec(mergeDialog::close);
                                }
                            }
                        }
                    });
                } catch (InvocationTargetException | InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Caught Exception while processing merge.", e); //$NON-NLS-1$
                }
                break;
            case Dialog.CANCEL:
            default:
                //Do nothing
                break;
        }
    }

    /**
     * Opens an Eclipse workspace to review the changes of the merge.
     */
    private void openWorkspace() {
        final DirectorySelectionDialog dialog = createWorkspaceSelectionDialog();
        switch (dialog.open()) {
            case Dialog.OK:
                final Path workspacePath = dialog.getSelectedPath();
                configuration.setLastEclipseWorkspacePath(workspacePath);

                final String eclipseApp = configuration.getEclipseApplicationPath().toString();
                final String workspaceLocation = "-data " + workspacePath.toString();
                final String otherParameters = configuration.getEclipseApplicationParameters();

                final String command = eclipseApp + ' ' + workspaceLocation + ' ' + otherParameters;
                try {
                    RuntimeUtil.exec(command);
                } catch (CmdUtilException e) {
                    LogUtil.throwing(e);
                }
                break;
            case Dialog.CANCEL:
            default:
                //Do nothing
                break;
        }

    }

    /**
     * Commits the changes in the given repository.
     * 
     * @param repositoryPath
     * @param mergeUnit
     * @param shellProvider
     * @return {@code true} if the changes in the given repository were commited successfully
     */
    private boolean commit(final Path repositoryPath, String message, final IShellProvider shellProvider) {
        if (StringUtils.isEmpty(message)) {
            throw new IllegalArgumentException("The commit message must not be empty.");
        }
        try {
            if (svnClient.hasConflicts(repositoryPath)) {
                shellProvider.getShell().getDisplay().syncExec(() -> {
                    MessageBox messageBox = new MessageBox(shellProvider.getShell().getDisplay().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
                    messageBox.setText("Conflicts");
                    messageBox.setMessage("There are still conflicts in the repository. Commit not possible.");
                    messageBox.open();
                });
                return false;
            }
            svnClient.commit(repositoryPath, message);
            return true;
        } catch (SvnClientException e) {
            LOGGER.log(Level.SEVERE, String.format("Could not commit repository '%s'", repositoryPath), e);
        }
        return false;
    }

    /**
     * @return the commit message for the merge unit or {@code null} if the commit message could not be build.
     */
    private Optional<String> getCommitMessage() {
        if (mergeUnit instanceof SVNMergeUnit) {
            final String message = ((SVNMergeUnit) mergeUnit).getMessage();
            return Optional.ofNullable(message);
        } else {
            throw new UnsupportedVersionControlSystemSupportException();
        }
    }

    /**
     * Creates the {@link Dialog} for selecting the workspace.
     * 
     * @return the dialog
     */
    private DirectorySelectionDialog createWorkspaceSelectionDialog() {
        final DirectorySelectionDialog dialog = new DirectorySelectionDialog(shellProvider.getShell());
        dialog.setTitle("Select an Eclipse Workspace");
        dialog.setMessage(
                "Select the Eclipse workspace to review the changes from the merge. Notice that no project imports are done. The workspace must already be set up.");
        dialog.setDirectoryDialogTitle("Select Eclipse workspace directory");
        dialog.setDirectoryDialogMessage(
                "Select the eclipse workspace directory to open. Notice that for the selected directory no project imports are done. The workspace must already be set up.");
        dialog.setDirectoryDialogFilterPath("C:/dev/eclipseworkspaces");
        dialog.setSelectedPath(configuration.getLastEclipseWorkspacePath());
        return dialog;
    }

    /**
     * Creates the {@link Dialog} for selecting the repository.
     * 
     * @param mergeUnit the {@link SVNMergeUnit} to merge
     * @return the dialog
     */
    private DirectorySelectionDialog createRepositorySelectionDialog(SVNMergeUnit mergeUnit) {
        final DirectorySelectionDialog dialog = new DirectorySelectionDialog(shellProvider.getShell());
        dialog.setMergeFrom(mergeUnit.getBranchSource());
        dialog.setMergeTo(mergeUnit.getBranchTarget());
        dialog.setTitle("Select a repository to merge into");
        dialog.setMessage(
                "Select the repository where changes should be merged into. Notice that the files to merge has to exist in the repository in the correct tree hierarchy.");
        dialog.setDirectoryDialogTitle("Select SVN Repository");
        dialog.setDirectoryDialogMessage(
                "Select the repository where the merge should be done. Notify that the working copy requires the hierarchy of the repository to work correctly.");
        dialog.setDirectoryDialogFilterPath("C:/dev/repositories");
        dialog.setPathValidationFunction(path -> checkRepositoryAgainstBranch(path, mergeUnit));
        dialog.setSelectedPath(configuration.getLastRepositoryPath());
        return dialog;
    }

    /**
     * Checks, if the given repository path matches with the target branch of the merge unit.
     * 
     * @param path the repository path
     * @param mergeUnit the merge unit to match
     * @return {@code true} if the given repository path matches with the target branch of the merge unit
     */
    private boolean checkRepositoryAgainstBranch(final Path path, final SVNMergeUnit mergeUnit) {
        try {
            final URL repository = svnClient.getSvnUrl(path);
            return repository.toURI().equals(new URI(mergeUnit.getUrlTarget()));
        } catch (SvnClientException | URISyntaxException e) {
            Logger.getLogger(DirectorySelectionDialog.class.getName()).log(Level.SEVERE,
                    "Exception on checking repository against the target branch", e); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * Creates the {@link WorkspaceMergeDialog}.
     * 
     * @param repositoryPath the repository path where the merge takes place
     * @return the dialog
     */
    private WorkspaceMergeDialog createWorkspaceMergeDialog(final Path repositoryPath) {
        final WorkspaceMergeDialog dialog = new WorkspaceMergeDialog(shellProvider.getShell());
        try {
            dialog.setConfirmCommit(svnClient.hasModifications(repositoryPath));
        } catch (SvnClientException e1) {
            LogUtil.throwing(e1);
        }
        Dashboard.setShellProgressMonitorDialog(dialog);
        final String commitMessage = getCommitMessage().orElse("<Error on building commit message>");
        dialog.getCommitMessageText().setText(commitMessage);
        dialog.getOpenButton().addListener(SWT.Selection, e -> openWorkspace());
        dialog.getOpenWorkingCopyButton().addListener(SWT.Selection,
                e -> MergeProcessorUtil.openFile(repositoryPath.toString()));
        dialog.getOpenTortoiseSvnButton().addListener(SWT.Selection, e -> {
            try {
                SvnUtil.openTortoiseSVN(repositoryPath.toString());
            } catch (SvnUtilException exception) {
                LogUtil.throwing(exception);
            }
        });
        dialog.getCloseButton().addListener(SWT.Selection, e -> {
            if (MessageDialog.openConfirm(dialog.getShell(), "Commit has to be done manually.",
                    "Do you really want to close the merge? The commit will have to be done manually.")) {
                try {
                    SftpUtil.getInstance().moveMergeUnitFromRemoteToManual(mergeUnit);
                } catch (SftpUtilException exception) {
                    LOGGER.log(Level.SEVERE, "Could not move merge unit to manual.", exception); //$NON-NLS-1$
                }
                dialog.closeDialog();
                shellProvider.refresh();
            }
        });
        dialog.getCommitButton().addListener(SWT.Selection, e -> {
            if (commit(repositoryPath, commitMessage, dialog)) {
                try {
                    SftpUtil.getInstance().moveMergeUnitFromRemoteToDone(mergeUnit);
                } catch (SftpUtilException exception) {
                    LOGGER.log(Level.SEVERE, "Could not move merge unit to done.", exception); //$NON-NLS-1$
                }
                dialog.closeDialog();
                shellProvider.refresh();
            }
        });
        return dialog;
    }
}
