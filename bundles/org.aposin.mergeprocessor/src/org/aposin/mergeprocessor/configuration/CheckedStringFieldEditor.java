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
package org.aposin.mergeprocessor.configuration;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * This {@link StringFieldEditor} shows an error message when an illegal value is defined.
 * 
 * @author Stefan Weiser
 *
 */
class CheckedStringFieldEditor extends StringFieldEditor {

	/**
	 * @param name the field name
	 * @param labelText the label text
	 * @param parent the parent composite where the control is instantiated
	 * @param errorMessage the error message if the value is invalid
	 */
	CheckedStringFieldEditor(String name, String labelText, Composite parent, String errorMessage) {
		super(name, labelText, parent);
		setErrorMessage(errorMessage);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean checkState() {
		if (check()) {
			clearErrorMessage();
			return true;
		} else {
			showErrorMessage();
			return false;
		}
	}

	/**
	 * Checks if the current value is valid. 
	 * The default behaviour is a check against an empty String.
	 * 
	 * @return {@code true} if the current value is valid
	 */
	protected boolean check() {
		return !getTextControl().getText().isEmpty();
	}

}