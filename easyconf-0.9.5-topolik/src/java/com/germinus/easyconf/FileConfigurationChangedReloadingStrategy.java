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

import java.io.File;
import java.net.URL;

import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reloads the configuration file even if a base path wasn't originally 
 * specified. This happens for example when it has been loaded from
 * the classpath. It works too for files that are inside JAR files.
 *
 */
public class FileConfigurationChangedReloadingStrategy extends
        FileChangedReloadingStrategy {
    private URL sourceURL;
    private static final Log log = LogFactory.getLog(FileConfigurationChangedReloadingStrategy.class);
    
    public void setConfiguration(FileConfiguration configuration) {
        super.setConfiguration(configuration);
        setSourceURL(configuration.getURL());
     }
    
    protected void setSourceURL(URL url) {
        sourceURL = url;        
    }
    
    protected URL getSourceURL() {
        return sourceURL;
    }

    /**
     * Update the last modified time.
     */
    protected void updateLastModified() {
        lastModified = getFile().lastModified();
    }

    /**
     * Check if the configuration has changed since the last time it was loaded.
     */
    protected boolean hasChanged() {
        File file = getFile();
        if (!file.exists()) {
			if (log.isDebugEnabled()) {
				log.debug("File does not exist: " + file);
			}
            return false;
        }
        boolean result = (file.lastModified() > lastModified);
		lastChecked = System.currentTimeMillis();
		return result;
        
    }

    protected File getFile() {        
        if ("file".equals(sourceURL.getProtocol())) {
            return new File(sourceURL.getPath());
        } else if ("jar".equals(sourceURL.getProtocol())) {
            String path = sourceURL.getPath();
            String jarFilePath = path.substring("file:".length(), path.indexOf('!'));
            return new File(jarFilePath);
        } else if (configuration != null) {
            return configuration.getFile();
        } else if ("classloader".equals(sourceURL.getProtocol())) {
			if (log.isDebugEnabled()) {
				log.debug("Reloading will not work for files loaded by the classloader: " + sourceURL);
			}
			return new File(sourceURL.getFile());
        }
		log.warn("Cannot determine the filesystem file which contains the " +
				"configuration file for: " + sourceURL);
		return new File(sourceURL.getFile());
    }
}
