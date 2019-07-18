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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.configuration.svn.TempSvnRepositoryFactory;
import org.aposin.mergeprocessor.configuration.svn.TempSvnRepositoryFactory.TempSvnRepository;
import org.aposin.mergeprocessor.model.IVersionProvider;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.ISvnClient.SvnClientException;
import org.aposin.mergeprocessor.model.svn.SvnClientJavaHl;
import org.aposin.mergeprocessor.renaming.TempH2DatabaseFactory.DBContainerObject;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.RuntimeUtil.CmdUtilException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SvnPackageMergeUnitFactory}.
 * 
 * @author Stefan Weiser
 *
 */
@Disabled
public class SvnPackageMergeUnitFactoryTest {

    private static final IVersionProvider TEST_VERSION_PROVIDER = svnUrl -> new Version("18.5");

    private static ISvnClient client;

    private IConfiguration configuration;
    private TempSvnRepository svnRepositoryInfo;
    private Connection connection;

    /**
     * Setup the {@link ISvnClient} for all tests.
     * 
     * @throws SvnClientException
     */
    @BeforeAll
    public static void setupSvnClient() throws SvnClientException {
        client = new SvnClientJavaHl(() -> new String[0], new JUnitConfiguration());
    }

    /**
     * Close the {@link ISvnClient} after all tests run.
     * 
     * @throws SvnClientException
     */
    @AfterAll
    public static void closeSvnClient() {
        client.close();
    }

    /**
     * Setup method to be called by the unit tests for settings up temporary SVN repository, 
     * h2 database and {@link IConfiguration}. 
     * 
     * @throws IOException
     * @throws SQLException
     * @throws CmdUtilException
     */
    private void setup(final String dbName) throws IOException, SQLException, CmdUtilException {
        svnRepositoryInfo = TempSvnRepositoryFactory.createAndFillTempSvnRepository();
        final DBContainerObject containerObject = TempH2DatabaseFactory.createAndFillInMemoryH2Instance(dbName);
        connection = containerObject.connection;
        attachEntriesToDb(containerObject.jdbc, svnRepositoryInfo);

        final JUnitConfiguration configuration = new JUnitConfiguration();
        configuration.setRenameDatabaseUrl(containerObject.jdbc);
        this.configuration = configuration;
    }

    /**
     * Clean up  configuration and svn repository after each test case.
     */
    @AfterEach
    public void cleanUpTestCase() {
        configuration = null;
        if (svnRepositoryInfo != null) {
            try {
                Files.delete(svnRepositoryInfo.testRepo);
            } catch (IOException e) {
                LogUtil.throwing(e);
            }
            svnRepositoryInfo = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LogUtil.throwing(e);
            }
            connection = null;
        }
    }

    /**
     * Attaches link mapping entries to the database required for tests in this class. 
     * 
     * @param jdbcUrl  the JDBC URL of the database where to add the entries.
     * @throws SQLException
     */
    private void attachEntriesToDb(final String jdbcUrl, final TempSvnRepository info) throws SQLException {
        try (final Connection connection = DriverManager.getConnection(jdbcUrl, "sa", null);
                final Statement statement = connection.createStatement()) {
            statement.executeLargeUpdate(String.format(
                    "INSERT INTO LINK_MAPPING VALUES(default, 'anotherFile3.txt', 'anotherFile4.txt', '18.0.102', '%s');",
                    info.testRepoUrlString));
        }
    }

    @Test
    public void testRunProducing1NewFileCoveredByLinking() throws SvnClientException, URISyntaxException,
            InterruptedException, CmdUtilException, IOException, SQLException {
        setup("testRunProducing1NewFileCoveredByLinking");
        final TestFileSystemProvider fileSystemProvider = new TestFileSystemProvider();
        final SvnPackageMergeUnitFactory factory = new SvnPackageMergeUnitFactory(configuration, fileSystemProvider,
                TEST_VERSION_PROVIDER, client);
        svnRepositoryInfo.createCommit(); //-> produces trunk/anotherFile4.txt
        factory.checkAndCreateNewSvnPackageMergeUnit();
        assertEquals(1, fileSystemProvider.result.size());
        assertTrue(fileSystemProvider.result.get(0)[0].endsWith(".svnmergepackage"));
        assertTrue(fileSystemProvider.result.get(0)[1].contains("trunk/anotherFile4.txt"));
    }

    @Test
    public void testRunProducing1NewFileNotCoveredByLinking()
            throws IOException, SQLException, CmdUtilException, SvnClientException, URISyntaxException {
        setup("testRunProducing1NewFileNotCoveredByLinking");
        final TestFileSystemProvider fileSystemProvider = new TestFileSystemProvider();
        final SvnPackageMergeUnitFactory factory = new SvnPackageMergeUnitFactory(configuration, fileSystemProvider,
                TEST_VERSION_PROVIDER, client);
        svnRepositoryInfo.createCommit(); //-> trunk/anotherFile4.txt
        svnRepositoryInfo.createCommit(); //-> trunk/anotherFile5.txt
        factory.checkAndCreateNewSvnPackageMergeUnit();
        assertEquals(1, fileSystemProvider.result.size());
        assertTrue(fileSystemProvider.result.get(0)[0].endsWith(".svnmergepackage"));
        assertTrue(fileSystemProvider.result.get(0)[1].contains("trunk/anotherFile4.txt"));
    }

    @Test
    public void testRunProducingNothingBecauseNoCommit() throws SvnClientException, URISyntaxException,
            InterruptedException, CmdUtilException, IOException, SQLException {
        setup("testRunProducingNothingBecauseNoCommit");
        final TestFileSystemProvider fileSystemProvider = new TestFileSystemProvider();
        final SvnPackageMergeUnitFactory factory = new SvnPackageMergeUnitFactory(configuration, fileSystemProvider,
                TEST_VERSION_PROVIDER, client);
        factory.checkAndCreateNewSvnPackageMergeUnit();
        assertTrue(fileSystemProvider.result.isEmpty());
    }

    @Test
    public void testLinkedArtifactTimerWithNullConfiguration()
            throws UnknownHostException, MalformedURLException, SvnClientException, URISyntaxException {
        assertThrows(NullPointerException.class, () -> new SvnPackageMergeUnitFactory(null,
                new TestFileSystemProvider(), TEST_VERSION_PROVIDER, client));
    }

    @Test
    public void testLinkedArtifactTimerWithNullFileSystemProvider()
            throws UnknownHostException, MalformedURLException, SvnClientException, URISyntaxException {
        assertThrows(NullPointerException.class,
                () -> new SvnPackageMergeUnitFactory(new JUnitConfiguration(), null, TEST_VERSION_PROVIDER, client));
    }

    @Test
    public void testLinkedArtifactTimerWithNullVersionProvider()
            throws UnknownHostException, MalformedURLException, SvnClientException, URISyntaxException {
        assertThrows(NullPointerException.class, () -> new SvnPackageMergeUnitFactory(new JUnitConfiguration(),
                new TestFileSystemProvider(), null, client));
    }

    /**
     * This provider gets the write operations from the factory. Therefore it is possible to evaluate the 
     * operations done by the factory.
     * 
     * @author Stefan Weiser
     *
     */
    private static class TestFileSystemProvider implements IFileSystemProvider {

        final List<String[]> result = new ArrayList<>();

        @Override
        public void write(String filePath, String content) throws IOException {
            result.add(new String[] { filePath, content });
        }

    }

}
