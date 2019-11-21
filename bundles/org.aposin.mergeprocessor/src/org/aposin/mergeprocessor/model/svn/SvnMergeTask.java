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
package org.aposin.mergeprocessor.model.svn;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.aposin.mergeprocessor.exception.SvnUtilException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff.SvnDiffAction;
import org.aposin.mergeprocessor.utils.ByteArrayUtil;
import org.aposin.mergeprocessor.utils.E4CompatibilityUtil;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * This class executes a merge unit by using the OS command line.
 * 
 * @author Stefan Weiser
 *
 */
public final class SvnMergeTask {

	private static final Logger LOGGER = Logger.getLogger(SvnMergeTask.class.getName());

	private final IProgressMonitor monitor;
	private final Consumer<String> commandConsumer;
	private final SVNMergeUnit mergeUnit;
	private final Path repository;
	private final ISvnClient svnClient;

	private final List<String> warnings = new ArrayList<>();

	/**
	 * @param monitor         the progress monitor to show the progress
	 * @param commandConsumer this consumer is used to trace any executed commands
	 * @param mergeUnit       the merge unit to merge
	 * @param repository      the repository where to merge into
	 * @param configuration   the configuration
	 */
	public SvnMergeTask(IProgressMonitor monitor, Consumer<String> commandConsumer, SVNMergeUnit mergeUnit,
			final Path repository) {
		this.monitor = monitor == null ? new NullProgressMonitor() : monitor;
		this.commandConsumer = commandConsumer;
		this.mergeUnit = Objects.requireNonNull(mergeUnit);
		if (Objects.requireNonNull(repository).toFile().exists()) {
			this.repository = Objects.requireNonNull(repository);
		} else {
			throw new IllegalArgumentException(String.format("The given path '%s' must exist.", repository));
		}
		svnClient = E4CompatibilityUtil.getApplicationContext().get(ISvnClient.class);
	}

	/**
	 * Executes the merge request.
	 * 
	 * @throws SvnUtilException
	 */
	public void merge() throws SvnUtilException {
		warnings.clear();
		svnClient.addCommandLineListener(commandConsumer);
		try {
			monitor.beginTask("Check Repository URL (1/6)", 1);
			try {
				checkRepository();
				checkTargetBranch();
			} catch (SvnClientException | MalformedURLException | URISyntaxException e) {
				throw LogUtil.throwing(new SvnUtilException(e));
			}
			monitor.worked(1);

			monitor.beginTask("Identify files to merge (2/6)", 1);
			final PathsToMerge pathsToMerge = getPathsToMerge();
			monitor.worked(1);

			final List<FromToPathTuple> recordMerges = new ArrayList<>();
			monitor.beginTask("Merge file (3/6)", pathsToMerge.contentChanges.size());
			for (final Path pathToMerge : pathsToMerge.contentChanges) {
				final Path targetPath = mergeUnit.getRenameMappingWithParents().get(pathToMerge);
				monitor.subTask(pathToMerge.toString());
				if (repository.resolve(targetPath).toFile().exists()) {
					// File Hierarchy may have changed, if not existing
					final String sourceURL = mergeUnit.getUrlSource() + '/' + pathToMerge.toString().replace('\\', '/');
					svnClient.update(repository.resolve(targetPath));
					svnClient.merge(repository.resolve(targetPath), new URL(sourceURL), mergeUnit.getRevisionEnd());
					if (!svnClient.hasModifications(repository.resolve(targetPath))) {
						warnings.add("Merge did not change anything on " + targetPath);
						continue;
					}
				} else {
					warnings.add("Path not found: " + targetPath);
					continue;
				}
				monitor.worked(1);

				recordMerges.add(new FromToPathTuple(pathToMerge, targetPath));
			}

			// Do the property merges
			monitor.beginTask("Merge properties (4/6)", pathsToMerge.propertyChanges.size());
			for (final Path pathToMerge : pathsToMerge.propertyChanges) {
				monitor.subTask(pathToMerge.toString());
				final Path targetPath = mergeUnit.getRenameMapping().get(pathToMerge);
				final String sourceURL = mergeUnit.getUrlSource() + '/' + pathToMerge.toString().replace('\\', '/');
				final Path localTargetPath = repository.resolve(targetPath);
				if (Files.exists(localTargetPath)) {
					svnClient.update(localTargetPath);
					svnClient.merge(localTargetPath, new URL(sourceURL), mergeUnit.getRevisionEnd(), false);
				} else {
					warnings.add(String.format(
							"Record merge on '%s' not possible. Path does not exist. Record merge skipped on path.",
							localTargetPath));
				}
				monitor.worked(1);
			}

			// Do the records after update
			List<FromToPathTuple> recordMergesToDo = calculateRecordMerges(recordMerges);
			recordMergesToDo = recordMergesToDo.stream()
					.filter(tuple -> !pathsToMerge.contentChanges.contains(tuple.from)).collect(Collectors.toList());
			monitor.beginTask("Merge record (5/6)", recordMergesToDo.size());
			for (final FromToPathTuple tuple : recordMergesToDo) {
				monitor.subTask(tuple.to.toString());
				update(tuple);
				mergeRecord(tuple);
				monitor.worked(1);
			}

			monitor.beginTask("Fix package definitions for new java classes (6/6)", 1);
			renamePackageInNewJavaClasses();
			monitor.worked(1);

			final List<String> distinctMissingPaths = warnings.stream().distinct().collect(Collectors.toList());
			warnings.clear();
			warnings.addAll(distinctMissingPaths);
		} catch (SvnClientException | MalformedURLException e) {
			throw new SvnUtilException(e);
		} finally {
			svnClient.removeCommandLineListener(commandConsumer);
		}
	}

	/**
	 * Calculate the required paths for the record merge.
	 * 
	 * @param doneMerges the merges paths
	 * @return the record paths to merge
	 */
	private static List<FromToPathTuple> calculateRecordMerges(List<FromToPathTuple> doneMerges) {
		final List<FromToPathTuple> result = new ArrayList<>();
		for (final FromToPathTuple doneMerge : doneMerges) {
			Path from = doneMerge.from.getParent();
			Path to = doneMerge.to.getParent();
			while (from != null && to != null) {
				if (Objects.equals(from.getFileName().toString(), to.getFileName().toString())) {
					final FromToPathTuple tuple = new FromToPathTuple(from, to);
					if (!result.contains(tuple)) {
						result.add(tuple);
					}
					from = from.getParent();
					to = to.getParent();
				} else {
					break;
				}
			}

			while (from != null) {
				if (doneMerge.to.startsWith(from)) {
					final FromToPathTuple tuple = new FromToPathTuple(from, from);
					if (!result.contains(tuple)) {
						result.add(tuple);
					}
				}
				from = from.getParent();
			}
		}
		return result;
	}

	/**
	 * @return all missing paths which could not be found while merging.
	 */
	public List<String> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	/**
	 * Updates the given path.
	 * 
	 * @param path    the path to update
	 * @param service the renaming service
	 * @throws SvnClientException
	 */
	private void update(final FromToPathTuple tuple) throws SvnClientException {
		final Path pathToUpdate = repository.resolve(tuple.to);
		if (pathToUpdate.toFile().exists()) {
			svnClient.updateEmpty(pathToUpdate);
		} else {
			warnings.add("Path not found: " + pathToUpdate);
		}
	}

	/**
	 * Executes a record merge for the given path.
	 * 
	 * @param path    the path to merge
	 * @param service the renaming service
	 * @throws SvnUtilException
	 * @throws SvnClientException
	 * @throws MalformedURLException
	 */
	private void mergeRecord(final FromToPathTuple tuple)
			throws SvnUtilException, MalformedURLException, SvnClientException {
		final Path absolutePath = repository.resolve(tuple.to);
		final String source = mergeUnit.getUrlSource() + '/' + tuple.from.toString().replace('\\', '/');
		if (absolutePath.toFile().exists()) {
			svnClient.merge(absolutePath, new URL(source), mergeUnit.getRevisionEnd(), false, true);
		} else {
			warnings.add("Path not found: " + absolutePath);
		}
	}

	/**
	 * Checks if the workspace matches with the required SVN repository from the
	 * merge unit. If the workspace does not match with the repository, a
	 * {@link SvnUtilException} is thrown.
	 * 
	 * @throws SvnUtilException   if the workspace does not match with the
	 *                            repository
	 * @throws SvnClientException
	 */
	private void checkRepository() throws SvnClientException, SvnUtilException {
		final URL repoUrl = svnClient.getRepositoryUrl(repository);
		if (!mergeUnit.getUrlTarget().contains(repoUrl.toString())) {
			throw LogUtil.throwing(new SvnUtilException("SVN repository does not match."));
		}
	}

	/**
	 * Checks if the workspace checked out the required target branch for the merge
	 * unit. If the workspace does not have checked out the correct target branch, a
	 * {@link SvnUtilException} is thrown.
	 * 
	 * @throws SvnUtilException      if the workspace does not have checkout the
	 *                               correct target branch
	 * @throws SvnClientException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	private void checkTargetBranch()
			throws SvnUtilException, SvnClientException, MalformedURLException, URISyntaxException {
		final URL svnUrl = svnClient.getSvnUrl(repository);
		if (!svnUrl.toURI().equals(new URI(mergeUnit.getUrlTarget()))) {
			throw LogUtil.throwing(new SvnUtilException("SVN target branch does not match."));
		}
	}

	/**
	 * Renames the package definition of new Java classes.
	 * 
	 * @param service the renaming service
	 * @throws SvnUtilException
	 */
	private void renamePackageInNewJavaClasses() throws SvnUtilException {
		// Identify all added java files, which have a new package structure
		final Map<Path, Path> renameMapping = mergeUnit.getRenameMapping();
		final List<Path> addedAndRenamedJavaPath = mergeUnit.getSvnDiff().stream() //
				.filter(diff -> diff.getAction() == SvnDiffAction.ADDED) // Only added entries
				.filter(diff -> diff.getUrl().toString().endsWith(".java")) // Only java files
				.filter(diff -> renameMapping.containsKey(mergeUnit.convertSvnDiffToPath(diff))) // Only renamed files
				.map(diff -> mergeUnit.convertSvnDiffToPath(diff)) // Convert to Path
				.collect(Collectors.toList());

		for (final Path oldPath : addedAndRenamedJavaPath) {
			final Path newPath = renameMapping.get(oldPath);
			final Path javaFile = repository.resolve(newPath);

			final Optional<String> oldPackageName = getPackageNameFromClassContent(javaFile);
			final Optional<String> newPackageName = getPackageNameFromRelativToProjectFile(javaFile);
			if (oldPackageName.isPresent() && newPackageName.isPresent()) {
				try {
					final byte[] fileInBytes = FileUtils.readFileToByteArray(javaFile.toFile());
					final byte[] bytesToReplace = ("package " + oldPackageName.get()).getBytes();
					final byte[] bytesReplacing = ("package " + newPackageName.get()).getBytes();

					final byte[] newFileInBytes = ByteArrayUtil.replace(fileInBytes, bytesToReplace, bytesReplacing);
					FileUtils.writeByteArrayToFile(javaFile.toFile(), newFileInBytes);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Could not replace the package definition in the Java file: " + javaFile,
							e);
				}
			}
		}
	}

	/**
	 * Returns the package name by parsing the content of the class.
	 * 
	 * @param javaClassPath the path of the class to parse
	 * @return the package name if it could be parsed from the content
	 */
	private static Optional<String> getPackageNameFromClassContent(final Path javaClassPath) {
		try {
			final LineIterator lineIterator = FileUtils.lineIterator(javaClassPath.toFile());
			while (lineIterator.hasNext()) {
				final String line = lineIterator.next();
				if (line.startsWith("package ")) {
					return Optional.of(line.substring(8, line.indexOf(';')));
				}
			}
			LogUtil.getLogger().warning("No Package could be parsed from the Java file content: " + javaClassPath);
		} catch (IOException e) {
			LogUtil.getLogger().log(Level.SEVERE,
					"An error occurred during parsing the Java file content: " + javaClassPath, e);
		}
		return Optional.empty();
	}

	/**
	 * Returns the package name by evaluating the path structure. The file
	 * '.project' is expected to exist, so the path starting from this file
	 * represents the package name.
	 * 
	 * @param path the path of the class
	 * @return the package name if it could be evaluated from the path
	 */
	private static Optional<String> getPackageNameFromRelativToProjectFile(final Path path) {
		Path p = path;
		// p.getParent() because we expect an existing src folder
		while (p.getParent() != null) {
			if (p.getParent().resolve(".project").toFile().exists()) {
				return Optional.of(p.relativize(path).getParent().toString().replace('\\', '.').replace('/', '.'));
			} else {
				p = p.getParent();

			}
		}
		LOGGER.warning(String
				.format("Could not evaluate package name for given class '%s', as '.project' file not found.", path));
		return Optional.empty();
	}

	/**
	 * Returns a list of paths, which have to be merged.
	 * 
	 * @param revision the revision for which to identify the paths to merge
	 * @param url      the SVN url
	 * @return a list of all Paths which have to be merged
	 * @throws SvnUtilException
	 * @throws SvnClientException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	private PathsToMerge getPathsToMerge() throws SvnUtilException {
		final List<Path> contentChanges = new ArrayList<>();
		final List<Path> propertyChanges = new ArrayList<>();
		final List<SvnDiff> diff = mergeUnit.getSvnDiff();
		for (final SvnDiff entry : diff) {
			final Path parentPath;
			switch (entry.getAction()) {
			case ADDED:
			case REPLACED:
				parentPath = mergeUnit.convertSvnDiffToPath(entry).getParent();
				if (!contentChanges.contains(parentPath)) {
					contentChanges.add(parentPath);
				}
				break;
			case DELETED:
				boolean isParentAlsoDeleted = false;
				for (final SvnDiff entry1 : diff) {
					if (!entry.getUrl().toString().equals(entry1.getUrl().toString())
							&& entry.getUrl().toString().contains(entry1.getUrl().toString())
							&& entry1.getAction() == SvnDiffAction.DELETED) {
						isParentAlsoDeleted = true;
						break;
					}
				}
				if (isParentAlsoDeleted) {
					break;
				} else {
					parentPath = mergeUnit.convertSvnDiffToPath(entry).getParent();
					if (!contentChanges.contains(parentPath)) {
						contentChanges.add(parentPath);
					}
				}
				break;
			case MODIFIED:
				contentChanges.add(mergeUnit.convertSvnDiffToPath(entry));
				break;
			case PROPERTY_CHANGED:
				propertyChanges.add(mergeUnit.convertSvnDiffToPath(entry));
				break;
			default:
				throw LogUtil.throwing(new SvnUtilException("Unknown SvnDiffAction: " + entry.getAction()));
			}

		}

		// Filter paths if a parent is also a path to merge
		final List<Path> contentChanges2 = contentChanges.stream().filter(path -> {
			Path potentialParent = path.getParent();
			while (potentialParent != null) {
				if (contentChanges.contains(potentialParent)) {
					return false;
				}
				potentialParent = potentialParent.getParent();
			}
			return true;
		}).collect(Collectors.toList());
		return new PathsToMerge(contentChanges2, propertyChanges);
	}

	private static class PathsToMerge {

		private final List<Path> contentChanges;
		private final List<Path> propertyChanges;

		/**
		 * @param contentChanges
		 * @param propertyChanges
		 */
		private PathsToMerge(List<Path> contentChanges, List<Path> propertyChanges) {
			this.contentChanges = Collections.unmodifiableList(contentChanges);
			this.propertyChanges = Collections.unmodifiableList(propertyChanges);
		}

	}

	/**
	 * Container object showing the from-to merge paths.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class FromToPathTuple {

		private final Path from;
		private final Path to;

		/**
		 * @param from
		 * @param to
		 */
		private FromToPathTuple(Path from, Path to) {
			this.from = from;
			this.to = to;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
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
			FromToPathTuple other = (FromToPathTuple) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}

	}

}
