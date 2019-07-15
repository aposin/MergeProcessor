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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.aposin.mergeprocessor.configuration.ConfigurationException;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.ICredentialProvider;
import org.aposin.mergeprocessor.model.ICredentialProvider.AuthenticationException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnDiff.SvnDiffAction;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog.SvnLogAction;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog.SvnLogEntry;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.javahl.JhlClientAdapterFactory;
import org.tigris.subversion.svnclientadapter.utils.Depth;

/**
 * Implementation of {@link ISvnClient} using JavaHl.
 * 
 * @author Stefan Weiser
 *
 */
public class SvnClientJavaHl extends AbstractSvnClient {

    private final ISVNClientAdapter client;
    private final List<CommandLineListener> listeners = new ArrayList<>();

    private boolean isClosed = false;

    /**
     * @param provider to authenticate when required
     * @param configuration the configuration to get and set the username and password
     * @throws SvnClientException 
     */
    @Inject
    public SvnClientJavaHl(ICredentialProvider provider, IConfiguration configuration) throws SvnClientException {
        try {
            if (!SVNClientAdapterFactory.isSVNClientAvailable(JhlClientAdapterFactory.JAVAHL_CLIENT)) {
                JhlClientAdapterFactory.setup();
            }
            client = SVNClientAdapterFactory.createSVNClient(JhlClientAdapterFactory.JAVAHL_CLIENT);
            final String username = configuration.getSvnUsername();
            if (username != null) {
                client.setUsername(username);
            }
            final String password = configuration.getSvnPassword();
            if (password != null) {
                client.setPassword(password);
            }
            client.addPasswordCallback(new SVNPromptUserPassword(provider, configuration, client));
        } catch (SVNClientException | ConfigurationException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String cat(URL url) throws SvnClientException {
        try {
            try (final InputStream content = client.getContent(toSVNUrl(url), SVNRevision.HEAD)) {
                return IOUtils.toString(content, StandardCharsets.UTF_8);
            }
        } catch (IOException | SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SvnDiff> diff(final URL url, long fromRevision, long toRevision) throws SvnClientException {
        final List<SvnDiff> list = new ArrayList<>();
        try {
            final SVNDiffSummary[] diffSummarize = client.diffSummarize(toSVNUrl(url), new Number(fromRevision),
                    toSVNUrl(url), new Number(toRevision), Depth.infinity, true);
            for (final SVNDiffSummary svnDiffSummary : diffSummarize) {
                final SvnDiffAction action;
                if (svnDiffSummary.getDiffKind() == SVNDiffKind.ADDED) {
                    action = SvnDiffAction.ADDED;
                } else if (svnDiffSummary.getDiffKind() == SVNDiffKind.DELETED) {
                    action = SvnDiffAction.DELETED;
                } else if (svnDiffSummary.getDiffKind() == SVNDiffKind.MODIFIED) {
                    action = SvnDiffAction.MODIFIED;
                } else if (svnDiffSummary.getDiffKind() == SVNDiffKind.NORMAL) {
                    if (svnDiffSummary.propsChanged()) {
                        action = SvnDiffAction.PROPERTY_CHANGED;
                    } else {
                        throw LogUtil
                                .throwing(new SvnClientException("Unknown state of SVNDiffSummary " + svnDiffSummary));
                    }
                } else {
                    throw LogUtil
                            .throwing(new SvnClientException("Unknown SvnDiffAction " + svnDiffSummary.getDiffKind()));
                }
                list.add(new SvnDiff(action, new URL(convertURLToString(url) + '/' + svnDiffSummary.getPath())));
            }
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
        return list;
    }

    /**
     * {@inheritDoc}
     * @throws SvnClientException 
     */
    @Override
    public long showRevision(URL url) throws SvnClientException {
        try {
            final ISVNInfo info = client.getInfo(toSVNUrl(url));
            return info.getRevision().getNumber();
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(String.format("Exception occurred on showRevision(URL) with '%s'.", url), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SvnLog> log(URL url, long fromRevision, long toRevision, String author) throws SvnClientException {
        final List<SvnLog> logs = new ArrayList<>();
        try {
            final ISVNLogMessage[] logMessages = client.getLogMessages(toSVNUrl(url), new Number(fromRevision),
                    new Number(toRevision));
            for (final ISVNLogMessage logMessage : logMessages) {
                if (author == null || Objects.equals(logMessage.getAuthor(), author)) {
                    final List<SvnLogEntry> entries = new ArrayList<>();
                    for (final ISVNLogMessageChangePath changePath : logMessage.getChangedPaths()) {
                        final SvnLogAction action;
                        switch (changePath.getAction()) {
                            case 'A':
                                action = SvnLogAction.ADDED;
                                break;
                            case 'D':
                                action = SvnLogAction.DELETED;
                                break;
                            case 'R':
                                action = SvnLogAction.REPLACED;
                                break;
                            case 'M':
                                action = SvnLogAction.MODIFIED;
                                break;
                            default:
                                throw LogUtil.throwing(new SvnClientException(
                                        String.format("Unknown action character '%s'", changePath.getAction())));
                        }
                        entries.add(new SvnLogEntry(action, new URL(url.toString() + changePath.getPath())));
                    }
                    final LocalDateTime dateTime = LocalDateTime
                            .ofInstant(Instant.ofEpochMilli(logMessage.getTimeMillis()), ZoneId.systemDefault());
                    logs.add(new SvnLog(logMessage.getRevision().getNumber(), entries, logMessage.getMessage(),
                            dateTime, logMessage.getAuthor()));
                }
            }
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
        return logs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listDirectories(URL url) throws SvnClientException {
        try {
            final ISVNDirEntry[] list = client.getList(toSVNUrl(url), SVNRevision.HEAD, false);
            return Arrays.stream(list) //
                    .filter(entry -> entry.getNodeKind() == SVNNodeKind.DIR) //only directories 
                    .map(ISVNDirEntry::getPath) //get path of entry
                    .collect(Collectors.toList());
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] updateEmpty(List<Path> paths) throws SvnClientException {
        /*
         * ISVNClientAdapter#update(File[], SVNRevision, int, boolean, boolean, boolean) 
         * does not work as expected
         */
        long[] result = new long[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            try {
                /*
                 * setDepth = false : Otherwise the depth is set to the local checked out repository
                 *                    and files are deleted existing in the directory. We don't want
                 *                    to modify the checked out hierarchy, only update the file.
                 */
                result[i] = client.update(paths.get(i).toFile(), SVNRevision.HEAD, Depth.empty, /*setDepth*/false,
                        false, true);
            } catch (SVNClientException e) {
                LogUtil.getLogger().log(Level.WARNING, String.format("Could not update '%s'.", paths.get(i).toFile()),
                        e);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkoutEmpty(Path path, URL url) throws SvnClientException {
        Objects.requireNonNull(url);
        checkPath(path);
        try {
            client.checkout(toSVNUrl(url), path.toFile(), SVNRevision.HEAD, Depth.empty, false, false);
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * Checks the given path to be valid and throws an {@link Exception} if something is wrong.
     * 
     * @param path the path to check
     * @throws SvnClientException
     */
    private static void checkPath(final Path path) {
        Objects.requireNonNull(path);
        if (path.toFile().exists()) {
            if (!path.toFile().isDirectory()) {
                throw new IllegalArgumentException("The given path already exists and is not a directory");
            }
        } else {
            throw new IllegalArgumentException("The given path does not exist.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void merge(Path path, URL url, long revision, boolean recursivly, boolean recordOnly)
            throws SvnClientException {
        try {
            final SVNRevisionRange[] revisionRange = new SVNRevisionRange[] {
                    new SVNRevisionRange(new Number(revision - 1), new Number(revision)) };
            client.merge(toSVNUrl(url), //SVN URL 
                    SVNRevision.HEAD, //pegRevision
                    revisionRange, //revisions to merge (must be in the form N-1:M)
                    path.toFile(), //target local path
                    false, //force
                    recursivly ? Depth.infinity : Depth.empty, //how deep to traverse into subdirectories
                    false, //ignoreAncestry
                    false, //dryRun
                    recordOnly); //recordOnly
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit(Path path, String message) throws SvnClientException {
        try {
            client.commit(new File[] { path.toFile() }, message, true);
        } catch (SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getConflicts(Path path) throws SvnClientException {
        try {
            final ISVNStatus[] statusArray = client.getStatus(path.toFile(), true, true);
            return Arrays.stream(statusArray) //
                    .filter(status -> status.hasTreeConflict() || status.getConflictWorking() != null || status.getTextStatus() == SVNStatusKind.CONFLICTED) //only conflicts of interest
                    .map(SvnClientJavaHl::getConflictPath) //get path of conflicted file
                    .collect(Collectors.toList());
        } catch (SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    private static String getConflictPath(ISVNStatus status) {
        if (status.hasTreeConflict()) {
            return status.getConflictDescriptor().getPath();
        } else if (status.getConflictWorking() != null) {
            return status.getConflictWorking().toString();
        } else if (status.getTextStatus() == SVNStatusKind.CONFLICTED) {
            return status.getFile().toString();
        } else  {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Path path) throws SvnClientException {
        try {
            client.update(path.toFile(), SVNRevision.HEAD, true);
        } catch (SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getSvnUrl(Path path) throws SvnClientException {
        try {
            final ISVNInfo info = client.getInfoFromWorkingCopy(path.toFile());
            return new URL(info.getUrlString());
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getRepositoryUrl(Path path) throws SvnClientException {
        try {
            final ISVNInfo info = client.getInfoFromWorkingCopy(path.toFile());
            return new URL(info.getRepository().toString());
        } catch (MalformedURLException | SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasModifications(Path path) throws SvnClientException {
        try {
            final ISVNStatus[] status = client.getStatus(path.toFile(), true, false, false);
            return Arrays.stream(status).parallel().filter(SvnClientJavaHl::isModified).findAny().isPresent();
        } catch (SVNClientException e) {
            throw new SvnClientException(e);
        }
    }

    /**
     * Checks the given {@link ISVNStatus} if modifications exist.
     * 
     * @param status the {@link ISVNStatus}
     * @return {@code true} if modifications exist
     */
    private static boolean isModified(ISVNStatus status) {
        final SVNStatusKind textStatus = status.getTextStatus();
        if (textStatus == SVNStatusKind.ADDED || textStatus == SVNStatusKind.CONFLICTED
                || textStatus == SVNStatusKind.DELETED || textStatus == SVNStatusKind.MERGED
                || textStatus == SVNStatusKind.MODIFIED || textStatus == SVNStatusKind.REPLACED) {
            return true;
        } else {
            final SVNStatusKind propStatus = status.getPropStatus();
            return propStatus == SVNStatusKind.ADDED || propStatus == SVNStatusKind.CONFLICTED
                    || propStatus == SVNStatusKind.DELETED || propStatus == SVNStatusKind.MERGED
                    || propStatus == SVNStatusKind.MODIFIED || propStatus == SVNStatusKind.REPLACED;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCommandLineListener(Consumer<String> consumer) {
        final CommandLineListener listener = new CommandLineListener(consumer);
        listeners.add(listener);
        client.addNotifyListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCommandLineListener(Consumer<String> consumer) {
        listeners.stream() //
                .filter(p -> p.consumer == consumer) // Find listener with the consumer instance
                .findAny() // Any match is OK, even if more are existing
                .ifPresent(listener -> { //If existing remove it
                    listeners.remove(listener);
                    client.removeNotifyListener(listener);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (!isClosed) {
            client.dispose();
            isClosed = true;
        }
    }

    /**
     * When username or password are required, the given {@link ICredentialProvider} is asked the
     * input. After calling {@link ICredentialProvider#authenticate()} the result is set to the
     * {@link IConfiguration}.
     * 
     * @author Stefan Weiser
     *
     */
    private static class SVNPromptUserPassword extends SVNPromptUserPasswordAdapter {

        private final ICredentialProvider credentialProvider;
        private final IConfiguration configuration;
        private final ISVNClientAdapter client;

        private String username;
        private String password;

        /**
         * @param credentialProvider the {@link ICredentialProvider}
         * @param configuration the {@link IConfiguration}
         * @param client the {@link ISVNClientAdapter} working on
         */
        private SVNPromptUserPassword(final ICredentialProvider credentialProvider, final IConfiguration configuration,
                final ISVNClientAdapter client) {
            this.credentialProvider = credentialProvider;
            this.configuration = configuration;
            this.client = client;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean userAllowedSave() {
            //No SVN cache
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean promptUser(String arg0, String arg1, boolean arg2) {
            return authenticate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean prompt(String arg0, String arg1, boolean arg2) {
            return authenticate();
        }

        /**
         * Authenticate with the {@link ICredentialProvider} and set the result to the
         * {@link IConfiguration}.
         * 
         * @return {@code true} if user authenticated, {@code false} on {@link Exception}
         */
        private boolean authenticate() {
            try {
                final String[] authenticate = credentialProvider.authenticate();
                username = authenticate[0];
                password = authenticate[1];
                configuration.setSvnUsername(username);
                configuration.setSvnPassword(password);
                client.setUsername(username);
                client.setPassword(password);
                return true;
            } catch (AuthenticationException | ConfigurationException e) {
                LogUtil.throwing(e);
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUsername() {
            return username;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPassword() {
            return password;
        }

    }

    /**
     * This implementation of {@link ISVNNotifyListener} delegates all command line logs, 
     * called in {@link ISVNNotifyListener#logCommandLine(String)}, to a given {@link Consumer}.
     * 
     * @author Stefan Weiser
     *
     */
    private static class CommandLineListener extends SVNNotifyListener {

        private final Consumer<String> consumer;

        /**
         * @param consumer the consumer to delegate to
         */
        private CommandLineListener(final Consumer<String> consumer) {
            this.consumer = consumer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void logCommandLine(String arg0) {
            consumer.accept(arg0);
        }

    }

}
