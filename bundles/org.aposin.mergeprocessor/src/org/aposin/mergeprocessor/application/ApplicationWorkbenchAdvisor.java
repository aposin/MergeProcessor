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
package org.aposin.mergeprocessor.application;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.configuration.WorkbenchPreferencePage;
import org.aposin.mergeprocessor.utils.CommandLineArgsUtil;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.SftpUtil;
import org.aposin.mergeprocessor.utils.ShutdownHook;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 *
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final Logger LOGGER = Logger.getLogger(ApplicationWorkbenchAdvisor.class.getName());
	private static final String PERSPECTIVE_ID = "org.aposin.mergeprocessor.perspective"; //$NON-NLS-1$

	private RandomAccessFile raf;
	private FileChannel fc;
	private FileLock lockSingleInstance;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void preStartup() {
		super.preStartup();
		if (!checkDefinedWorkingFolder()) {
			return;
		}
		if (!checkSingleInstance()) {
			return;
		}
		logStartup();

		// add shutdown hook
		ShutdownHook shutdownHook = ShutdownHook.getInstance();
		shutdownHook.activate();
		LogUtil.exiting();
	}

	/**
	 * Checks if a working folder is defined. Without a working folder it is not possible to start
	 * the application. Inform the user that something went wrong during installation.
	 * 
	 * @return {@code true} if the defined working folder is valid
	 */
	private boolean checkDefinedWorkingFolder() {
		final String workingFolder = Configuration.getWorkingFolder();
		String errorMessage = null;
		Throwable throwable = null;
		if (!StringUtils.isEmpty(workingFolder)) {
			final Path path = Paths.get(workingFolder);
			if (Files.exists(path)) {
				if (Files.isDirectory(path)) {
					return true;
				} else {
					errorMessage = String.format(
							"The given path '%s' already exists as file but is configured as directory. Please remove or rename the file or define an alternative working folder.",
							workingFolder);
				}
			} else {
				try {
					Files.createDirectory(path);
					return true;
				} catch (IOException e) {
					LogUtil.throwing(e);
					errorMessage = "The given directory '%s' could not be created as working folder.";
					throwable = e;
				}
			}
		}
		if (errorMessage == null) {
			errorMessage = "No valid working folder is defined by the property 'WORKING_FOLDER'. Please contact support as the installation did not work correctly.";
		}
		StatusManager.getManager().handle(ValidationStatus.error(errorMessage, throwable),
				StatusManager.BLOCK | StatusManager.SHOW);
		getWorkbenchConfigurer().emergencyClose();
		return false;
	}

	/**
	 * Checks if another instance of MergeProcessor is running. 
	 * 
	 * @return {@code true} if no other instance of MergeProcessor is running
	 */
	private boolean checkSingleInstance() {
		if (lockFileIfSingleInstance()) {
			// acquired lock for lock file
			CommandLineArgsUtil.parseCommandLineArgs();

			IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
			preferenceStore.addPropertyChangeListener(ApplicationWorkbenchAdvisor::handlePropertyChange);
			try {
				Logger.getLogger("").addHandler(new LogFileHandler());
			} catch (SecurityException | IOException e) {
				LogUtil.throwing(e);
			}
			Logger.getLogger("").setLevel(Configuration.getLogLevel());
			return true;
		} else {
			LOGGER.severe(Messages.ApplicationWorkbenchAdvisor_AnotherInstance_Message);
			StatusManager.getManager().handle(
					ValidationStatus.error(Messages.ApplicationWorkbenchAdvisor_AnotherInstance_Message),
					StatusManager.BLOCK | StatusManager.SHOW);
			getWorkbenchConfigurer().emergencyClose();
			return false;
		}
	}

	/**
	 * Logs the startup of the MergeProcessor.
	 */
	private static void logStartup() {
		String localHostIp = "undef"; //$NON-NLS-1$
		String localHostname = "undef"; //$NON-NLS-1$
		try {
			InetAddress addr = InetAddress.getLocalHost();
			localHostIp = addr.getHostAddress();
			localHostname = addr.getCanonicalHostName();
		} catch (UnknownHostException e) {
			LOGGER.log(Level.WARNING, "Couldn't get local ip address and local hostname.", e); //$NON-NLS-1$
		}

		String windowsDomain = System.getenv("USERDOMAIN"); //$NON-NLS-1$

		if (LOGGER.isLoggable(Level.INFO)) {
			final IConfiguration configuration = E4CompatibilityUtil.getApplicationContext().get(IConfiguration.class);
			LOGGER.info(String.format(
					"MergeProcessor is starting up. version=%s, user=%s, windowsDomain=%s, localHostname=%s, localHostIp=%s sftpHost=%s", //$NON-NLS-1$
					Configuration.getVersion(), configuration.getUser(), windowsDomain, localHostname, localHostIp,
					configuration.getSftpConfiguration().getHost()));
		}
	}

	/**
	 * Handle the property change event
	 * 
	 * @param event the event
	 */
	private static void handlePropertyChange(PropertyChangeEvent event) {
		LogUtil.entering(event);
		String property = event.getProperty();

		if (property == null) {
			LOGGER.fine(() -> String.format(
					"Ignoring event with null property: property=%s, newValue=%s, oldValue=%s, source=%s, ", //$NON-NLS-1$
					property, event.getNewValue(), event.getOldValue(), event.getSource()));
		} else if (event.getNewValue() == null) {
			LOGGER.fine(() -> String.format(
					"Ignoring event with null newValue: property=%s, newValue=%s, oldValue=%s, source=%s, ", //$NON-NLS-1$
					property, event.getNewValue(), event.getOldValue(), event.getSource()));
		} else {
			if (WorkbenchPreferencePage.LOG_LEVEL.equals(property)) {
				Logger.getLogger("").setLevel(Configuration.getLogLevel());
			}
		}
	}

	/**
	 * Tries to acquire the <code>lock</code> for the lock file.
	 * @return <code>true</code> if the was acquired by this process.
	 */
	private boolean lockFileIfSingleInstance() {
		// Only close the resources raf and fc in case we didn't acquire the lock.
		// If we would close the resources when we had the lock, we would also lose the
		// lock.
		// So we cannot use try-with-resource here.
		File fLockFile = new File(Configuration.getPathLockFile());
		if (!fLockFile.exists()) {
			File parent = fLockFile.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
		}

		try {
			raf = new RandomAccessFile(fLockFile, "rw"); //$NON-NLS-1$
			fc = raf.getChannel();
			lockSingleInstance = fc.tryLock();
		} catch (IOException e) {
			LogUtil.throwing(e);
			releaseAndCloseFileLock();
			return false;
		}

		return lockSingleInstance == null ? false : lockSingleInstance.isValid();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean preShutdown() {
		LogUtil.entering();
		// close SFTP session
		SftpUtil.getInstance().disconnect();
		releaseAndCloseFileLock();
		LOGGER.info("MergeProcessor is shutting down"); //$NON-NLS-1$
		return LogUtil.exiting(true);
	}

	/**
	 * Releases and closes the file lock.
	 */
	private void releaseAndCloseFileLock() {
		try (RandomAccessFile raf1 = raf; //
				FileChannel fc1 = fc; //
				FileLock lockSingleInstance1 = lockSingleInstance) {
			if (lockSingleInstance1 != null) {
				lockSingleInstance1.release();
			}
		} catch (IOException e1) {
			LogUtil.throwing(e1);
		}
	}

}
