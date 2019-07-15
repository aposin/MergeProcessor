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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.utils.MergeProcessorUtilException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link H2DBCopyTask}.
 * 
 * @author Stefan Weiser
 *
 */
public class H2DatabaseSetupTest {

    private H2DatabaseSetup task;

    @Test
    public void testIsDownloadRequiredWithNullConfiguration() {
        assertThrows(NullPointerException.class, () -> new H2DatabaseSetup(null));
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with {@code null}.
     */
    @Test
    public void testIsDownloadRequiredWhenDbUrlIsNull() {
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(null);
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with an empty {@link String}
     */
    @Test
    public void testIsDownloadRequiredWhenDbUrlIsEmpty() {
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("");
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a MySql database.
     */
    @Test
    public void testIsDownloadRequiredWhenDbIsMySQL() {
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("jdbc:mysql://host1:33060/mergeprocessor");
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a H2 tcp url.
     */
    @Test
    public void testIsDownloadRequiredWhenDbIsH2Tcp() {
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("dbc:h2:tcp://host1:33060/mergeprocessor");
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a H2 in memory db.
     */
    @Test
    public void testIsDownloadRequiredWhenDbIsH2InMemoryDb() {
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("jdbc:h2:mem:someName");
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a H2 database file not existing.
     * @throws IOException 
     */
    @Test
    public void testIsDownloadRequiredWhenExactDbPathDoesNotExist() throws IOException {
        final Path dir = Files.createTempDirectory("renameFolder");
        final Path file = Paths.get(dir.toString(), "rename.mv.db");
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(
                "jdbc:h2:file:" + file.toString().substring(0, file.toString().length() - ".mv.db".length()));
        task = new H2DatabaseSetup(config);
        config.getLocalH2RenameDatabase();
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a H2 database file older than the existing one.
     */
    @Test
    public void testIsDownloadRequiredWhenExactDbPathIsSpecifiedAndNewDbIsOlder()
            throws IOException, InterruptedException {
        final Path file = Files.createTempFile("rename", ".mv.db");
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(
                "jdbc:h2:file:" + file.toString().substring(0, file.toString().length() - ".mv.db".length()));
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a directory which should own a H2 database but doesn't.
     */
    @Test
    public void testIsDownloadRequiredWhenDbPathIsDirectoryAndNoDatabaseFileExists() throws IOException {
        final Path dir = Files.createTempDirectory("renameFolder");
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("jdbc:h2:file:" + dir);
        task = new H2DatabaseSetup(config);
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#isDownloadRequired()} with a directory which should own a H2 database but doesn't.
     * @throws InterruptedException 
     */
    @Test
    public void testIsDownloadRequiredWhenDbPathIsDirectoryAnd3OlderDatabaseFilesExist()
            throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory("renameFolder");
        final Path file1 = Paths.get(dir.toString(), "rename1.mv.db");
        final Path file2 = Paths.get(dir.toString(), "rename2.mv.db");
        final Path file3 = Paths.get(dir.toString(), "rename3.mv.db");
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("jdbc:h2:file:" + dir);
        task = new H2DatabaseSetup(config);
        Files.createFile(file1); //Must be created first
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        Files.createFile(file2); //Must be created second
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        Files.createFile(file3); //Must be created third
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        config.getLocalH2RenameDatabase(); //Must be created last
        assertFalse(isDownloadRequired());
    }

    /**
     * {@link H2DBCopyTask#getListOfDownloadableDBs()} with a directory which should own a H2 database but doesn't.
     */
    @Test
    public void testGetListOfRenameDatabasesWhenDbPathIsDirectoryAnd3YoungerDatabaseFilesExist()
            throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory("renameFolder");
        final Path file1 = Paths.get(dir.toString(), "rename1.mv.db");
        final Path file2 = Paths.get(dir.toString(), "rename2.mv.db");
        final Path file3 = Paths.get(dir.toString(), "rename3.mv.db");
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("jdbc:h2:file:" + dir);
        config.getLocalH2RenameDatabase(); //Must be created first
        task = new H2DatabaseSetup(config);
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        Files.createFile(file1); //Must be created second
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        Files.createFile(file2); //Must be created third
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        Files.createFile(file3); //Must be created fourth
        assertArrayEquals(new Path[] { file3, file2, file1 }, getListOfDownloadableDBs().toArray());
    }

    /**
     * {@link H2DBCopyTask#getListOfDownloadableDBs()} with a H2 database file younger than the existing one.
     */
    @Test
    public void testGetListOfRenameDatabasesWhenExactDbPathIsSpecifiedAndNewDbIsYounger()
            throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory("renameFolder");
        final Path file = Paths.get(dir.toString(), "rename.mv.db");
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(
                "jdbc:h2:file:" + file.toString().substring(0, file.toString().length() - ".mv.db".length()));
        config.getLocalH2RenameDatabase(); //Must be created first
        task = new H2DatabaseSetup(config);
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        Files.createFile(file); //Must be created second
        assertArrayEquals(new Path[] { file }, getListOfDownloadableDBs().toArray());
    }

    @Test
    public void testDownloadWhen1ValidDatabaseIsFound() throws IOException, SQLException, MergeProcessorUtilException {
        final String jdbc = TempH2DatabaseFactory.createAndFillTempH2Database();
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(jdbc);
        final Path target = Paths.get(Files.createTempDirectory("renameFolder").toString(), "rename.mv.db");
        config.setLocalH2RenameDatabase(target);
        task = new H2DatabaseSetup(config);
        download();
        final byte[] expected = Files.readAllBytes(Paths.get(jdbc.replace("jdbc:h2:file:", "") + ".mv.db"));
        final byte[] actual = Files.readAllBytes(target);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testDownloadWhen2ValidDatabasesAreFound() throws IOException, SQLException, InterruptedException {
        final Path tempDir = Files.createTempDirectory(null);
        TempH2DatabaseFactory.createAndFillTempH2Database(tempDir.resolve("db1"));
        Thread.sleep(10); //Wait some time, so the time stamps are not equal, as machine may be too fast :-)
        final String jdbc2 = TempH2DatabaseFactory.createAndFillTempH2Database(tempDir.resolve("db2"));

        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl("jdbc:h2:file:" + tempDir);

        final Path target = Paths.get(Files.createTempDirectory("renameFolder").toString(), "rename.mv.db");
        config.setLocalH2RenameDatabase(target);
        task = new H2DatabaseSetup(config);
        download();
        final byte[] expected = Files.readAllBytes(Paths.get(jdbc2.replace("jdbc:h2:file:", "") + ".mv.db"));
        final byte[] actual = Files.readAllBytes(target);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testDownloadWhenLinkTableIsMissing() throws IOException, SQLException, MergeProcessorUtilException {
        final String jdbc = TempH2DatabaseFactory.createAndFillTempH2Database();
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(jdbc);
        final Path target = Paths.get(Files.createTempDirectory("renameFolder").toString(), "rename.mv.db");
        config.setLocalH2RenameDatabase(target);
        task = new H2DatabaseSetup(config);

        try (final Connection connection = DriverManager.getConnection(jdbc, config.getRenameDatabaseUser(),
                config.getRenameDatabasePassword()); final Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE LINK_MAPPING");
        }
        download();
        assertFalse(Files.exists(target));
    }

    @Test
    public void testDownloadWhenRenameTableIsMissing() throws IOException, SQLException, MergeProcessorUtilException {
        final String jdbc = TempH2DatabaseFactory.createAndFillTempH2Database();
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(jdbc);
        final Path target = Paths.get(Files.createTempDirectory("renameFolder").toString(), "rename.mv.db");
        config.setLocalH2RenameDatabase(target);
        task = new H2DatabaseSetup(config);

        try (final Connection connection = DriverManager.getConnection(jdbc, config.getRenameDatabaseUser(),
                config.getRenameDatabasePassword()); final Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE RENAME_MAPPING");
        }
        download();
        assertFalse(Files.exists(target));
    }

    @Test
    public void testDownloadWhen1LinkTableColumnIsMissing()
            throws IOException, SQLException, MergeProcessorUtilException {
        final String jdbc = TempH2DatabaseFactory.createAndFillTempH2Database();
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(jdbc);
        final Path target = Paths.get(Files.createTempDirectory("renameFolder").toString(), "rename.mv.db");
        config.setLocalH2RenameDatabase(target);
        task = new H2DatabaseSetup(config);

        try (final Connection connection = DriverManager.getConnection(jdbc, config.getRenameDatabaseUser(),
                config.getRenameDatabasePassword()); final Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE LINK_MAPPING DROP COLUMN VERSION");
        }
        download();
        assertFalse(Files.exists(target));
    }

    @Test
    public void testDownloadWhen1RenameTableColumnIsMissing()
            throws IOException, SQLException, MergeProcessorUtilException {
        final String jdbc = TempH2DatabaseFactory.createAndFillTempH2Database();
        final JUnitConfiguration config = new JUnitConfiguration();
        config.setRenameDatabaseUrl(jdbc);
        final Path target = Paths.get(Files.createTempDirectory("renameFolder").toString(), "rename.mv.db");
        config.setLocalH2RenameDatabase(target);
        task = new H2DatabaseSetup(config);

        try (final Connection connection = DriverManager.getConnection(jdbc, config.getRenameDatabaseUser(),
                config.getRenameDatabasePassword()); final Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE RENAME_MAPPING DROP COLUMN VERSION");
        }
        download();
        assertFalse(Files.exists(target));
    }

    /**
     * Executes {@link H2DatabaseSetup#download()}.
     */
    private void download() {
        try {
            final Method method = H2DatabaseSetup.class.getDeclaredMethod("download");
            method.setAccessible(true);
            method.invoke(task);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                fail(e);
            }
        }
    }

    /**
     * Executes {@link H2DatabaseSetup#getListOfDownloadableDBs()}.
     * 
     * @return the result of {@link H2DatabaseSetup#getListOfDownloadableDBs()}
     */
    private List<?> getListOfDownloadableDBs() {
        try {
            final Method method = H2DatabaseSetup.class.getDeclaredMethod("getListOfDownloadableDBs");
            method.setAccessible(true);
            return (List<?>) method.invoke(task);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                fail(e);
                return List.of();
            }
        }
    }

    /**
     * Executes {@link H2DatabaseSetup#isDownloadRequired()}.
     * 
     * @return the result of {@link H2DatabaseSetup#isDownloadRequired()}
     */
    private boolean isDownloadRequired() {
        try {
            final Method method = H2DatabaseSetup.class.getDeclaredMethod("isDownloadRequired");
            method.setAccessible(true);
            return (boolean) method.invoke(task);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                fail(e);
                return false;
            }
        }
    }

}
