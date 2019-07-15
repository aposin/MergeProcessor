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

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This utility class provides methods which are not directly available in Eclipse when running
 * in compatibility mode. When the application is migrated to e4, which class should be deleted,
 * as it should not be necessary any more.
 * 
 * @author Stefan Weiser
 *
 */
public class E4CompatibilityUtil {

    private E4CompatibilityUtil() {
        //Utility class containing only static methods
    }

    /**
     * @return the application context
     */
    public static IEclipseContext getApplicationContext() {
        return ((IWorkbench) PlatformUI.getWorkbench()).getApplication().getContext();
    }

}
