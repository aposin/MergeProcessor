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
package org.aposin.mergeprocessor.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.renaming.Version;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This version providers delivers the version from a pom.xml. The content of
 * the XML looks like follows:
 * 
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi=
"http://www.w3.org/2001/XMLSchema-instance"
 *     xsi:schemaLocation=
"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
 *     <modelVersion>4.0.0</modelVersion>
 *
 *     <groupId>org.aposin.mergeprocessor</groupId>
 *     <artifactId>org.aposin.mergeprocessor.parent</artifactId>
 *     <version>19.0.101-SNAPSHOT</version><!-- THIS FIELD WE WANT TO READ OUT -->
 *     <packaging>pom</packaging>
 *
 *     <description>Some description of the pom</description>
 *     ...
 * 
 * }
 * </pre>
 * 
 * @author Stefan Weiser
 *
 */
public class PomFileVersionProvider implements IVersionProvider {

	/** Size of cache which should not exceed. */
	private static final int CACHE_SIZE = 20;

	private final IConfiguration configuration;
	private final ISvnClient svnClient;
	private final LinkedList<Container> cache = new LinkedList<>();

	/**
	 * @param configuration the configuration
	 */
	@Inject
	public PomFileVersionProvider(final IConfiguration configuration, final ISvnClient svnClient) {
		this.configuration = Objects.requireNonNull(configuration);
		this.svnClient = Objects.requireNonNull(svnClient);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Version forSvnUrl(String svnUrl) {
		LogUtil.entering(svnUrl);
		final Container cacheResult = getFromCache(svnUrl);
		if (cacheResult != null) {
			return LogUtil.exiting(cacheResult.version);
		} else {
			final Container container = getFromPomFile(svnUrl);
			cache.add(container);
			while (cache.size() > CACHE_SIZE) {
				cache.remove(0);
			}
			return LogUtil.exiting(container.version);
		}
	}

	/**
	 * Resolves the mapping between SVN URL and the version from the pom.xml.
	 * 
	 * @param svnUrl the SVN URL
	 * @return the version or {@code null} if the version could not be resolved for
	 *         the given SVN URL
	 */
	private Container getFromPomFile(String svnUrl) {
		LogUtil.entering(svnUrl);
		final List<Path> paths = configuration.getVersionInfoPaths();
		for (final Path path : paths) {
			final Path fileName = path.getFileName();
			if (fileName != null && "pom.xml".equals(fileName.toString())) {
				try {
					final String content = svnClient.cat(new URL(svnUrl + '/' + path.toString().replace('\\', '/')));
					final Version version = new Version(getVersionFromPomXml(content));
					final Container container = new Container(svnUrl, version);
					return LogUtil.exiting(container);
				} catch (SvnClientException e) {
					final Logger logger = LogUtil.getLogger();
					if (logger.getLevel() == Level.SEVERE || logger.getLevel() == Level.WARNING) {
						logger.log(Level.INFO, String.format("SVN Path '%s' does not exist.", path));
					} else {
						logger.log(Level.INFO, String.format("SVN Path '%s' does not exist.", path), e);
					}
				} catch (ParserConfigurationException | SAXException | IOException e) {
					LogUtil.getLogger().log(Level.WARNING, String.format("Error on parsing '%s'", path), e);
				}
			} else {
				LogUtil.getLogger().info(() -> String.format("Path '%s' is not a pom.xml.", path));
			}
		}
		LogUtil.getLogger().info(() -> String.format("The version could be identified for '%s'.", svnUrl));
		return LogUtil.exiting(new Container(svnUrl, Version.ZERO));
	}

	/**
	 * Resolves the mapping between SVN URL and the version from the internal cache.
	 * 
	 * @param svnUrl the SVN URL
	 * @return the version or {@code null} if the cache does not contain a match for
	 *         the given SVN URL
	 */
	private Container getFromCache(String svnUrl) {
		LogUtil.entering(svnUrl);
		for (final Container container : cache) {
			if (svnUrl.equals(container.svnUrl)) {
				return LogUtil.exiting(container);
			}
		}
		return LogUtil.exiting(null);
	}

	/**
	 * Parses the given pom.xml {@link String} and returns the String representing
	 * the version.
	 * 
	 * @param pomXmlContent the pom.xml as {@link String}
	 * @return the version as {@link String}
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private static String getVersionFromPomXml(final String pomXmlContent)
			throws SAXException, IOException, ParserConfigurationException {
		final StringBuilder sb = new StringBuilder();
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		final SAXParser parser = factory.newSAXParser();
		try (final InputStream contentStream = IOUtils.toInputStream(pomXmlContent, StandardCharsets.UTF_8)) {
			parser.parse(contentStream, new DefaultHandler() {

				private final String[] match = new String[] { "version", "project" };
				private ArrayDeque<String> deque = new ArrayDeque<>();

				/**
				 * {@inheritDoc}
				 */
				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					deque.push(qName);
				}

				/**
				 * {@inheritDoc}
				 */
				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException {
					deque.pop();
				}

				/**
				 * {@inheritDoc}
				 */
				@Override
				public void characters(char[] ch, int start, int length) throws SAXException {
					if (deque.size() == 2 && Arrays.equals(deque.toArray(), match)) {
						final char[] targetContent = Arrays.copyOfRange(ch, start, start + length);
						sb.append(new String(targetContent).replace("-SNAPSHOT", ""));
					}
				}

			});
			return sb.toString();
		}
	}

	/**
	 * Container object for a given SVN URL and its version.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class Container {

		private final String svnUrl;
		private final Version version;

		/**
		 * @param svnUrl
		 * @param version
		 */
		private Container(String svnUrl, Version version) {
			this.svnUrl = svnUrl;
			this.version = version;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((svnUrl == null) ? 0 : svnUrl.hashCode());
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Container other = (Container) obj;
			if (svnUrl == null) {
				if (other.svnUrl != null)
					return false;
			} else if (!svnUrl.equals(other.svnUrl))
				return false;
			if (version == null) {
				if (other.version != null)
					return false;
			} else if (!version.equals(other.version))
				return false;
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return svnUrl + '=' + version;
		}

	}

}
