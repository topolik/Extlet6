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
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.configuration.DatabaseConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents the URL to a datasource as specified in a properties file
 * @author  Jorge Ferrer
 */
public class DatasourceURL {

	private static final Log log = LogFactory.getLog(DatasourceURL.class);
    private static final String DATASOURCE_PREFIX = Conventions.DATASOURCE_PREFIX;
    public static final String CONFIGURATION_OBJECTS_TABLE = Conventions.CONFIGURATION_OBJECTS_TABLE;
    public static final String PROPERTIES_TABLE = Conventions.PROPERTIES_TABLE;
    private static InitialContext ctx = null;
    private String dataSourceName;
    private String companyId;
    private String componentName;
	private String tableName;

    public DatasourceURL(String datasourcePath, String companyId, 
    		String componentName, String tableName) {
        this.dataSourceName = datasourcePath.substring(DATASOURCE_PREFIX.length());
        this.companyId = companyId;
        this.componentName = componentName;
        this.tableName = tableName;
    }

    public DataSource getDatasource() {        
        try {
        	DataSource ds = null;
        	String[] dsFinders = {
        			"java:/comp/env/" + dataSourceName,
        			dataSourceName,
        	};
        	for (int i = 0; i < dsFinders.length; i++) {
        		try {
        			ds = (DataSource)getContext().lookup(dsFinders[i]);
        			break;
        		} catch (NameNotFoundException e) {
        			if (log.isDebugEnabled()) {
        				log.debug("Datasource " + dataSourceName + " not found", e);
        			}
        		}
        	}
        	if (ds == null) {
        		throw new ConfigurationException("Cannot find datasource: " + dataSourceName);
        	}
        	return ds;
        } catch (NamingException e) {
            throw new ConfigurationException("Error loading datasource " + dataSourceName);
        }
    }

    private synchronized InitialContext getContext() throws NamingException {
        if (ctx == null) {
            ctx = new InitialContext();
        }
        return ctx;
    }

    protected String getTableName() {
    	return tableName;
    }

    protected String getComponentColumnName() {
        return "component";
    }

    protected String getKeyColumnName() {
        return "key";
    }

    protected String getValueColumnName() {
        return "value";
    }

    public static boolean isDatasource(String fileName) {
    	if (fileName == null) return false;

    	return fileName.startsWith(DATASOURCE_PREFIX);
    }

    public DatabaseConfiguration getConfiguration() {
        return new DatabaseConfiguration(getDatasource(),
                getTableName(),
                getComponentColumnName(),
                getKeyColumnName(),
                getValueColumnName(),
                getCompanyComponentValue());
    }

	private String getCompanyComponentValue() {
		if (companyId != null) {
			return companyId + ":" + componentName;
		} else {
			return componentName;
		}
	}
}
