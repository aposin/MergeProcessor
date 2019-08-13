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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff;
import org.aposin.mergeprocessor.model.svn.SVNMergeUnit;
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
	 * Tests that {@link SVNMergeUnit#convertSvnDiffToPath(SvnDiff)} is able to deal on files with blanks.
	 * @throws SvnUtilException 
	 */
	@Test
	public void testConvertSvnDiffToPathUsingSpaces() throws MalformedURLException {
		final SVNMergeUnit mergeUnit = new SVNMergeUnit(null, null, null, null, 0l, 0l,
				"https://my.svn.repository.com/branches/V18.0", null, null, 0l, null, null, null, null, null);
		final SvnDiff svnDiff = new SvnDiffMock(
				new URL("https://my.svn.repository.com/branches/V18.0/file with blanks.txt"));
		assertEquals(Paths.get("file with blanks.txt"), mergeUnit.convertSvnDiffToPath(svnDiff));
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

}
