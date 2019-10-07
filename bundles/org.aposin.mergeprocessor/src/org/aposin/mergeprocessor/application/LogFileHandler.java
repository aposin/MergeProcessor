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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.aposin.mergeprocessor.configuration.Configuration;

import com.ibm.icu.text.SimpleDateFormat;

/**
 * {@link FileHandler} logging to the log folder of the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public class LogFileHandler extends FileHandler {

	private static final Format DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public LogFileHandler() throws IOException {
		super(getPattern());
		setFormatter(new LogFileFormatter());
	}

	private static String getPattern() {
		final Date date = new Date();
		final Format fileDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		final String pathLogFileFolder = Configuration.getPathLogFileFolder();
		final Path logFileFolder = Paths.get(pathLogFileFolder);
		if (Files.notExists(logFileFolder)) {
			try {
				Files.createDirectories(logFileFolder);
			} catch (IOException e) {
				Logger.getLogger(LogFileHandler.class.getName()).log(Level.SEVERE, "Could not create log file folder",
						e);
			}
		}
		return pathLogFileFolder + "mp_" + fileDateFormat.format(date) + ".log";
	}

	/**
	 * Formatter for the {@link LogFileHandler}.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static final class LogFileFormatter extends SimpleFormatter {

		private final Date dat = new Date();

		/**
		 * {@inheritDoc}
		 */
		@Override
		public synchronized String format(LogRecord record) {
			dat.setTime(record.getMillis());
			final StringBuilder sb = new StringBuilder(DATE_FORMATER.format(dat));
			sb.append(' ');
			sb.append(record.getSourceClassName());
			sb.append(' ');
			sb.append(record.getSourceMethodName());
			sb.append(System.lineSeparator());
			sb.append(record.getLevel().getLocalizedName());
			sb.append(':').append(' ');
			sb.append(formatMessage(record));
			if (record.getThrown() != null) {
				final StringWriter sw = new StringWriter();
				try (final PrintWriter pw = new PrintWriter(sw)) {
					pw.println();
					record.getThrown().printStackTrace(pw);
				}
				sb.append(sw.toString());
			}
			sb.append(System.lineSeparator());
			return sb.toString();
		}
	}

}