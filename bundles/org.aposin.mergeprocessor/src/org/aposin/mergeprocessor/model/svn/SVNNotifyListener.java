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
package org.aposin.mergeprocessor.model.svn;

import java.io.File;

import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * Empty implementations of {@link ISVNNotifyListener}.
 * 
 * @author Stefan Weiser
 *
 */
public abstract class SVNNotifyListener implements ISVNNotifyListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void logCommandLine(String arg0) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logCompleted(String arg0) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logError(String arg0) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logMessage(String arg0) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logRevision(long arg0, String arg1) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotify(File arg0, SVNNodeKind arg1) {
        //NOOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommand(int arg0) {
        //NOOP
    }

}
