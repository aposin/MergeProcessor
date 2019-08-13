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
/**
 * 
 */
package org.aposin.mergeprocessor.model.git;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitStatus;

/**
 * @author Stefan Weiser
 *
 */
public final class GITMergeUnit implements IMergeUnit {

	private static final Comparator<?> DEFAULT_COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

	private final String host;
	private final String repository;
	private final LocalDateTime date;
	private final String commitId;
	private final String branchSource;
	private String branchTarget;
	private final String fileName;
	private final List<String> affectedFiles;
	private final IConfiguration configuration;

	private MergeUnitStatus status;
	private String remotePath;

	/**
	 * @param host the server of the repository
	 * @param repository
	 * @param date the date when the {@link GITMergeUnit} has been created
	 * @param commitId the commit id of the original commit
	 * @param branchSource
	 * @param branchTarget the branch where the merge should happen
	 * @param fileName
	 * @param affectedFiles
	 */
	public GITMergeUnit(String host, String repository, LocalDateTime date, String commitId, String branchSource,
			String branchTarget, final String fileName, final List<String> affectedFiles,
			IConfiguration configuration) {
		this.host = host;
		this.repository = repository;
		this.date = date;
		this.commitId = commitId;
		this.branchSource = branchSource;
		this.branchTarget = branchTarget;
		this.fileName = fileName;
		this.affectedFiles = new ArrayList<>(affectedFiles);
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(IMergeUnit o) {
		if (o == null) {
			return 1;
		}

		final int dateCompare = Objects.compare(getDate(), o.getDate(), (Comparator<LocalDateTime>) DEFAULT_COMPARATOR);
		if (dateCompare != 0) {
			return dateCompare;
		}

		final int repoCompare = Objects.compare(getRepository(), o.getRepository(),
				(Comparator<String>) DEFAULT_COMPARATOR);
		if (repoCompare != 0) {
			return repoCompare;
		}

		final int hostCompare = Objects.compare(getHost(), o.getHost(), (Comparator<String>) DEFAULT_COMPARATOR);
		if (hostCompare != 0) {
			return hostCompare;
		}

		final int revisionCompare = Objects.compare(getRevisionInfo(), o.getRevisionInfo(),
				(Comparator<String>) DEFAULT_COMPARATOR);
		if (revisionCompare != 0) {
			return revisionCompare;
		}

		final int branchTargetCompare = Objects.compare(getBranchTarget(), o.getBranchTarget(),
				(Comparator<String>) DEFAULT_COMPARATOR);
		if (branchTargetCompare != 0) {
			return branchTargetCompare;
		}

		return getClass().toString().compareTo(o.getClass().toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((branchSource == null) ? 0 : branchSource.hashCode());
		result = prime * result + ((branchTarget == null) ? 0 : branchTarget.hashCode());
		result = prime * result + ((commitId == null) ? 0 : commitId.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((remotePath == null) ? 0 : remotePath.hashCode());
		result = prime * result + ((repository == null) ? 0 : repository.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GITMergeUnit other = (GITMergeUnit) obj;
		if (branchSource == null) {
			if (other.branchSource != null)
				return false;
		} else if (!branchSource.equals(other.branchSource))
			return false;
		if (branchTarget == null) {
			if (other.branchTarget != null)
				return false;
		} else if (!branchTarget.equals(other.branchTarget))
			return false;
		if (commitId == null) {
			if (other.commitId != null)
				return false;
		} else if (!commitId.equals(other.commitId))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (remotePath == null) {
			if (other.remotePath != null)
				return false;
		} else if (!remotePath.equals(other.remotePath))
			return false;
		if (repository == null) {
			if (other.repository != null)
				return false;
		} else if (!repository.equals(other.repository))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileName() {
		return fileName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRemotePath() {
		return remotePath;
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStatus(MergeUnitStatus status) {
		this.status = status;
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
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBranchSource() {
		return branchSource;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBranchTarget() {
		return branchTarget;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBranchTarget(String branchTarget) {
		this.branchTarget = branchTarget;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAffectedSourceFiles() {
		return Collections.unmodifiableList(affectedFiles);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAffectedTargetFiles() {
		return Collections.unmodifiableList(affectedFiles);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRevisionInfo() {
		return commitId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRepository() {
		return repository;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> listBranches() {
		return GitMergeUtil.listRemoteBranches(configuration, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Path, Path> getRenameMapping() {
		throw new UnsupportedOperationException("Renaming not implemented for GIT");
	}

}
