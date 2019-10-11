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

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Svn client implementation mocking the SVN behaviour.
 * 
 * @author Stefan Weiser
 *
 */
public class SvnClientMock implements ISvnClient {

	@Override
	public String cat(URL url) throws SvnClientException {
		return null;
	}

	@Override
	public List<SvnDiff> diff(URL url, long fromRevision, long toRevision) throws SvnClientException {
		return null;
	}

	@Override
	public boolean hasModifications(Path path) throws SvnClientException {
		return false;
	}

	@Override
	public long showRevision(URL url) throws SvnClientException {
		return 0;
	}

	@Override
	public List<SvnLog> log(URL url, long fromRevision, long toRevision, String author) throws SvnClientException {
		return null;
	}

	@Override
	public List<String> listDirectories(URL url) throws SvnClientException {
		return null;
	}

	@Override
	public long[] updateEmpty(List<Path> paths) throws SvnClientException {
		return null;
	}

	@Override
	public void checkoutEmpty(Path path, URL url) throws SvnClientException {
		//NOOP
	}

	@Override
	public void merge(Path path, URL url, long revision, boolean recursivly, boolean recordOnly)
			throws SvnClientException {
		//NOOP
	}

	@Override
	public void commit(Path path, String message) throws SvnClientException {
		//NOOP
	}

	@Override
	public List<String> getConflicts(Path path) throws SvnClientException {
		return null;
	}

	@Override
	public void update(Path path) throws SvnClientException {
		//NOOP
	}

	@Override
	public URL getSvnUrl(Path path) throws SvnClientException {
		return null;
	}

	@Override
	public URL getRepositoryUrl(Path path) throws SvnClientException {
		return null;
	}

	@Override
	public void addCommandLineListener(Consumer<String> consumer) {
		//NOOP
	}

	@Override
	public void removeCommandLineListener(Consumer<String> consumer) {
		//NOOP
	}

	@Override
	public void close() {
		//NOOP
	}

}
