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
package org.aposin.mergeprocessor.configuration.svn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff.SvnDiffAction;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog.SvnLogAction;
import org.aposin.mergeprocessor.utils.RuntimeUtil.CmdUtilException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for Implementations of the interface {@link ISvnClient}.
 * 
 * @author Stefan Weiser
 *
 */
public abstract class AbstractSvnClientTest {

	private static String testRepoUrlString;

	@BeforeAll
	public static void setUp() throws CmdUtilException, IOException {
		testRepoUrlString = TempSvnRepositoryFactory.createAndFillTempSvnRepository().testRepoUrlString;
	}

	@Test
	public void testCat() throws IOException, SvnClientException {
		final URL url = new URL(testRepoUrlString + "/trunk/file2.txt");
		final String expected = IOUtils.toString(TempSvnRepositoryFactory.getFile("file2.txt"), StandardCharsets.UTF_8);
		assertEquals(expected, getClient().cat(url));
	}

	@Test
	public void testCatWithNotExistingFile() throws MalformedURLException {
		final URL url = new URL(testRepoUrlString + "/trunk/notExisting.txt");
		assertThrows(SvnClientException.class, () -> getClient().cat(url));
	}

	@Test
	public void testDiff() throws MalformedURLException, SvnClientException {
		final List<SvnDiff> result = getClient().diff(new URL(testRepoUrlString), 1l, 2l);
		assertEquals(1, result.size());
		assertEquals(SvnDiffAction.ADDED, result.get(0).getAction());
		assertEquals(new URL(testRepoUrlString + "/trunk/file2.txt"), result.get(0).getUrl());
	}

	@Test
	public void testDiffWithNoChangesInDirectory() throws MalformedURLException, SvnClientException {
		final List<SvnDiff> result = getClient().diff(new URL(testRepoUrlString + "/trunk/subfolder"), 1l, 2l);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testDiffWithNotExistingRevisionNumber() {
		assertThrows(SvnClientException.class, () -> getClient().diff(new URL(testRepoUrlString), 5000l, 5001l));
	}

	@Test
	public void testDiffWithNotExistingDirectory() {
		assertThrows(SvnClientException.class,
				() -> getClient().diff(new URL(testRepoUrlString + "/trunk/notExisting"), 1l, 2l));
	}

	@Test
	public void testShowRevision() throws MalformedURLException, SvnClientException {
		assertEquals(3l, getClient().showRevision(new URL(testRepoUrlString)));
	}

	@Test
	public void testLog() throws MalformedURLException, SvnClientException {
		final List<SvnLog> result = getClient().log(new URL(testRepoUrlString), 2l, 2l, "testuser");
		assertEquals(1, result.size());
		assertEquals(1, result.get(0).getEntries().size());
		assertEquals(new URL(testRepoUrlString + "/trunk/file2.txt"), result.get(0).getEntries().get(0).getUrl());
		assertEquals(SvnLogAction.ADDED, result.get(0).getEntries().get(0).getAction());
		assertEquals("Initial Import file2.txt", result.get(0).getMessage());
		assertEquals("testuser", result.get(0).getAuthor());
		assertNotNull(result.get(0).getDate());
	}

	@Test
	public void testLogForUserWithNoEntries() throws MalformedURLException, SvnClientException {
		final List<SvnLog> result = getClient().log(new URL(testRepoUrlString), 2l, 2l, "bla");
		assertEquals(0, result.size());
	}

	@Test
	public void testLogWithoutUser() throws MalformedURLException, SvnClientException {
		final List<SvnLog> result = getClient().log(new URL(testRepoUrlString), 2l, 2l);
		assertEquals(1, result.size());
		assertEquals(1, result.get(0).getEntries().size());
		assertEquals(new URL(testRepoUrlString + "/trunk/file2.txt"), result.get(0).getEntries().get(0).getUrl());
		assertEquals(SvnLogAction.ADDED, result.get(0).getEntries().get(0).getAction());
		assertEquals("Initial Import file2.txt", result.get(0).getMessage());
	}

	@Test
	public void testListDirectories() throws MalformedURLException, SvnClientException {
		final List<String> result = getClient().listDirectories(new URL(testRepoUrlString + "/trunk"));
		final List<String> expected = Arrays.asList("subfolder");
		assertLinesMatch(expected, result);
	}

	@Test
	public void testCheckoutEmpty() throws IOException, SvnClientException {
		final Path testRepo = Files.createTempDirectory("testCheckoutEmpty");
		try {
			assertFalse(testRepo.resolve(".svn").toFile().exists());
			getClient().checkoutEmpty(testRepo, new URL(testRepoUrlString));
			assertTrue(testRepo.resolve(".svn").toFile().exists());
			assertFalse(Files.list(testRepo).anyMatch(path -> !path.getFileName().toString().equals(".svn")));
		} finally {
			FileUtils.deleteDirectory(testRepo.toFile());
		}
	}

	@Test
	public void testCheckoutEmptyWithNullPath() {
		assertThrows(NullPointerException.class, () -> getClient().checkoutEmpty(null, new URL(testRepoUrlString)));
	}

	@Test
	public void testCheckoutEmptyWithIllegalPath() {
		assertThrows(IllegalArgumentException.class,
				() -> getClient().checkoutEmpty(Paths.get("a", "b", "c"), new URL(testRepoUrlString)));
	}

	@Test
	public void testCheckoutEmptyWithNotExistingRepository() throws IOException {
		final Path testRepo = Files.createTempDirectory("testCheckoutEmpty");
		try {
			assertThrows(SvnClientException.class,
					() -> getClient().checkoutEmpty(testRepo, new URL("http://www.opasin.org")));
		} finally {
			FileUtils.deleteDirectory(testRepo.toFile());
		}
	}

	@Test
	public void testCheckoutEmptyWithNullURL() throws IOException {
		final Path testRepo = Files.createTempDirectory("testCheckoutEmpty");
		try {
			assertThrows(NullPointerException.class, () -> getClient().checkoutEmpty(testRepo, null));
		} finally {
			FileUtils.deleteDirectory(testRepo.toFile());
		}
	}

	@Test
	public void testUpdateEmpty() throws IOException, SvnClientException {
		final Path testRepo = Files.createTempDirectory("testUpdateEmpty");
		try {
			getClient().checkoutEmpty(testRepo, new URL(testRepoUrlString));
			final Path trunk = testRepo.resolve("trunk");
			final Path subfolder = trunk.resolve("subfolder");
			final Path file1 = subfolder.resolve("file1.txt");
			final Path file2 = trunk.resolve("file2.txt");
			final Path file3 = trunk.resolve("file3.txt");
			getClient().updateEmpty(Arrays.asList(trunk, subfolder, file1));
			assertTrue(trunk.toFile().exists());
			assertTrue(subfolder.toFile().exists());
			assertTrue(file1.toFile().exists());
			assertFalse(file2.toFile().exists());
			assertFalse(file3.toFile().exists());
		} finally {
			FileUtils.deleteDirectory(testRepo.toFile());
		}
	}

	abstract protected ISvnClient getClient();

}
