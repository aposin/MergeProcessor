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
package org.aposin.mergeprocessor.view;

import java.util.Arrays;

/**
 * Defines the order of the Columns
 */
public enum Column {
	// the order of the enums defines the order of the columns in the gui

	COLUMN_STATUS, //
	COLUMN_HOST, //
	COLUMN_REPOSITORY, //
	COLUMN_DATE, //
	COLUMN_REVISIONS, //
	COLUMN_BRANCH_SOURCE, //
	COLUMN_BRANCH_TARGET, //
	COLUMN_MERGESCRIPT, //
	COLUMN_RENAMING;

	/**
	 * @return an array with the columns in the order they appear in the GUI.
	 */
	public static Column[] sortedValues() {
		Column[] columns = Column.values();
		Arrays.sort(columns);
		return columns;
	}

	/**
	 * @param columnIndex the index of the column
	 * @return the column for the given index
	 */
	public static Column valueForIndex(int columnIndex) {
		return sortedValues()[columnIndex];
	}

	public static int indexForValue(Column column) {
		return Arrays.binarySearch(sortedValues(), column);
	}
}