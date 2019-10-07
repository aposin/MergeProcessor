/**
 * Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aposin.mergeprocessor.renaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aposin.mergeprocessor.renaming.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link Version}.
 * 
 * @author Stefan Weiser
 *
 */
public class VersionTest {

	@Test
	public void testValidVersion() {
		final String string = "18.5.123";
		assertEquals(string, new Version(string).toString());
	}

	@Test
	public void testValidVersionWithZerosAtTheEnd() {
		final String string = "18.5.300";
		assertEquals(string, new Version(string).toString());
	}

	@Test
	public void testValidVersionWithUselessZeros() {
		final String string = "18.5.123.0.0";
		assertEquals("18.5.123", new Version(string).toString());
	}

	@Test
	public void testValidVersionWithoutDots() {
		final String string = "123456789";
		assertEquals(string, new Version(string).toString());
	}

	@Test
	public void testInvalidVersionWithNull() {
		assertThrows(NullPointerException.class, () -> new Version(null));
	}

	@Test
	public void testInvalidVersionWithEmptyString() {
		assertThrows(IllegalArgumentException.class, () -> new Version(""));
	}

	@Test
	public void testInvalidVersionWithIllegalCharacters() {
		assertThrows(IllegalArgumentException.class, () -> new Version("abc"));
	}

	@Test
	public void testInvalidVersionWithTrailingDots() {
		assertThrows(IllegalArgumentException.class, () -> new Version("18.5.123...."));
	}

	@Test
	public void testEquals() {
		final String string = "18.5.123";
		assertTrue(new Version(string).equals(new Version(string)));
	}

	@Test
	public void testEqualsWithUselessZeros() {
		final String string1 = "18.5.123";
		final String string2 = "18.5.123.0.0";
		assertTrue(new Version(string1).equals(new Version(string2)));
	}

	@Test
	public void testEqualsWithDifferentVersions() {
		final String string1 = "18.5.123";
		final String string2 = "19.0.000";
		assertFalse(new Version(string1).equals(new Version(string2)));
	}

	@Test
	public void testCompareToWithEqualVersions() {
		final String string = "18.5.123";
		assertEquals(0, new Version(string).compareTo(new Version(string)));
	}

	@Test
	public void testCompareToWithNull() {
		assertEquals(1, new Version("18.5.123").compareTo(null));
	}

	@DisplayName("Test 18.5.101 compareTo 19.6.202")
	@Test
	public void testCompareTo1() {
		assertTrue(new Version("18.5.101").compareTo(new Version("19.6.202")) < 0);
	}

	@DisplayName("Test 18.5 compareTo 18.5.100")
	@Test
	public void testCompareTo2() {
		assertTrue(new Version("18.5").compareTo(new Version("18.5.100")) < 0);
	}

	@DisplayName("Test 18.5.101 compareTo 19.0")
	@Test
	public void testCompareTo3() {
		assertTrue(new Version("18.5.101").compareTo(new Version("19.0")) < 0);
	}

	@Test
	public void testSortOrder() {
		final Version v1 = new Version("18.5.101");
		final Version v2 = new Version("19.0");
		final Version v3 = new Version("22222");
		final Version v4 = new Version("1.2.3");
		final Version v5 = new Version("18.4.202");
		final List<Version> list = new ArrayList<>();
		list.add(v1);
		list.add(v2);
		list.add(v3);
		list.add(v4);
		list.add(v5);

		final List<Version> expect = new ArrayList<>();
		expect.add(v4);
		expect.add(v5);
		expect.add(v1);
		expect.add(v2);
		expect.add(v3);

		Collections.sort(list);
		assertIterableEquals(expect, list);
	}

	@Test
	public void testIsOlderThan() {
		final Version v1 = new Version("18.0");
		final Version v2 = new Version("19.0");
		assertTrue(v1.isOlderThan(v2));
	}

	@Test
	public void testIsOlderThanWithSameVersion() {
		final Version v1 = new Version("19.0");
		final Version v2 = new Version("19.0");
		assertFalse(v1.isOlderThan(v2));
	}

}
