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
package org.aposin.mergeprocessor.configuration;

/**
 * Interface defining configurable SFTP parameters for the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public interface ISftpConfiguration {

	/**
	 * @return the path on the sftp server to the folder with the to do files for
	 *         this user.
	 */
	String getTodoFolder();

	/**
	 * @return the path on the sftp server to the folder with the done files for
	 *         this user.
	 */
	String getDoneFolder();

	/**
	 * @return the path on the sftp server to the folder with the ignored files for
	 *         this user.
	 */
	String getIgnoredFolder();

	/**
	 * @return the path on the sftp server to the folder with the canceled files for
	 *         this user.
	 */
	String getCanceledFolder();

	/**
	 * @return the path on the sftp server to the folder with the manual merged
	 *         files for this user.
	 */
	String getManualFolder();

	/**
	 * @return the host of the sftp server
	 */
	String getHost();

	/**
	 * @return the user to login on the sftp server
	 */
	String getUser();

	/**
	 * @return the password to login on the sftp server
	 */
	String getPassword();

}
