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
 * Thrown when the base properties file is not found and the getProperties
 * method is explicitly called in the configuration
 *
 * @author jferrer
 */
public class ConfigurationNotFoundException extends ConfigurationException {
    public ConfigurationNotFoundException(String componentName,
                                          String msg,
                                          Throwable thr) {
        super(componentName, msg, thr);
    }

    protected ConfigurationNotFoundException(String componentName) {
        super(componentName);
    }

    protected ConfigurationNotFoundException(String componentName,
                                             Throwable e) {
        super(componentName, e);
    }

    public ConfigurationNotFoundException(String componentName, String msg) {
        super(componentName, msg);
    }
}
