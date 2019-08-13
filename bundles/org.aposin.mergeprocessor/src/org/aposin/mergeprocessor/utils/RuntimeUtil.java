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
package org.aposin.mergeprocessor.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

/**
 * Utility class when dealing with {@link Runtime}. 
 * 
 * @author Stefan Weiser
 *
 */
public class RuntimeUtil {

	private static final Logger LOGGER = Logger.getLogger(RuntimeUtil.class.getName());

	private RuntimeUtil() {
		// Utility class with only static methods
	}

	/**
	 * Executes a given runtime command.
	 * 
	 * @param command the command to execute
	 * @param path the path where to run
	 * @return a new instance of {@link CmdResult} containing the standard output
	 * @throws CmdUtilException
	 */
	public static CmdResult exec(final String command) throws CmdUtilException {
		return exec(command, null);
	}

	/**
	 * Executes a given runtime command.
	 * 
	 * @param command the command to execute
	 * @param path the path where to run
	 * @return a new instance of {@link CmdResult} containing the standard output
	 * @throws CmdUtilException
	 */
	public static CmdResult exec(final String command, final Path path) throws CmdUtilException {
		try {
			LOGGER.info(() -> (path == null ? "" : path.toString()) + '>' + command);
			final Process exec;
			if (path == null) {
				exec = Runtime.getRuntime().exec(command);
			} else {
				exec = Runtime.getRuntime().exec(command, null, path.toFile());
			}
			try (final InputStream error = exec.getErrorStream(); final InputStream output = exec.getInputStream()) {
				LineIterator it1 = IOUtils.lineIterator(output, StandardCharsets.UTF_8);
				final StringBuilder sbOutput = new StringBuilder();
				while (it1.hasNext()) {
					sbOutput.append(it1.nextLine());
					sbOutput.append(System.lineSeparator());
				}
				LineIterator it2 = IOUtils.lineIterator(error, StandardCharsets.UTF_8);
				final StringBuilder sbError = new StringBuilder();
				while (it2.hasNext()) {
					sbError.append(it2.nextLine());
					sbError.append(System.lineSeparator());
				}
				return new CmdResult(sbOutput.toString().getBytes(), sbError.toString().getBytes());
			}
		} catch (IOException e) {
			throw LogUtil.throwing(new CmdUtilException(e));
		}
	}

	/**
	 * A wrapper object for the result of a runtime command. The result is accessible by different methods. 
	 * If the result is consumed by calling a method, the content is not available any more.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	public static final class CmdResult {

		private final byte[] standardOutput;
		private final byte[] errorOutput;

		private CmdResult(final byte[] inputStream, final byte[] errorStream) {
			standardOutput = inputStream;
			errorOutput = errorStream;
		}

		/**
		 * Returns the standard output as {@link String}. If an exception occurs on reading the output {@code null} is
		 * returned and a log message is written.
		 * 
		 * @return the standard output as String or {@code null} on an error
		 */
		public String getString() {
			try {
				return IOUtils.toString(standardOutput, StandardCharsets.UTF_8.toString());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
		}

		/**
		 * @return the standard output as integer
		 */
		public int getInteger() {
			return Integer.parseInt(getString());
		}

		/**
		 * Return the standard output as a list of {@link String} lines. If an exception occurs on reading the output
		 * an empty list is returned and a log message is written
		 * 
		 * @return the standard output as list with the single lines or an empty list on an error
		 */
		public List<String> getLines() {
			try (final Reader reader = new InputStreamReader(new ByteArrayInputStream(standardOutput))) {
				return IOUtils.readLines(reader);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				return Collections.emptyList();
			}
		}

		/**
		 * Returns the error output as {@link String}. If an exception occurs on reading the output {@code null} is
		 * returned and a log message is written.
		 * 
		 * @return the standard output as String or {@code null} on an error
		 */
		public String getErrorOutputAsString() {
			try {
				return IOUtils.toString(errorOutput, StandardCharsets.UTF_8.toString());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
		}

	}

	/**
	 * Exception which gets only thrown by {@link RuntimeUtil}.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	public static final class CmdUtilException extends Exception {

		private static final long serialVersionUID = -7338092212474551568L;

		/**
		 * @param  cause the cause (which is saved for later retrieval by the
		 *         {@link #getCause()} method).  (A <tt>null</tt> value is
		 *         permitted, and indicates that the cause is nonexistent or
		 *         unknown.)
		 */
		private CmdUtilException(Throwable cause) {
			super(cause);
		}

	}

}
