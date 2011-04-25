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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Main class to obtain the configuration of a software component.
 *
 * The main method is <code>getConfiguration</code> which must be
 * given the name of a component.
 *
 * @author jferrer@germinus.com
 */
public class EasyConf {
    private static final Log log = LogFactory.getLog(EasyConf.class);
    private static Map cache = new HashMap();

    private EasyConf() {
    }

    /**
     * Get the full configuration of the given component.
     * 
     * The configuration will be cached so that next calls will not need
     * to reload the properties or XML files again.
     * @param componentName any String which can be used to identified a
     * configuration component.
     * @return a <code>ComponentConf</code> instance 
     */
    public static ComponentConfiguration getConfiguration(String componentName) {
        try {
            ComponentConfiguration componentConf = (ComponentConfiguration)
            cache.get(componentName);
            if (componentConf == null) {
                componentConf = new ComponentConfiguration(componentName);
                cache.put(componentName, componentConf);
            }
            return componentConf;
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(componentName, 
                    "Error reading the configuration", e); 
        }    
    }

    /**
     * Get the full configuration of the given component, for the given company.
     * This method should be used when the application is to be deployed in
     * an ASP model where several companies want different configurations for
     * the same running applications
     * 
     * 
     * The configuration will be cached so that next calls will not need
     * to reload the properties or XML files again.
     * @param companyId the identifier of the company whose specific 
     * configuration should be read
     * @param componentName any String which can be used to identified a
     * configuration component.
     * @return a <code>ComponentConf</code> instance 
     */
    public static ComponentConfiguration getConfiguration(String companyId, 
                                                          String componentName) {
        try {
            final String cacheKey = companyId + componentName;
            ComponentConfiguration componentConf = (ComponentConfiguration)
            cache.get(cacheKey);
            if (componentConf == null) {
                componentConf = new ComponentConfiguration(companyId, componentName);
                cache.put(cacheKey, componentConf);
            }
            return componentConf;
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(componentName, 
                    "Error reading the configuration for " + companyId, e); 
        }
    }

    /**
     * Refresh the configuration of the given component
     * 
     * KNOWN BUG: this method does not refresh the properties configuration because the underlying
     * library Jakarta Commons Configuration also contains a cache which is not refreshable. This 
     * issue is scheduled to be solved after version 1.0 of such library. 
     */
    public static void refreshComponent(String componentName) {
        ComponentConfiguration componentConf = (ComponentConfiguration) cache.get(componentName);
        if (componentConf != null) {
            cache.remove(componentName);
            log.info("Refreshed the configuration of component " + componentName);
        }
    }
    /**
     * Refresh the configuration of all components
     * 
     * KNOWN BUG: this method does not refresh the properties configuration because the underlying
     * library Jakarta Commons Configuration also contains a cache which is not refreshable. This 
     * issue is scheduled to be solved after version 1.0 of such library. 
     */
    public static void refreshAll() {
        cache = new HashMap();
        log.info("Refreshed the configuration of all components");
    }

    // ************************** Deprecated methods ***************************


}
