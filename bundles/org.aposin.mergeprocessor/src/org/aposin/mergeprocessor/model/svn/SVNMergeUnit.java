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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.IVersionProvider;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog;
import org.aposin.mergeprocessor.renaming.RenamingService;
import org.aposin.mergeprocessor.renaming.Version;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.SvnUtil;

/**
 * Contains metadata of the merge file.
 */
public final class SVNMergeUnit implements IMergeUnit, PropertyChangeListener {

	private static final Logger LOGGER = Logger.getLogger(SVNMergeUnit.class.getName());

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final IConfiguration configuration;
	private final ISvnClient svnClient;
	private final String host;
	private final String repositoryName;
	private final LocalDateTime date;
	private final long revisionStart;
	private final long revisionEnd;
	private final String urlSource;
	private String urlTarget;
	private final long revisionWorkingCopy;
	private final List<String> affectedSourceFiles;
	private final List<String> affectedTargetFiles;
	private final List<String> targetFilesToDelete;
	private final List<String> targetFilesToAdd;

	private String pathMergeScript;
	private MergeUnitStatus status;

	private List<SvnDiff> changedPaths;
	private Map<Path, Path> renameMapping;
	private Map<Path, Path> renameMappingWithParents;

	private Version sourceVersion = null;
	private Version targetVersion = null;

	private static final String BRANCH_NAME_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	private static final String COMMIT_MESSAGE_MERGE_CURRENT = "MP [%1$d:%2$d] %3$s -> %4$s\n"; //$NON-NLS-1$
	protected static final String COMMIT_MESSAGE_MERGE_PREVIOUS = "r%1$s: [%2$s] (%3$tY-%3$tm-%3$td %3$tH:%3$tM:%3$tS) %4$s\n"; //$NON-NLS-1$

	/**
	 * @param host
	 * @param repositoryName
	 * @param date
	 * @param status
	 * @param revisionStart
	 * @param revisionEnd
	 * @param urlSource
	 * @param urlTarget
	 * @param pathMergeScript
	 * @param revisionWorkingCopy
	 * @param affectedSourceFiles
	 * @param affectedTargetFiles
	 * @param targetFilesToDelete
	 * @param targetFilesToAdd
	 */
	public SVNMergeUnit(String host, String repositoryName, LocalDateTime date, MergeUnitStatus status,
			long revisionStart, long revisionEnd, String urlSource, String urlTarget, String pathMergeScript,
			long revisionWorkingCopy, List<String> affectedSourceFiles, List<String> affectedTargetFiles,
			List<String> targetFilesToDelete, List<String> targetFilesToAdd, IConfiguration configuration, ISvnClient svnClient) {
		LogUtil.entering(host, repositoryName, date, status, revisionStart, revisionEnd, urlSource, urlTarget,
				pathMergeScript, revisionWorkingCopy, affectedSourceFiles, affectedTargetFiles, targetFilesToDelete,
				configuration);
		this.host = host;
		this.repositoryName = repositoryName;
		this.date = date;
		this.status = status;
		this.revisionStart = revisionStart;
		this.revisionEnd = revisionEnd;
		this.urlSource = urlSource;
		this.urlTarget = urlTarget;
		this.pathMergeScript = pathMergeScript;
		this.revisionWorkingCopy = revisionWorkingCopy;
		this.affectedSourceFiles = affectedSourceFiles;
		this.affectedTargetFiles = affectedTargetFiles;
		this.targetFilesToDelete = targetFilesToDelete;
		this.targetFilesToAdd = targetFilesToAdd;
		this.configuration = configuration;
		this.svnClient = svnClient;
		changedPaths = null;
	}

	private List<SvnDiff> getChangedPathsFromSVN() {
		try {
			final ISvnClient svnClient = E4CompatibilityUtil.getApplicationContext().get(ISvnClient.class);
			return svnClient.diff(new URL(getUrlSource()), getRevisionStart(), getRevisionEnd());
		} catch (MalformedURLException | SvnClientException e) {
			LogUtil.getLogger().log(Level.SEVERE, "Could not evaluate the changes for the given merge unit from SVN. ",
					e);
			return Collections.emptyList();
		}
	}

	/**
	 * @return the date
	 */
	public LocalDateTime getDate() {
		return date;
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
		propertyChangeSupport.firePropertyChange("status", this.status, this.status = status); //$NON-NLS-1$
	}

	/**
	 * @return the revisionStart
	 */
	public long getRevisionStart() {
		return revisionStart;
	}

	/**
	 * @return the revisionEnd
	 */
	public long getRevisionEnd() {
		return revisionEnd;
	}

	/**
	 * @return the urlSource
	 */
	public String getUrlSource() {
		return urlSource;
	}

	/**
	 * @return the urlTarget
	 */
	public String getUrlTarget() {
		return urlTarget;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRepository() {
		return repositoryName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAffectedSourceFiles() {
		return Collections.unmodifiableList(affectedSourceFiles);
	}

	public List<String> getTargetFilesToDelete() {
		return Collections.unmodifiableList(targetFilesToDelete);
	}

	public List<String> getTargetFilesToAdd() {
		return Collections.unmodifiableList(targetFilesToAdd);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAffectedTargetFiles() {
		return Collections.unmodifiableList(affectedTargetFiles);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemotePath() {
		return pathMergeScript;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRemotePath(String pathMergeScript) {
		propertyChangeSupport.firePropertyChange("pathMergeScript", this.pathMergeScript, //$NON-NLS-1$
				this.pathMergeScript = pathMergeScript);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileName() {
		int beginIndex = pathMergeScript.lastIndexOf('/');
		if (beginIndex == -1) {
			beginIndex = pathMergeScript.lastIndexOf('\\');
		}

		return pathMergeScript.substring(beginIndex + 1);
	}

	/**
	 * @return the revisionWorkingCopy
	 */
	public long getRevisionWorkingCopy() {
		return revisionWorkingCopy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * checks if any of the variables is null or not valid.
	 * 
	 * @return <code>true</code> if the mergeUnit seems to be valid.
	 */
	protected boolean isValid() {
		boolean isValid = true;

		StringBuilder errors = new StringBuilder();

		if (this.date == null) {
			errors.append("\ndate is null!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.pathMergeScript == null) {
			errors.append("\npathMergeScript is null!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.repositoryName == null) {
			errors.append("\nrepositoryName is null!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.revisionStart < 1) {
			errors.append("\nrevisionStart is smaller 1!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.revisionEnd < 1) {
			errors.append("\nrevisionEnd is smaller 1!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.status == null) {
			errors.append("\nstatus is null!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.urlSource == null) {
			errors.append("\nurlSource is null!"); //$NON-NLS-1$
			isValid = false;
		}
		if (this.urlTarget == null) {
			errors.append("\nurlTarget is null!"); //$NON-NLS-1$
			isValid = false;
		}

		if (LOGGER.isLoggable(Level.WARNING) && errors.length() > 0) {
			LOGGER.warning(String.format("Errors found during validation of mergeUnit=%s:%s", this.toString(), //$NON-NLS-1$
					errors.toString()));
		}

		return isValid;
	}

	/**
	 * Prints the member variables.
	 */
	@Override
	public String toString() {
		return String.format(
				"repositoryName=[%s], date=[%s], status=[%s], revisionStart=[%d], revisionEnd=[%d], urlSource=[%s], urlTarget=[%s], pathMergeScript=[%s]", //$NON-NLS-1$
				repositoryName, date, status, revisionStart, revisionEnd, urlSource, urlTarget, pathMergeScript);
	}

	/**
	 * Orders MergeUnits by date.
	 */
	@Override
	public int compareTo(IMergeUnit o) {
		// if we couldn't parse the revision ranges compare the dates
		final int dateCompare = getDate().compareTo(o.getDate());
		if (dateCompare != 0) {
			return dateCompare;
		}

		final int repoCompare = getRepository().compareTo(o.getRepository());
		if (repoCompare != 0) {
			return repoCompare;
		}

		final int hostCompare = getHost().compareTo(o.getHost());
		if (hostCompare != 0) {
			return hostCompare;
		}

		final int revisionCompare = getRevisionInfo().compareTo(o.getRevisionInfo());
		if (revisionCompare != 0) {
			return revisionCompare;
		}

		final int branchTargetCompare = getBranchTarget().compareTo(o.getBranchTarget());
		if (branchTargetCompare != 0) {
			return branchTargetCompare;
		}

		if (o.getClass() != getClass()) {
			return getClass().toString().compareTo(o.getClass().toString());
		}
		SVNMergeUnit otherMergeUnit = (SVNMergeUnit) o;

		// if the repository is the same then compare the revision ranges.
		{
			int retVal = Long.compare(revisionStart, otherMergeUnit.revisionStart);

			if (retVal != 0) {
				return retVal;
			}
		}

		{
			int retVal = this.urlSource.compareTo(otherMergeUnit.urlSource);
			if (retVal != 0) {
				return retVal;
			}
		}
		{
			int retVal = this.urlTarget.compareTo(otherMergeUnit.urlTarget);
			if (retVal != 0) {
				return retVal;
			}
		}

		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(date, repositoryName, revisionStart, revisionEnd,
				revisionWorkingCopy, urlSource, urlTarget);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SVNMergeUnit)) {
			return false;
		}
		SVNMergeUnit other = (SVNMergeUnit) obj;
		return Objects.equals(date, other.date) &&
		Objects.equals(repositoryName, other.repositoryName) &&
		Objects.equals(revisionStart, other.revisionStart) &&
		Objects.equals(revisionEnd, other.revisionEnd) &&
		Objects.equals(revisionWorkingCopy, other.revisionWorkingCopy) &&
		Objects.equals(urlSource, other.urlSource) &&
		Objects.equals(urlTarget, other.urlTarget);
	}

	/**
	 * @param propertyName
	 * @param listener
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		propertyChangeSupport.firePropertyChange("status", null, status); //$NON-NLS-1$
		propertyChangeSupport.firePropertyChange("pathMergeScript", null, pathMergeScript); //$NON-NLS-1$
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

	public Version getSourceVersion() {
		if (sourceVersion == null) {
			sourceVersion = E4CompatibilityUtil.getApplicationContext().get(IVersionProvider.class)
					.forSvnUrl(getUrlSource());
		}
		return sourceVersion;
	}

	public Version getTargetVersion() {
		if (targetVersion == null) {
			targetVersion = E4CompatibilityUtil.getApplicationContext().get(IVersionProvider.class)
					.forSvnUrl(getUrlTarget());
		}
		return targetVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBranchTarget(String branchTarget) {
		final String oldBranchTarget = getBranchTarget();
		if (!Objects.equals(branchTarget, oldBranchTarget)) {
			if ("trunk".equals(branchTarget)) {
				final String basePath = getUrlTarget().substring(0,
						getUrlTarget().indexOf("branches/" + oldBranchTarget));
				urlTarget = basePath + "trunk";
			} else if ("trunk".equals(oldBranchTarget)) {
				final String baseUrl = urlTarget.substring(0, urlTarget.length() - "trunk".length());
				urlTarget = baseUrl + "branches/" + branchTarget;
			} else {
				urlTarget = urlTarget.replace(oldBranchTarget, branchTarget);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRevisionInfo() {
		return Long.toString(revisionStart) + ':' + Long.toString(revisionEnd);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> listBranches() {
		return SVNMergeUtil.listBranches(this);
	}

	public String getMessage() {
		LogUtil.entering();

		StringBuilder sb = new StringBuilder();
		String branchNameSource = SvnUtil.getBranchName(getUrlSource());
		String branchNameTarget = SvnUtil.getBranchName(getUrlTarget());
		if (branchNameSource == null) {
			LOGGER.severe(() -> String.format("branchNameSource of mergeUnit is null. mergeUnit=%s", this)); //$NON-NLS-1$
			branchNameSource = BRANCH_NAME_UNKNOWN;
		}
		if (branchNameTarget == null) {
			LOGGER.severe(() -> String.format("branchNameTarget of mergeUnit is null. mergeUnit=%s", this)); //$NON-NLS-1$
			branchNameTarget = BRANCH_NAME_UNKNOWN;
		}
		sb.append(String.format(COMMIT_MESSAGE_MERGE_CURRENT, getRevisionStart(), getRevisionEnd(), branchNameSource,
				branchNameTarget));

		// append commit messages from the merged revisions to this commit message
		String previousCommitInfo = getCommitInfo();
		if (previousCommitInfo != null) {
			sb.append(previousCommitInfo);
		}

		return LogUtil.exiting(sb.toString());
	}

	public List<SvnDiff> getSvnDiff() {
		if (changedPaths == null) {
			changedPaths = Collections.unmodifiableList(getChangedPathsFromSVN());
		}
		return changedPaths;
	}

	private List<Path> getChangedPaths() {
		return getSvnDiff().stream() // Stream
				.map(this::convertSvnDiffToPath) // Get the path from the SvnDiff
				.filter(Objects::nonNull) // Filter nulls caused by exceptions
				.collect(Collectors.toList());
	}

	public Path convertSvnDiffToPath(final SvnDiff diff) {
		/*
		 * diff.getUrl().toURI() cannot be used as it is not able to deal with spaces.
		 * So we use simple String replacement to achieve the same functionality
		 */
		if (diff.getUrl().toString().startsWith(getUrlSource())) {
			return Paths.get(diff.getUrl().toString().substring(getUrlSource().length() + 1)); // +1 to escape the
																								// leading slash
		} else {
			throw new IllegalArgumentException(
					String.format("The URL '%s' of the given SvnDiff is not a subpath of the repository '%s'.",
							diff.getUrl(), getUrlSource()));
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showChanges() {
		long revisionStart = getRevisionStart();
		long revisionEnd = getRevisionEnd();

		if (revisionStart > 0 && revisionEnd > 0) {
			String command = String.format("Tortoiseproc.exe /command:log /path:%s /startrev:%d /endrev:%d", //$NON-NLS-1$
					getUrlSource(), revisionStart, revisionEnd);

			try {
				Runtime.getRuntime().exec(command, null, null);
			} catch (IOException e1) {
				LOGGER.log(Level.WARNING, "Caught exception while starting Tortoise Log.", e1); //$NON-NLS-1$
			}
		} else {
			if (LOGGER.isLoggable(Level.WARNING)) {
				LOGGER.log(Level.WARNING, String.format(
						"Couldn't start Tortoise Log because revisionRange is invalid. revisionStart=%s, revisionEnd=%s", //$NON-NLS-1$
						revisionStart, revisionEnd));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Path, Path> getRenameMapping() {
		if (renameMapping == null) {
			final List<Path> changedPaths = getChangedPaths();
			final Map<Path, Path> renameMapping = new HashMap<>(changedPaths.size());
			try (final RenamingService service = new RenamingService(configuration,
					SvnUtil.getRepositoryRootOfUrl(getUrlTarget()), getSourceVersion(), getTargetVersion())) {
				final List<Path> renamedArtifacts = service.getRenamedArtifacts(changedPaths);
				for (int i = 0; i < changedPaths.size(); i++) {
					renameMapping.put(changedPaths.get(i), renamedArtifacts.get(i));
				}
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "An error occured on evalution if merge unit contains renamed artifacts.", e);
				for (final Path path : changedPaths) {
					renameMapping.put(path, path);
				}
			}
			this.renameMapping = renameMapping;
		}
		return Collections.unmodifiableMap(renameMapping);
	}

	public Map<Path, Path> getRenameMappingWithParents() {
		if (renameMappingWithParents == null) {
			final Map<Path, Path> mapping = new HashMap<>(getRenameMapping());
			try (final RenamingService service = new RenamingService(configuration,
					SvnUtil.getRepositoryRootOfUrl(getUrlTarget()), getSourceVersion(), getTargetVersion())) {
				final List<Path> from = mapping.keySet().stream() // Stream on keys
						.map(key -> key.getParent()) // get parents of keys
						.distinct() // filter duplicates
						.filter(parent -> !mapping.containsKey(parent)) // filter keys already in mapping
						.collect(Collectors.toList());
				final List<Path> to = service.getRenamedArtifacts(from);
				for (int i = 0; i < from.size(); i++) {
					mapping.put(from.get(i), to.get(i));
				}
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "An error occured on evalution if merge unit contains renamed artifacts.", e);
			}
			renameMappingWithParents = mapping;
		}
		return Collections.unmodifiableMap(renameMappingWithParents);
	}

	private String getCommitInfo() {
		LogUtil.entering();

		long revision = getRevisionEnd();
		if (revision < 1) {
			LOGGER.severe(() -> String.format("Revision range is invalid. mergeUnit=%s", this)); //$NON-NLS-1$
			return LogUtil.exiting(null);
		} else {
			Optional<SvnLog> logEntry = getLogEntry(getUrlSource(), revision);
			if (logEntry.isPresent()) {
				SvnLog entry = logEntry.get();
				return LogUtil.exiting(String.format(COMMIT_MESSAGE_MERGE_PREVIOUS, entry.getRevision(),
						entry.getAuthor(), entry.getDate(), entry.getMessage()));
			} else {
				return LogUtil.exiting(null);
			}
		}
	}

	private Optional<SvnLog> getLogEntry(String url, long revision) {
		try {
			return svnClient.log(new URL(url), revision);
		} catch (Exception e) {
			LogUtil.throwing(e);
			return Optional.empty();
		}
	}

}
