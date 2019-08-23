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
package org.aposin.mergeprocessor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aposin.mergeprocessor.application.ApplicationUtil;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.model.UnsupportedVersionControlSystemSupportException;
import org.aposin.mergeprocessor.model.git.GITMergeUnit;
import org.aposin.mergeprocessor.model.git.GitMergeUtil;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
import org.aposin.mergeprocessor.model.svn.SVNMergeUtil;
import org.aposin.mergeprocessor.model.svn.SvnMergeTask;
import org.aposin.mergeprocessor.view.MessageDialogScrollable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.osgi.util.NLS;

/**
 *
 */
public final class MergeProcessorUtil {

	private static final Logger LOGGER = Logger.getLogger(MergeProcessorUtil.class.getName());

	private MergeProcessorUtil() {
		// Utility class
	}

	/**
	 * Performs all the necessary steps to process the merge defined in the
	 * mergeUnit.
	 * 
	 * @param pmd       progress monitor
	 * @param monitor
	 * @param mergeUnit the mergeUnit to process
	 * @return <code>true</code> if the user cancelled the merge. Otherwise
	 *         <code>false</code>.
	 * @throws SvnClientException
	 */
	public static boolean merge(ProgressMonitorDialog pmd, IProgressMonitor monitor, IConfiguration configuration,
			IMergeUnit mergeUnit) throws SvnClientException {
		if (mergeUnit instanceof SVNMergeUnit) {
			return SVNMergeUtil.merge(pmd, monitor, (SVNMergeUnit) mergeUnit);
		} else if (mergeUnit instanceof GITMergeUnit) {
			return GitMergeUtil.merge(pmd, monitor, configuration, (GITMergeUnit) mergeUnit);
		} else {
			throw new UnsupportedVersionControlSystemSupportException();
		}
	}

	/**
	 * Performs all the necessary steps to process the merge defined in the
	 * mergeUnit.
	 * 
	 * @param monitor           progress monitor
	 * @param mergeUnit         the mergeUnit to process
	 * @param workspaceLocation the workspace where to merge into
	 * @param configuration     the configuration
	 * @throws SvnUtilException
	 */
	public static List<String> merge(IProgressMonitor monitor, Consumer<String> commandConsumer, IMergeUnit mergeUnit,
			final Path workspaceLocation) throws SvnUtilException {
		if (mergeUnit instanceof SVNMergeUnit) {
			final SvnMergeTask task = new SvnMergeTask(monitor, commandConsumer, (SVNMergeUnit) mergeUnit,
					workspaceLocation);
			task.merge();
			return task.getWarnings();
		} else {
			throw new UnsupportedVersionControlSystemSupportException();
		}
	}

	/**
	 * @param includeDone    include MergeUnits from the "done" folder
	 * @param includeIgnored include MergeUnits from the "done" folder with status
	 *                       ignored
	 * @return a {@link List} with all found {@link SVNMergeUnit}s.
	 * @throws MergeProcessorUtilException
	 */
	public static List<IMergeUnit> getMergeUnits(boolean includeDone, boolean includeIgnored)
			throws MergeProcessorUtilException {
		LogUtil.entering(includeDone, includeIgnored);
		List<IMergeUnit> list = new ArrayList<>();

		try {
			SftpUtil sftp = SftpUtil.getInstance();
			list.addAll(sftp.getMergeUnitsTodo());
			list.addAll(sftp.getMergeUnitsCanceled());
			if (includeDone) {
				list.addAll(sftp.getMergeUnitsDone());
				list.addAll(sftp.getMergeUnitsManual());
			}

			if (includeIgnored) {
				list.addAll(sftp.getMergeUnitsIgnored());
			}
		} catch (SftpUtilException e) {
			throw LogUtil.throwing(new MergeProcessorUtilException("Couldn't get merge units from server.", e));
		}

		return LogUtil.exiting(list);
	}

	/**
	 * Moves the merge unit from its current folder on the sftp server to the
	 * canceled folder.
	 * 
	 * @param mergeUnit
	 */
	public static void canceled(IMergeUnit mergeUnit) {
		LogUtil.entering(mergeUnit);

		boolean cancel = false;

		while (!cancel) {
			try {
				SftpUtil.getInstance().moveMergeUnitFromRemoteToCanceled(mergeUnit);
				mergeUnit.setStatus(MergeUnitStatus.CANCELLED);
				break;
			} catch (SftpUtilException e) {
				String message = String.format("Caught exception while moving mergeUnit=[%s] to ignore.", mergeUnit); //$NON-NLS-1$
				LOGGER.log(Level.WARNING, message, e);

				String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_Ignore_Error_Message, e.getMessage());
				if (bugUserToFixProblem(Messages.MergeProcessorUtil_Ignore_Error_Title, messageScrollable)) {
					// user wants to retry
					LOGGER.fine(String.format("User wants to retry ignore mergeUnit. mergeUnit=%s.", mergeUnit)); //$NON-NLS-1$
					continue;
				} else {
					// user didn't say 'retry' so we cancel the whole merge...
					LOGGER.fine(String.format("User cancelled ignore mergeUnit. mergeUnit=%s.", mergeUnit)); //$NON-NLS-1$
					cancel = true;
				}
			}
		}
		LogUtil.exiting();
	}

	/**
	 * Moves mergeUnit from its current folder on the sftp server to the ignored
	 * folder.
	 * 
	 * @param mergeUnit
	 */
	public static void ignore(IMergeUnit mergeUnit) {
		LogUtil.entering(mergeUnit);
		boolean cancel = false;

		while (!cancel) {
			try {
				SftpUtil.getInstance().moveMergeUnitFromRemoteToIgnore(mergeUnit);
				mergeUnit.setStatus(MergeUnitStatus.IGNORED);
				break;
			} catch (SftpUtilException e) {
				String message = String.format("Caught exception while moving mergeUnit=[%s] to ignore.", mergeUnit); //$NON-NLS-1$
				LOGGER.log(Level.WARNING, message, e);

				String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_Ignore_Error_Message, e.getMessage());
				if (bugUserToFixProblem(Messages.MergeProcessorUtil_Ignore_Error_Title, messageScrollable)) {
					// user wants to retry
					LOGGER.fine(String.format("User wants to retry ignore mergeUnit. mergeUnit=%s.", mergeUnit)); //$NON-NLS-1$
					continue;
				} else {
					// user didn't say 'retry' so we cancel the whole merge...
					LOGGER.fine(String.format("User cancelled ignore mergeUnit. mergeUnit=%s.", mergeUnit)); //$NON-NLS-1$
					cancel = true;
				}
			}
		}
		LogUtil.exiting();
	}

	/**
	 * Moves mergeUnit from the ignored folder on the sftp server to the todo
	 * folder.
	 * 
	 * @param mergeUnit
	 */
	public static void todo(IMergeUnit mergeUnit) {
		LogUtil.entering(mergeUnit);
		boolean cancel = false;

		while (!cancel) {
			try {
				SftpUtil.getInstance().moveMergeUnitFromRemoteToTodo(mergeUnit);
				mergeUnit.setStatus(MergeUnitStatus.TODO);
				break;
			} catch (SftpUtilException e) {
				String message = String.format("Caught exception while moving mergeUnit=[%s] to ignore.", mergeUnit); //$NON-NLS-1$
				LOGGER.log(Level.WARNING, message, e);

				String messageScrollable = NLS.bind(Messages.MergeProcessorUtil_Unignore_Error_Message, e.getMessage());
				if (bugUserToFixProblem(Messages.MergeProcessorUtil_Unignore_Error_Title, messageScrollable)) {
					// user wants to retry
					LOGGER.fine(String.format("User wants to retry unignore mergeUnit. mergeUnit=[{}].", mergeUnit)); //$NON-NLS-1$
					continue;
				} else {
					// user didn't say 'retry' so we cancel the whole merge...
					LOGGER.fine(String.format("User cancelled unignore mergeUnit. mergeUnit=[{}].", mergeUnit)); //$NON-NLS-1$
					cancel = true;
				}
			}
		}
		LogUtil.exiting();
	}

	/**
	 * Opens the given file with the associated editor.
	 * 
	 * @param path the path to the file
	 */
	public static void openFile(String path) {
		String name = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		String command = null;

		if (name.contains("win")) { //$NON-NLS-1$
			command = String.format("cmd /Q /C start \"Open file '%s'\" ", path); //$NON-NLS-1$
		} else if (name.contains("mac")) { //$NON-NLS-1$
			command = "edit "; //$NON-NLS-1$
		} else /* if (name.contains("linux")) */ {
			command = "open "; //$NON-NLS-1$
		}

		command += '"' + path + '"';
		processCommand(command);
	}

	/**
	 * Opens a dialog with the given message and the option to retry or cancel.
	 * 
	 * @param message
	 * @return <code>true</code> if the user chose to retry.
	 */
	public static boolean bugUserToFixProblem(final String message, final String messageScrollable) {
		final AtomicBoolean retVal = new AtomicBoolean();

		E4CompatibilityUtil.getApplicationContext().get(UISynchronize.class).syncExec(() -> {
			MessageDialogScrollable dialog = new MessageDialogScrollable(ApplicationUtil.getApplicationShell(),
					Messages.MergeProcessorUtil_BugUserToFixProblem_Title, null, message, messageScrollable,
					MessageDialogScrollable.ERROR, new String[] { Messages.MergeProcessorUtil_BugUserToFixProblem_Retry,
							Messages.MergeProcessorUtil_BugUserToFixProblem_Cancel },
					0);
			boolean retry = dialog.open() == 0;
			if (retry) {
				LOGGER.fine(String.format("User wants to retry. message=%s, messageScrollable=%s", message, //$NON-NLS-1$
						messageScrollable));
			} else {
				LOGGER.fine(String.format("User wants to cancel. message=%s, messageScrollable=%s", message, //$NON-NLS-1$
						messageScrollable));
			}
			retVal.set(retry);
		});

		return retVal.get();
	}

	/**
	 * Opens the given file with the associated editor.
	 * 
	 * @param path the path to the file
	 */
	public static void openFolder(String path) {
		String name = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		String command = null;

		if (name.contains("win")) { //$NON-NLS-1$
			command = "cmd /Q /C explorer "; //$NON-NLS-1$
		} else if (name.contains("mac")) { //$NON-NLS-1$
			command = "open "; //$NON-NLS-1$
		} else /* if (name.contains("linux")) */ {
			command = "open "; //$NON-NLS-1$
		}

		command += '"' + path + '"';
		processCommand(command);
	}

	private static void processCommand(final String command) {
		LogUtil.entering(command);
		if (command != null) {
			Thread t = new Thread(String.format("processCommand(%s)", command)) { //$NON-NLS-1$

				@Override
				public void run() {
					String line;
					StringBuilder stdOut = new StringBuilder();
					StringBuilder stdErr = new StringBuilder();

					LOGGER.fine(() -> String.format("Start execution of command=%s.", command)); //$NON-NLS-1$

					try {
						Process p = Runtime.getRuntime().exec(command);

						try (BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
								BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
							while ((line = bri.readLine()) != null) {
								stdOut.append(line).append('\n');
							}
							while ((line = bre.readLine()) != null) {
								stdErr.append(line).append('\n');
							}

							p.waitFor();
						}

						LOGGER.fine(() -> String.format("STDOUT of process from command=%s:%n%s", command, stdOut)); //$NON-NLS-1$
						LOGGER.fine(() -> String.format("STDERR of process from command=%s:%n%s", command, stdErr)); //$NON-NLS-1$
						LOGGER.fine(() -> String.format("Exit code of process from command=%s: %s", command, //$NON-NLS-1$
								p.exitValue()));
					} catch (IOException | InterruptedException e) {
						String message = String.format("Caught Exception while processing command=[%s].", command); //$NON-NLS-1$
						LOGGER.log(Level.WARNING, message, e);
					}
				}
			};
			t.start();
		}
		LogUtil.exiting();
	}

}
