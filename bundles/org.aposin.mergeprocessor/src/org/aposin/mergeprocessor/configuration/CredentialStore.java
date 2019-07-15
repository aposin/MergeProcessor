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

import org.aposin.mergeprocessor.utils.LogUtil;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * Credential store which uses org.eclipse.equinox.security as secure storage.
 */
@Creatable
public class CredentialStore {

    private static final String NODE_PATH_NAME = "org.aposin.mergeprocessor.secure"; //$NON-NLS-1$

    private static final String KEY_USERNAME = "username"; //$NON-NLS-1$
    private static final String KEY_PASSWORD = "password"; //$NON-NLS-1$

    private static ISecurePreferences getSecureNode() {
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
        return securePreferences.node(NODE_PATH_NAME);
    }

    /**
     * @return the stored username or <code>null</code>
     * @throws ConfigurationException 
     */
    public String getUsername() throws ConfigurationException {
        try {
            return getSecureNode().get(KEY_USERNAME, ""); //$NON-NLS-1$
        } catch (StorageException e) {
            throw LogUtil.throwing(new ConfigurationException("StorageException while getting username.", e));
        }
    }

    /**
     * @return the stored password or <code>null</code>
     * @throws ConfigurationException 
     */
    public String getPassword() throws ConfigurationException {
        try {
            return getSecureNode().get(KEY_PASSWORD, ""); //$NON-NLS-1$
        } catch (StorageException e) {
            throw LogUtil.throwing(new ConfigurationException("StorageException while getting password.", e));
        }
    }

    /**
     * @param username the username to store
     * @throws ConfigurationException 
     */
    public void setUsername(String username) throws ConfigurationException {
        try {
            getSecureNode().put(KEY_USERNAME, username, false);
        } catch (StorageException e) {
            throw LogUtil.throwing(new ConfigurationException("StorageException while setting username.", e));
        }
    }

    /**
     * @param password the password to store
     * @throws ConfigurationException 
     */
    public void setPassword(String password) throws ConfigurationException {
        try {
            getSecureNode().put(KEY_PASSWORD, password, true);
        } catch (StorageException e) {
            throw LogUtil.throwing(new ConfigurationException("StorageException while setting password.", e));
        }
    }

    /**
     * Removes all stored values from the node.
    
     */
    public void clearCredentials() {
        getSecureNode().clear();
    }

}
