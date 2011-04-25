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

import org.apache.commons.configuration.Configuration;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Métodos de utilidad para trabajar con parámetros de configuración que representan
 * clases
 * Fecha: 09-jul-2004 -- 12:06:34
 * @author Jesús Jáimez Rodríguez <jesusjaimez@germinus.com>
 */
public class ClassParameter {

    private static final Log log = LogFactory.getLog(ClassParameter.class);

    public static Object getNewInstance(Configuration conf, String propertyName)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        String className = conf.getString(propertyName);
        log.info("Returning " + className + " class instance.");
        return ClasspathUtil.locateClass(className).newInstance();
    }

    public static Object getNewInstance(Properties props, String propertyName)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        String className = props.getProperty(propertyName);
        log.info("Returning " + className + " class instance.");
        return ClasspathUtil.locateClass(className).newInstance();
    }
}