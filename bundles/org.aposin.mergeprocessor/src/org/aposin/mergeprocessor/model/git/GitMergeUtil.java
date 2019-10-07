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
/**
 * 
 */
package org.aposin.mergeprocessor.model.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aposin.mergeprocessor.application.ApplicationUtil;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.MergeUnitException;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtil;
import org.aposin.mergeprocessor.utils.SftpUtil;
import org.aposin.mergeprocessor.utils.SftpUtilException;
import org.aposin.mergeprocessor.view.MessageDialogScrollable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

/**
 * Utility class for merging in GIT.
 * 
 * @author Stefan Weiser
 *
 */
public class GitMergeUtil {

	private static final Logger LOGGER = Logger.getLogger(GitMergeUtil.class.getName());

	/**
	 * Enum representing the user handling on an occuring exception.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private enum UserHandling {
		/** Retry the merge */
		RETRY(Messages.GitMergeUtil_retry),
		/** Revert any changes in the repository and retry the merge */
		REVERT_AND_RETRY(Messages.GitMergeUtil_revertAndRetry),
		/** Cancel the merge */
		CANCEL(Messages.GitMergeUtil_cancel),
		/** Open the repository */
		OPEN_REPOSITORY("Open Repository");

		private final String label;

		private UserHandling(final String label) {
			this.label = label;
		}

		private String getLabel() {
			return label;
		}
	}

	private GitMergeUtil() {
		// Utility class
	}

	/**
	 * Lists all remote branches available in the git repository of the given merge
	 * unit. If the repository is not cloned locally an empty list is removed.
	 * 
	 * @param configuration the configuration
	 * @param mergeUnit     the merge unit for which repository the remote branches
	 *                      should be listed
	 * @return the list of existing remote branches or an empty list if the remote
	 *         branches could not be listed
	 */
	public static List<String> listRemoteBranches(IConfiguration configuration, GITMergeUnit mergeUnit) {
		final String pathSvnkitFolder = configuration.getGitRepositoryFolder();
		final String repository = mergeUnit.getRepository();
		final String localRepository;
		if (repository.lastIndexOf('/') > -1) {
			localRepository = pathSvnkitFolder + File.separator
					+ repository.substring(repository.lastIndexOf('/') + 1, repository.length() - 4);
		} else {
			localRepository = pathSvnkitFolder + File.separator + repository;
		}
		final File file = Paths.get(localRepository).toFile();
		if (file.exists()) {
			try (final Git git = Git.open(file)) {
				final List<Ref> result = git.branchList().setListMode(ListMode.REMOTE).call();
				final List<String> branchList = result.stream().map(ref -> ref.getName().replace("refs/", ""))
						.collect(Collectors.toList());
				Collections.reverse(branchList);
				return branchList;
			} catch (IOException | GitAPIException e) {
				LOGGER.log(Level.SEVERE, "Cannot list remove branches of the git repository.", e); //$NON-NLS-1$
			}
		} else {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info(String.format("Local git repository '%s' not cloned.", file));
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Merges the given {@link GITMergeUnit}.
	 * 
	 * @param pmd           the progress monitor dialog
	 * @param monitor       the progress monitor
	 * @param configuration the configuration
	 * @param mergeUnit     the {@link GITMergeUnit}
	 * @return {@code true} if the merge was canceled
	 * @throws MergeUnitException
	 */
	public static boolean merge(ProgressMonitorDialog pmd, IProgressMonitor monitor, IConfiguration configuration,
			GITMergeUnit mergeUnit) {
		LogUtil.entering(pmd, monitor, configuration, mergeUnit);
		boolean retry = true;
		boolean canceled = false;
		while (retry) {
			retry = false;
			monitor.beginTask(Messages.GitMergeUtil_mergeGitMergeUnit, 10);
			try (final GitMergeUnitProcessor processor = new GitMergeUnitProcessor(mergeUnit, configuration)) {
				try {
					processor.run(pmd, monitor);
				} catch (MergeUnitException | SftpUtilException e) {
					switch (handleByUser(e)) {
					case RETRY:
						retry = true;
						break;
					case REVERT_AND_RETRY:
						processor.revert();
						retry = true;
						break;
					default:
						canceled = true;
						break;
					}
				} catch (MergeCancelException e) {
					canceled = true;
				}
			} catch (IOException e1) {
				E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class).syncExec(
						new GitMergeErrorDialog(e1, new String[] { UserHandling.CANCEL.getLabel() }, 0)::open);
				canceled = true;
			}
		}
		return LogUtil.exiting(canceled);
	}

	/**
	 * Opens a dialog asking the user what to do.
	 * 
	 * @param e the exception to report to the user
	 * @return the selection of the user how to continue
	 */
	private static UserHandling handleByUser(final Exception e) {
		final AtomicInteger result = new AtomicInteger();
		E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class).syncExec(() -> {
			final String[] buttons = { UserHandling.RETRY.getLabel(), UserHandling.REVERT_AND_RETRY.getLabel(),
					UserHandling.CANCEL.getLabel() };
			final GitMergeErrorDialog dialog = new GitMergeErrorDialog(e, buttons, 0);
			result.set(dialog.open());
		});
		switch (result.get()) {
		case 0:
			return UserHandling.RETRY;
		case 1:
			return UserHandling.REVERT_AND_RETRY;
		case 2:
		default:
			return UserHandling.CANCEL;
		}
	}

	/**
	 * Dialog for showing a git merge error.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class GitMergeErrorDialog extends MessageDialogScrollable {

		/**
		 * @param e                  the exception to show to the user
		 * @param dialogButtonLabels the button available for user selection to continue
		 * @param defaultIndex       the default selection index
		 */
		public GitMergeErrorDialog(final Exception e, String[] dialogButtonLabels, int defaultIndex) {
			super(ApplicationUtil.getApplicationShell(), Messages.GitMergeUtil_mergeErrorTitle, null,
					Messages.GitMergeUtil_mergeErrorMessage,
					Messages.GitMergeUtil_mergeErrorDetailMessage + ExceptionUtils.getMessage(e) + '\n'
							+ ExceptionUtils.getStackTrace(e),
					MessageDialogScrollable.ERROR, dialogButtonLabels, defaultIndex);
		}

	}

	/**
	 * Thrown when the merge was canceled.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class MergeCancelException extends Exception {

		private static final long serialVersionUID = 1L;

		MergeCancelException() {
			super();
		}

	}

	/**
	 * Process which executes the merge process for the given merge unit.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class GitMergeUnitProcessor implements AutoCloseable {

		private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

		private final GITMergeUnit mergeUnit;
		private final IConfiguration configuration;

		private Path repoPath;
		private Git repo;
		private MergeUnitException exception;
		private IProgressMonitor monitor;
		private String commitMessage;

		/**
		 * @param mergeUnit the merge unit to process
		 */
		private GitMergeUnitProcessor(final GITMergeUnit mergeUnit, final IConfiguration configuration) {
			this.mergeUnit = mergeUnit;
			this.configuration = configuration;
		}

		/**
		 * {@code git fetch} {@code git reset --hard origin}
		 */
		private void revert() {
			try {
				repo.fetch().call();
				repo.reset().setMode(ResetType.HARD).setRef(mergeUnit.getBranchTarget()).call();
			} catch (GitAPIException e) {
				LOGGER.log(Level.SEVERE, String.format("Could no revert repository '%s'.", repoPath), e); //$NON-NLS-1$
			}
		}

		/**
		 * Run the processor.
		 * 
		 * @param pmd     the monitor dialog
		 * @param monitor the process monitor
		 * @throws MergeUnitException
		 * @throws SftpUtilException
		 * @throws MergeCancelException
		 */
		private void run(final ProgressMonitorDialog pmd, final IProgressMonitor monitor)
				throws MergeUnitException, SftpUtilException, MergeCancelException {
			this.monitor = monitor;
			run(Messages.GitMergeUtil_createRepositoryDirectory, this::createLocalRepositoryIfNotExisting);
			run(Messages.GitMergeUtil_clone, this::cloneRepositoryIfNotExisting);

			run(Messages.GitMergeUtil_pull, this::pull);
			run(Messages.GitMergeUtil_evaluteCommitMessage, this::evaluteCommitMessage);
			run(Messages.GitMergeUtil_checkoutBranch, this::checkout);
			run(Messages.GitMergeUtil_checkStatus, this::checkStatus);
			run(Messages.GitMergeUtil_cherryPick, this::cherryPick);
			run(Messages.GitMergeUtil_commit, this::commit);

			// When pushed, no way of return
			pmd.setCancelable(false);
			run(Messages.GitMergeUtil_push, this::push);

			monitor.subTask(Messages.GitMergeUtil_moveMergeUnit);
			SftpUtil.getInstance().moveMergeUnitFromRemoteToDone(mergeUnit);
			monitor.worked(1);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws IOException {
			repoPath = null;
			repo.close();
			repo = null;
			exception = null;
			this.monitor = null;
		}

		/**
		 * @param taskName the task name to set into the monitor
		 * @param runnable the runnable to execute
		 * @throws MergeUnitException   thrown when {@link #exception} is available
		 * @throws MergeCancelException thrown when canceled by the user
		 */
		private void run(final String taskName, final Runnable runnable)
				throws MergeUnitException, MergeCancelException {
			monitor.subTask(taskName);
			runnable.run();
			if (exception != null) {
				throw exception;
			}
			if (monitor.isCanceled()) {
				throw new MergeCancelException();
			}
			monitor.worked(1);
		}

		/**
		 * Creates a local repository for the given merge unit, if it does not exist.
		 */
		private void createLocalRepositoryIfNotExisting() {
			final String repository = mergeUnit.getRepository();
			final Matcher matcher = Pattern.compile("\\/[^\\/]*\\.git$").matcher(mergeUnit.getRepository()); //$NON-NLS-1$
			if (matcher.find()) {
				final String result = matcher.group();
				if (matcher.find()) {
					exception = new MergeUnitException(String.format("Unknown repository format: '%s'", repository)); //$NON-NLS-1$
				} else {
					final String pathSvnkitFolder = configuration.getGitRepositoryFolder();
					repoPath = Paths.get(pathSvnkitFolder, result.substring(1, result.length() - 4));
					if (!repoPath.toFile().exists()) {
						try {
							Files.createDirectories(repoPath);
							LOGGER.info(() -> String.format("Directory '%s' created.", repoPath)); //$NON-NLS-1$
						} catch (IOException e) {
							exception = new MergeUnitException(
									String.format("Directory '%s' could not be created.", repoPath), e); //$NON-NLS-1$
						}
					}
				}
			} else {
				exception = new MergeUnitException(String.format("Unknown repository format: '%s'", repository)); //$NON-NLS-1$
			}
		}

		/**
		 * {@code git clone} (only if required)
		 */
		private void cloneRepositoryIfNotExisting() {
			final String repository = Objects.requireNonNull(mergeUnit).getRepository();
			try {
				repo = Git.open(repoPath.toFile());
			} catch (IOException e) {
				// Does not exist -> Clone it
				try {
					repo = Git.cloneRepository().setURI(repository).setDirectory(repoPath.toFile()).call();
					LOGGER.info(String.format("Cloned repository '%s' to '%s'.", repository, repoPath)); //$NON-NLS-1$
				} catch (GitAPIException e1) {
					exception = new MergeUnitException(String.format("Could not clone the repository '%s'", repository), //$NON-NLS-1$
							e1);
				}
			}
		}

		/**
		 * {@code git pull}
		 */
		private void pull() {
			try {
				final PullResult call = repo.pull().call();
				if (!call.isSuccessful()) {
					exception = new MergeUnitException(
							String.format("Could not pull from remote repository '%s'", repo)); //$NON-NLS-1$
				}
			} catch (GitAPIException e) {
				exception = new MergeUnitException(String.format("Could not push the local repository '%s'.", repo), e); //$NON-NLS-1$
			}
		}

		/**
		 * Evaluate the commit message for the merge unit.
		 */
		private void evaluteCommitMessage() {
			final String localBranch = getLocalSourceBranch();
			if (localBranch == null) {
				return;
			}
			try {
				final List<Ref> branchListResult = repo.branchList().call();
				final boolean localBranchExists = branchListResult.stream()
						.anyMatch(ref -> ref.getName().endsWith(localBranch));
				CheckoutCommand branchCmd = repo.checkout().setName(localBranch);
				if (!localBranchExists) {
					branchCmd = branchCmd.setCreateBranch(true).setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
							.setStartPoint(mergeUnit.getBranchSource());
				}
				branchCmd.call();

				final ObjectId id = ObjectId.fromString(mergeUnit.getRevisionInfo());
				commitMessage = repo.log().add(id).call().iterator().next().getFullMessage();
				/*
				 * Remove merge processor commit information, if the commmit is done by merge
				 * processor e.g. MP [29c64cf540d8f3ea116c3683eade862c3d996dfb] V1.1 -> master:
				 * (2018-03-06 12:17:22)
				 */
				commitMessage = commitMessage.replaceAll("MP \\[[A-Za-z0-9]*\\] .* -> .*: \\(.*\\) ", ""); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (GitAPIException | MissingObjectException | IncorrectObjectTypeException e) {
				exception = new MergeUnitException(String.format("Could not checkout branch '%s'", localBranch), e); //$NON-NLS-1$
			}
		}

		/**
		 * {@code git checkout}
		 */
		private void checkout() {
			final String localBranch = getLocalTargetBranch();
			if (localBranch == null) {
				return;
			}
			try {
				final List<Ref> branchListResult = repo.branchList().call();
				final boolean localBranchExists = branchListResult.stream()
						.anyMatch(ref -> ref.getName().endsWith(localBranch));
				CheckoutCommand branchCmd = repo.checkout().setName(localBranch);
				if (!localBranchExists) {
					branchCmd = branchCmd.setCreateBranch(true).setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
							.setStartPoint(mergeUnit.getBranchTarget());
				}
				branchCmd.call();
			} catch (GitAPIException e) {
				exception = new MergeUnitException(String.format("Could not checkout branch '%s'", localBranch), e); //$NON-NLS-1$
			}
		}

		/**
		 * {@code git status} {@code git cherry}
		 */
		private void checkStatus() {
			try {
				final Status result = repo.status().call();
				if (!result.isClean()) {
					exception = new MergeUnitException(String.format("The local repository is not clean: '%s'", repo)); //$NON-NLS-1$
				} else {
					/*
					 * JGit does not support "git cherry", so checking for unpushed commits is done
					 * by comparing the last commit id of the local and the remote branch. If JGit
					 * supports "git cherry" it should be refactored to this command.
					 */
					final String localTargetBranch = getLocalTargetBranch();
					if (localTargetBranch == null) {
						return;
					}
					final RevCommit lastCommitOfLocalBranch = getLastCommit(localTargetBranch);
					if (lastCommitOfLocalBranch == null) {
						return;
					}
					final RevCommit lastCommitOfRemoteBranch = getLastCommit(mergeUnit.getBranchTarget());
					if (lastCommitOfRemoteBranch == null) {
						return;
					}
					if (!lastCommitOfLocalBranch.equals(lastCommitOfRemoteBranch)) {
						exception = new MergeUnitException(
								String.format("The local repository is not clean: '%s'", repo)); //$NON-NLS-1$
					}
				}
			} catch (GitAPIException e) {
				exception = new MergeUnitException(
						String.format("Could not check the status of the local repository '%s'.", repo), e); //$NON-NLS-1$
			}
		}

		/**
		 * {@code git cherryPick}
		 * 
		 * @throws MergeCancelException
		 */
		private void cherryPick() {
			final ObjectId id = ObjectId.fromString(mergeUnit.getRevisionInfo());
			try {
				final CherryPickResult result = repo.cherryPick().include(id).setNoCommit(true).call();
				switch (result.getStatus()) {
				case CONFLICTING:
					final Collection<String> conflicts = repo.status().call().getConflicting();
					resolveConflicts(conflicts);
					break;
				case FAILED:
					exception = new MergeUnitException(String.format("Could not cherry pick the given commit '%s'", //$NON-NLS-1$
							mergeUnit.getRevisionInfo()));
					break;
				default:
					break;
				}
			} catch (GitAPIException e) {
				exception = new MergeUnitException(String.format("Could not cherry pick from the given id %s.", id), e); //$NON-NLS-1$
			}
		}

		/**
		 * Resolve the conflicts.
		 * 
		 * @param conflicts the collection of current existing conflicts in the
		 *                  repository
		 */
		private void resolveConflicts(final Collection<String> conflicts) {
			final List<String> conflicts2 = new ArrayList<>();
			conflicts2.addAll(conflicts);
			boolean isFixedOrCanceled = false;
			while (!isFixedOrCanceled) {
				switch (resolveConflictsByUser(conflicts2)) {
				case OPEN_REPOSITORY:
					MergeProcessorUtil.openFile(repo.getRepository().getWorkTree().toString());
					break;
				case RETRY:
					try {
						final Status call = repo.status().call();
						if (call.getConflicting().isEmpty()) {
							isFixedOrCanceled = true;
						} else {
							conflicts2.clear();
							conflicts2.addAll(call.getConflicting());
						}
					} catch (NoWorkTreeException | GitAPIException e) {
						exception = new MergeUnitException(
								String.format("Could not check the status of the local repository '%s'.", repo), e); //$NON-NLS-1$
						return;
					}
					break;
				case CANCEL:
				default:
					monitor.setCanceled(true);
					isFixedOrCanceled = true;
					break;
				}
			}
		}

		/**
		 * Resolve the conflicts by showing the information to the user and ask him to
		 * fix them.
		 * 
		 * @param conflicts the conflicts to fix by the user
		 * @return the handling the user selected
		 */
		private UserHandling resolveConflictsByUser(final Collection<String> conflicts) {
			final AtomicInteger result = new AtomicInteger();
			E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class).syncExec(() -> {
				final String[] buttons = { UserHandling.OPEN_REPOSITORY.getLabel(), UserHandling.RETRY.getLabel(),
						UserHandling.CANCEL.getLabel() };
				final StringBuilder sb = new StringBuilder();
				for (String conflict : conflicts) {
					sb.append(conflict).append(System.lineSeparator());
				}
				final MessageDialogScrollable dialog = new MessageDialogScrollable(
						ApplicationUtil.getApplicationShell(), "Merge Conflict", null, "Fix the merge conflict.",
						"The following files contain merge conflicts:" + System.lineSeparator() + sb.toString(),
						MessageDialogScrollable.QUESTION, buttons, 1);
				result.set(dialog.open());
			});
			switch (result.get()) {
			case 0:
				return UserHandling.OPEN_REPOSITORY;
			case 1:
				return UserHandling.RETRY;
			case 2:
			default:
				return UserHandling.CANCEL;
			}
		}

		/**
		 * {@code git commit}
		 */
		private void commit() {
			final String localSourceBranch = getLocalSourceBranch();
			final String localTargetBranch = getLocalTargetBranch();
			final String date = LocalDateTime.now().format(DATE_TIME_FORMATTER);
			final String message = String.format("MP [%s] %s -> %s: (%s) %s", mergeUnit.getRevisionInfo(), //$NON-NLS-1$
					localSourceBranch, localTargetBranch, date, commitMessage);
			try {
				repo.commit().setMessage(message).call();
			} catch (GitAPIException e) {
				exception = new MergeUnitException("Could not commit.", e); //$NON-NLS-1$
			}
		}

		/**
		 * {@code git push}
		 */
		private void push() {
			try {
				final Iterable<PushResult> resultIterable = repo.push().call();
				for (final PushResult pushResult : resultIterable) {
					for (RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
						if (refUpdate.getStatus() != RemoteRefUpdate.Status.OK) {
							// Push was rejected
							exception = new MergeUnitException(String
									.format("Could not push the local repository: '%s'", pushResult.getMessages()));
							return;
						}
					}
				}
			} catch (GitAPIException e) {
				exception = new MergeUnitException(String.format("Could not push the local repository '%s'.", repo), e); //$NON-NLS-1$
			}
		}

		/**
		 * Returns the last commit in the given branch of the given git repository.
		 */
		private RevCommit getLastCommit(final String branch) {
			try {
				final Iterable<RevCommit> call = repo.log().setMaxCount(1).add(repo.getRepository().resolve(branch))
						.call();
				return call.iterator().next();
			} catch (RevisionSyntaxException | IOException | GitAPIException e) {
				exception = new MergeUnitException(
						String.format("Could not get last commit for the given branch '%s'", branch), e); //$NON-NLS-1$
				return null;
			}
		}

		/**
		 * @return the local branch name for the remote target branch of the given merge
		 *         unit.
		 */
		private String getLocalTargetBranch() {
			return getLocalBranch(mergeUnit.getBranchTarget());
		}

		/**
		 * @return the local branch name for the remote source branch of the given merge
		 *         unit.
		 */
		private String getLocalSourceBranch() {
			return getLocalBranch(mergeUnit.getBranchSource());
		}

		/**
		 * @param branchName the branch with additional local or remote information
		 * @return the name of the branch without additional local or remote information
		 */
		private String getLocalBranch(final String branchName) {
			final Matcher matcher = Pattern.compile("\\/[^\\/]*$").matcher(branchName); //$NON-NLS-1$
			if (matcher.find()) {
				return matcher.group(0).substring(1);
			} else {
				exception = new MergeUnitException(String.format("Unknown target branch format '%s'", branchName)); //$NON-NLS-1$
				return null;
			}
		}

	}

}
