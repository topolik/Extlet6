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

/**
 * Conventions used by EasyConf that can be expressed as contansts
 * @author  Jorge Ferrer
 * @version $Revision: 1.7 $
 *
 */
public interface Conventions {

    char SELECTOR_START = '[';
    char SELECTOR_END = ']';
    char DOT = '.';
    char SLASH = '-';

    String INCLUDE_PROPERTY = "include-and-override";
    String GLOBAL_CONFIGURATION_FILE = "global-configuration";

    String DIGESTERRULES_EXTENSION = ".digesterRules.xml";
    String XML_EXTENSION = ".xml";
    String PROPERTIES_EXTENSION = ".properties";

    String DATASOURCE_PREFIX = "datasource:";
    String JNDI_PREFIX = "jndi:";

    String RELOAD_DELAY_PROPERTY = "easyconf:reload-delay";
	String CONFIGURATION_OBJECTS_SOURCE_PROPERTY = "easyconf:configuration-objects-source";

	String CONFIGURATION_OBJECTS_TABLE = "easyconf_configuration_objects";
	String PROPERTIES_TABLE = "easyconf_properties";
    
    String COMPANY_ID_PROPERTY = "easyconf:companyId";
    String COMPONENT_NAME_PROPERTY = "easyconf:componentName";
    String PREFIX_SEPARATOR = ":";
    
	String DEFAULT_CONF_OBJECT_NAME = "DEFAULT_CONF_OBJECT";
}
