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
package org.aposin.mergeprocessor.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface IMergeUnit extends Comparable<IMergeUnit> {

	/**
	 * @return the status of the merge unit
	 */
	MergeUnitStatus getStatus();

	/**
	 * @param status the merge status to set
	 */
	void setStatus(MergeUnitStatus status);

	/**
	 * @return the name of the file representing the merge unit
	 */
	String getFileName();

	/**
	 * @return the path on the server of the file representing the merge unit
	 */
	String getRemotePath();

	/**
	 * @param remotePath the path on the server of the file representing the merge
	 *                   unit
	 */
	void setRemotePath(String remotePath);

	/**
	 * @return the date
	 */
	LocalDateTime getDate();

	/**
	 * @return the source branch where to merge from
	 */
	String getBranchSource();

	/**
	 * @return the target branch where to merge to
	 */
	String getBranchTarget();

	/**
	 * @param branchTarget the target branch where to merge to
	 */
	void setBranchTarget(String branchTarget);

	/**
	 * @return the affected files where to merge from
	 */
	List<String> getAffectedSourceFiles();

	/**
	 * @return the affected files where to merge to
	 */
	List<String> getAffectedTargetFiles();

	/**
	 * @return the revision information
	 */
	String getRevisionInfo();

	/**
	 * @return the repository
	 */
	String getRepository();

	/**
	 * @return the host
	 */
	String getHost();

	/**
	 * @return a list of all available branches
	 */
	List<String> listBranches();

	/**
	 * Checks if any involved artifacts of the merge unit are renamed for the given
	 * target branch.
	 * 
	 * @return {@code true} if renamed are involved for this merge unit
	 */
	default boolean hasRenaming() {
		return getRenameMapping().entrySet().stream()
				.anyMatch(entry -> !Objects.equals(entry.getKey(), entry.getValue()));
	}
	
	/**
	 * Shows the changes of the merge unit.
	 */
	default void showChanges() {
		throw new UnsupportedVersionControlSystemSupportException();
	}

	/**
	 * @return the source-target mapping for the involved artifacts of the merge
	 *         unit. The key is the name of the source branch, the value is the name
	 *         of the target branchs
	 */
	Map<Path, Path> getRenameMapping();

}
