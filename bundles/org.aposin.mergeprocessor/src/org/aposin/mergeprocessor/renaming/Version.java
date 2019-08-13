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

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * <p>This class represents a version information and makes it possible to compare them. A version
 * String only contains digits and dots. Trailing 0 version parts are trimmed, e.g.</p>
 * 
 * <blockquote><pre>
 * 18.5.101 = 18.5.101.0.0.000
 * </pre></blockquote>
 * 
 * <p>When comparing version with each other the values are compared starting with the first version part, e.g.</p>
 * 
 * <blockquote><pre>
 * 18.5.101 < 19.6.202
 * 18.5     < 18.5.100
 * 18.5.101 < 19.0
 * </pre></blockquote>
 * 
 * @author Stefan Weiser
 *
 */
public final class Version implements Comparable<Version> {

	/** Version with number {@code 0}.*/
	public static final Version ZERO = new Version("0");
	private static final String REGEX_FORMAT_CHECK = "^\\d+(\\.\\d+)*$";

	private final int[] parts;

	/**
	 * @param version the {@link String} to interpret as version
	 */
	public Version(final String version) {
		Objects.requireNonNull(version, "Null does not represent a Version.");
		if (!version.matches(REGEX_FORMAT_CHECK)) {
			throw new IllegalArgumentException(
					String.format("The given String '%s' does not represent a Version.", version));
		}
		final String[] split = version.replaceAll("\\.[\\.0+]*$", "") // trim all '.0' at the end as they are useless
				.split("\\."); // split for '.'
		parts = Arrays.stream(split).filter(NumberUtils::isDigits).mapToInt(Integer::parseInt).toArray();
		if (parts.length == 0) {
			throw new IllegalArgumentException(
					String.format("The given String '%s' does not represent a Version.", version));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Version o) {
		if (o == null) {
			return 1;
		}
		if (Objects.equals(this, o)) {
			return 0;
		}
		if (parts.length == o.parts.length) {
			// Same length
			for (int i = 0; i < parts.length; i++) {
				int r = Integer.compare(parts[i], o.parts[i]);
				if (r != 0) {
					return r;
				}
			}
			return 0; // Should normally not be reached
		} else if (parts.length > o.parts.length) {
			// This has more parts
			for (int i = 0; i < o.parts.length; i++) {
				int r = Integer.compare(parts[i], o.parts[i]);
				if (r != 0) {
					return r;
				}
			}
			return 1;
		} else { // parts.length < o.parts.length
			// Other has more parts
			for (int i = 0; i < parts.length; i++) {
				int r = Integer.compare(parts[i], o.parts[i]);
				if (r != 0) {
					return r;
				}
			}
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(parts);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		return Arrays.equals(parts, other.parts);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		Arrays.stream(parts).forEach(part -> sb.append(part).append('.'));
		return sb.substring(0, sb.length() - 1);
	}

	/**
	 * Checks if this version is an older version than the given one.
	 * 
	 * @param version the version to check
	 * @return {@code true} if this version is older than the given one
	 */
	public boolean isOlderThan(final Version version) {
		return compareTo(version) < 0;
	}

	/**
	 * Checks, if this version is between the source and the target version. This method
	 * also returns {@code true} if the target version is equals to this.
	 * 
	 * @param source the source version
	 * @param target the target version
	 * @return {@code true} if this version is between
	 */
	public boolean isBetween(final Version source, final Version target) {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		// source < this <= target
		return source.isOlderThan(this) && (this.isOlderThan(target) || this.equals(target));
	}

}