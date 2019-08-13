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
package org.aposin.mergeprocessor.renaming;

import java.io.IOException;

/**
 * This interface defines the possibility to interact with different file systems on
 * an abstract layer.
 * 
 * @author Stefan Weiser
 *
 */
public interface IFileSystemProvider {

	/**
	 * Writes some file content to a given file path. Any existing files will be overwritten.
	 * 
	 * @param filePath the file path where to write to
	 * @param content the content to write
	 * @throws IOException
	 */
	void write(final String filePath, final String content) throws IOException;

}
