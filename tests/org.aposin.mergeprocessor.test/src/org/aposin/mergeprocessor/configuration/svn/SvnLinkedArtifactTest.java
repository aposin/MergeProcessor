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
package org.aposin.mergeprocessor.configuration.svn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.configuration.svn.TempSvnRepositoryFactory.TempSvnRepository;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog.SvnLogAction;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnLog.SvnLogEntry;
import org.aposin.mergeprocessor.model.svn.SvnClientJavaHl;
import org.aposin.mergeprocessor.model.svn.SvnLinkedArtifact;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.RuntimeUtil.CmdUtilException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

/**
 * Tests for {@link SvnLinkedArtifact}.
 * 
 * @author Stefan Weiser
 *
 */
@Disabled
public class SvnLinkedArtifactTest {

    private static String testRepoUrlString;
    private static ISvnClient client;

    @BeforeAll
    public static void setup() throws CmdUtilException, IOException, SvnClientException {
        LogUtil.entering();
        final TempSvnRepository createAndFillTempSvnRepository = TempSvnRepositoryFactory
                .createAndFillTempSvnRepository();
        testRepoUrlString = createAndFillTempSvnRepository.testRepoUrlString;
        client = new SvnClientJavaHl(() -> new String[0], new JUnitConfiguration());
    }

    @AfterAll
    public static void closeSvnClient() {
        client.close();
    }

    @Test
    public void testCheckForNewReturning2Entry() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 1, client);
        final List<SvnLog> result = svnLinkedArtifact.checkForNew("testuser");
        assertTrue(result.size() == 2);
        final SvnLogEntry file2Entry = result.get(0).getEntries().get(0);
        assertTrue(
                file2Entry.getAction() == SvnLogAction.ADDED && file2Entry.getUrl().getFile().endsWith("/file2.txt"));
        final SvnLogEntry file3Entry = result.get(1).getEntries().get(0);
        assertTrue(
                file3Entry.getAction() == SvnLogAction.ADDED && file3Entry.getUrl().getFile().endsWith("/file3.txt"));
    }

    @Test
    public void testCheckForNewReturning1Entry() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 2, client);
        final List<SvnLog> result = svnLinkedArtifact.checkForNew("testuser");
        assertTrue(result.size() == 1);
        final SvnLogEntry file3Entry = result.get(0).getEntries().get(0);
        assertTrue(
                file3Entry.getAction() == SvnLogAction.ADDED && file3Entry.getUrl().getFile().endsWith("/file3.txt"));
    }

    @Test
    public void testCheckForNewReturningNothingBecauseNoNewRevisionAvailable() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 3, client);
        final List<SvnLog> result = svnLinkedArtifact.checkForNew("testuser");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckForNewReturningNothingBecauseOfWrongUser() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 2, client);
        final List<SvnLog> result = svnLinkedArtifact.checkForNew("abc");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckForNewWhenNotExistingRevisionIsSet() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 100, client);
        final List<SvnLog> result = svnLinkedArtifact.checkForNew("testuser");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckForNewWithEmptyUser() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 2, client);
        assertThrows(IllegalArgumentException.class, () -> svnLinkedArtifact.checkForNew(""));
    }

    @Test
    public void testCheckForNewWithNullUser() throws MalformedURLException {
        final SvnLinkedArtifact svnLinkedArtifact = new SvnLinkedArtifact(new URL(testRepoUrlString), 2, client);
        assertThrows(IllegalArgumentException.class, () -> svnLinkedArtifact.checkForNew(null));
    }

    @Test
    public void testGetOrCreateWhenCorruptFileIsAvailable() throws IOException, SvnClientException {
        final Path tempFile = Files.createTempFile("svnLinkedArtifact", null);
        tempFile.toFile().deleteOnExit();
        final SvnLinkedArtifact artifact = SvnLinkedArtifact.getOrCreate(new URL(testRepoUrlString), tempFile, client);
        assertEquals(3l, getRevision(artifact));
    }

    @Test
    public void testGetOrCreateWhenNoFileIsAvailable() throws IOException, SvnClientException {
        final SvnLinkedArtifact artifact = SvnLinkedArtifact.getOrCreate(new URL(testRepoUrlString),
                Paths.get("a", "b", "c"), client);
        assertEquals(3l, getRevision(artifact));
    }

    @Test
    public void testGetOrCreateWhenFileIsAvailable() throws IOException, SvnClientException {
        final Path tempFile = Files.createTempFile("svnLinkedArtifact", null);
        tempFile.toFile().deleteOnExit();
        final URL url = new URL(testRepoUrlString);
        final SvnLinkedArtifact artifact1 = new SvnLinkedArtifact(url, 2, client);
        artifact1.persist(tempFile);
        assertEquals(2l, getRevision(artifact1));

        final SvnLinkedArtifact artifact2 = SvnLinkedArtifact.getOrCreate(url, tempFile, client);
        assertEquals(2l, getRevision(artifact2));
    }

    /**
     * Returns the internally saved revision number from the given {@link SvnLinkedArtifact}.
     * 
     * @param artifact the artifact
     * @return the internally saved revision number
     */
    private static long getRevision(final SvnLinkedArtifact artifact) {
        try {
            final Field serializationObjectField = SvnLinkedArtifact.class.getDeclaredField("serializationObject");
            serializationObjectField.setAccessible(true);
            final Object serializationObject = serializationObjectField.get(artifact);

            final Class<?>[] classes = SvnLinkedArtifact.class.getDeclaredClasses();
            final Optional<Class<?>> internalClass = Arrays.stream(classes)
                    .filter(clazz -> clazz.getSimpleName().equals("SvnLinkedArtifactSerializationObject")).findAny();
            if (internalClass.isPresent()) {
                final Field revisionField = internalClass.get().getDeclaredField("revision");
                revisionField.setAccessible(true);
                return (long) revisionField.get(serializationObject);
            } else {
                throw new JUnitException("Internal class 'SvnLinkedArtifactSerializationObject' not found.");
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new JUnitException("Problem getting revision from SvnLinkedArtifact.", e);
        }
    }

}
