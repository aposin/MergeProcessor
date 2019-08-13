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

import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.SvnClientJavaHl;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

/**
 * Tests for {@link SvnClientJavaHl}.
 * 
 * @author Stefan Weiser
 *
 */
@Disabled
public class SvnClientJavaHlTest extends AbstractSvnClientTest {

	private static ISvnClient client;

	@BeforeAll
	public static void createClient() throws SvnClientException {
		client = new SvnClientJavaHl(() -> new String[0], new JUnitConfiguration());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ISvnClient getClient() {
		return client;
	}

	@AfterAll
	public static void disposeClient() throws Exception {
		client.close();
	}

}
