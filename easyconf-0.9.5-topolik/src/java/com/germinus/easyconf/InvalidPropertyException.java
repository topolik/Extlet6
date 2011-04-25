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
 * Thrown when an XML configuration file contains a variable which either
 * is not of the form <code>${variableName}</code> or is not
 * defined in any of the properties files associated with its component
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.2 $
 *
 */
public class InvalidPropertyException extends ConfigurationException {

    public InvalidPropertyException(String componentName, Throwable e) {
        super(componentName, e);
    }

    public String getMessage() {
        return super.getMessage() + ". A variable is either malformed or " +
        		"makes a reference to " +
        		"a property which is not defined for this component in " +
        		"any of its configuration files.";
    }
}
