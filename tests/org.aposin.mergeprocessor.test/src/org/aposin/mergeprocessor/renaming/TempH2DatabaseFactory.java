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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.IOUtils;

/**
 * Factory creating temporary H2 databases which are deleted on shut down of the application.
 * 
 * @author Stefan Weiser
 *
 */
public final class TempH2DatabaseFactory {

    private TempH2DatabaseFactory() {
        //Factory class
    }

    /**
     * Creates and fills a H2 in-memory database with all required test data. The data is provided by the setup.sql file.
     * The database gets deleted when the last {@link Connection} is closed.
     * 
     * @param dbName
     * @return
     * @throws SQLException
     * @throws MalformedURLException
     * @throws IOException
     */
    public static DBContainerObject createAndFillInMemoryH2Instance(final String dbName)
            throws SQLException, MalformedURLException, IOException {
        final String jdbc = "jdbc:h2:mem:" + dbName;
        final Connection connection = DriverManager.getConnection(jdbc, "sa", null);
        try (final Statement statement = connection.createStatement()) {
            final String setupSql = IOUtils.toString(getSetupFile(), StandardCharsets.UTF_8);
            statement.executeLargeUpdate(setupSql);
        }
        return new DBContainerObject(jdbc, connection);
    }

    /**
     * Create and fill a H2 database with all required test data. The data are provided by the setup.sql file.
     * The database gets deleted when the application is shut down.
     * 
     * @return the JDBC String to the database
     * @throws IOException
     * @throws SQLException
     */
    public static String createAndFillTempH2Database(final Path path) throws IOException, SQLException {
        path.toFile().deleteOnExit();
        return createAndFillTempH2DatabaseInternal(path);
    }

    /**
     * Create and fill a H2 database with all required test data. The data are provided by the setup.sql file.
     * The database gets deleted when the application is shut down.
     * 
     * @return the JDBC String to the database
     * @throws IOException
     * @throws SQLException
     */
    public static String createAndFillTempH2Database() throws IOException, SQLException {
        final Path path = Files.createTempDirectory(null).resolve("testRenamingService");
        path.toFile().deleteOnExit();
        return createAndFillTempH2DatabaseInternal(path);
    }

    private static String createAndFillTempH2DatabaseInternal(final Path path)
            throws SQLException, MalformedURLException, IOException {
        final String jdbcUrl = "jdbc:h2:file:" + path.toString();
        try (final Connection connection = DriverManager.getConnection(jdbcUrl, "sa", null);
                final Statement statement = connection.createStatement()) {
            final String setupSql = IOUtils.toString(getSetupFile(), StandardCharsets.UTF_8);
            statement.executeLargeUpdate(setupSql);
        }
        return jdbcUrl;
    }

    /**
     * Returns the {@link URL} for the setup.sql, which is available in the same directory structure as this class.
     * 
     * @return the {@link URL} for the setup.sql
     * @throws MalformedURLException
     */
    private static URL getSetupFile() throws MalformedURLException {
        return new File("src/org/aposin/mergeprocessor/renaming/setup.sql").toURI().toURL();
    }

    /**
     * Container object providing the JDBC url and an open {@link Connection}. 
     * 
     * @author Stefan Weiser
     *
     */
    public static final class DBContainerObject {

        public final String jdbc;
        public final Connection connection;

        /**
         * @param jdbc the JDBC URL
         * @param connection an open connection
         */
        private DBContainerObject(String jdbc, Connection connection) {
            this.jdbc = jdbc;
            this.connection = connection;
        }

    }

}
