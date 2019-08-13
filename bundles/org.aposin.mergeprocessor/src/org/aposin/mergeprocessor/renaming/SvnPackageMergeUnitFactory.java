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
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.IVersionProvider;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.SvnLinkedArtifact;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog.SvnLogEntry;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.e4.core.di.annotations.Creatable;

/**
 * This implementation checks (when #run is called) in an interval if new linked artifacts have to be created.
 * This loop is running as long as the thread gets interrupted or {@link #stop()} is called. 
 * 
 * @author Stefan Weiser
 *
 */
@Creatable
public class SvnPackageMergeUnitFactory {

	private final IConfiguration configuration;
	private final IFileSystemProvider fileSystemProvider;
	private final IVersionProvider versionProvider;

	private final String localIp;
	private final Map<SvnLinkedArtifact, Path> linkedArtifacts;

	/**
	 * @param configuration the configuration
	 * @param fileSystemProvider provider where to write the created merge file
	 * @param versionProvider the provider delivering  the version for an SVN URL
	 * @param client the client to communicate to SVN
	 * @throws SvnClientException
	 * @throws UnknownHostException
	 * @throws URISyntaxException
	 */
	@Inject
	public SvnPackageMergeUnitFactory(final IConfiguration configuration, final IFileSystemProvider fileSystemProvider,
			final IVersionProvider versionProvider, final ISvnClient client)
			throws SvnClientException, UnknownHostException, URISyntaxException {
		this.configuration = Objects.requireNonNull(configuration);
		this.fileSystemProvider = Objects.requireNonNull(fileSystemProvider);
		this.versionProvider = Objects.requireNonNull(versionProvider);
		Objects.requireNonNull(client);
		localIp = InetAddress.getLocalHost().getHostAddress();

		linkedArtifacts = new HashMap<>();
		for (final URL url : RenamingService.getObservableSvnRepositoriesForLinkedArtifacts(configuration)) {
			try {
				final Path path = configuration.getUserWorkingFolder()
						.resolve("svnLinkedArtifact" + url.toURI().hashCode());
				final SvnLinkedArtifact artifact = SvnLinkedArtifact.getOrCreate(url, path, client);
				linkedArtifacts.put(artifact, path);
			} catch (SvnClientException e) {
				LogUtil.throwing(e);
			}
		}
	}

	/**
	 * Checks for new linked artifacts, creates merge files for them and writes them to the given {@link IFileSystemProvider}.
	 */
	public void checkAndCreateNewSvnPackageMergeUnit() {
		LogUtil.entering();
		for (final Entry<SvnLinkedArtifact, Path> entry : linkedArtifacts.entrySet()) {
			final SvnLinkedArtifact artifact = entry.getKey();
			LogUtil.getLogger()
					.fine(() -> String.format("Check for new commits of SVN repository %s.", artifact.getUrl()));
			try {
				final List<Container> packageMergeInfos = createSvnPackageMergeInfos(artifact);
				// Write remote files
				for (final Container container : packageMergeInfos) {
					fileSystemProvider.write(container.fileName, container.content);
				}
				// persist the status
				artifact.persist(entry.getValue());
			} catch (IOException e) {
				LogUtil.throwing(e);
			}
		}
		LogUtil.exiting();
	}

	private List<Container> createSvnPackageMergeInfos(final SvnLinkedArtifact artifact) throws IOException {
		final List<Container> result = new ArrayList<>();
		final List<SvnLog> logs = artifact.checkForNew(configuration.getUser());
		final ZonedDateTime now = ZonedDateTime.now();
		final String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now);
		for (final SvnLog log : logs) {
			// Build the file content
			if (log.getMessage().matches("MP\\s\\[\\d+:\\d+\\]\\s(.|\\n|\\r)*")) {
				/*
				 * Messages from MergeProcessor look like: MP [12345:12346] bla bla bla ...
				 */
				continue;
			}
			final List<String> workingCopyFiles = getWorkingCopyFiles(log, artifact.getUrl());
			if (workingCopyFiles.isEmpty()) {
				continue;
			}
			final long revision = log.getRevision();
			final StringBuilder sb = new StringBuilder(
					String.format("# Created on host=[%s] at datetime=[%s] by user[%s].", localIp, time,
							configuration.getSftpConfiguration().getUser())).append(System.lineSeparator());
			final String branch = getBranchUrlString(log, artifact.getUrl());
			sb.append("URL_BRANCH_SOURCE=").append(branch).append(System.lineSeparator());
			sb.append("URL_BRANCH_TARGET=").append(branch).append(System.lineSeparator());
			sb.append("REVISION_START=").append(revision - 1l).append(System.lineSeparator());
			sb.append("REVISION_END=").append(revision).append(System.lineSeparator());
			for (final String workingCopyFile : workingCopyFiles) {
				sb.append("WORKING_COPY_FILE=").append(workingCopyFile).append(System.lineSeparator());
			}

			// Build the file name
			final StringBuilder fileName = new StringBuilder();
			fileName.append("abscore_r");
			fileName.append(revision);
			fileName.append('_');
			fileName.append(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_xxxx").format(now));
			fileName.append(Configuration.SVN_PACKAGE_MERGE_EXTENSION_FILE);

			result.add(new Container(configuration.getSftpConfiguration().getTodoFolder() + fileName.toString(),
					sb.toString()));
		}
		return result;
	}

	/**
	 * @param log the log entry for which to get the branch URL String
	 * @return the branch URL String
	 */
	private String getBranchUrlString(SvnLog log, final URL svnRepository) {
		if (log.getEntries().isEmpty()) {
			throw new IllegalArgumentException("SvnLog without entries cannot be used to determine the branch.");
		}
		final String entriesWithoutSvnRepositoryUrl = StringUtils
				.removeStart(log.getEntries().get(0).getUrl().toString(), svnRepository.toString()).substring(1);
		if (entriesWithoutSvnRepositoryUrl.startsWith("trunk")) {
			return svnRepository.toString() + "/trunk";
		} else if (entriesWithoutSvnRepositoryUrl.startsWith("branches")) {
			final String partAfterBranches = entriesWithoutSvnRepositoryUrl.substring("branches".length() + 1);
			final String[] split = partAfterBranches.split("/");
			if (split.length > 0) {
				return svnRepository.toString() + "/branches/" + split[0];
			} else {
				return svnRepository.toString() + "/branches";
			}
		} else {
			final String[] split = entriesWithoutSvnRepositoryUrl.split("/");
			if (split.length > 0) {
				return svnRepository.toString() + '/' + split[0];
			} else {
				return svnRepository.toString();
			}
		}
	}

	/**
	 * @param log the log file entry which provides the file changes done by the log.
	 * @return the list of file changes done the the SVN commit for the given log
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
	private List<String> getWorkingCopyFiles(SvnLog log, final URL svnRepository) throws IOException {
		final List<String> list = new ArrayList<>(log.getEntries().size());
		if (!log.getEntries().isEmpty()) {
			final String branchUrlString = getBranchUrlString(log, svnRepository);
			final Version version = versionProvider.forSvnUrl(branchUrlString);
			final String svnUrl = svnRepository.toString().startsWith("file:/")
					? svnRepository.toString().replace("file:/", "file:///")
					: svnRepository.toString();
			try (final RenamingService service = new RenamingService(configuration, svnUrl, version, version)) {
				for (final SvnLogEntry entry : log.getEntries()) {
					final String from = StringUtils.removeStart(entry.getUrl().toString(),
							svnRepository.toString() + '/');
					final Path linkedArtifactResult = service.getLinkedArtifact(
							Paths.get(StringUtils.remove(entry.getUrl().toString(), branchUrlString).substring(1)));
					final String to = StringUtils
							.removeStart(branchUrlString + '/' + linkedArtifactResult.toString().replace('\\', '/'),
									svnRepository.toString())
							.substring(1);
					if (Objects.equals(from, to)) {
						continue;
					}
					final StringBuilder sb = new StringBuilder();
					switch (entry.getAction()) {
					case ADDED:
						sb.append('A');
						break;
					case DELETED:
						sb.append('D');
						break;
					case MODIFIED:
						sb.append('M');
						break;
					case REPLACED:
						sb.append('R');
						break;
					default:
						sb.append(' ');
						break;
					}
					sb.append("   ");
					sb.append(from);
					sb.append(">");
					sb.append(to);
					list.add(sb.toString());
				}
			}
		}
		return list;
	}

	/**
	 * Container object matching the file name and its content.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class Container {

		private final String fileName;
		private final String content;

		/**
		 * @param fileName
		 * @param content
		 */
		private Container(String fileName, String content) {
			this.fileName = fileName;
			this.content = content;
		}

	}

}
