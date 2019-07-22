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
package org.aposin.mergeprocessor.configuration.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.aposin.mergeprocessor.configuration.JUnitConfiguration;
import org.aposin.mergeprocessor.model.MergeUnitStatus;
import org.aposin.mergeprocessor.model.git.GITMergeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GITMergeUnit}.
 * 
 * @author Stefan Weiser
 *
 */
@DisplayName("Tests for GITMergeUnit")
public class GITMergeUnitTest {

    private static final String DEFAULT_TEST_HOST = "localhost";
    private static final String DEFAULT_REPOSITORY = "git@my.repo.at";
    private static final String DEFAULT_COMMIT_ID = "12345";
    private static final String DEFAULT_BRANCH_SOURCE = "remotes/origin/V1.0";
    private static final String DEFAULT_BRANCH_TARGET = "remotes/origin/master";
    private static final String DEFAULT_FILE_NAME = "mergeprocessor-test_12345.gitmerge";

    @DisplayName("Tests compareTo() using the same GITMergeUnit instance.")
    @Test
    public void testCompareToWithSameMergeUnit() {
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, LocalDateTime.now(),
                DEFAULT_COMMIT_ID, DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        assertEquals(0, unit.compareTo(unit));
    }

    @DisplayName("Tests compareTo() using 2 equal GITMergeUnits")
    @Test
    public void testCompareToWithEqualMergeUnit() {
        final LocalDateTime date = LocalDateTime.now();
        final GITMergeUnit unit1 = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        assertEquals(0, unit.compareTo(unit1));
    }

    @DisplayName("Tests compareTo() using 2 GITMergeUnits with different dates")
    @Test
    public void testCompareToWithEqualMergeUnitButDifferentDates() {
        final LocalDateTime date1 = LocalDateTime.now();
        final LocalDateTime date2 = LocalDateTime.now().plusDays(2);
        final GITMergeUnit unit1 = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date1, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date2, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, "theFile1", new ArrayList<>(0), new JUnitConfiguration());
        assertNotEquals(0, unit.compareTo(unit1));
        assertNotEquals(unit.getDate(), unit1.getDate());
    }

    @DisplayName("Tests compareTo() using 2 equal GITMergeUnits with different states")
    @Test
    public void testCompareToWithEqualMergeUnitButDifferentState() {
        final LocalDateTime date = LocalDateTime.now();
        final GITMergeUnit unit1 = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, "theFile1", new ArrayList<>(0), new JUnitConfiguration());
        unit1.setStatus(MergeUnitStatus.TODO);
        unit.setStatus(MergeUnitStatus.DONE);
        assertEquals(0, unit.compareTo(unit1));
        assertNotEquals(unit.getStatus(), unit1.getStatus());
    }

    @DisplayName("Tests compareTo() using 2 equal GITMergeUnits with different file names")
    @Test
    public void testCompareToWithEqualMergeUnitButDifferentFileNames() {
        final LocalDateTime date = LocalDateTime.now();
        final GITMergeUnit unit1 = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, "theFile1", new ArrayList<>(0), new JUnitConfiguration());
        assertEquals(0, unit.compareTo(unit1));
        assertNotEquals(unit.getFileName(), unit1.getFileName());
    }

    @DisplayName("Tests compareTo() using a different type of IMergeUnit")
    @Test
    public void testCompareToWithAnObject() {
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, LocalDateTime.now(),
                DEFAULT_COMMIT_ID, DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        assertNotEquals(0, unit.compareTo(new MockMergeUnit()));
    }

    @DisplayName("Tests compareTo() using 2 GITMergeUnits with different host")
    @Test
    public void testCompareToWithDifferentHost() {
        final LocalDateTime date = LocalDateTime.now();
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit1 = new GITMergeUnit("localhost1", DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, "theFile1", new ArrayList<>(0), new JUnitConfiguration());
        assertNotEquals(0, unit.equals(unit1));
    }

    @DisplayName("Tests compareTo() using 2 GITMergeUnits with different commitId")
    @Test
    public void testCompareToWithDifferentCommitId() {
        final LocalDateTime date = LocalDateTime.now();
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit1 = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, "67890",
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, "theFile1", new ArrayList<>(0), new JUnitConfiguration());
        assertNotEquals(0, unit.equals(unit1));
    }

    @DisplayName("Tests compareTo() using 2 GITMergeUnits with different target branch")
    @Test
    public void testCompareToWithDifferentTargetBranch() {
        final LocalDateTime date = LocalDateTime.now();
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, DEFAULT_COMMIT_ID,
                DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        final GITMergeUnit unit1 = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, date, "67890",
                DEFAULT_BRANCH_SOURCE, "remotes/origin/V1.1", "theFile1", new ArrayList<>(0), new JUnitConfiguration());
        assertNotEquals(0, unit.equals(unit1));
    }

    @DisplayName("Tests compareTo() using null as parameter")
    @Test
    public void testCompareToWithNull() {
        final GITMergeUnit unit = new GITMergeUnit(DEFAULT_TEST_HOST, DEFAULT_REPOSITORY, LocalDateTime.now(),
                DEFAULT_COMMIT_ID, DEFAULT_BRANCH_SOURCE, DEFAULT_BRANCH_TARGET, DEFAULT_FILE_NAME, new ArrayList<>(0),
                new JUnitConfiguration());
        assertNotEquals(0, unit.compareTo(null));
    }

}
