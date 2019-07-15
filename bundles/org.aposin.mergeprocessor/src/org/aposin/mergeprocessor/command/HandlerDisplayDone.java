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

package org.aposin.mergeprocessor.command;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.view.dashboard.Dashboard;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;

/**
 * Handler for showing/hiding the merge units with the status DONE.
 * 
 * @author Stefan Weiser
 *
 */
public class HandlerDisplayDone {

    @Execute
    public void execute(MItem item, MPart part, IConfiguration configuration, UISynchronize uiSynch) {
        LogUtil.entering(item, part, configuration, uiSynch);
        configuration.setDisplayDone(item.isSelected());
        final Object object = part.getObject();
        if (object instanceof Dashboard) {
            final Dashboard view = (Dashboard) object;
            uiSynch.syncExec(view::refresh);
        }
        LogUtil.exiting();
    }

}