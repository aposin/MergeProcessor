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

import java.net.MalformedURLException;
import java.net.URL;

import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Base class for implementations of {@link ISvnClient}, which provides utility functions for the client.
 * 
 * @author Stefan Weiser
 *
 */
public abstract class AbstractSvnClient implements ISvnClient {

	/**
	 * Converts the given {@link URL} to a {@link String}. The URL may link to a local file. 
	 * SVN requires this link as String starting with "file:///..." but {@link URL#toString()}
	 * only returns "file:/...". This is corrected by this method.
	 * 
	 * @param url the URL to convert
	 * @return the URL as String
	 */
	protected static String convertURLToString(final URL url) {
		return url.toString().startsWith("file:/") ? url.toString().replace("file:/", "file:///") : url.toString();
	}

	/**
	 * Converts the given {@link URL} to a {@link String}.
	 * 
	 * @param url the URL to convert
	 * @return the {@link SVNUrl}
	 * @throws MalformedURLException
	 */
	protected static SVNUrl toSVNUrl(final URL url) throws MalformedURLException {
		return new SVNUrl(convertURLToString(url));
	}

}
