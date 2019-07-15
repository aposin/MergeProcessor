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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog;
import org.aposin.mergeprocessor.utils.LogUtil;

/**
 * This artifact is is able to check for new commits on a given SVN repository URL. The last checked
 * revision number is saved and only commits between the current HEAD and the last checked revision
 * are delivered by {@link #checkForNew(String)}. When calling this method the last checked
 * revision number is set to the current HEAD revision.
 * 
 * @author Stefan Weiser
 *
 */
public class SvnLinkedArtifact {

    private final SvnLinkedArtifactSerializationObject serializationObject;
    private final ISvnClient svnClient;

    /**
     * @param url the SVN repository URL
     * @param svnClient the client to communicate with SVN
     * @throws SvnClientException
     */
    public SvnLinkedArtifact(final URL url, final ISvnClient svnClient) throws SvnClientException {
        this(url, svnClient.showRevision(url), svnClient);
    }

    /**
     * @param url the SVN repository URL
     * @param revision the last checked revision number
     * @param svnClient the client to communicate with SVN
     * @throws SvnClientException
     */
    public SvnLinkedArtifact(final URL url, final long revision, final ISvnClient svnClient) {
        this(new SvnLinkedArtifactSerializationObject(url, revision), svnClient);
    }

    /**
     * @param serializationObject the serialized object
     * @param svnClient the client to communiate with SVN
     */
    private SvnLinkedArtifact(final SvnLinkedArtifactSerializationObject serializationObject,
            final ISvnClient svnClient) {
        this.serializationObject = serializationObject;
        this.svnClient = svnClient;
    }

    /**
     * Returns all commits for a given user which are done between the last checked revision and the current
     * HEAD revision. When the method runs successfully (no exception occurs) the last revision is set
     * to the current HEAD revision. So calling the method twice may deliver different results, because the
     * first call already consumed new commits.
     * 
     * @param user the user who has done the commits.
     * @return a list of new commits done by the given user.
     */
    public List<SvnLog> checkForNew(final String user) {
        LogUtil.entering(user);
        if (StringUtils.isEmpty(user)) {
            throw new IllegalArgumentException("The given user String must not be empty or null.");
        }
        try {
            final long head = svnClient.showRevision(serializationObject.url);
            if (head == serializationObject.revision) {
                Logger.getLogger(getClass().getName())
                        .fine("Revision is HEAD -> not required to search for new LinkedArtifacts.");
                return Collections.emptyList();
            } else if (head < serializationObject.revision) {
                Logger.getLogger(getClass().getName()).warning("Revision does not exist.");
                serializationObject.revision = head;
                return Collections.emptyList();
            } else {
                final List<SvnLog> log = svnClient.log(serializationObject.url, serializationObject.revision + 1, head,
                        user);
                Logger.getLogger(getClass().getName()).fine(log.size() + " new LinkedArtifacts found.");
                serializationObject.revision = head;
                return log;
            }
        } catch (SvnClientException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Persists the instance to the given path.
     * 
     * @param path the path where to persist
     * @throws IOException when the file could not be persisted to the given path
     */
    public void persist(final Path path) throws IOException {
        try (final FileOutputStream file = new FileOutputStream(path.toFile());
                final ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(serializationObject);
        }
    }

    /**
     * @return the SVN repository URL
     */
    public URL getUrl() {
        return serializationObject.url;
    }

    /**
     * Gets or creates a new instance for the given SVN repository URL from the given path. If no existing
     * instance could be loaded from the given path a new instance is created. The new instance starts from
     * the current HEAD revision of the repository, so calling {@link #checkForNew(String)} afterwards 
     * most time does not deliver any results.
     * 
     * @param url the SVN repository URL
     * @param path the path where to load from
     * @param svnClient the client to communicate with SVN
     * @return an deserialized or new instance
     * @throws SvnClientException
     */
    public static SvnLinkedArtifact getOrCreate(final URL url, final Path path, final ISvnClient svnClient)
            throws SvnClientException {
        if (path.toFile().exists()) {
            try (final FileInputStream file = new FileInputStream(path.toFile());
                    final ObjectInputStream in = new ObjectInputStream(file)) {
                final SvnLinkedArtifactSerializationObject serializationObject = (SvnLinkedArtifactSerializationObject) in
                        .readObject();
                return new SvnLinkedArtifact(serializationObject, svnClient);
            } catch (IOException | ClassNotFoundException e) {
                Logger.getLogger(SvnLinkedArtifact.class.getName()).log(Level.WARNING,
                        String.format("Could not load SvnLinkedArtifact. New instance for '%s' was created.", url), e);
                return new SvnLinkedArtifact(url, svnClient);
            }
        } else {
            return new SvnLinkedArtifact(url, svnClient);
        }
    }

    /**
     * Object to serialize the last checked revision number for a given SVN URL.
     * 
     * @author Stefan Weiser
     *
     */
    private static class SvnLinkedArtifactSerializationObject implements Serializable {

        private static final long serialVersionUID = -2303950325189293590L;

        private final URL url;
        private long revision;

        /**
         * @param url the SVN repository URL
         * @param revision the last checked revision number
         */
        private SvnLinkedArtifactSerializationObject(URL url, long revision) {
            this.url = url;
            this.revision = revision;
        }

    }

}
