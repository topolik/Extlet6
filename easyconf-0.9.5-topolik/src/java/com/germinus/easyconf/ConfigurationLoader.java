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

import org.apache.commons.configuration.DatabaseConfiguration;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Substitutor;
import org.apache.commons.digester.substitution.MultiVariableExpander;
import org.apache.commons.digester.substitution.VariableSubstitutor;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * Handles the actual reading of the configuration
 * 
 * @author jferrer
 */
class ConfigurationLoader {
    private static final Log log = LogFactory.getLog(ConfigurationLoader.class);

    private static final ConfigurationSerializer serializer = ConfigurationSerializer.getSerializer();

    public ComponentProperties readPropertiesConfiguration(String companyId, String componentName) {
        AggregatedProperties properties = new AggregatedProperties(companyId, componentName);
//        if (companyId != null) {
//            properties.addGlobalFileName(Conventions.GLOBAL_CONFIGURATION_FILE + Conventions.SLASH + companyId
//                    + Conventions.PROPERTIES_EXTENSION);
//        }
        properties.addGlobalFileName(Conventions.GLOBAL_CONFIGURATION_FILE + 
                Conventions.PROPERTIES_EXTENSION);
//        if (companyId != null) {
//            properties.addBaseFileName(componentName + Conventions.SLASH + companyId + 
//                    Conventions.PROPERTIES_EXTENSION);
//        }
        properties.addBaseFileName(componentName + Conventions.PROPERTIES_EXTENSION);

        log.info("Properties for " + componentName + " loaded from " + properties.loadedSources());
        return new ComponentProperties(properties);
    }

    public ConfigurationObjectCache readConfigurationObject(String companyId,
            String componentName, String confName, ComponentProperties properties) 
    	throws IOException, SAXException {
        log.info("Reading the configuration object for " + componentName);
        ConfigurationObjectCache result = null;
        
        String inexistentSource = null;
        String sourceName = properties.getString(Conventions.CONFIGURATION_OBJECTS_SOURCE_PROPERTY, inexistentSource);
        if (sourceName == inexistentSource) {
        	// ignore
        } else if (DatasourceURL.isDatasource(sourceName)) {
        	result = readConfigurationObjectFromDatabase(
        			companyId, componentName, confName, properties, sourceName);
        } else {
        	throw new ConfigurationException("The specified value for " +
        			"easyconf:configuration-object-source is not valid: " +
        			sourceName);
        }
        if (result == null) {
        	result = readConfigurationObjectFromXMLFile(
        			companyId, componentName, confName, properties);
        }
        return result;

    }
    
	private ConfigurationObjectCache readConfigurationObjectFromDatabase(String companyId, String componentName, String confName, ComponentProperties properties, String sourceName) {
		ConfigurationObjectCache result;
		DatasourceURL dsURL = new DatasourceURL(sourceName, companyId,
				componentName, DatasourceURL.CONFIGURATION_OBJECTS_TABLE);
		String confObjXML = dsURL.getConfiguration().
			getString(confName);
		if (confObjXML == null) return null;
		Object confObject = serializer.deserialize(confObjXML);
		result = new ConfigurationObjectCache(confObject, null, properties, confName);
		return result;
	}

	private ConfigurationObjectCache readConfigurationObjectFromXMLFile(String companyId, String componentName, String confName, ComponentProperties properties) throws FileNotFoundException, IOException, SAXException {
		ConfigurationObjectCache result;
		String confFileName = null;
        URL confFile = null;
        if (companyId != null) {
            confFileName = componentName + Conventions.SLASH + companyId + Conventions.XML_EXTENSION;
            confFile = ClasspathUtil.locateResource(null, confFileName);
            log.info("Loaded " + confFileName + ": " + confFile);
        }
        if (confFile == null) { 
        	if (confName == Conventions.DEFAULT_CONF_OBJECT_NAME) {
        		confFileName = componentName + Conventions.XML_EXTENSION;
        	} else {
        		confFileName = componentName + Conventions.DOT +
					confName + Conventions.XML_EXTENSION;
        	}
	        confFile = ClasspathUtil.locateResource(null, confFileName);
        }
        if (confFile == null) {
            throw new FileNotFoundException("File " + confFileName + " not found");
        }
        Object confObj = loadXMLFile(confFile, properties);
        result = new ConfigurationObjectCache(confObj, confFile, properties, confName);
        Long delay = properties.getDelayPeriod();
        if (delay != null) {
            result.setReloadingStrategy(
                    new FileURLChangedReloadingStrategy(confFile, delay.longValue()));
        }
		return result;
	}

    /**
     * Read an XML file and return an Object representation of its contents
     */	
    Object loadXMLFile(URL confFileUrl, ComponentProperties properties) 
    	throws IOException, SAXException {
        log.debug("Loading XML file: " + confFileUrl);
        String componentName = properties.getComponentName();
        String rulesFileName = componentName + Conventions.DIGESTERRULES_EXTENSION;
        URL digesterRulesUrl = ClasspathUtil.locateResource(rulesFileName);
        if (digesterRulesUrl == null) {
            throw new DigesterRulesNotFoundException(componentName,
                    rulesFileName);
        }
        Digester digester = DigesterLoader.createDigester(digesterRulesUrl);
        digester.setUseContextClassLoader(true);
        digester.setValidating(false);
        
        MultiVariableExpander expander = new MultiVariableExpander();
        expander.addSource("$", properties.toMap());
        Substitutor substitutor = new VariableSubstitutor(expander);
        digester.setSubstitutor(substitutor);
        
        try {
            Object confObj = digester.parse(confFileUrl.openStream());
            log.info("Read configuration from " + confFileUrl);
            return confObj;
        } catch (IllegalArgumentException e) {
            //FIXME: it does not catch the exception
            throw new InvalidPropertyException(properties.getComponentName(), e);
        }
    }

    public void saveConfigurationObjectIntoDatabase(Object configurationObject, String companyId, String componentName, String confName, ComponentProperties properties) {
    	log.info("Saving the configuration object into the database for " + componentName);
        String inexistentSource = null;
        String sourceName = properties.getString(Conventions.CONFIGURATION_OBJECTS_SOURCE_PROPERTY, inexistentSource);
        if (sourceName == inexistentSource) {
        	throw new ConfigurationException("It is imposible to save the configuration object. "
        			+ "Please specify a valid datasource in property " 
        			+ Conventions.CONFIGURATION_OBJECTS_SOURCE_PROPERTY); 
        } else if (DatasourceURL.isDatasource(sourceName)) {
        	DatasourceURL dsURL = new DatasourceURL(sourceName, companyId,
					componentName, DatasourceURL.CONFIGURATION_OBJECTS_TABLE);
			DatabaseConfiguration dbConf = dsURL
					.getConfiguration();
			String xml = serializer.serialize(configurationObject);
			dbConf.setProperty(confName, xml);
        } else {
        	throw new ConfigurationException("The specified value for " +
        			"easyconf:configuration-object-source is not valid: " +
        			sourceName);
        }
    }
    
}
