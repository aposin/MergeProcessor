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
package org.aposin.mergeprocessor.model;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public enum MergeUnitStatus {
	/**
	 * MergeUnit is in "todo" folder and needs to be processed.
	 */
	TODO,
	/**
	 * Merge has been attempted but has been cancelled by the user.
	 */
	CANCELLED,
	/**
	 * The changes have been committed to the repository and the file is in "done".
	 */
	DONE,
	/**
	 * No merge has been executed and the file is in "done".
	 */
	IGNORED,
	/**
	 * The merge is done directly into a workspace. The commit has to be done manually
	 * by the developer. This may be required because of changed directory structures 
	 * or file naming.
	 */
	MANUAL;

	/**
	 * @return a string representation which can be used in the GUI.
	 */
	@Override
	public String toString() {
		String retVal = null;

		switch (this) {
		case TODO:
			retVal = Messages.MergeUnitStatus_Todo;
			break;
		case CANCELLED:
			retVal = Messages.MergeUnitStatus_Cancelled;
			break;
		case DONE:
			retVal = Messages.MergeUnitStatus_Done;
			break;
		case IGNORED:
			retVal = Messages.MergeUnitStatus_Ignored;
			break;
		case MANUAL:
			retVal = Messages.MergeUnitStatus_Manual;
			break;
		default:
			String message = String.format("Unexpected value=[%s]", this); //$NON-NLS-1$
			final RuntimeException exception = new RuntimeException(message);
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, exception.getMessage(), exception);
			throw exception;
		}

		return retVal;
	}
}
