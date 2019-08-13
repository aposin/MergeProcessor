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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FileUtils}.
 * 
 * @author Stefan Weiser
 *
 */
public class FileUtilsTest {

	private Path source;
	private Path target;

	@BeforeEach
	public void before() throws IOException {
		source = createRandomFileWith60MBs();
		target = Files.createTempFile("tempFileToCopyTo", ".tmp");
	}

	@AfterEach
	public void after() throws IOException {
		Files.deleteIfExists(source);
		Files.deleteIfExists(target);
	}

	@Test
	public void testCopyFile() throws IOException {
		if (org.aposin.mergeprocessor.utils.FileUtils.copyFiles(source, target, new NullProgressMonitor())) {
			assertArrayEquals(FileUtils.readFileToByteArray(source.toFile()),
					FileUtils.readFileToByteArray(target.toFile()));
		} else {
			fail("Copy job was not successful.");
		}
	}

	@Test
	public void testCopyNotExistingSourceFile() {
		assertFalse(org.aposin.mergeprocessor.utils.FileUtils.copyFiles(Paths.get("Z:\\a\\b\\c"), target,
				new NullProgressMonitor()));
	}

	@Test
	public void testCopyWithNullSource() {
		assertFalse(org.aposin.mergeprocessor.utils.FileUtils.copyFiles(null, target, new NullProgressMonitor()));
	}

	@Test
	public void testCopyWithNullTarget() {
		assertThrows(NullPointerException.class,
				() -> org.aposin.mergeprocessor.utils.FileUtils.copyFiles(source, null, new NullProgressMonitor()));
	}

	@Test
	public void testCopyWithNullAsProgressMonitor() throws IOException {
		if (org.aposin.mergeprocessor.utils.FileUtils.copyFiles(source, target, null)) {
			assertArrayEquals(FileUtils.readFileToByteArray(source.toFile()),
					FileUtils.readFileToByteArray(target.toFile()));
		} else {
			fail("Copy job was not successful.");
		}
	}

	/**
	 * Creates a random file with 50 bytes content.
	 * 
	 * @return the path of the created random file
	 * @throws IOException
	 */
	private static Path createRandomFileWith60MBs() throws IOException {
		final Random random = new Random();
		final byte[] result = new byte[63_000_000]; // about 60 MB
		random.nextBytes(result);

		final Path tempFile = Files.createTempFile("tempFileToCopy", ".tmp");
		FileUtils.writeByteArrayToFile(tempFile.toFile(), result);
		return tempFile;
	}

}
