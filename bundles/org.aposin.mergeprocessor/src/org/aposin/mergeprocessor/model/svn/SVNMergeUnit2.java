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
package org.aposin.mergeprocessor.model.svn;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitException;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.utils.SvnUtil;

/**
 * Contains metadata of the merge file.
 */
public final class SVNMergeUnit2 implements IMergeUnit {

	private static final Logger LOGGER = Logger.getLogger(SVNMergeUnit2.class.getName());
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_Z"); //$NON-NLS-1$

	private static final Pattern PATTERN_DATE_MATCH = Pattern
			.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_(\\+|\\-)\\d\\d\\d\\d"); //$NON-NLS-1$
	private static final Pattern PATTERN_REMOVE_ALL_EXCLUDE_REPONAME = Pattern
			.compile("_r\\d*_\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d_(\\+|\\-)\\d\\d\\d\\d.*"); //$NON-NLS-1$
	private static final Pattern PATTERN_REVISION_START = Pattern.compile("REVISION_START=(\\d)*");
	private static final Pattern PATTERN_REVISION_END = Pattern.compile("REVISION_END=(\\d)*");
	private static final Pattern PATTERN_URL_BRANCH_SOURCE = Pattern.compile("URL_BRANCH_SOURCE=[^\\r\\n]*");
	private static final Pattern PATTERN_URL_BRANCH_TARGET = Pattern.compile("URL_BRANCH_TARGET=[^\\r\\n]*");

	private static final String REVISION_START = "REVISION_START=";
	private static final String REVISION_END = "REVISION_END=";
	private static final String URL_BRANCH_SOURCE = "URL_BRANCH_SOURCE=";
	private static final String URL_BRANCH_TARGET = "URL_BRANCH_TARGET=";

	private final Path remotePath;
	private final String content;

	private MergeUnitStatus status;

	/**
	 * @param remotePath the remote path of the merge unit
	 * @param content the content as a {@link String}
	 * @param status the merge status
	 */
	public SVNMergeUnit2(final Path remotePath, final String content, final MergeUnitStatus status) {
		this.remotePath = Objects.requireNonNull(remotePath);
		this.content = Objects.requireNonNull(content);
		this.status = Objects.requireNonNull(status);
	}

	/**
	 * @return the date
	 * @throws MergeUnitException 
	 */
	public LocalDateTime getDate() {
		final String fileName = getFileName();
		final Matcher matcher = PATTERN_DATE_MATCH.matcher(fileName);
		if (matcher.find()) {
			final String match = matcher.group(0);
			if (LOGGER.isLoggable(Level.WARNING) && matcher.find()) {
				LOGGER.warning(String.format("More than 1 Date found in '%s'", fileName)); //$NON-NLS-1$
			}
			return LocalDateTime.parse(match, DATE_TIME_FORMATTER);
		} else {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.warning(String.format("No LocalDateTime identified in the given file name '%s'", fileName));
			}
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MergeUnitStatus getStatus() {
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatus(MergeUnitStatus status) {
		this.status = status;
	}

	/**
	 * @return the revisionStart
	 */
	public long getRevisionStart() {
		final Matcher matcher = PATTERN_REVISION_START.matcher(content);
		if (matcher.find()) {
			final String match = matcher.group(0);
			return Long.valueOf(match.replace(REVISION_START, ""));
		} else {
			LOGGER.warning("No start revision identified");
			return 0l;
		}
	}

	/**
	 * @return the revisionEnd
	 */
	public long getRevisionEnd() {
		final Matcher matcher = PATTERN_REVISION_END.matcher(content);
		if (matcher.find()) {
			final String match = matcher.group(0);
			return Long.valueOf(match.replace(REVISION_END, ""));
		} else {
			LOGGER.warning("No end revision identified");
			return 0l;
		}
	}

	/**
	 * @return the urlSource
	 */
	public String getUrlSource() {
		final Matcher matcher = PATTERN_URL_BRANCH_SOURCE.matcher(content);
		if (matcher.find()) {
			final String match = matcher.group(0);
			final String urlSource = match.replace(URL_BRANCH_SOURCE, "");
			if (!urlSource.isEmpty()) {
				return urlSource;
			}
		}
		LOGGER.warning("No URL source identified");
		return null;

	}

	/**
	 * @return the urlTarget
	 */
	public String getUrlTarget() {
		final Matcher matcher = PATTERN_URL_BRANCH_TARGET.matcher(content);
		if (matcher.find()) {
			final String match = matcher.group(0);
			final String urlTarget = match.replace(URL_BRANCH_TARGET, "");
			if (!urlTarget.isEmpty()) {
				return urlTarget;
			}
		}

		LOGGER.warning("No URL target identified");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRepository() {
		final String fileName = getFileName();
		final Matcher matcher = PATTERN_REMOVE_ALL_EXCLUDE_REPONAME.matcher(fileName);
		if (matcher.find()) {
			final String repository = matcher.replaceAll(""); //$NON-NLS-1$
			if (!repository.isEmpty()) {
				return repository;
			}
		}
		if (LOGGER.isLoggable(Level.WARNING)) {
			LOGGER.warning(String.format("No LocalDateTime identified in the given file name '%s'", fileName));
		}
		return null;
	}

	private String removeSourceBranchInfoFromFileString(final String string) {
		final String branchSource = getBranchSource();
		final String stringNoSvnInfo = string.substring(4);
		return stringNoSvnInfo.substring(stringNoSvnInfo.indexOf(branchSource) + branchSource.length());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAffectedSourceFiles() {
		final List<String> workingCopyEntries = getWorkingCopyEntries();
		final List<String> addedFiles = workingCopyEntries.stream().filter(s -> s.charAt(0) == 'A')
				.map(this::removeSourceBranchInfoFromFileString).collect(Collectors.toList());
		final List<String> notAddedFiles = workingCopyEntries.stream().filter(s -> s.charAt(0) != 'A')
				.map(this::removeSourceBranchInfoFromFileString).collect(Collectors.toList());

		final List<String> neededWorkingCopyFiles = new ArrayList<>();
		for (String file : notAddedFiles) {
			final Path path = getPathFromString(file, 0);
			Path parent = path;
			boolean found = false;
			while ((parent = parent.getParent()) != null) {
				if (neededWorkingCopyFiles.contains(parent.toString())) {
					/*
					 * if parent is just added with this merge, this element is not available yet in
					 * this branch so it can't be a needed file
					 */
					found = true;
					break;
				}
			}
			if (!found) {
				neededWorkingCopyFiles.add(file);
			}
		}
		for (String file : addedFiles) {
			final Path path = getPathFromString(file, 0);
			final Path parent = path.getParent();
			if (parent == null) {
				if (LOGGER.isLoggable(Level.WARNING)) {
					LOGGER.warning(String.format("Added file seems to have no parent. addedFile=[%s]", file)); //$NON-NLS-1$
				}
			} else {
				/*
				 * parent is already a needed file -> nothing to do... OR parent is already an
				 * added file. it will be tested if the parents parent is a needed file in the
				 * parents iteration -> nothing to do...
				 */
				if (!neededWorkingCopyFiles.contains(parent.toString()) && !addedFiles.contains(parent.toString())) {
					neededWorkingCopyFiles.add(parent.toString());
				}
			}
		}
		Collections.sort(neededWorkingCopyFiles);
		return neededWorkingCopyFiles;
	}

	private static Path getPathFromString(final String string, final int splitIndex) {
		if (string.indexOf('>') == -1) {
			return Paths.get(string);
		} else {
			final String[] split = string.split(">");
			assert (split.length >= splitIndex + 1);
			return Paths.get(split[splitIndex]);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAffectedTargetFiles() {
		// TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemotePath() {
		return remotePath.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRemotePath(String pathMergeScript) {
		// TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileName() {
		return Objects.toString(remotePath.getFileName(), null);
	}

	/**
	 * @return the revisionWorkingCopy
	 */
	public long getRevisionWorkingCopy() {
		// TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		// TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * Prints the member variables.
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Remote Path: ");
		sb.append(':').append(' ');
		sb.append(remotePath);
		sb.append('\n');
		sb.append(content);
		return sb.toString();
	}

	/**
	 * Orders MergeUnits by date.
	 */
	@Override
	public int compareTo(IMergeUnit other) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((remotePath == null) ? 0 : remotePath.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SVNMergeUnit2 other = (SVNMergeUnit2) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (remotePath == null) {
			if (other.remotePath != null)
				return false;
		} else if (!remotePath.equals(other.remotePath))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBranchSource() {
		return SvnUtil.getBranchName(getUrlSource());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBranchTarget() {
		return SvnUtil.getBranchName(getUrlTarget());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBranchTarget(String branchTarget) {
		// TODO
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRevisionInfo() {
		return Long.toString(getRevisionStart()) + ':' + Long.toString(getRevisionEnd());
	}

	/**
	 * @return the svn info for the affected target files
	 */
	public List<String> getSvnInfoForAffectedTargetFiles() {
		// TODO
		throw new UnsupportedOperationException();
	}

	private List<String> getWorkingCopyEntries() {
		final Pattern pattern = Pattern.compile("WORKING_COPY_FILE=[^\\r\\n]*");
		final Matcher matcher = pattern.matcher(content);
		final List<String> workingCopyFiles = new ArrayList<>();
		while (matcher.find()) {
			workingCopyFiles.add(matcher.group().replace("WORKING_COPY_FILE=", ""));
		}
		return workingCopyFiles;
	}

	@Override
	public List<String> listBranches() {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Path, Path> getRenameMapping() {
		// TODO
		throw new UnsupportedOperationException();
	}

}
