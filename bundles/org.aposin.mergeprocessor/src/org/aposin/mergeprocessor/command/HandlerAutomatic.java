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
package org.aposin.mergeprocessor.command;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;

/**
 * Handler for activating the automatic merge, when merge units with the status
 * TO DO are available.
 * 
 * @author Stefan Weiser
 *
 */
public class HandlerAutomatic {

	@Execute
	public void execute(MItem item, IConfiguration configuration) {
		LogUtil.entering(item, configuration);
		configuration.setAutomatic(item.isSelected());
		LogUtil.exiting();
	}

}