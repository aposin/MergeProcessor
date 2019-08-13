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
package org.aposin.mergeprocessor.configuration.svn;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.SvnClientJavaHl;
import org.aposin.mergeprocessor.utils.RuntimeUtil;
import org.aposin.mergeprocessor.utils.RuntimeUtil.CmdUtilException;

/**
 * Factory creating temporary SVN repositories which are deleted on shut down of the application.
 * 
 * @author Stefan Weiser
 *
 */
public final class TempSvnRepositoryFactory {

	private TempSvnRepositoryFactory() {
		// Factory class
	}

	/**
	 * Create and fill a temporary SVN repository with all required test data. 
	 * 
	 * @return object containing the repository path and the URL path.
	 * @throws CmdUtilException
	 * @throws IOException
	 */
	public static TempSvnRepository createAndFillTempSvnRepository() throws CmdUtilException, IOException {
		final Path testRepo = Files.createTempDirectory("svnRepo");
		testRepo.toFile().deleteOnExit();
		RuntimeUtil.exec("svnadmin create testProject", testRepo);
		final String testRepoUrlString = "file:///" + testRepo.toString().replace('\\', '/') + "/testProject";

		final String file1 = getFile("file1.txt").getFile().substring(1);
		RuntimeUtil.exec(String.format(
				"svn import %s %s/trunk/subfolder/file1.txt -m \"Initial Import file1.txt\" --username testuser", file1,
				testRepoUrlString));

		final String file2 = getFile("file2.txt").getFile().substring(1);
		RuntimeUtil.exec(
				String.format("svn import %s %s/trunk/file2.txt -m \"Initial Import file2.txt\" --username testuser",
						file2, testRepoUrlString));

		final String file3 = getFile("file3.txt").getFile().substring(1);
		RuntimeUtil.exec(
				String.format("svn import %s %s/trunk/file3.txt -m \"Initial Import file3.txt\" --username testuser",
						file3, testRepoUrlString));
		return new TempSvnRepository(testRepo, testRepoUrlString);
	}

	/**
	 * Object providing the repository path and the URL path for a temporary SVN repository.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	public static final class TempSvnRepository {

		public final Path testRepo;
		public final String testRepoUrlString;

		private TempSvnRepository(final Path testRepo, final String testRepoUrlString) {
			this.testRepo = testRepo;
			this.testRepoUrlString = testRepoUrlString;
		}

		/**
		 * Creates a new commit to the SVN repository.
		 * 
		 * @throws MalformedURLException
		 * @throws SvnClientException
		 * @throws CmdUtilException
		 */
		public void createCommit() throws MalformedURLException, SvnClientException, CmdUtilException {
			final String file = getFile("nextFile.txt").getFile().substring(1);
			try (ISvnClient client = new SvnClientJavaHl(() -> new String[0], new JUnitConfiguration())) {
				final long revision = client.showRevision(new URL(testRepoUrlString));
				RuntimeUtil.exec(String.format(
						"svn import %s %s/trunk/anotherFile%s.txt -m \"Initial Import anotherFile%s.txt\" --username testuser",
						file, testRepoUrlString, revision + 1, revision + 1));
			}
		}

	}

	/**
	 * Returns the {@link URL} for the given file name, which is available in the same directory structure as this class.
	 * 
	 * @param fileName the file name
	 * @return the {@link URL} of the file to find
	 * @throws MalformedURLException
	 */
	static URL getFile(final String fileName) throws MalformedURLException {
		return new File("src/org/aposin/mergeprocessor/configuration/svn/" + fileName).toURI().toURL();
	}

}
