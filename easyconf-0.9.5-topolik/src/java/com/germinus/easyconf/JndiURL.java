/*
 * Copyright 2004-2005 Germinus XXI
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package com.germinus.easyconf;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.JNDIConfiguration;

/**
 * Represents the URL to a JNDI tree as specified in a properties file
 * TODO: Add support for ASP applications
 * @author  Jorge Ferrer
 * @version $Revision: 1.2 $
 *
 */
public class JndiURL {
    private static final String JNDI_PREFIX = Conventions.JNDI_PREFIX;
    private static InitialContext ctx = null;
    private String jndiPrefix;
    private String companyId;
    private String componentName;

    public JndiURL(String jndiPath, String companyId, String componentName) {
        this.jndiPrefix = jndiPrefix.substring(JNDI_PREFIX.length());
        this.companyId = companyId;
        this.componentName = componentName;
    }

    private synchronized InitialContext getContext() throws NamingException {
        if (ctx == null) {
            ctx = new InitialContext();
        }
        return ctx;
    }

    public static boolean isJndi(String sourcePath) {
        return sourcePath.startsWith(JNDI_PREFIX);
    }

    public String getPrefix() {
        return jndiPrefix;
    }

    public Configuration getConfiguration() {
        try {
            return new JNDIConfiguration(getPrefix());
        } catch (NamingException e) {
            throw new ConfigurationException("Error loading JNDI configuration for " 
                    + getPrefix());
        }
    }


}
