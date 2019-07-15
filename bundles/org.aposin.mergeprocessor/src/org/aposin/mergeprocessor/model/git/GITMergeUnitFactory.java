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
package org.aposin.mergeprocessor.model.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.model.MergeUnitException;
import org.aposin.mergeprocessor.model.MergeUnitStatus;

/**
 * Factory for creating a {@link GITMergeUnit}.
 * 
 * @author Stefan Weiser
 *
 */
public class GITMergeUnitFactory {

    private static final String REPOSITORY = "URL";
    private static final String DATE = "DATE";
    private static final String COMMID_ID = "COMMID_ID";
    private static final String SOURCE_BRANCH = "SOURCE_BRANCH";
    private static final String TARGET_BRANCH = "TARGET_BRANCH";
    private static final String WORKING_COPY_FILE = "WORKING_COPY_FILE";

    private static final Collector<String, List<String>, List<String>> AFFECTED_FILES_COLLECTOR = createAffectedFilesCollector();

    private GITMergeUnitFactory() {
        //Factory has only static methods
    }

    /**
     * @return a new instance of {@link GITMergeUnit}.
     * @throws MergeUnitException 
     */
    public static GITMergeUnit create(final IConfiguration configuration, final Path path, final InputStream is)
            throws MergeUnitException {

        final String host = configuration.getSftpConfiguration().getHost();
        final MergeUnitStatus status = getStatus(configuration, path);
        final List<String> inputLines = getLinesOfInputStream(is);
        final String repository = getProperty(REPOSITORY, inputLines);
        final LocalDateTime date = LocalDateTime.parse(getProperty(DATE, inputLines),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        final String commitId = getProperty(COMMID_ID, inputLines);
        final String sourceBranch = getProperty(SOURCE_BRANCH, inputLines);
        final String targetBranch = getProperty(TARGET_BRANCH, inputLines);
        final String fileName = path.getFileName().toString();
        final List<String> affectedFiles = inputLines.stream().filter(line -> line.startsWith(WORKING_COPY_FILE))
                .collect(AFFECTED_FILES_COLLECTOR);

        final GITMergeUnit mergeUnit = new GITMergeUnit(host, repository, date, commitId, sourceBranch, targetBranch,
                fileName, affectedFiles, configuration);
        mergeUnit.setStatus(status);
        mergeUnit.setRemotePath(path.toString().replace('\\', '/'));
        return mergeUnit;
    }

    private static String getProperty(final String name, List<String> properties) {
        final Optional<String> result = properties.stream().filter(line -> line.startsWith(name)).findFirst();
        if (result.isPresent()) {
            return result.get().split("=")[1].trim();
        } else {
            //Should normally not happen
            return "";
        }
    }

    /**
     * Returns all lines of the given {@link InputStream} in form of a list.
     * 
     * @param is the {@link InputStream}
     * @return the list of lines
     * @throws MergeUnitException when the {@link InputStream} could not be read
     */
    private static List<String> getLinesOfInputStream(final InputStream is) throws MergeUnitException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new MergeUnitException("The given InputStream could not be read.", e);
        }
    }

    /**
     * Get the {@link MergeUnitStatus} for the given file path.
     * 
     * @param configuration the configuration
     * @param path the file path
     * @return the status
     * @throws MergeUnitException if the status could not be identified
     */
    private static MergeUnitStatus getStatus(final IConfiguration configuration, final Path path)
            throws MergeUnitException {
        if (path.startsWith(configuration.getSftpConfiguration().getTodoFolder())) {
            return MergeUnitStatus.TODO;
        } else if (path.startsWith(configuration.getSftpConfiguration().getIgnoredFolder())) {
            return MergeUnitStatus.IGNORED;
        } else if (path.startsWith(configuration.getSftpConfiguration().getDoneFolder())) {
            return MergeUnitStatus.DONE;
        } else if (path.startsWith(configuration.getSftpConfiguration().getCanceledFolder())) {
            return MergeUnitStatus.CANCELLED;
        } else {
            throw new MergeUnitException(String.format("Unknown MergeUnitStatus for path '%s'", path));
        }
    }

    /**
     * @return the collector for collecting the affected files from lines
     */
    private static Collector<String, List<String>, List<String>> createAffectedFilesCollector() {
        final BiConsumer<List<String>, String> accumulator = (collection, line) -> collection
                .add(line.replaceAll("\\s{2,}", " ").trim().split(" ")[1]);
        final BinaryOperator<List<String>> combiner = (collection1, collection2) -> Stream
                .concat(collection1.stream(), collection2.stream()).collect(Collectors.toCollection(ArrayList::new));
        return Collector.of(ArrayList::new, accumulator, combiner);
    }

}
