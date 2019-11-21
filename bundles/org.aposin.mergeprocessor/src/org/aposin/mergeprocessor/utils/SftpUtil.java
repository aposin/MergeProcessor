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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.exception.SftpUtilException;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitException;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.model.git.GITMergeUnitFactory;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnitFactory;
import org.eclipse.core.runtime.Path;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 *
 */
public class SftpUtil {

	private static final Logger LOGGER = Logger.getLogger(SftpUtil.class.getName());

	private static SftpUtil instance = null;

	private final IConfiguration configuration;
	private Session session = null;
	private ChannelSftp sftpChannel = null;

	private SftpUtil(IConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * @return the instance of the singleton Configuration.
	 */
	public static synchronized SftpUtil getInstance() {
		if (instance == null) {
			instance = new SftpUtil(E4CompatibilityUtil.getApplicationContext().get(IConfiguration.class));
		}
		return instance;
	}

	/**
	 * Deletes the given mergeunit
	 * 
	 * @param path
	 * @throws SftpUtilException
	 */
	public synchronized void deleteRemoteMergeUnit(String path) throws SftpUtilException {
		LogUtil.entering(path);
		connectIfNotConnected();

		try {
			sftpChannel.rm(path);
		} catch (SftpException e) {
			String message = String.format("Couldn't delete file=[%s].", path); //$NON-NLS-1$
			throw new SftpUtilException(message, e);
		}
		LogUtil.exiting();
	}

	/**
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void copyMergeUnitToWork(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		connectIfNotConnected();

		String pathRemote = mergeUnit.getRemotePath();
		File fileLocal = new File(Configuration.getPathLocalMergeFile(mergeUnit));

		// delete a possibly existing file
		FileUtils.deleteQuietly(fileLocal);

		// create parent folder
		File fileLocalParent = fileLocal.getParentFile();
		if (!fileLocalParent.exists() && !fileLocalParent.mkdirs()) {
			String message = String.format("Couldn't create local folder. fileLocalParent=[%s].", //$NON-NLS-1$
					fileLocalParent.getAbsolutePath());
			throw LogUtil.throwing(new SftpUtilException(message));
		}

		try {
			// create file
			if (!fileLocal.createNewFile()) {
				String message = String.format("Couldn't create local file. fileLocal=[%s].", //$NON-NLS-1$
						fileLocal.getAbsolutePath());
				throw LogUtil.throwing(new SftpUtilException(message));
			}
			LOGGER.fine(
					() -> String.format("Copy from remote=%s to local=%s.", pathRemote, fileLocal.getAbsolutePath())); //$NON-NLS-1$

			try (final InputStream is = sftpChannel.get(pathRemote);
					final OutputStream outputStream = new FileOutputStream(fileLocal)) {
				IOUtils.copy(is, outputStream);
			}
		} catch (IOException | SftpException e) {
			String message = String.format("Couldn't copy remote=[%s] to local=[%s].", pathRemote, //$NON-NLS-1$
					fileLocal.getAbsolutePath());
			throw LogUtil.throwing(new SftpUtilException(message, e));
		}
		LogUtil.exiting();
	}

	/**
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void copyMergeUnitFromWorkToDoneAndDeleteInTodo(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		String pathRemote = configuration.getSftpConfiguration().getDoneFolder() + mergeUnit.getFileName();
		copyMergeUnitFromWorkToRemote(mergeUnit, pathRemote);
		final String path;
		if (mergeUnit.getStatus() == MergeUnitStatus.CANCELLED) {
			path = configuration.getSftpConfiguration().getCanceledFolder() + mergeUnit.getFileName();
		} else {
			path = configuration.getSftpConfiguration().getTodoFolder() + mergeUnit.getFileName();
		}
		deleteRemoteMergeUnit(path);
		LogUtil.exiting();
	}

	/**
	 * 
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void moveMergeUnitFromRemoteToIgnore(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		String target = configuration.getSftpConfiguration().getIgnoredFolder() + mergeUnit.getFileName();

		moveMergeUnit(mergeUnit, target);
		mergeUnit.setStatus(MergeUnitStatus.IGNORED);
		LogUtil.exiting();
	}

	/**
	 * 
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void moveMergeUnitFromRemoteToCanceled(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		String target = configuration.getSftpConfiguration().getCanceledFolder() + mergeUnit.getFileName();

		moveMergeUnit(mergeUnit, target);
		mergeUnit.setStatus(MergeUnitStatus.CANCELLED);
		LogUtil.exiting();
	}

	/**
	 * 
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void moveMergeUnitFromRemoteToDone(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		String target = configuration.getSftpConfiguration().getDoneFolder() + mergeUnit.getFileName();

		moveMergeUnit(mergeUnit, target);
		mergeUnit.setStatus(MergeUnitStatus.DONE);
		LogUtil.exiting();
	}

	/**
	 * 
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void moveMergeUnitFromRemoteToManual(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		String target = configuration.getSftpConfiguration().getManualFolder() + mergeUnit.getFileName();

		moveMergeUnit(mergeUnit, target);
		mergeUnit.setStatus(MergeUnitStatus.MANUAL);
		LogUtil.exiting();
	}

	/**
	 * 
	 * @param mergeUnit
	 * @throws SftpUtilException
	 */
	public synchronized void moveMergeUnitFromRemoteToTodo(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		String target = configuration.getSftpConfiguration().getTodoFolder() + mergeUnit.getFileName();
		moveMergeUnit(mergeUnit, target);
		mergeUnit.setStatus(MergeUnitStatus.TODO);
		LogUtil.exiting();
	}

	private void moveMergeUnit(IMergeUnit mergeUnit, String target) throws SftpUtilException {
		LogUtil.entering(mergeUnit, target);
		String source = mergeUnit.getRemotePath();

		if (source.equals(target)) {
			LOGGER.fine(
					() -> String.format("Source=%s and target=%s are the same. Nothing to do here...", source, target)); //$NON-NLS-1$
		} else {
			LOGGER.fine(() -> String.format("Moving mergeUnit=%s from %s to %s.", mergeUnit, source, target)); //$NON-NLS-1$
			try {
				try {
					sftpChannel.ls(target.replace('/' + mergeUnit.getFileName(), ""));
				} catch (SftpException e) {
					sftpChannel.mkdir(target.replace('/' + mergeUnit.getFileName(), ""));
				}

				sftpChannel.rename(source, target);
				mergeUnit.setRemotePath(target);
			} catch (SftpException e) {
				String message = String.format("Couldn't move mergeUnit=[%s] from source=[%s] to target=[%s].", //$NON-NLS-1$
						mergeUnit, source, target);
				throw LogUtil.throwing(new SftpUtilException(message, e));
			}
		}
		LogUtil.exiting();
	}

	/**
	 * @param mergeunit
	 * @param pathRemote
	 * @throws SftpUtilException
	 */
	private void copyMergeUnitFromWorkToRemote(IMergeUnit mergeUnit, String pathRemote) throws SftpUtilException {
		LogUtil.entering(mergeUnit, pathRemote);
		connectIfNotConnected();
		File fileLocal = new File(Configuration.getPathLocalMergeFile(mergeUnit));
		File fileLocalParent = fileLocal.getParentFile();
		if (!fileLocalParent.exists() && !fileLocalParent.mkdirs()) {
			String message = String.format("Couldn't create local folder. fileLocalParent=[%s].", //$NON-NLS-1$
					fileLocalParent.getAbsolutePath());
			throw LogUtil.throwing(new SftpUtilException(message));
		}

		LOGGER.info(() -> String.format("Copy from local=%s to remote=%s.", fileLocal.getAbsolutePath(), pathRemote)); //$NON-NLS-1$

		try (final InputStream is = new FileInputStream(fileLocal);
				final OutputStream outputStream = sftpChannel.put(pathRemote)) {
			IOUtils.copy(is, outputStream);
		} catch (IOException | SftpException e) {
			String message = String.format("Couldn't copy local=[%s] to remote=[%s].", fileLocal.getAbsolutePath(), pathRemote); //$NON-NLS-1$
			throw new SftpUtilException(message, e);
		}

		mergeUnit.setRemotePath(pathRemote);
		LogUtil.exiting();
	}

	/**
	 * @return the parsed todo files on the sftp server
	 * @throws SftpUtilException
	 */
	public synchronized List<IMergeUnit> getMergeUnitsTodo() throws SftpUtilException {
		LogUtil.entering();
		List<IMergeUnit> mergeUnitsTodo = getMergeUnitsFromFolder(configuration.getSftpConfiguration().getTodoFolder());
		return LogUtil.exiting(mergeUnitsTodo);
	}

	/**
	 * @return the parsed done files on the sftp server
	 * @throws SftpUtilException
	 */
	public synchronized List<IMergeUnit> getMergeUnitsDone() throws SftpUtilException {
		LogUtil.entering();
		List<IMergeUnit> mergeUnitsDone = getMergeUnitsFromFolder(configuration.getSftpConfiguration().getDoneFolder());
		return LogUtil.exiting(mergeUnitsDone);
	}

	/**
	 * @return the parsed ignored files on the sftp server
	 * @throws SftpUtilException
	 */
	public synchronized List<IMergeUnit> getMergeUnitsIgnored() throws SftpUtilException {
		LogUtil.entering();
		List<IMergeUnit> mergeUnitsIgnored = getMergeUnitsFromFolder(
				configuration.getSftpConfiguration().getIgnoredFolder());
		return LogUtil.exiting(mergeUnitsIgnored);
	}

	/**
	 * @return the parsed canceled files on the sftp server
	 * @throws SftpUtilException
	 */
	public synchronized List<IMergeUnit> getMergeUnitsCanceled() throws SftpUtilException {
		LogUtil.entering();
		List<IMergeUnit> mergeUnitsCanceled = getMergeUnitsFromFolder(
				configuration.getSftpConfiguration().getCanceledFolder());
		return LogUtil.exiting(mergeUnitsCanceled);
	}

	public synchronized List<IMergeUnit> getMergeUnitsManual() throws SftpUtilException {
		LogUtil.entering();
		List<IMergeUnit> mergeUnitsManual = getMergeUnitsFromFolder(
				configuration.getSftpConfiguration().getManualFolder());
		return LogUtil.exiting(mergeUnitsManual);
	}

	public InputStream createInputStream(final String path) throws SftpException {
		return sftpChannel.get(path);
	}

	/**
	 * Writes a given {@link String} to a remote {@link Path}. It overwrites any
	 * existing content.
	 * 
	 * @param content the string to write
	 * @param path    the remote {@link Path}
	 * @throws SftpException
	 * @throws IOException
	 * @throws SftpUtilException
	 */
	public void writeToRemotePath(final String content, final String path)
			throws SftpException, IOException, SftpUtilException {
		connectIfNotConnected();
		try (final InputStream is = IOUtils.toInputStream(content, StandardCharsets.UTF_8)) {
			sftpChannel.put(is, path);
		}
	}

	@SuppressWarnings("unchecked")
	private List<IMergeUnit> getMergeUnitsFromFolder(String pathFolder) throws SftpUtilException {
		LogUtil.entering(pathFolder);

		connectIfNotConnected();

		Vector<LsEntry> files = null;

		List<IMergeUnit> mergeunits = new ArrayList<>();

		try {
			LOGGER.fine(() -> String.format("List files from remote=%s.", pathFolder)); //$NON-NLS-1$
			files = sftpChannel.ls(pathFolder);
		} catch (SftpException e) {
			if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
				// Directory does not exist, let's create it
				LOGGER.log(Level.INFO, "File does not exist.", e);
				try {
					sftpChannel.mkdir(pathFolder);
				} catch (SftpException e1) {
					LOGGER.log(Level.SEVERE, "Could not create directory.", e1);
				}
			}
			return mergeunits;
		}

		// filter directory entries '.' and '..'
		files.removeIf(file -> file.getFilename().equals(".") || file.getFilename().equals("..")); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest(String.format("files.size=%s.", files.size())); //$NON-NLS-1$
			}
			for (LsEntry file : files) {
				handleMergeFile(pathFolder, mergeunits, file);
			}
		} catch (SftpException | MergeUnitException e) {
			throw LogUtil.throwing(new SftpUtilException("Caught Exception while parsing files from sftp server.", e)); //$NON-NLS-1$
		}

		return LogUtil.exiting(mergeunits);
	}

	private void handleMergeFile(String pathFolder, List<IMergeUnit> mergeunits, LsEntry file)
			throws MergeUnitException, SftpException, SftpUtilException {
		String fileName = file.getFilename();
		String attributes = file.getLongname();
		String path = pathFolder + fileName;

		LOGGER.fine(() -> String.format("Getting file fileName=%s, path=%s, attributes=%s", fileName, path, //$NON-NLS-1$
				attributes));
		try (InputStream is = sftpChannel.get(path)) {

			IMergeUnit mergeunit = null;
			if (fileName.endsWith(Configuration.EXTENSION_PLAINMERGE_FILE)
					|| fileName.endsWith(Configuration.SVN_EXTENSION_FILE)
					|| fileName.endsWith(Configuration.SVN_PACKAGE_MERGE_EXTENSION_FILE)) {
				LOGGER.fine(() -> String.format("Parsing SVN merge file %s.", path)); //$NON-NLS-1$
				mergeunit = SVNMergeUnitFactory.createMergeUnitFromPlainMergeFile(configuration, path, fileName,
						is);
			} else if (fileName.endsWith(Configuration.GIT_EXTENSION_FILE)) {
				LOGGER.fine(() -> String.format("Parsing GIT merge file %s.", path)); //$NON-NLS-1$
				mergeunit = GITMergeUnitFactory.create(configuration, Paths.get(path), is);
			} else {
				LOGGER.info(() -> String.format("Skipping file fileName=%s, path=%s, attributes=%s", //$NON-NLS-1$
						fileName, path, attributes));
				return;
			}
			mergeunits.add(mergeunit);
		} catch (IOException e) {
			String message = String.format("Caught exception while parsing merge unit from path=[%s].", path); //$NON-NLS-1$
			throw LogUtil.throwing(new SftpUtilException(message, e));
		}
	}

	private void connectIfNotConnected() throws SftpUtilException {
		LogUtil.entering();
		if (sftpChannel == null || !sftpChannel.isConnected()) {
			connect();
		}
		LogUtil.exiting();
	}

	private void connect() throws SftpUtilException {
		LogUtil.entering();

		if (sftpChannel != null) {
			LOGGER.fine("First close old connection."); //$NON-NLS-1$
			disconnect();
		}

		String host = configuration.getSftpConfiguration().getHost();
		String user = configuration.getSftpConfiguration().getUser();
		String password = configuration.getSftpConfiguration().getPassword();
		String workingFolder = Configuration.getPathSftpWorkingFolder();
		String knownHosts = workingFolder + "known_hosts"; //$NON-NLS-1$

		File fWorkingFolder = new File(workingFolder);

		if (!fWorkingFolder.exists()) {
			fWorkingFolder.mkdirs();
		}

		try {
			JSch jsch = new JSch();
			jsch.setKnownHosts(knownHosts);

			session = jsch.getSession(user, host);
			session.setPassword(password);
			// "interactive" version
			session.setUserInfo(new SftpUserInfo(configuration, password));
			session.connect();
			LOGGER.fine("session is connected."); //$NON-NLS-1$

			Channel channel = session.openChannel("sftp"); //$NON-NLS-1$
			channel.connect();
			LOGGER.fine("channel is connected."); //$NON-NLS-1$

			sftpChannel = (ChannelSftp) channel;
			LOGGER.info("sftpChannel is set."); //$NON-NLS-1$
		} catch (JSchException e) {
			disconnect();
			throw LogUtil.throwing(new SftpUtilException("Couldn't connect to sftp server.", e)); //$NON-NLS-1$
		}

		LogUtil.exiting();
	}

	/**
	 * Closes all open connections.
	 */
	public synchronized void disconnect() {
		LogUtil.entering();
		if (sftpChannel != null && sftpChannel.isConnected()) {
			LOGGER.fine("Exiting channel."); //$NON-NLS-1$
			sftpChannel.exit();
			sftpChannel = null;
		}
		if (session != null && session.isConnected()) {
			LOGGER.fine("Disconnecting session."); //$NON-NLS-1$
			session.disconnect();
			session = null;
		}
		LogUtil.exiting();
	}

	/**
	 * Returns the script of the merge unit as a {@link String}.
	 * 
	 * @param mergeUnit the merge unit
	 * @return the script as a {@link String}
	 * @throws SftpUtilException
	 */
	public String getContent(IMergeUnit mergeUnit) throws SftpUtilException {
		LogUtil.entering(mergeUnit);
		connectIfNotConnected();
		final String pathRemote = mergeUnit.getRemotePath();
		try (InputStream is = sftpChannel.get(pathRemote)) {
			return LogUtil.exiting(IOUtils.toString(is, StandardCharsets.UTF_8));
		} catch (IOException | SftpException e) {
			String message = String.format("Couldn't read remote=[%s].", pathRemote); //$NON-NLS-1$
			throw LogUtil.throwing(new SftpUtilException(message, e));
		}
	}

	private static class SftpUserInfo implements UserInfo, UIKeyboardInteractive {

		private final IConfiguration configuration;
		private String password;

		private SftpUserInfo(IConfiguration configuration, String password) {
			this.configuration = configuration;
			this.password = password;
		}

		@Override
		public synchronized void showMessage(String message) {
			LOGGER.info(message);
		}

		@Override
		public synchronized boolean promptYesNo(String message) {
			LOGGER.info(message);
			// We always trust our connections
			return true;
		}

		@Override
		public synchronized boolean promptPassword(String message) {
			LOGGER.info(message);
			return false;
		}

		@Override
		public synchronized boolean promptPassphrase(String message) {
			LOGGER.info(message);
			return false;
		}

		@Override
		public synchronized String getPassword() {
			return password;
		}

		@Override
		public synchronized String getPassphrase() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public synchronized String[] promptKeyboardInteractive(String destination, String name, String instruction,
				String[] prompt, boolean[] echo) {
			LogUtil.entering(destination, name, instruction, prompt, echo);
			String[] retVal = new String[prompt.length];

			if (destination.equals(configuration.getSftpConfiguration().getUser() + "@" //$NON-NLS-1$
					+ configuration.getSftpConfiguration().getHost())) {
				for (int i = 0; i < prompt.length; i++) {
					if (prompt[i].equals("Password: ")) { //$NON-NLS-1$
						retVal[i] = password;
					} else {
						retVal[i] = null;
					}
				}
			}

			return LogUtil.exiting(retVal);
		}
	}
}
