/*
 * Copyright 2004-2005 Germinus XXI
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.germinus.easyconf;

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.JNDIConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.*;

/**
 * Provides configuration properties from several sources making distintion
 * from:
 * <ul>
 * <li>Base properties specific to the current component
 * <li>Global properties which may be prefixed
 * <li>System properties (so that they are available as variables to the other
 * property files)
 * </ul>
 * It also knows the source the a property to offer user information.
 * 
 * @author jferrer
 */
public class AggregatedProperties extends MergingCompositeConfiguration {
	private static final Log log = LogFactory
			.getLog(AggregatedProperties.class);

	private CompositeConfiguration baseConf = new MergingCompositeConfiguration();

	private CompositeConfiguration globalConf = new MergingCompositeConfiguration();

	private SystemConfiguration systemConfiguration = new SystemConfiguration();

	private Configuration prefixedSystemConfiguration = new SubsetConfiguration(
			systemConfiguration, getPrefix(), null);

	private String componentName;

	private String companyId;

	private List loadedSources = new ArrayList();

	private boolean baseConfigurationLoaded = false;

	public AggregatedProperties(String companyId, String componentName) {
		this.componentName = componentName;
		this.companyId = companyId;
	}

	/**
	 * Look for the property in environment, global and base configuration, in
	 * this order
	 * 
	 * @param key
	 * @return
	 */
	public Object getProperty(String key) {
		Object value = null;
		if (value == null) {
			// value = prefixedSystemConfiguration.getProperty(key);
			value = System.getProperty(getPrefix() + key);
		}
		if (value == null) {
			value = globalConf.getProperty(getPrefix() + key);
		}
		if (value == null) {
			value = globalConf.getProperty(key);
		}
		if (value == null) {
			value = baseConf.getProperty(key);
		}
		if (value == null) {
			value = super.getProperty(key);
		}
		if (value == null) {
			// value = systemConfiguration.getProperty(key);
			value = System.getProperty(key);
		}
		if ((value == null) && (key.equals(Conventions.COMPANY_ID_PROPERTY))) {
			value = companyId;
		}
		if ((value == null)
				&& (key.equals(Conventions.COMPONENT_NAME_PROPERTY))) {
			value = componentName;
		}
		return value;
	}

	private String getPrefix() {
		return componentName + Conventions.PREFIX_SEPARATOR;
	}

	public void addBaseFileName(String fileName) {
		Configuration conf = addPropertiesSource(fileName, baseConf);
		if ((conf != null) && (!conf.isEmpty())) {
			baseConfigurationLoaded = true;
		}
	}

	public void addGlobalFileName(String fileName) {
		addPropertiesSource(fileName, globalConf);
	}

	/**
	 * Read the given source of properties and add it to the composite
	 * configuration. The added configuration will be returned. If it is not
	 * found null will be returned.
	 */
	private Configuration addPropertiesSource(String sourceName,
			CompositeConfiguration loadedConf) {
		try {
			Configuration newConf;
			if (DatasourceURL.isDatasource(sourceName)) {
				newConf = addDatasourceProperties(sourceName);
			} else if (JndiURL.isJndi(sourceName)) {
				newConf = addJndiProperties(sourceName);
			} else if (isClass(sourceName)){
				newConf = addConfigurationClass(sourceName);
			} else {
				newConf = addFileProperties(sourceName, loadedConf);
			}
			if (newConf != null) {
				loadedConf.addConfiguration(newConf);
				super.addConfiguration(newConf);
				if (newConf instanceof AbstractFileConfiguration) {
					loadedSources.add(((AbstractFileConfiguration) newConf)
							.getURL().toString());
				} else {
					loadedSources.add(sourceName);
				}
			}
			return newConf;
		} catch (Exception ignore) {
			if (log.isDebugEnabled()) {
				log.debug("Configuration source " + sourceName + " ignored");
			}
			return null;
		}
	}

	private boolean isClass(String sourceName){
		return sourceName != null && sourceName.startsWith(Conventions.CLASS_PREFIX);
	}


	private Configuration addConfigurationClass(String sourceName) throws ConfigurationException {
		String className = sourceName.substring(Conventions.CLASS_PREFIX.length());
		Class c;
		try {
			c = ClasspathUtil.locateClass(className);
		} catch (ClassNotFoundException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Configuration source " + sourceName + " ignored", ex);
			}
			return null;
		}
		if(Configuration.class.isAssignableFrom(c)){
			Configuration newConf;
			try {
				newConf = (Configuration) c.newInstance();
				return newConf;
			} catch (Exception ex) {
				if (log.isWarnEnabled()) {
					log.warn("Configuration class " + c.getName() + " cannot be created!", ex);
				}
			}
		} else {
			if (log.isWarnEnabled()) {
				log.warn("Configuration class " + c.getName() + " doesn't implement Configuration interface!");
			}
		}
		return null;
	}

	private Configuration addFileProperties(String fileName,
			CompositeConfiguration loadedConf) throws ConfigurationException {
		try {
			FileConfiguration newConf = new PropertiesConfiguration(fileName);
			URL fileURL = newConf.getURL();
			log.debug("Adding file: " + fileURL);

			Long delay = getReloadDelay(loadedConf, newConf);
			if (delay != null) {
				FileChangedReloadingStrategy reloadingStrategy = new FileConfigurationChangedReloadingStrategy();
				if (log.isDebugEnabled()) {
					log.debug("File " + fileURL + " will be reloaded every "
							+ delay + " seconds");
				}
				long milliseconds = delay.longValue() * 1000;
				reloadingStrategy.setRefreshDelay(milliseconds);
				newConf.setReloadingStrategy(reloadingStrategy);
			}

			addIncludedPropertiesSources(newConf, loadedConf);
			return newConf;
		} catch (org.apache.commons.configuration.ConfigurationException e) {
			if (log.isDebugEnabled()) {
				log.debug("Configuration source " + fileName + " ignored");
			}
			return null;
		}
	}

	private Long getReloadDelay(CompositeConfiguration loadedConf,
			FileConfiguration newConf) {
		Long delay = newConf.getLong(Conventions.RELOAD_DELAY_PROPERTY, null);
		if (delay == null) {
			delay = loadedConf.getLong(Conventions.RELOAD_DELAY_PROPERTY, null);
		}
		return delay;
	}

private Configuration addDatasourceProperties(String datasourcePath) {
		DatasourceURL dsUrl = new DatasourceURL(datasourcePath, companyId, componentName, 
								DatasourceURL.PROPERTIES_TABLE);
		return dsUrl.getConfiguration();
	}	private Configuration addJndiProperties(String sourcePath) {
		JNDIConfiguration conf = null;
		JndiURL jndiUrl = new JndiURL(sourcePath, companyId, componentName);
		return jndiUrl.getConfiguration();
	}

	private void addIncludedPropertiesSources(Configuration newConf,
			CompositeConfiguration loadedConf) {
		CompositeConfiguration tempConf = new CompositeConfiguration();
		tempConf.addConfiguration(prefixedSystemConfiguration);
		tempConf.addConfiguration(newConf);
		tempConf.addConfiguration(systemConfiguration);
		tempConf.addProperty(Conventions.COMPANY_ID_PROPERTY, companyId);
		tempConf
				.addProperty(Conventions.COMPONENT_NAME_PROPERTY, componentName);
		String[] fileNames = tempConf
				.getStringArray(Conventions.INCLUDE_PROPERTY);
		for (int i = fileNames.length - 1; i >= 0; i--) {
			String iteratedFileName = fileNames[i];
			addPropertiesSource(iteratedFileName, loadedConf);
		}
	}

	public List loadedSources() {
		return loadedSources;
	}

	public boolean hasBaseConfiguration() {
		return baseConfigurationLoaded;
	}

	public String getComponentName() {
		return componentName;
	}
}
