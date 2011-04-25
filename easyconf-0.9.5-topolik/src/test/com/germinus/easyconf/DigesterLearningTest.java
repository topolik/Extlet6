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

import java.io.IOException;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.xml.sax.SAXException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * <a href="DigesterLearningTest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.3 $
 *
 */
public class DigesterLearningTest extends TestCase {
    public DigesterLearningTest(String testName) {
        super(testName);
    }
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DigesterLearningTest("testXmlRulesDigester"));
        return suite;
    }
    public void testXmlRulesDigester() throws ClassNotFoundException, IOException, SAXException {
        URL digesterRulesUrl = ClasspathUtil.locateResource("test_module.digesterRules.xml");
        Digester digester = DigesterLoader.createDigester(digesterRulesUrl);
        
        Object configuration = readConfig(digester);
        DatabaseAssert.assertContents(configuration);
        DatabaseConf dbConf = (DatabaseConf) configuration;
        assertEquals("Incorrect number of tables. The XML file was not read correctly",
        		2, dbConf.getTables().size());
    }


    public void testProgramaticDigester() throws ClassNotFoundException, IOException, SAXException {
        Digester digester = new Digester();
        digester.addObjectCreate("database", "com.germinus.easyconf.Databases");
        digester.addObjectCreate("database/tables/table", "com.germinus.easyconf.Table");
        digester.addSetProperties("database/tables/table");
        digester.addSetNext("database/tables/table", "addTable", "com.germinus.easyconf.Table");
        
        Object configuration = readConfig(digester);

        DatabaseAssert.assertContents(configuration);
        DatabaseConf dbConf = (DatabaseConf) configuration;
        assertEquals("Incorrect number of tables. The XML file was not read correclty", 
        		2, dbConf.getTables().size());
    }
    
    // .......................... Helper methods .........................
    
    private Object readConfig(Digester digester) throws IOException, SAXException {
        Object configuration;
        digester.setUseContextClassLoader(true);
        digester.setValidating(false);
        URL confFile = ClasspathUtil.locateResource(null, "test_module.xml");
        assertNotNull("Configuration file not found", confFile);
        configuration = digester.parse(confFile.openStream());
        return configuration;
    }
}
