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
package org.aposin.mergeprocessor.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.aposin.mergeprocessor.utils.ByteArrayUtil;
import org.junit.jupiter.api.Test;

public class ByteArrayUtilTest {

    @Test
    public void testReplaceStartingBytes() {
        final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
        final byte[] replacedSequence = "abc".getBytes();
        final byte[] replacingSequence = "123".getBytes();
        final byte[] expected = "123defghijklmnopqrstuvwxyz".getBytes();
        assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
    }

    @Test
    public void testReplaceIntermediateBytes() {
        final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
        final byte[] replacedSequence = "mno".getBytes();
        final byte[] replacingSequence = "123".getBytes();
        final byte[] expected = "abcdefghijkl123pqrstuvwxyz".getBytes();
        assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
    }

    @Test
    public void testReplaceEndingBytes() {
        final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
        final byte[] replacedSequence = "xyz".getBytes();
        final byte[] replacingSequence = "123".getBytes();
        final byte[] expected = "abcdefghijklmnopqrstuvw123".getBytes();
        assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
    }

    @Test
    public void testReplaceWithNotExistingReplacement() {
        final byte[] sequence = "abcdefghijklmnopqrstuvwxyz".getBytes();
        final byte[] replacedSequence = "123".getBytes();
        final byte[] replacingSequence = "456".getBytes();
        assertArrayEquals(sequence, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
    }

    @Test
    public void testReplaceWithNullSequence() {
        final byte[] replacedSequence = "123".getBytes();
        final byte[] replacingSequence = "456".getBytes();
        assertArrayEquals(null, ByteArrayUtil.replace(null, replacedSequence, replacingSequence));
    }

    @Test
    public void testReplaceWithEmptySequence() {
        final byte[] replacedSequence = "123".getBytes();
        final byte[] replacingSequence = "456".getBytes();
        assertArrayEquals(new byte[0], ByteArrayUtil.replace(new byte[0], replacedSequence, replacingSequence));
    }

    @Test
    public void testReplaceWithReoccuringgReplacement() {
        final byte[] sequence = "abcdefghijklmnopqrstuvwxyzabc".getBytes();
        final byte[] replacedSequence = "abc".getBytes();
        final byte[] replacingSequence = "123".getBytes();
        final byte[] expected = "123defghijklmnopqrstuvwxyzabc".getBytes();
        assertArrayEquals(expected, ByteArrayUtil.replace(sequence, replacedSequence, replacingSequence));
    }

}
