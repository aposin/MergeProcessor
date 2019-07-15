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
package org.aposin.mergeprocessor.renaming;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.renaming.TempH2DatabaseFactory.DBContainerObject;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RenamingService}.
 * 
 * @author Stefan Weiser
 *
 */
public class RenamingServiceTest {

    private static IConfiguration configuration;
    private static Connection connection;

    private RenamingService service;

    @BeforeAll
    public static void setup() throws IOException, SQLException {
        final DBContainerObject containerObject = TempH2DatabaseFactory
                .createAndFillInMemoryH2Instance("renameServiceTest");
        final JUnitConfiguration configuration = new JUnitConfiguration();
        configuration.setRenameDatabaseUrl(containerObject.jdbc);
        RenamingServiceTest.configuration = configuration;
        connection = containerObject.connection;
    }

    @AfterAll
    public static void closeAfterAll() {
        try {
            connection.close();
        } catch (SQLException e) {
            LogUtil.throwing(e);
        }
    }

    @AfterEach
    public void closeAfterEach() {
        try {
            if (service != null) {
                service.close();
                service = null;
            }
        } catch (IOException e) {
            LogUtil.throwing(e);
        }
    }

    private void setupService(final String source, final String target) {
        service = new RenamingService(configuration, "https://svn-testrepository.at", source, target);
    }

    /* ################################################################################################
     * Tests dealing with rename entries
     * ################################################################################################
     */

    @Test
    public void testIsRenameArtifactWithBlankInPath() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework/file with blanks.png");
        assertTrue(service.isRenamedArtifact(source));
    }

    @Test
    public void testIsRenameArtifactWithMatchingVersion() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework");
        assertTrue(service.isRenamedArtifact(source));
    }

    @Test
    public void testIsRenameArtifactWithWrongRepository() {
        closeAfterEach();
        service = new RenamingService(configuration, "https://bla.bla.bla", "18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework");
        assertFalse(service.isRenamedArtifact(source));
    }

    @Test
    public void testIsRenameArtifactWithNotMatchingVersion() {
        setupService("19.0", "20.0");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework");
        assertFalse(service.isRenamedArtifact(source));
    }

    @Test
    public void testIsRenamedeArtifactWithExistingRenamedArtifactButNotDirectlyDefinedInDb() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework/newFile.txt");
        assertTrue(service.isRenamedArtifact(source));
    }

    @Test
    public void testhasRenameArtifactsWithMatchingVersion() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get("platform/java/plugins/org.aposin.framework");
        final Path source2 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession.java");
        final List<Path> list = new ArrayList<>();
        list.add(source1);
        list.add(source2);
        assertTrue(service.hasRenamedArtifacts(list));
    }

    @Test
    public void testhasRenameArtifactsWithMatchingVersionAndNotDirectlyDefinedInDb() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get("platform/java/plugins/org.aposin.framework");
        final Path source2 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession.java");
        final Path source3 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession2.java");
        final Path source4 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic2/OpinSession.java");
        final List<Path> list = new ArrayList<>();
        list.add(source1);
        list.add(source2);
        list.add(source3);
        list.add(source4);
        assertTrue(service.hasRenamedArtifacts(list));
    }

    @Test
    public void testGetRenamedArtifactWithExistingRenamedArtifact() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework");
        final Path expected = Paths.get("platform/java/plugins/org.opin.framework");
        assertEquals(expected, service.getRenamedArtifact(source));
    }

    @Test
    public void testGetRenamedArtifactWithNotExistingRenamedArtifact() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.eclipse.platform");
        assertEquals(source, service.getRenamedArtifact(source));
    }

    @Test
    public void testGetRenamedeArtifactWithExistingRenamedArtifactButNotDirectlyDefinedInDb() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.aposin.framework/newFile.txt");
        final Path expected = Paths.get("platform/java/plugins/org.opin.framework/newFile.txt");
        assertEquals(expected, service.getRenamedArtifact(source));
    }

    @Test
    public void testGetRenamedArtifactsWithMatchingVersion() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get("platform/java/plugins/org.aposin.framework");
        final Path source2 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession.java");
        final List<Path> list = asList(source1, source2);

        final Path expected1 = Paths.get("platform/java/plugins/org.opin.framework");
        final Path expected2 = Paths
                .get("platform/java/plugins/org.opin.framework/src/org/opin/framework/logic/OpinSession.java");
        final List<Path> expected = asList(expected1, expected2);
        assertTrue(CollectionUtils.isEqualCollection(expected, service.getRenamedArtifacts(list)));
    }

    @Test
    public void testGetRenamedArtifactsWithMatchingVersionAndNotDirectlyDefinedInDb() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get("platform/java/plugins/org.aposin.framework");
        final Path source2 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession.java");
        final Path source3 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession2.java");
        final Path source4 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic2/OpinSession.java");
        final List<Path> list = asList(source1, source2, source3, source4);

        final Path expected1 = Paths.get("platform/java/plugins/org.opin.framework");
        final Path expected2 = Paths
                .get("platform/java/plugins/org.opin.framework/src/org/opin/framework/logic/OpinSession.java");
        final Path expected3 = Paths
                .get("platform/java/plugins/org.opin.framework/src/org/opin/framework/logic/OpinSession2.java");
        final Path expected4 = Paths
                .get("platform/java/plugins/org.opin.framework/src/org/opin/framework/logic2/OpinSession.java");
        final List<Path> expected = asList(expected1, expected2, expected3, expected4);
        assertTrue(CollectionUtils.isEqualCollection(expected, service.getRenamedArtifacts(list)));
    }

    @Test
    public void testGetRenamedArtifactsWith2RenamingsForOneFile() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get(
                "platform/java/plugins/org.aposin.framework/src/org/aposin/framework/messages/messages.properties");
        final Path source2 = Paths.get(
                "platform/java/plugins/org.aposin.framework/src/org/aposin/framework/messages/messages_en.properties");
        final List<Path> source = asList(source1, source2);

        final Path expected1 = Paths
                .get("platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages_de.properties");
        final Path expected2 = Paths
                .get("platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages.properties");
        final List<Path> expected = asList(expected1, expected2);
        assertTrue(CollectionUtils.isEqualCollection(expected, service.getRenamedArtifacts(source)));
    }

    @Test
    public void testGetRenamedArtifactWithRenamingsAndMovements() {
        setupService("17.0", "19.0");
        final Path source = Paths.get(
                "platform/java/plugins/com.aposin.abc.core.logik.journal/src/com/aposin/abc/core/logik/journal/logic/produktionssteuerung/schadenvertrag/altersstruktur/BoClaimContract.java");
        final Path target = Paths.get(
                "platform/java/plugins/org.opin.productioncontrol/src/org/opin/productioncontrol/claimcontract/agestructure/BoClaimContract.java");
        assertEquals(target, service.getRenamedArtifact(source));
    }

    /* ################################################################################################
     * Tests dealing with link entries
     * ################################################################################################
     */

    @Test
    public void testIsLinkedArtifactWithMatchingVersion() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        assertTrue(service.isLinkedArtifact(source));
    }

    @Test
    public void testIsLinkedArtifactWithWrongRepository() {
        closeAfterEach();
        service = new RenamingService(configuration, "https://bla.bla.bla", "18.0.300", "18.5.300");
        final Path source = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        assertFalse(service.isLinkedArtifact(source));
    }

    @Test
    public void testIsLinkedArtifactWithNotMatchingVersion() {
        setupService("16.0", "17.0");
        final Path source = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        assertFalse(service.isLinkedArtifact(source));
    }

    @Test
    public void testGetLinkedArtifactWithExistingRenamedArtifact() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        final Path expected = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2");
        assertEquals(expected, service.getLinkedArtifact(source));
    }

    @Test
    public void testGetLinkedArtifactWithExistingRenamedArtifactReversed() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2");
        final Path expected = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        assertEquals(expected, service.getLinkedArtifact(source));
    }

    @Test
    public void testGetLinkedArtifactWithNotExistingRenamedArtifact() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("platform/java/plugins/org.eclipse.platform");
        assertEquals(source, service.getLinkedArtifact(source));
    }

    @Test
    public void testGetLinkedArtifactWithExistingLinkedArtifactButNotDirectlyDefinedInDb() {
        setupService("18.0.300", "18.5.300");
        final Path source = Paths.get("www/direct/java/plugins/org.opin.framework.direct/newFile.txt");
        final Path expected = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2/newFile.txt");
        assertEquals(expected, service.getLinkedArtifact(source));
    }

    @Test
    public void testGetLinkedArtifactsWithMatchingVersion() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        final Path source2 = Paths.get("www/direct/java/plugins/org.opin.framework.direct/newFile.txt");
        final List<Path> list = asList(source1, source2);

        final Path expected1 = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2");
        final Path expected2 = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2/newFile.txt");
        final List<Path> expected = asList(expected1, expected2);
        assertTrue(CollectionUtils.isEqualCollection(expected, service.getLinkedArtifacts(list)));
    }

    @Test
    public void testGetLinkedArtifactsWithMatchingVersionAndNotDirectlyDefinedInDb() {
        setupService("18.0.300", "18.5.300");
        final Path source1 = Paths.get("www/direct/java/plugins/org.opin.framework.direct");
        final Path source2 = Paths.get("www/direct/java/plugins/org.opin.framework.direct/newFile.txt");
        final Path source3 = Paths
                .get("www/direct/java/plugins/org.opin.framework.direct/subfolder/subsubfolder/anotherFile.txt");
        final Path source4 = Paths
                .get("platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession.java");
        final List<Path> list = asList(source1, source2, source3, source4);

        final Path expected1 = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2");
        final Path expected2 = Paths.get("www/adapter_v2/java/plugins/org.opin.framework.adapter.v2/newFile.txt");
        final Path expected3 = Paths.get(
                "www/adapter_v2/java/plugins/org.opin.framework.adapter.v2/subfolder/subsubfolder/anotherFile.txt");
        final List<Path> expected = asList(expected1, expected2, expected3, source4);
        assertTrue(CollectionUtils.isEqualCollection(expected, service.getLinkedArtifacts(list)));
    }

    @Test
    public void testGetObservableSvnRepositoriesForLinkedArtifacts() throws MalformedURLException {
        final List<URL> result = RenamingService.getObservableSvnRepositoriesForLinkedArtifacts(configuration);
        assertTrue(result.size() == 1);
        assertEquals(new URL("https://svn-testrepository.at"), result.get(0));
    }

}
