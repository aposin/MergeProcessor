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
package org.aposin.mergeprocessor.utils;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.primitives.Bytes;

/**
 * Utility class for dealing with byte arrays.
 * 
 * @author Stefan Weiser
 *
 */
public class ByteArrayUtil {

	private ByteArrayUtil() {
		// Utility class
	}

	/**
	 * Replaces in {@code sequence} the first occurrence of the specific {@code replacedSequence} 
	 * with {@code replacingSequence}. 
	 * 
	 * @param sequence the sequence where to replace
	 * @param replacedSequence the replaced sequence
	 * @param replacingSequence the replacement sequence
	 * @return
	 */
	public static byte[] replace(byte[] sequence, byte[] replacedSequence, byte[] replacingSequence) {
		if (sequence == null) {
			return null;
		}
		if (sequence.length == 0) {
			return new byte[0];
		}
		final int startIndex = Bytes.indexOf(sequence, replacedSequence);
		if (startIndex == -1) {
			return Arrays.copyOf(sequence, sequence.length);
		}
		final int endIndex = startIndex + replacedSequence.length;
		final byte[] sequenceWithoutReplacement = ArrayUtils.removeAll(sequence,
				createIndexArray(startIndex, endIndex));
		return ArrayUtils.insert(startIndex, sequenceWithoutReplacement, replacingSequence);
	}

	/**
	 * <p>Creates an integer array with all values between the start and the end value.</p>
	 * <pre>
	 * ByteArrayUtil.createIndexArray(0,3)       = {0, 1, 2}
	 * ByteArrayUtil.createIndexArray(0,5)       = {0, 1, 2, 3, 4}
	 * </pre>
	 * 
	 * @param start the start value
	 * @param end the end value
	 * @return the integer array
	 */
	private static int[] createIndexArray(int start, int end) {
		int[] array = new int[end - start];
		int nextArrayIndex = 0;
		for (int i = start; i < end; i++) {
			array[nextArrayIndex++] = i;
		}
		return array;
	}

}
