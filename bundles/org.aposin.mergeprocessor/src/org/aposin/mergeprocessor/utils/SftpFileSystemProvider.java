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
/**
 * 
 */
package org.aposin.mergeprocessor.utils;

import java.io.IOException;

import org.aposin.mergeprocessor.renaming.IFileSystemProvider;

import com.jcraft.jsch.SftpException;

/**
 * This implementation uses {@link SftpUtil}.
 * 
 * @author Stefan Weiser
 *
 */
public class SftpFileSystemProvider implements IFileSystemProvider {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(String filePath, String content) throws IOException {
		try {
			SftpUtil.getInstance().writeToRemotePath(content, filePath);
			LogUtil.getLogger().info(() -> String.format("Write to remote path %s.", filePath));
		} catch (SftpException | SftpUtilException e) {
			throw LogUtil.throwing(new IOException(e));
		}
	}

}
