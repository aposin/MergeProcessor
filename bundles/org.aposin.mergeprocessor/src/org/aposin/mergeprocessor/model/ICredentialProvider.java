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
package org.aposin.mergeprocessor.model;

/**
 * Implementations of this interface provides credentials for a login.
 * The credentials are bypassed by an array where the first element at index 0
 * is the user name and the second element at index 1 is the password. If
 * the authentication could not be done an {@link AuthenticationException} is
 * thrown.
 * 
 * @author Stefan Weiser
 *
 */
public interface ICredentialProvider {

	/**
	 * Runs the authentication process.
	 * 
	 * @return an array containing the credentials where the first element at index 0
	 * is the user name and the second element at index 1 is the password
	 * @throws AuthenticationException if the authentication could not be done
	 */
	String[] authenticate() throws AuthenticationException;

	/**
	 * Thrown if the authentication in instances of {@link ICredentialProvider} could not be done.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	public static class AuthenticationException extends Exception {

		private static final long serialVersionUID = 700441587309940753L;

		/**
		 * @param e the root cause
		 */
		public AuthenticationException(final Exception e) {
			super("Authentication failed.", e);
		}

		/**
		 * @param message the detail message.
		 */
		public AuthenticationException(final String message) {
			super(message);
		}

	}

}
