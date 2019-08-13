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
package org.aposin.mergeprocessor.model.svn;

import org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword;

/**
 * Empty implementations of {@link ISVNPromptUserPassword}.
 * 
 * @author Stefan Weiser
 *
 */
public abstract class SVNPromptUserPasswordAdapter implements ISVNPromptUserPassword {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String askQuestion(String arg0, String arg1, boolean arg2, boolean arg3) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int askTrustSSLServer(String arg0, boolean arg1) {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean askYesNo(String arg0, String arg1, boolean arg2) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPassword() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSSHPort() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSSHPrivateKeyPassphrase() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSSHPrivateKeyPath() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSSLClientCertPassword() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSSLClientCertPath() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUsername() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean prompt(String arg0, String arg1, boolean arg2) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean promptSSH(String arg0, String arg1, int arg2, boolean arg3) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean promptSSL(String arg0, boolean arg1) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean promptUser(String arg0, String arg1, boolean arg2) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean userAllowedSave() {
		return false;
	}

}
