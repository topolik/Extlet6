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
 * Thrown when an XML configuration file for a requested component exists
 * but there is not a file which defines de digester rules which should be
 * used to parse it 
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.2 $
 *
 */
public class DigesterRulesNotFoundException extends ConfigurationException {

    private String digesterRulesFileName;
    public DigesterRulesNotFoundException(String componentName, String digesterRulesFileName) {
        super(componentName);
        this.digesterRulesFileName = digesterRulesFileName;
    }

    public String getDigesterRulesFileName() {
        return digesterRulesFileName;
    }
    
    public String getMessage() {
        return super.getMessage() + ". There should be a file named " + 
        	digesterRulesFileName + " which should contain the digester rules " +
        	"for parsing the XML file";
    }
}
