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
/**
 * 
 */
package org.aposin.mergeprocessor.application;

import javax.inject.Inject;
import javax.inject.Named;

import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolItem;

/**
 * This processor refreshes the selection state of the checkable toolbar items.
 * 
 * @author Stefan Weiser
 *
 */
public class ToolbarProcessor {

    public static final String TOOLBAR_ID = "org.aposin.mergeprocessor.toolbar";
    private static final String TOOL_ITEM_AUTOMMATIC = "org.aposin.mergeprocessor.handledtoolitem.automatic";
    private static final String TOOL_ITEM_DISPLAY_DONE = "org.aposin.mergeprocessor.handledtoolitem.displaydone";
    private static final String TOOL_ITEM_DISPLAY_IGNORED = "org.aposin.mergeprocessor.handledtoolitem.displayIgnored";

    @Named(TOOLBAR_ID)
    @Inject
    public MToolBar toolbar;

    @Execute
    public void execute(IConfiguration configuration) {
        for (final MToolBarElement element : toolbar.getChildren()) {
            if (element instanceof MToolItem) {
                switch (element.getElementId()) {
                    case TOOL_ITEM_AUTOMMATIC:
                        ((MToolItem) element).setSelected(configuration.isAutomatic());
                        break;
                    case TOOL_ITEM_DISPLAY_DONE:
                        ((MToolItem) element).setSelected(configuration.isDisplayDone());
                        break;
                    case TOOL_ITEM_DISPLAY_IGNORED:
                        ((MToolItem) element).setSelected(configuration.isDisplayIgnored());
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
