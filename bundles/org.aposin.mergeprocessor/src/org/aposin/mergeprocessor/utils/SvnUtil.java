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
package org.aposin.mergeprocessor.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.aposin.mergeprocessor.application.ApplicationUtil;
import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
import org.aposin.mergeprocessor.view.MessageDialogScrollable;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;

import com.google.common.base.Joiner;

/**
 * 
 */
public class SvnUtil {

	private static final Logger LOGGER = Logger.getLogger(SvnUtil.class.getName());

	private static final String CMD_TASKKILL = "taskkill /F /IM "; //$NON-NLS-1$
	private static final String PROCESS_TORTOISESVN_CACHE = "TSVNCache.exe"; //$NON-NLS-1$

	private static final String CMD_TORTOISESVN = "tortoiseproc.exe"; //$NON-NLS-1$
	private static final String PARAMETER_TORTOISESVN_REPOSTATUS = " /command:repostatus"; //$NON-NLS-1$
	private static final String PARAMETER_TORTOISESVN_PATH = " /path:"; //$NON-NLS-1$

	private static final String KEYWORD_TRUNK = "/trunk"; //$NON-NLS-1$
	private static final String KEYWORD_BRANCHES = "/branches/"; //$NON-NLS-1$
	private static final String BRANCH_NAME_TRUNK = "trunk"; //$NON-NLS-1$

	protected static final String BRANCH_NAME_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	protected static final String COMMIT_MESSAGE_MERGE_CURRENT = "MP [%1$d:%2$d] %3$s -> %4$s\n"; //$NON-NLS-1$
	protected static final String COMMIT_MESSAGE_MERGE_PREVIOUS = "r%1$s: [%2$s] (%3$tY-%3$tm-%3$td %3$tH:%3$tM:%3$tS) %4$s\n"; //$NON-NLS-1$

	/**
	 * Builds a working copy of the target branch of the given mergeUnit.
	 * This working copy consists at least of the given paths and is most likely not a whole checkout.
	 * @param mergeUnit
	 * @return <code>true</code> when user cancelled
	 * @throws SvnUtilException
	 */
	public static boolean buildMinimalWorkingCopy(SVNMergeUnit mergeUnit, ISvnClient client) throws SvnUtilException {
		LogUtil.entering(mergeUnit);

		boolean cancel = false;

		// setup working copy
		boolean success = false;

		if (!success && !cancel) {
			LOGGER.fine("We can't use the existing working copy. Deleting and recreating it."); //$NON-NLS-1$
			cancel = recreateEmptyWorkingCopy(mergeUnit, client);
		}

		if (!cancel) {
			// add needed files
			cancel = setupWorkingCopy(mergeUnit, client);
		}

		return LogUtil.exiting(cancel);
	}

	/**
	 * Deletes a possibly existing working copy and creates a new one of the given branch.
	 * @param mergeUnit
	 * @return true if cancelled by user
	 * @throws SvnUtilException
	 */
	public static boolean recreateEmptyWorkingCopy(SVNMergeUnit mergeUnit, ISvnClient client) throws SvnUtilException {
		LogUtil.entering(mergeUnit);

		boolean cancel = false;

		File folderWorkingCopy = new File(Configuration.getPathSvnWorkingCopy());

		// delete working copy
		if (folderWorkingCopy.exists()) {
			LOGGER.fine("Deleting working copy folder."); //$NON-NLS-1$
			try {
				FileUtils.forceDelete(folderWorkingCopy);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Could not delete working copy.", e); //$NON-NLS-1$
			}

			if (folderWorkingCopy.exists()) {
				LOGGER.fine("Working copy folder still exists. Trying to kill TSVNCache process."); //$NON-NLS-1$
				try {
					killTsvncacheProcess();
					Thread.sleep(1000);
					FileUtils.forceDelete(folderWorkingCopy);
				} catch (InterruptedException | IOException e) {
					LOGGER.log(Level.SEVERE, "Caught InterruptedException while sleeping.", e); //$NON-NLS-1$
				}
			}
			if (folderWorkingCopy.exists()) {
				String message = String.format(
						"Couldn't delete working copy folder. Maybe another process still has a handle on this folder or on it's subelements. folderWorkingCopy=[%s]", //$NON-NLS-1$
						folderWorkingCopy.getAbsolutePath());
				throw LogUtil.throwing(new SvnUtilException(message));
			} else {
				LOGGER.fine("Working copy folder is deleted."); //$NON-NLS-1$
			}
		}

		// create folder for working copy
		if (folderWorkingCopy.exists()) {
			String message = String.format("Couldn't create working copy folder. folderWorkingCopy=[%s]", //$NON-NLS-1$
					folderWorkingCopy.getAbsolutePath());
			throw LogUtil.throwing(new SvnUtilException(message));
		}

		// checkout empty branch
		if (mergeUnit != null) {
			checkoutEmptyBranch(mergeUnit, client);
		}

		return LogUtil.exiting(cancel);
	}

	/**
	 * Sometimes a TortoiseSVN process keeps holding handles of SVN repositories.
	 * To delete a repository we try to kill this process. Killing this process is considered safe.
	 * @throws SvnUtilException
	 */
	private static void killTsvncacheProcess() {
		LogUtil.entering();
		String processName = PROCESS_TORTOISESVN_CACHE;
		Runtime rt = Runtime.getRuntime();
		try {
			rt.exec(CMD_TASKKILL + processName);
		} catch (IOException e) {
			String message = String.format("Caught IOException while trying to kill '%s'. Maybe it is not running.", //$NON-NLS-1$
					processName);
			LOGGER.log(Level.WARNING, message, e);
		}
		LogUtil.exiting();
	}

	/**
	 * Checks out the root of the branch with depth empty.
	 * @param mergeUnit
	 * @return <code>true</code> if the user cancelled the operation
	 * @throws SvnUtilException
	 */
	private static boolean checkoutEmptyBranch(SVNMergeUnit mergeUnit, ISvnClient client) throws SvnUtilException {
		LogUtil.entering(mergeUnit);
		boolean cancel = false;
		try {
			final URL url = new URL(mergeUnit.getUrlTarget());
			final Path path = Paths.get(Configuration.getPathSvnWorkingCopy());
			if (!path.toFile().exists()) {
				path.toFile().mkdirs();
			}
			client.checkoutEmpty(path, url);
		} catch (MalformedURLException | SvnClientException e) {
			throw LogUtil.throwing(new SvnUtilException(e));
		}
		return LogUtil.exiting(cancel);
	}

	/**
	 * Updates an existing, empty working copy so it contains the files needed for the merge.
	 * @param mergeUnit
	 * @return <code>true</code> if the user cancelled the operation
	 * @throws SvnUtilException
	 */
	private static boolean setupWorkingCopy(SVNMergeUnit mergeUnit, ISvnClient client) throws SvnUtilException {
		LogUtil.entering(mergeUnit);
		boolean cancel = false;

		final List<String> neededFiles = mergeUnit.getAffectedTargetFiles();
		final List<String> targetFile = new ArrayList<>(neededFiles);
		targetFile.addAll(mergeUnit.getTargetFilesToDelete());

		File workingCopy = new File(Configuration.getPathSvnWorkingCopy());
		if (!workingCopy.exists()) {
			workingCopy.mkdirs();
		}

		List<Path> paths = new ArrayList<>(targetFile.size() + 1);

		if (targetFile.isEmpty()) {
			LOGGER.fine("mergeUnit has no needed files for the working copy."); //$NON-NLS-1$
		} else {
			// update neededFiles AND working copy folder (so we don't end up with a mixed
			// revision working copy)
			paths.add(workingCopy.toPath());

			for (String target : targetFile) {
				final List<Path> pathsToAdd = new ArrayList<>();
				Path path = Paths.get(target);
				while (path != null) {
					pathsToAdd.add(Paths.get(workingCopy.getPath().toString(), path.toString()));
					path = path.getParent();
				}
				Collections.reverse(pathsToAdd);
				paths.addAll(pathsToAdd);
			}
		}
		paths = paths.stream().distinct().collect(Collectors.toList());

		try {
			final long[] revisions = client.updateEmpty(paths);
			LOGGER.info(() -> String.format("Update return revisions:%n%s.", Arrays.toString(revisions))); //$NON-NLS-1$
		} catch (SvnClientException e) {
			String message = String.format("Caught SVNException while setting up working copy '%s'.", //$NON-NLS-1$
					Configuration.getPathSvnWorkingCopy());
			throw new SvnUtilException(message, e);
		}

		// check if files are missing.
		// files might be missing when they have been renamed in the target branch.

		List<Path> missingFiles = new ArrayList<>();

		if (paths != null) {
			for (Path path : paths) {
				final String relPath = Paths.get(workingCopy.toString()).relativize(path).toString().replace('\\', '/');
				final boolean isAddedPath = mergeUnit.getTargetFilesToAdd().contains('/' + relPath + '/')
						|| mergeUnit.getTargetFilesToAdd().contains('/' + relPath);
				if (Files.notExists(path) && !isAddedPath) {
					missingFiles.add(path);
				}
			}
		}

		if (!missingFiles.isEmpty()) {
			// so there are files missing...
			// inform the user about the missing files.
			// we give the user to choices
			// * 1 cancel the merge.
			// * 2 continue the merge without merging the missing files. the user must merge
			// the missing files manually.

			String joinedMissingFiles = Joiner.on(",\n\t").join(missingFiles); //$NON-NLS-1$
			LOGGER.info(() -> String.format("Found %s missing files:%n%s", missingFiles.size(), joinedMissingFiles)); //$NON-NLS-1$

			String dialogMessageScrollable;
			String workingFolderManual = "C:\\dev\\manual_merge\\"; //$NON-NLS-1$

			String messageIntro = NLS.bind(Messages.SvnUtilSvnkit_MovedFiles_Intro, joinedMissingFiles);
			String commandCreateFolder = String.format("MD %s%n", workingFolderManual); //$NON-NLS-1$
			String commandChangeFolder = String.format("CD %s%n", workingFolderManual); //$NON-NLS-1$
			String commandCheckout = String.format("SVN checkout %s <PATH_OF_MOVED_FILE_IN_TARGET_BRANCH>%n", //$NON-NLS-1$
					mergeUnit.getUrlTarget());
			String commandMerge = String.format(
					"SVN merge -r %d:%d %s <PATH_OF_MOVED_FILE_IN_SOURCE_BRANCH> <FILENAME> --accept postpone %n", //$NON-NLS-1$
					mergeUnit.getRevisionStart(), mergeUnit.getRevisionEnd(), mergeUnit.getUrlSource());
			String commandCommit = String.format("SVN commit -m \"Manual Merge: [%d:%d] %s -> %s\" %s%n", //$NON-NLS-1$
					mergeUnit.getRevisionStart(), mergeUnit.getRevisionEnd(), getBranchName(mergeUnit.getUrlSource()),
					getBranchName(mergeUnit.getUrlTarget()), workingFolderManual);
			String commandRemoveFolder = String.format("RD /S /Q %s%n", workingFolderManual); //$NON-NLS-1$

			StringBuilder sb = new StringBuilder();
			sb.append(messageIntro);
			sb.append(commandCreateFolder);
			sb.append(commandChangeFolder);
			sb.append(commandCheckout);
			sb.append(commandMerge);
			sb.append(commandCommit);
			sb.append(commandRemoveFolder);

			dialogMessageScrollable = sb.toString();

			String dialogMessage = NLS.bind(Messages.SvnUtilSvnkit_MovedFiles_Description, missingFiles.size());
			final MessageDialog dialog = new MessageDialogScrollable(ApplicationUtil.getApplicationShell(),
					Messages.SvnUtilSvnkit_MovedFiles_Title, null, dialogMessage, dialogMessageScrollable,
					MessageDialog.WARNING, new String[] { Messages.SvnUtilSvnkit_MovedFiles_CancelMerge,
							Messages.SvnUtilSvnkit_MovedFiles_ContinueWithoutMergingMissingFiles },
					0);

			final AtomicInteger choice = new AtomicInteger();
			E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class)
					.syncExec(() -> choice.set(dialog.open()));

			if (choice.get() == 0) {
				LOGGER.info("User cancelled merged because of missing files."); //$NON-NLS-1$
				cancel = true;
			} else {
				LOGGER.info("User continues merge despite of missing files."); //$NON-NLS-1$
			}
		}

		return LogUtil.exiting(cancel);
	}

	/**
	 * Merges the changes of the given mergeUnit into the working copy.
	 * @param mergeUnit
	 * @throws SvnUtilException on errors
	 */
	public static void mergeChanges(SVNMergeUnit mergeUnit, final ISvnClient client) throws SvnUtilException {
		if (Objects.equals(mergeUnit.getAffectedSourceFiles(), mergeUnit.getAffectedTargetFiles())) {
			mergeChangesBundled(mergeUnit, client);
		} else {
			mergeChangesSeparately(mergeUnit, client);
		}
	}

	/**
	 * Merges the unit bundled with one call.
	 * 
	 * @param mergeUnit  the merge unit
	 * @throws SvnUtilException
	 */
	private static void mergeChangesBundled(SVNMergeUnit mergeUnit, final ISvnClient client) throws SvnUtilException {
		LogUtil.entering(mergeUnit);
		try {
			final Path path = Paths.get(Configuration.getPathSvnWorkingCopy());
			final URL url = new URL(mergeUnit.getUrlSource());
			final long revision = mergeUnit.getRevisionEnd();
			client.merge(path, url, revision);
		} catch (SvnClientException | MalformedURLException e) {
			throw LogUtil.throwing(new SvnUtilException(e));
		}
		LogUtil.exiting();
	}

	/**
	 * Merges the unit separately by merging each file change with a separate call.
	 * 
	 * @param mergeUnit
	 * @throws SvnUtilException
	 */
	private static void mergeChangesSeparately(SVNMergeUnit mergeUnit, final ISvnClient client)
			throws SvnUtilException {
		LogUtil.entering(mergeUnit);
		final String urlSourceString = mergeUnit.getUrlSource();
		try {
			for (int i = 0; i < mergeUnit.getAffectedSourceFiles().size(); i++) {
				final String targetFile = mergeUnit.getAffectedTargetFiles().get(i);
				if (mergeUnit.getTargetFilesToDelete().contains(targetFile.replace('\\', '/'))) {
					continue;
				}
				final Path path = Paths.get(Configuration.getPathSvnWorkingCopy(), targetFile);
				final URL url = new URL(urlSourceString + mergeUnit.getAffectedSourceFiles().get(i).replace('\\', '/'));
				final long revision = mergeUnit.getRevisionEnd();
				client.merge(path, url, revision);
			}
		} catch (MalformedURLException | SvnClientException e) {
			throw LogUtil.throwing(new SvnUtilException(e));
		}
		LogUtil.exiting();
	}

	/**
	 * Commits all changes in the working copy.
	 * @param mergeUnit
	 * @throws SvnUtilException on errors
	 */
	public static void commitChanges(SVNMergeUnit mergeUnit, final ISvnClient client) throws SvnUtilException {
		LogUtil.entering(mergeUnit);
		// contains conflicts?
		String[] conflicts = conflictsOfWorkingCopy(client);
		if (conflicts.length > 0) {
			String message = String.format("Can't commit changes: Workspace has conflicts. conflicts[%d]=[%s]", //$NON-NLS-1$
					conflicts.length, Arrays.toString(conflicts));
			throw LogUtil.throwing(new SvnUtilException(message));
		}

		String message = mergeUnit.getMessage();
		try {
			client.commit(Paths.get(Configuration.getPathSvnWorkingCopy()), message);
		} catch (SvnClientException e) {
			throw new SvnUtilException(e);
		}
		LogUtil.exiting();
	}

	/**
	 * @return an array with the paths of the conflict files. If there are no conflict files an empty array is returned.
	 * @throws SvnUtilException
	 */
	public static String[] conflictsOfWorkingCopy(final ISvnClient client) throws SvnUtilException {
		LogUtil.entering();
		List<String> conflicts;
		try {
			conflicts = client.getConflicts(Paths.get(Configuration.getPathSvnWorkingCopy()));
		} catch (SvnClientException e) {
			throw new SvnUtilException(e);
		}
		return LogUtil.exiting(conflicts.toArray(new String[conflicts.size()]));
	}

	/**
	 * Updates the working copy
	 * @throws SvnUtilException
	 */
	public static void update(final ISvnClient client) throws SvnUtilException {
		LogUtil.entering();
		try {
			client.update(Paths.get(Configuration.getPathSvnWorkingCopy()));
		} catch (SvnClientException e) {
			throw new SvnUtilException(e);
		}
		LogUtil.exiting();
	}

	/**
	 * @param url the absolute repository url of a versioned file.
	 * @return the branch name of the given url or <code>null</code> if no branch has been found.
	 */
	public static String getBranchName(String url) {
		if (url == null || url.isEmpty()) {
			return null;
		}
		final String retVal;
		final int posTrunk = url.indexOf(KEYWORD_TRUNK);
		final int posBranches = url.indexOf(KEYWORD_BRANCHES);
		if (posTrunk == -1 && posBranches == -1) {
			// no expected branch name, return null
			retVal = null;
		} else if (posTrunk > -1 && (posTrunk < posBranches || posBranches < 0)) {
			// trunk
			retVal = BRANCH_NAME_TRUNK;
		} else if (posBranches > -1 && (posBranches < posTrunk || posTrunk < 0)) {
			int start = posBranches + KEYWORD_BRANCHES.length();
			int end = url.indexOf('/', start + 1); // $NON-NLS-1$

			if (end == -1) {
				retVal = url.substring(start);
			} else {
				retVal = url.substring(start, end);
			}
		} else {
			retVal = null;
		}
		return LogUtil.exiting(retVal);
	}

	/**
	 * 
	 * @param url
	 * @return the repository root of the url. when no glues for the root have been found the url itself will be return because it is expected that the url is already the root. 
	 */
	public static String getRepositoryRootOfUrl(String url) {
		LogUtil.entering(url);

		String retVal = null;
		int posTrunk = url.indexOf(KEYWORD_TRUNK);
		int posBranches = url.indexOf(KEYWORD_BRANCHES);

		if (posTrunk == -1 && posBranches == -1) {
			// no expected branch name, return url
			retVal = url;
		} else if (posTrunk > -1 && (posTrunk < posBranches || posBranches < 0)) {
			retVal = url.substring(0, posTrunk);
		} else if (posBranches > -1 && (posBranches < posTrunk || posTrunk < 0)) {
			retVal = url.substring(0, posBranches);
		}

		return LogUtil.exiting(retVal);
	}

	/**
	 * List the directories for 
	 * 
	 * @param svnPath
	 * @return
	 * @throws Exception 
	 */
	public static List<String> listDirectories(final String svnPath) throws Exception {
		try (final ISvnClient client = E4CompatibilityUtil.getApplicationContext().get(ISvnClient.class)) {
			return client.listDirectories(new URL(svnPath));
		} catch (Exception e) {
			throw LogUtil.throwing(e);
		}
	}

	/**
	* Opens the TortoiseSVN "Check for modifications" dialog on the working copy.
	* @throws SvnUtilException
	*/
	public static void openTortoiseSVN(final String path) throws SvnUtilException {
		LogUtil.entering(path);

		StringBuilder command = new StringBuilder(CMD_TORTOISESVN);

		command.append(PARAMETER_TORTOISESVN_REPOSTATUS);
		command.append(PARAMETER_TORTOISESVN_PATH + path/* Configuration.getPathSvnWorkingCopy() */);

		try {
			Runtime.getRuntime().exec(command.toString(), null, null);
		} catch (IOException e) {
			throw new SvnUtilException("Caught SVNException while starting TortoiseSVN process.", e); //$NON-NLS-1$
		}
		LogUtil.exiting();
	}
}