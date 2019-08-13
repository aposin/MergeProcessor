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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Utility class for dealing with files.
 * 
 * @author Stefan Weiser
 *
 */
public class FileUtils {

	/**
	 * The file copy buffer size (5 MB)
	 */
	private static final int FILE_COPY_BUFFER_SIZE = (int) org.apache.commons.io.FileUtils.ONE_MB * 5;

	/**
	 * Copies the file of the source path to the file of the target path. The progress is visualized in the given 
	 * {@link IProgressMonitor}.
	 * 
	 * @param source source path to copy from
	 * @param target target path to copy to
	 * @param monitor the progress monitor to visualize the progress
	 */
	public static boolean copyFiles(final Path source, final Path target, final IProgressMonitor monitorParam) {
		final IProgressMonitor monitor = monitorParam == null ? new NullProgressMonitor() : monitorParam;
		if (source == null || !source.toFile().exists()) {
			return false;
		}
		long size = 0;
		int steps = 0;
		try {
			size = Files.size(source);
			steps = (int) (size / FILE_COPY_BUFFER_SIZE);
		} catch (IOException e) {
			Logger.getLogger(FileUtils.class.getName()).log(Level.WARNING,
					String.format("Could not evaluate size of %s.", target), e); //$NON-NLS-1$
		}
		monitor.beginTask(String.format(Messages.FileUtils2_copyTask, source, target), steps);
		monitor.subTask(size > 0 ? String.format(Messages.FileUtils2_copyZeroCopied, '%', size) : "..."); // $NON-NLS-2$
		try (final InputStream is = new FileInputStream(source.toFile());
				final OutputStream os = new FileOutputStream(target.toFile())) {
			byte[] buffer = new byte[FILE_COPY_BUFFER_SIZE];
			int length;
			double copied = 0;
			while ((length = is.read(buffer)) > 0) {
				if (monitor.isCanceled()) {
					break;
				}
				os.write(buffer, 0, length);
				copied += length;
				monitor.worked(1);
				if (size > 0) {
					monitor.subTask(String.format(Messages.FileUtils2_copyXCopied, (int) ((copied / size) * 100), '%',
							((long) copied), size));
				}
			}
		} catch (IOException e) {
			Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE,
					String.format("Could not copy from %s to %s.", source, target), e); //$NON-NLS-1$
			deleteUnfinishedFile(target);
		}
		try {
			Files.setLastModifiedTime(target, Files.getLastModifiedTime(source));
		} catch (IOException e) {
			Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE,
					String.format("Exception occurred on setting the last modified time to %s", target), e); //$NON-NLS-1$
		}
		if (monitor.isCanceled()) {
			monitor.worked(steps);
			monitor.subTask(Messages.FileUtils2_copyCancelClean);
			deleteUnfinishedFile(target);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Tries to delete the target file. If delete job is not successful, it retries several
	 * times (10x).
	 * 
	 * @param target the target file to delete
	 */
	private static void deleteUnfinishedFile(final Path target) {
		if (target.toFile().exists()) {
			try {
				Thread.sleep(250); // Wait so the file may be not locked any more.
				for (int i = 0; i < 10; i++) {
					try {
						Files.delete(target);
						break; // Success
					} catch (IOException e) {
						LogUtil.throwing(e);
						Thread.sleep(250);
					}
				}
			} catch (InterruptedException e) {
				Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE,
						String.format("Could not delete unfinished copied file %s.", target), e); //$NON-NLS-1$
			}
		}
	}

}
