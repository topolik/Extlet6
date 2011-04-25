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

import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * <a href="ConfigurationLearningTest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.4 $
 *
 */
public class ConfigurationLearningTest extends TestCase {
    

    public ConfigurationLearningTest(String testName) {
        super(testName);
    }
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ConfigurationLearningTest("testGetFromClasspath"));
        return suite;
    }
    public void testGetFromClasspath() throws ConfigurationException {
        URL result = ConfigurationUtils.locate(null, "test_module.properties");
        assertNotNull(result);
        PropertiesConfiguration conf = new PropertiesConfiguration();
        conf.load("test_module.properties");
        assertEquals("Error reading properties file from classpath",
                "test_module",
                conf.getString("property-only-in-test-module"));
    }
    
    public void testSubset() {
        Configuration conf = new PropertiesConfiguration();
        conf.setProperty("prefix.key", "value");
        Configuration subset = conf.subset("prefix");
        assertTrue("The subset functionality does not work", subset.containsKey("key"));
    }

}
