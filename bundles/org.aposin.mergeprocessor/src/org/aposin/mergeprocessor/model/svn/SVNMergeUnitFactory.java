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
package org.aposin.mergeprocessor.model.svn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.MergeUnitException;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.utils.LogUtil;

/**
 *
 */
public final class SVNMergeUnitFactory {

	private static final Logger LOGGER = Logger.getLogger(SVNMergeUnitFactory.class.getName());

	/**
	 * Symbol to identify URL of the source branch
	 */
	public static final String URL_BRANCH_SOURCE = "URL_BRANCH_SOURCE="; //$NON-NLS-1$

	/**
	 * Symbol to identify URL of the target branch
	 */
	public static final String URL_BRANCH_TARGET = "URL_BRANCH_TARGET="; //$NON-NLS-1$

	/**
	 * Symbol to identify URL a working copy file
	 */
	public static final String WORKING_COPY_FILE = "WORKING_COPY_FILE="; //$NON-NLS-1$

	/**
	 * Symbol to identify the start revision
	 */
	public static final String REVISION_START = "REVISION_START="; //$NON-NLS-1$

	/**
	 * Symbol to identify the end revision
	 */
	public static final String REVISION_END = "REVISION_END="; //$NON-NLS-1$

	/**
	 * Symbol to identify commented lines which are ignored
	 */
	public static final String SYMBOL_COMMENT = "#"; //$NON-NLS-1$
	public static final String SYMBOL_COMMENT_COMPLETED = SYMBOL_COMMENT + " COMPLETED: "; //$NON-NLS-1$

	/**
	 * Symbol to identify warning lines which the user must resolve
	 */
	public static final String SYMBOL_WARNING = "> Warning: "; //$NON-NLS-1$

	/**
	 * Symbol to identify warning lines which the user should notice
	 */
	public static final String SYMBOL_INFORMATION = "> Information: "; //$NON-NLS-1$

	/**
	 * Symbol to separte the file name segments
	 */
	public static final String SYMBOL_FILENAME_SEPARATOR = "_"; //$NON-NLS-1$

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_Z"); //$NON-NLS-1$
	private static final Pattern PATTERN_DATE_MATCH = Pattern
			.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_(\\+|\\-)\\d\\d\\d\\d"); //$NON-NLS-1$
	private static final Pattern PATTERN_REMOVE_ALL_EXCLUDE_REPONAME = Pattern
			.compile("_r\\d*_\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_(\\+|\\-)\\d\\d\\d\\d.*"); //$NON-NLS-1$

	private SVNMergeUnitFactory() {
		// Static factory class
	}

	public static SVNMergeUnit2 createMergeUnit(final IConfiguration configuration, final Path remotePath,
			final InputStream is) throws MergeUnitException {
		final MergeUnitStatus status = getMergeUnitStatus(configuration, remotePath.toString());
		try {
			return new SVNMergeUnit2(remotePath, IOUtils.toString(is, StandardCharsets.UTF_8), status);
		} catch (IOException e) {
			String message = String.format("Couldn't parse merge unit file. fileName=[%s]", remotePath); //$NON-NLS-1$
			throw LogUtil.throwing(new MergeUnitException(message, e));
		}
	}

	/**
	 * Parse the merge script file for the given path and initialize an instance
	 * of this class.
	 * 
	 * @param pathMergeScript
	 *            the path of the merge script
	 * @param fileName
	 * @param is
	 * @return the created {@link SVNMergeUnit}
	 * @throws MergeUnitException
	 */
	public static SVNMergeUnit createMergeUnitFromPlainMergeFile(final IConfiguration configuration,
			String pathMergeScript, String fileName, InputStream is) throws MergeUnitException {
		LogUtil.entering(configuration, pathMergeScript, fileName, is);

		int revisionWorkingCopy = -1;
		final SVNMergeUnitFileData fileData;
		try {
			fileData = new SVNMergeUnitFileData(is);
		} catch (IOException | NumberFormatException e) {
			String message = String.format("Couldn't parse merge unit file. fileName=[%s]", fileName); //$NON-NLS-1$
			throw LogUtil.throwing(new MergeUnitException(message, e));
		}

		final LocalDateTime localDate = getDate(fileName);
		final String repositoryName = getRepositoryName(fileName);
		final MergeUnitStatus status = getMergeUnitStatus(configuration, pathMergeScript);
		final ListTuple<String> neededWorkingCopyFiles = getNeededWorkingCopyFiles(fileData.changedFiles);
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(configuration.getSftpConfiguration().getHost(), repositoryName,
				localDate, status, fileData.revisionStart, fileData.revisionEnd, fileData.urlBranchSource,
				fileData.urlBranchTarget, pathMergeScript, revisionWorkingCopy, neededWorkingCopyFiles.listA,
				neededWorkingCopyFiles.listB, getTargetFilesToDelete(fileData), getTargetFilesToAdd(fileData),
				configuration);

		// sanity check
		if (!mergeUnit.isValid()) {
			String message = String.format("Parsed mergeUnit is invalid. fileName=[%s], mergeUnit=[%s]", fileName, //$NON-NLS-1$
					mergeUnit);
			throw LogUtil.throwing(new MergeUnitException(message));
		}
		return LogUtil.exiting(mergeUnit);
	}

	private static List<String> getTargetFilesToDelete(SVNMergeUnitFileData fileData) {
		final List<String> result = new ArrayList<>();
		for (final String changedFile : fileData.changedFiles) {
			if (changedFile.charAt(0) == 'D') {
				if (changedFile.indexOf('>') >= 0) {
					result.add(changedFile.substring(4).split(">")[1].replaceFirst("branches/[^/]+|trunk", ""));
				} else {
					result.add(changedFile.substring(4).replaceFirst("branches/[^/]+|trunk", ""));
				}

			}
		}
		return result;
	}

	private static List<String> getTargetFilesToAdd(SVNMergeUnitFileData fileData) {
		final List<String> result = new ArrayList<>();
		for (final String changedFile : fileData.changedFiles) {
			if (changedFile.charAt(0) == 'A') {
				if (changedFile.indexOf('>') >= 0) {
					result.add(changedFile.substring(4).split(">")[1].replaceFirst("branches/[^/]+|trunk", ""));
				} else {
					result.add(changedFile.substring(4).replaceFirst("branches/[^/]+|trunk", ""));
				}

			}
		}
		return result;
	}

	/**
	 * Extracts a {@link LocalDateTime} of the given {@link String}. If no {@link LocalDateTime} could 
	 * be parsed a {@link MergeUnitException} is thrown. If more than 1 date is found in the given index, 
	 * the first match gets parsed.
	 * 
	 * @param fileName the {@link String} to parse
	 * @return the {@link LocalDateTime}
	 * @throws MergeUnitException if no {@link LocalDateTime} could be parsed
	 */
	private static LocalDateTime getDate(String fileName) throws MergeUnitException {
		final Matcher matcher = PATTERN_DATE_MATCH.matcher(fileName);
		if (matcher.find()) {
			final String match = matcher.group(0);
			if (LOGGER.isLoggable(Level.WARNING) && matcher.find()) {
				LOGGER.warning(String.format("More than 1 Date found in '%s'", fileName)); //$NON-NLS-1$
			}
			return LocalDateTime.parse(match, DATE_TIME_FORMATTER);
		} else {
			throw LogUtil.throwing(new MergeUnitException(
					String.format("No LocalDateTime identified in the given file name '%s'", fileName))); //$NON-NLS-1$
		}
	}

	/**
	 * Extracts the repository name of the given {@link String}. If no repository name could be parsed a
	 * {@link MergeUnitException} is thrown.
	 *  
	 * @param fileName the {@link String} to parse
	 * @return the repository name
	 * @throws MergeUnitException if no repository name could be parsed
	 */
	private static String getRepositoryName(String fileName) throws MergeUnitException {
		final Matcher matcher = PATTERN_REMOVE_ALL_EXCLUDE_REPONAME.matcher(fileName);
		if (matcher.find()) {
			final String repository = matcher.replaceAll(""); //$NON-NLS-1$
			if (!repository.isEmpty()) {
				return repository;
			}
		}
		throw LogUtil.throwing(new MergeUnitException(
				String.format("No repository identified in the given file name '%s'", fileName))); //$NON-NLS-1$
	}

	/**
	 * Returns the {@link MergeUnitStatus} for  the given path of the merge script.
	 * 
	 * @param configuration the configuration
	 * @param pathMergeScript the path of the merge script
	 * @return the status
	 * @throws MergeUnitException
	 */
	private static MergeUnitStatus getMergeUnitStatus(final IConfiguration configuration, final String pathMergeScript)
			throws MergeUnitException {
		final MergeUnitStatus status;
		if (pathMergeScript.startsWith(configuration.getSftpConfiguration().getTodoFolder())) {
			status = MergeUnitStatus.TODO;
		} else if (pathMergeScript.startsWith(configuration.getSftpConfiguration().getDoneFolder())) {
			status = MergeUnitStatus.DONE;
		} else if (pathMergeScript.startsWith(configuration.getSftpConfiguration().getIgnoredFolder())) {
			status = MergeUnitStatus.IGNORED;
		} else if (pathMergeScript.startsWith(configuration.getSftpConfiguration().getCanceledFolder())) {
			status = MergeUnitStatus.CANCELLED;
		} else if (pathMergeScript.startsWith(configuration.getSftpConfiguration().getManualFolder())) {
			status = MergeUnitStatus.MANUAL;
		} else {
			String message = String.format("Couldn't find correct status for merge file with path=[%s].", //$NON-NLS-1$
					pathMergeScript);
			throw LogUtil.throwing(new MergeUnitException(message));
		}
		return status;
	}

	/**
	 * Returns a list of required working copy files for the given change set.
	 * 
	 * @param changedFiles the change set
	 * @return a list of required working copy files
	 * @throws MergeUnitException
	 */
	private static ListTuple<String> getNeededWorkingCopyFiles(Set<String> changedFiles) throws MergeUnitException {
		boolean fileSeparatedMergeNeeded = false;
		for (final String line : changedFiles) {
			if (line.indexOf('>') != -1) {
				// File separated merge required
				fileSeparatedMergeNeeded = true;
				break;
			}
		}

		if (fileSeparatedMergeNeeded) {
			final List<String> fromList = new ArrayList<>(changedFiles.size());
			final List<String> toList = new ArrayList<>(changedFiles.size());
			for (final String line : changedFiles) {
				final String svnInfo = line.substring(0, 4);
				final String lineWithoutSvnInfo = line.substring(4, line.length());
				final String[] split = lineWithoutSvnInfo.split(">");
				assert (split.length == 2);
				fromList.add(svnInfo + split[0].trim());
				toList.add(svnInfo + split[1].trim());
			}
			return new ListTuple<>(getNeededWorkingCopyFiles(fromList), getNeededWorkingCopyFiles(toList));
		} else {
			final List<String> neededFiles = getNeededWorkingCopyFiles(new ArrayList<>(changedFiles));
			return new ListTuple<>(neededFiles);
		}
	}

	private static List<String> getNeededWorkingCopyFiles(final Collection<String> changedFiles)
			throws MergeUnitException {
		final ListTuple<File> addedAndNotAddedFiles = getAddedAndNotAddedFiles(changedFiles);
		final List<File> listAddedFiles = addedAndNotAddedFiles.listA;

		// check if parent is already in listAddedFiles. if not add it to
		// listNeededWorkingCopyFiles
		final List<File> listNeededWorkingCopyFiles = new ArrayList<>();
		for (File neededWorkingCopyFile : addedAndNotAddedFiles.listB) {
			File parent = neededWorkingCopyFile;
			boolean found = false;
			while ((parent = parent.getParentFile()) != null) {
				if (listAddedFiles.contains(parent)) {
					/*
					 * if parent is just added with this merge, this element is not available yet in
					 * this branch so it can't be a needed file
					 */
					found = true;
					break;
				}
			}

			if (!found) {
				listNeededWorkingCopyFiles.add(neededWorkingCopyFile);
			}
		}

		for (File addedFile : listAddedFiles) {
			/*
			 * Also add new files to needed one as it is possible that the file already
			 * exists in the target branch. Otherwise the merge does not work, as SVN claims
			 * that the working copy is not up-to-date.
			 */
			listNeededWorkingCopyFiles.add(addedFile);
			File parent = addedFile.getParentFile();
			if (parent == null) {
				String message = String.format("Added file seems to have no parent. addedFile=[%s]", addedFile); //$NON-NLS-1$
				throw LogUtil.throwing(new MergeUnitException(message));
			} else {
				/*
				 * parent is already a needed file -> nothing to do... OR parent is already an
				 * added file. it will be tested if the parents parent is a needed file in the
				 * parents iteration -> nothing to do...
				 */
				if (!listNeededWorkingCopyFiles.contains(parent) && !listAddedFiles.contains(parent)) {
					listNeededWorkingCopyFiles.add(parent);
				}
			}
		}

		final List<String> result = new ArrayList<>(listNeededWorkingCopyFiles.size());
		for (final File file : listNeededWorkingCopyFiles) {
			result.add(file.getPath());
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Get the lists for added and for not added files. In the {@link ListTuple} list A 
	 * contains the added files, list B the not added files.
	 * 
	 * @param changedFiles the changed files of the merge unit
	 * @return the {@link ListTuple} with the added files (list A) and the not added files (list B)
	 * @throws MergeUnitException 
	 */
	private static ListTuple<File> getAddedAndNotAddedFiles(Collection<String> changedFiles) throws MergeUnitException {
		final List<File> listAddedFiles = new ArrayList<>();
		final List<File> listNotAddedFiles = new ArrayList<>();
		for (String changedFile : changedFiles) {
			final File element;
			if (changedFile.indexOf('>') == -1) {
				element = new File(changedFile.replaceFirst(".{4}(branches/[^/]+|trunk)", "")); //$NON-NLS-1$ //$NON-NLS-2$
				// ignore the first 4 chars at the beginning of the value,
				// because they are just svn info and not part of the path
			} else {
				// Split merge required
				final String[] split = changedFile.split(">"); //$NON-NLS-1$
				if (split.length == 2) {
					final String target = split[1].trim();
					element = new File(target.replaceFirst("branches/[^/]+|trunk", "")); //$NON-NLS-1$ //$NON-NLS-2$
					// ignore the first 4 chars at the beginning of the value,
					// because they are just svn info and not part of the path
				} else {
					throw LogUtil.throwing(new MergeUnitException(
							String.format("Unknown format of changed files: '%s'", changedFile))); //$NON-NLS-1$
				}
			}
			if (changedFile.charAt(0) == 'A' || changedFile.charAt(0) == 'D') { // $NON-NLS-1$
				listAddedFiles.add(element);
			} else {
				listNotAddedFiles.add(element);
			}

		}
		return new ListTuple<>(listAddedFiles, listNotAddedFiles);
	}

	/**
	 * Container object for 2 lists.
	 * 
	 * @author Stefan Weiser
	 *
	 * @param <T> the type of the elements in the lists
	 */
	private static class ListTuple<T> {

		private final List<T> listA;
		private final List<T> listB;

		/**
		 * @param list list representing list A and list B
		 */
		private ListTuple(List<T> list) {
			this(list, list);
		}

		/**
		 * @param listA list A
		 * @param listB list B
		 */
		private ListTuple(List<T> listA, List<T> listB) {
			this.listA = listA;
			this.listB = listB;
		}

	}

	/**
	 * Container object providing the 
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class SVNMergeUnitFileData {

		private long revisionStart = -1;
		private long revisionEnd = -1;
		private String urlBranchSource = null;
		private String urlBranchTarget = null;
		private HashSet<String> changedFiles = new HashSet<>();

		public SVNMergeUnitFileData(final InputStream is) throws IOException {
			for (final String line : IOUtils.readLines(is, StandardCharsets.UTF_8)) {
				if (line.isEmpty()) {
					if (LOGGER.isLoggable(Level.FINER)) {
						LOGGER.finer(String.format("Skipped empty line=%s", line)); //$NON-NLS-1$
					}
				} else if (line.startsWith(SYMBOL_COMMENT)) {
					if (LOGGER.isLoggable(Level.FINER)) {
						LOGGER.finer(String.format("Skipped comment line=%s", line)); //$NON-NLS-1$
					}
				} else if (line.startsWith(URL_BRANCH_SOURCE)) {
					urlBranchSource = line.substring(URL_BRANCH_SOURCE.length());
				} else if (line.startsWith(URL_BRANCH_TARGET)) {
					urlBranchTarget = line.substring(URL_BRANCH_TARGET.length());
				} else if (line.startsWith(REVISION_START)) {
					revisionStart = Integer.parseInt(line.substring(REVISION_START.length()));
				} else if (line.startsWith(REVISION_END)) {
					revisionEnd = Integer.parseInt(line.substring(REVISION_END.length()));
				} else if (line.startsWith(WORKING_COPY_FILE)) {
					changedFiles.add(line.substring(WORKING_COPY_FILE.length()));
				} else {
					if (LOGGER.isLoggable(Level.WARNING)) {
						LOGGER.warning(String.format("Skipped unknown line=%s", line)); //$NON-NLS-1$
					}
				}
			}
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine(String.format(
						"Parsed file to revisionStart=%s, revisionEnd=%s, urlSource=%s, urlBranchTarget=%s, neededWorkingCopyFiles=%s", //$NON-NLS-1$
						revisionStart, revisionEnd, urlBranchSource, urlBranchTarget,
						Arrays.toString(changedFiles.toArray(new String[0]))));
			}
		}

	}
}
