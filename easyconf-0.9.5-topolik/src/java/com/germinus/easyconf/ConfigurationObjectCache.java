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

import java.net.URL;

import org.apache.commons.configuration.reloading.InvariantReloadingStrategy;
import org.apache.commons.configuration.reloading.ReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Holds a configuration object and reloads it when necessary
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.4 $
 *
 */
public class ConfigurationObjectCache {

    Object configurationObject;
    ReloadingStrategy reloadingStrategy = new InvariantReloadingStrategy();
    Object reloadLock = new Object();
    ConfigurationLoader loader = new ConfigurationLoader();
    private static final Log log = LogFactory.getLog(ConfigurationObjectCache.class);
    private URL confFileUrl;
    private ComponentProperties properties;
	private String confName;
    
    public ConfigurationObjectCache(Object confObj, URL confFileUrl, ComponentProperties properties, String confName) {
        this.configurationObject = confObj;
        this.confFileUrl = confFileUrl;
        this.properties = properties;
        this.confName = confName;
    }
    
    public Object getConfigurationObject() {
        reload();
        return configurationObject;
    }
    
	public String getConfName() {
//		if (confName == null) {
//			try {
//				confName = (String) BeanUtils.getSimpleProperty(configurationObject, "id");
//			} catch (Exception e) {
//				confName = Conventions.DEFAULT_CONF_OBJECT_NAME;
//			}
//		}
		return confName;
	}

    private void reload() {
    	if (confFileUrl == null) {
    		return;
    	}
        synchronized (reloadLock) {
            if (reloadingStrategy.reloadingRequired()) {
                try {
                    configurationObject =  loader.loadXMLFile(confFileUrl, properties);
                    reloadingStrategy.reloadingPerformed();
                }
                catch (Exception e) {
                    log.error("Error loading " + confFileUrl, e);
                }
            }
        }
    }

    private ReloadingStrategy getReloadingStrategy() {
        return reloadingStrategy;
    }

    public void setReloadingStrategy(ReloadingStrategy strategy) {
        this.reloadingStrategy = strategy;        
    }

}
