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
/**
 * 
 */
package org.aposin.mergeprocessor.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.view.Column;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>Initializes the default preference for the merge processor.</p>
 * 
 * <p>A set of default values is directly defined within this class. These values are used as default values, as long
 * as no other default values are defined user specific. User specific default preferences can be defined in a property
 * file take from {@link Configuration#getUserPrefsPath()}. Any values from this property file overrule hardcoded
 * default preferences.</p>
 * 
 * @author Stefan Weiser
 *
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private final IConfiguration configuration;

	/**
	 * @param configuration the configuration
	 */
	public PreferenceInitializer(final IConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializeDefaultPreferences() {
		LogUtil.entering();
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		final Properties properties = getApplicationDefaultProperties();
		final Path path = configuration.getUserPrefsPath();
		if (path != null && path.toFile().exists()) {
			try (final InputStream is = Files.newInputStream(path)) {
				properties.load(is);
			} catch (IOException e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE,
						String.format("Could not load preferences from '%s'.", path), e); //$NON-NLS-1$
			}
		}
		validateAndConvertPropertyValues(properties);
		properties.forEach((key, value) -> setDefaultToPreferenceStore(store, key.toString(), value));
		LogUtil.exiting();
	}

	/**
	 * @return the application default properties, which are hardcoded
	 */
	private static Properties getApplicationDefaultProperties() {
		final Properties properties = new Properties();
		properties.put(WorkbenchPreferencePage.USER_ID, System.getProperty("user.name").toLowerCase()); //$NON-NLS-1$
		properties.put(WorkbenchPreferencePage.REFRESH_INTERVAL, 60);
		properties.put(WorkbenchPreferencePage.LOG_LEVEL, Level.INFO.getName());
		properties.put(WorkbenchPreferencePage.WINDOW_LOCATION, "50,50"); //$NON-NLS-1$
		properties.put(WorkbenchPreferencePage.WINDOW_SIZE, "688,320"); //$NON-NLS-1$
		properties.put(WorkbenchPreferencePage.OPTION_AUTOMATIC, false);
		properties.put(WorkbenchPreferencePage.OPTION_DISPLAY_DONE, false);
		properties.put(WorkbenchPreferencePage.OPTION_DISPLAY_IGNORED, false);
		properties.put(WorkbenchPreferencePage.SORT_COLUMN, Column.COLUMN_DATE.ordinal());
		properties.put(WorkbenchPreferencePage.SORT_DIRECTION, SWT.UP);
		properties.put(GitRepositoriesPreferencePage.GIT_REPOSITORIES_AUTO_REPO_CREATE, true);
		findEclipsePath().ifPresent(
				path -> properties.put(EclipseWorkspaceStartPeferencePage.ECLIPSE_APPLICATION_PATH, path.toString()));
		properties.put(EclipseWorkspaceStartPeferencePage.ECLIPSE_APPLICATION_PARAMETERS,
				"-Drefreshprojects -showlocation -clean -nl en -vmargs -Xms400M -Xmx1700M -XX:MaxPermSize=256M -Dorg.eclipse.ecf.provider.filetransfer.excludeContributors=org.eclipse.ecf.provider.filetransfer.httpclient4"); //$NON-NLS-1$
		return properties;
	}

	/**
	 * Validate and convert the properties to the correct type. The values are taken from the properties,
	 * checked for its type and converted to the correct type if they are {@link String} values.
	 * 
	 * @param properties the properties where to take the values from
	 */
	private static void validateAndConvertPropertyValues(final Properties properties) {
		validateAndConvertIntegerProperty(properties, WorkbenchPreferencePage.REFRESH_INTERVAL);
		validateAndConvertBooleanProperty(properties, WorkbenchPreferencePage.OPTION_AUTOMATIC);
		validateAndConvertBooleanProperty(properties, WorkbenchPreferencePage.OPTION_DISPLAY_DONE);
		validateAndConvertBooleanProperty(properties, WorkbenchPreferencePage.OPTION_DISPLAY_IGNORED);
		validateAndConvertIntegerProperty(properties, WorkbenchPreferencePage.SORT_COLUMN);
		validateAndConvertIntegerProperty(properties, WorkbenchPreferencePage.SORT_DIRECTION);
		validateAndConvertBooleanProperty(properties, GitRepositoriesPreferencePage.GIT_REPOSITORIES_AUTO_REPO_CREATE);
	}

	/**
	 * Validate and convert the property for the given property name to a {@link Boolean} value. The value is taken 
	 * from the properties, checked for its type and converted to a {@link Boolean} if it's a {@link String} value.
	 * 
	 * @param properties the properties where to take the value from
	 * @param name the name of the property to map {@link Boolean} values
	 */
	private static void validateAndConvertBooleanProperty(final Properties properties, final String name) {
		final String stringValue = properties.getProperty(name);
		if (stringValue != null) {
			final boolean booleanValue = Boolean.parseBoolean(stringValue);
			properties.put(name, booleanValue);
		}
	}

	/**
	 * Validate and convert the property for the given property name to an {@link Integer} value. The value is taken 
	 * from the properties, checked for its type and converted to an {@link Integer} if it's a {@link String} value.
	 * 
	 * @param properties the properties where to take the value from
	 * @param name the name of the property to map {@link Integer} values
	 */
	private static void validateAndConvertIntegerProperty(final Properties properties, final String name) {
		final String stringValue = properties.getProperty(name);
		if (stringValue != null) {
			try {
				final int intValue = Integer.parseInt(stringValue);
				properties.put(name, intValue);
			} catch (NumberFormatException e) {
				Logger.getLogger(PreferenceInitializer.class.getName()).log(Level.SEVERE,
						String.format("Property '%s' for %s is not a valid integer value.", stringValue, name), e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Sets the given key-value pair into the preference store.
	 * 
	 * @param store the preference store
	 * @param key the key
	 * @param value the value
	 */
	private static void setDefaultToPreferenceStore(final IPreferenceStore store, final String key,
			final Object value) {
		if (value instanceof String) {
			store.setDefault(key, (String) value);
		} else if (value instanceof Boolean) {
			store.setDefault(key, (boolean) value);
		} else if (value instanceof Integer) {
			store.setDefault(key, (Integer) value);
		} else if (value instanceof Long) {
			store.setDefault(key, (Long) value);
		} else if (value instanceof Float) {
			store.setDefault(key, (Float) value);
		} else if (value instanceof Double) {
			store.setDefault(key, (Double) value);
		}
	}

	/**
	 * @return the eclipse path to use if available
	 */
	private static Optional<Path> findEclipsePath() {
		if (SystemUtils.IS_OS_WINDOWS) {
			final List<Path> potentialEclipsePaths = new ArrayList<>();
			final Path programFiles = Paths.get("C:", "Program Files"); //$NON-NLS-1$
			potentialEclipsePaths.addAll(EclipsePathFinder.findEclipsePaths(programFiles));
			if (potentialEclipsePaths.isEmpty()) {
				// Try to find 32Bit Eclipse
				final Path programFilesX86 = Paths.get("C:", "Program Files (x86)"); //$NON-NLS-1$
				potentialEclipsePaths.addAll(EclipsePathFinder.findEclipsePaths(programFilesX86));
			}
			Collections.sort(potentialEclipsePaths, new EclipsePathComparator().reversed());
			if (!potentialEclipsePaths.isEmpty()) {
				return Optional.of(potentialEclipsePaths.get(0).resolve("eclipse.exe")); //$NON-NLS-1$
			} else {
				Logger.getLogger(EclipseWorkspaceStartPeferencePage.class.getName())
						.warning("No Eclipse application could be identified."); //$NON-NLS-1$
			}
		} else {
			Logger.getLogger(EclipseWorkspaceStartPeferencePage.class.getName())
					.warning("OS not supported to automatically identify Eclipse application."); //$NON-NLS-1$
		}
		return Optional.empty();
	}

	/**
	 * This file visitor searches for eclipse paths. Don't instantiate the class itself, instead call
	 * {@link #findEclipsePaths(Path)}.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static final class EclipsePathFinder extends SimpleFileVisitor<Path> {

		private final Path root;
		private final List<Path> potentialEclipsePaths = new ArrayList<>();

		private EclipsePathFinder(Path root) {
			this.root = root;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (dir.equals(root)) {
				return FileVisitResult.CONTINUE;
			} else {
				final Path fileName = dir.getFileName();
				if (fileName != null && fileName.startsWith("eclipse") && Files.exists(dir.resolve("eclipse.exe"))) {
					potentialEclipsePaths.add(dir);
				}
				return FileVisitResult.SKIP_SUBTREE;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			// Ignore any errors
			return FileVisitResult.SKIP_SUBTREE;
		}

		/**
		 * Returns a list of all eclipse paths found in the given root directory.
		 * 
		 * @param root the root directory where to search for eclipse paths
		 * @return a list of all eclipse paths
		 */
		public static List<Path> findEclipsePaths(final Path root) {
			try {
				final EclipsePathFinder visitor = new EclipsePathFinder(root);
				Files.walkFileTree(root, visitor);
				return Collections.unmodifiableList(visitor.potentialEclipsePaths);
			} catch (IOException e) {
				LogUtil.throwing(e);
				return Collections.emptyList();
			}
		}

	}

	/**
	 * This Comparator compares paths of eclipse applications by the eclipse version.
	 * The eclipse version is delivered by the bundle id of "org.eclipse.platform". 
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static final class EclipsePathComparator implements Comparator<Path> {

		private final Map<Path, String> eclipsePathVersionMapping = new HashMap<>();

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(Path o1, Path o2) {
			final String so1 = getVersionForEclipsePath(o1);
			final String so2 = getVersionForEclipsePath(o2);
			return StringUtils.compare(so1, so2);
		}

		/**
		 * Returns the version for the given eclipse path or {@code null} if the version
		 * could not be identified.
		 * 
		 * @param path the path of eclipse
		 * @return the version of the eclipse
		 */
		private String getVersionForEclipsePath(final Path path) {
			if (!eclipsePathVersionMapping.containsKey(path)) {
				final Path artifactsXML = path.resolve("artifacts.xml"); //$NON-NLS-1$
				if (artifactsXML.toFile().exists()) {
					try {
						DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						Document doc = db.parse(artifactsXML.toFile());
						doc.getDocumentElement().normalize();
						final Element repository = (Element) doc.getElementsByTagName("repository").item(0); //$NON-NLS-1$
						final Element artifacts = (Element) repository.getElementsByTagName("artifacts").item(0); //$NON-NLS-1$
						final NodeList artifactList = artifacts.getElementsByTagName("artifact"); //$NON-NLS-1$
						for (int i = 0; i < artifactList.getLength(); i++) {
							final Element artifact = (Element) artifactList.item(i);
							if ("org.eclipse.platform".equals(artifact.getAttribute("id")) //$NON-NLS-1$ //$NON-NLS-2$
									&& "osgi.bundle".equals(artifact.getAttribute("classifier"))) { //$NON-NLS-1$ //$NON-NLS-2$
								final String version = artifact.getAttribute("version"); //$NON-NLS-1$
								eclipsePathVersionMapping.put(path, version);
							}
						}
					} catch (ParserConfigurationException | SAXException | IOException e) {
						LogUtil.throwing(e);
					}
				}
			}
			return eclipsePathVersionMapping.get(path);
		}
	}

}
