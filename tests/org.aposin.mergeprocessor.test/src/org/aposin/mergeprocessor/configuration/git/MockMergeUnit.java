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
package org.aposin.mergeprocessor.configuration.git;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.model.MergeUnitStatus;

/**
 * Implementation of {@link IMergeUnit} for JUnit tests.
 * 
 * @author Stefan Weiser
 *
 */
public class MockMergeUnit implements IMergeUnit {

	@Override
	public int compareTo(IMergeUnit o) {
		return 0;
	}

	@Override
	public MergeUnitStatus getStatus() {
		return null;
	}

	@Override
	public String getFileName() {
		return null;
	}

	@Override
	public void setStatus(MergeUnitStatus status) {
		// NOOP
	}

	@Override
	public LocalDateTime getDate() {
		return null;
	}

	@Override
	public String getBranchSource() {
		return null;
	}

	@Override
	public String getBranchTarget() {
		return null;
	}

	@Override
	public String getRevisionInfo() {
		return null;
	}

	@Override
	public String getRepository() {
		return null;
	}

	@Override
	public String getRemotePath() {
		return null;
	}

	@Override
	public void setRemotePath(String remotePath) {
		// NOOP
	}

	@Override
	public String getHost() {
		return null;
	}

	@Override
	public List<String> getAffectedSourceFiles() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getAffectedTargetFiles() {
		return Collections.emptyList();
	}

	@Override
	public void setBranchTarget(String branchTarget) {
		// NOOP
	}

	@Override
	public List<String> listBranches() {
		return Collections.emptyList();
	}

	@Override
	public Map<Path, Path> getRenameMapping() {
		return Collections.emptyMap();
	}

}