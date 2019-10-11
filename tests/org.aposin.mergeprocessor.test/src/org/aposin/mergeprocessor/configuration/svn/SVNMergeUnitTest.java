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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
import org.aposin.mergeprocessor.model.svn.SvnClientMock;
import org.aposin.mergeprocessor.utils.SvnUtilException;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SVNMergeUnit}
 * 
 * @author Stefan Weiser
 *
 */
public class SVNMergeUnitTest {

	/**
	 * Tests that {@link SVNMergeUnit#convertSvnDiffToPath(SvnDiff)} is able to deal
	 * on files with blanks.
	 * 
	 * @throws SvnUtilException
	 */
	@Test
	public void testConvertSvnDiffToPathUsingSpaces() throws MalformedURLException {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 0l, 0l,
				"https://my.svn.repository.com/branches/V18.0", null, null, 0l, null, null, null, null, null, null);
		final SvnDiff svnDiff = new SvnDiffMock(
				new URL("https://my.svn.repository.com/branches/V18.0/file with blanks.txt"));
		assertEquals(Paths.get("file with blanks.txt"), mergeUnit.convertSvnDiffToPath(svnDiff));
	}

	/**
	 * Tests that {@link SVNMergeUnit#getMessage()} creates a valid commit message
	 * with only 1 source message info from SVN.
	 */
	@Test
	public void testGetMessage() {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 1l, 2l,
				"https://my.svn.repository.com/branches/V18.0", "https://my.svn.repository.com/branches/V19.0", null,
				0l, null, null, null, null, null, new SvnClientMock() {

					@Override
					public List<SvnLog> log(URL url, long fromRevision, long toRevision, String author)
							throws SvnClientException {
						final long count = toRevision - fromRevision + 1;
						final List<SvnLog> logs = new ArrayList<>();
						for (int i = 0; i < count; i++) {
							logs.add(new SvnLogMock(i, "Message " + i));
						}
						return logs;
					}

				});

		/*
		 * Message should look like: MP [1:2] V18.0 -> V19.0 r0: [Testauthor]
		 * (2019-10-07 08:38:47) Message 0
		 */
		final String message = mergeUnit.getMessage();
		assertTrue(message.trim().endsWith("Message 0"));
		assertFalse(message.contains("Message 1"));
		assertTrue(message.startsWith("MP [1:2] V18.0 -> V19.0"));
	}

	/**
	 * Tests that {@link SVNMergeUnit#getMessage()} does not throw an
	 * {@link Exception} when no valid branch could be identified.
	 */
	@Test
	public void testGetMessageWhenNoValidBranchCanBeIdentified() {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 1l, 2l,
				"https://my.svn.repository.com/hugo", "https://my.svn.repository.com/franz", null, 0l, null, null, null,
				null, null, new SvnClientMock() {

					@Override
					public List<SvnLog> log(URL url, long fromRevision, long toRevision, String author)
							throws SvnClientException {
						final long count = toRevision - fromRevision + 1;
						final List<SvnLog> logs = new ArrayList<>();
						for (int i = 0; i < count; i++) {
							logs.add(new SvnLogMock(i, "Message " + i));
						}
						return logs;
					}

				});
		assertDoesNotThrow(() -> mergeUnit.getMessage());
	}

	/**
	 * Tests that {@link SVNMergeUnit#getMessage()} creates a valid commit message
	 * even if no valid revision numbers are available.
	 */
	@Test
	public void testGetMessageOnIllegalRevisions() {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 2l, -1l,
				"https://my.svn.repository.com/V18.0", "https://my.svn.repository.com/V19.0", null, 0l, null, null,
				null, null, null, null);
		/*
		 * Message should look like: MP [2:-1] UNKNOWN -> UNKNOWN
		 */
		final String message = mergeUnit.getMessage();
		assertFalse(message.contains("Message 0"));
		assertFalse(message.contains("Message 1"));
	}

	/**
	 * Tests that {@link SVNMergeUnit#getMessage()} creates a valid commit message
	 * even if no logs can be queried via SVN.
	 */
	@Test
	public void testGetMessageWhenNoLogIsIdentified() {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 1l, 2l,
				"https://my.svn.repository.com/branches/V18.0", "https://my.svn.repository.com/branches/V19.0", null,
				0l, null, null, null, null, null, new SvnClientMock() {

					@Override
					public List<SvnLog> log(URL url, long fromRevision, long toRevision, String author)
							throws SvnClientException {
						return List.of();
					}

				});
		/*
		 * Message should look like: MP [1:2] V18.0 -> V19.0 r0: [Testauthor]
		 * (2019-10-07 08:38:47) Message 0
		 */
		final String message = mergeUnit.getMessage();
		assertFalse(message.contains("Message 0"));
		assertFalse(message.contains("Message 1"));
		assertTrue(message.trim().equals("MP [1:2] V18.0 -> V19.0"));
	}

	/**
	 * Tests that {@link SVNMergeUnit#getMessage()} creates a valid commit message
	 * even if an Exception is thrown by SVN.
	 */
	@Test
	public void testGetMessageWhenLogThrowsException() {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 1l, 2l,
				"https://my.svn.repository.com/branches/V18.0", "https://my.svn.repository.com/branches/V19.0", null,
				0l, null, null, null, null, null, new SvnClientMock() {

					@Override
					public List<SvnLog> log(URL url, long fromRevision, long toRevision, String author)
							throws SvnClientException {
						throw new SvnClientException("Test Exeption handling");
					}

				});
		/*
		 * Message should look like: MP [1:2] V18.0 -> V19.0 r0: [Testauthor]
		 * (2019-10-07 08:38:47) Message 0
		 */
		final String message = mergeUnit.getMessage();
		assertFalse(message.contains("Message 0"));
		assertFalse(message.contains("Message 1"));
		assertTrue(message.trim().equals("MP [1:2] V18.0 -> V19.0"));
	}

	/**
	 * Mocked version for instantiation.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class SvnDiffMock extends SvnDiff {

		public SvnDiffMock(final URL url) {
			super(SvnDiffAction.MODIFIED, url);
		}

	}

	/**
	 * Mocked version for instantiation.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class SvnLogMock extends SvnLog {

		public SvnLogMock(int revision, final String message) {
			super(revision, List.of(), message, LocalDateTime.now(), "Testauthor");
		}

	}

}
