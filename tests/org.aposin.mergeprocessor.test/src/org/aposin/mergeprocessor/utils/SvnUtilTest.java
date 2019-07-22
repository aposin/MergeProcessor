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
/**
 * 
 */
package org.aposin.mergeprocessor.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.aposin.mergeprocessor.utils.SvnUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SvnUtil}.
 * 
 * @author Stefan Weiser
 *
 */
@DisplayName("Tests for SvnUtil")
public class SvnUtilTest {

    @DisplayName("Get the branch name for 'https://svn.aposin.org/svn/aposin/trunk'")
    @Test
    public void testGetBranchNameTrunk() {
        final String url = "https://svn.aposin.org/svn/aposin/trunk";
        final String result = SvnUtil.getBranchName(url);
        assertEquals("trunk", result);
    }

    @DisplayName("Get the branch name for 'https://svn.aposin.org/svn/aposin/branches/ABS_175'")
    @Test
    public void testGetBranchName175() {
        final String url = "https://svn.aposin.org/svn/aposin/branches/ABS_175";
        final String result = SvnUtil.getBranchName(url);
        assertEquals("ABS_175", result);
    }

    @DisplayName("Get the branch name for 'https://svn.aposin.org/svn/aposin/branches/ABS_18003A_HOT'")
    @Test
    public void testGetBranchName18003A() {
        final String url = "https://svn.aposin.org/svn/aposin/branches/ABS_18003A_HOT";
        final String result = SvnUtil.getBranchName(url);
        assertEquals("ABS_18003A_HOT", result);
    }

    @DisplayName("Get the branch name with null")
    @Test
    public void testGetBranchNameNull() {
        assertNull(SvnUtil.getBranchName(null));
    }

    @DisplayName("Get the branch name with empty String")
    @Test
    public void testGetBranchNameEmptyString() {
        assertNull(SvnUtil.getBranchName(""));
    }

    @DisplayName("Get the branch name when inserting 'https://www.aposin.org'")
    @Test
    public void testGetBranchNameWithNoExpectedBranchname() {
        assertNull(SvnUtil.getBranchName("https://www.aposin.org"));
    }

}
