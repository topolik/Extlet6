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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * System Test of the whole functionality of easyconf.
 * Note that it depends on external files
 * 
 * @author Jorge Ferrer
 * @author Ismael F. Olmedo
 */
public class EasyConfTest extends TestCase {

	public static final Log log =  LogFactory.getLog(EasyConfTest.class);
	ComponentConfiguration componentConf;

    public EasyConfTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        componentConf = EasyConf.getConfiguration("test_module");
        System.setProperty("easyconf-environment", "local");
        System.setProperty("test_module:easyconf-environment", "local");
		getProperties().setThrowExceptionOnMissing(true);
    }

    protected void tearDown() throws Exception {
        System.setProperty("easyconf-environment", "BAD-BAD-BAD");
        System.setProperty("test_environment:easyconf-environment", "BAD-BAD-BAD");
		getProperties().setThrowExceptionOnMissing(false);
    }


    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(EasyConfTest.class);
//        suite.addTest(new EasyConfTest("testNamedConfiguration"));
        
        return suite;
    }

    public void testGetExistentClass() throws ClassNotFoundException {
        Class theClass = getProperties().getClass("database-configuration-class");
        assertEquals("An invalid class was loaded", 
                DatabaseConf.class, theClass);
        theClass = getProperties().getClass("database-configuration-class", Table.class);
        assertEquals("An invalid class was loaded", 
                DatabaseConf.class, theClass);
        theClass = getProperties().getClass("non-existent-property", Table.class);
        assertSame("The default class should have been used", 
                Table.class, theClass);
    }

    public void testGetNonexistentClass() {
        try {
            Class theClass = getProperties().getClass("non-existent-class");
            fail("A ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException success) {        
        }
        try {
            Class theClass = getProperties().getClass("non-existent-class", Table.class);
            fail("A ClassNotFoundException should have been thrown");
        } catch (ClassNotFoundException success) {        
        }
    }

    public void testGetClassArray() throws ClassNotFoundException {
        Class[] defaultClasses = new Class[]{Table.class};
        String key = "database-configuration-classes";
        Class[] result = getProperties().getClassArray(key);
        assertEquals("There should be two classes", 
                2, result.length);
        assertEquals("An invalid first class was loaded", 
                DatabaseConf.class, result[0]);
        assertEquals("An invalid second class was loaded", 
                Table.class, result[1]);
        result = getProperties().getClassArray(key, defaultClasses);
        assertEquals("The default should be ignored and there should be two classes", 
                2, result.length);
        result = getProperties().getClassArray("non-existent-property", defaultClasses);
        assertSame("The default class should have been used", 
                defaultClasses, result);
    }

    public void testAllFilesHaveBeenRead() {
    	assertTrue("File test_module.properties not loaded", 
    			getProperties().containsKey("property-only-in-test-module"));
    	assertTrue("File global-configuration.properties not loaded", 
    			getProperties().containsKey("property-only-in-global-configuration"));   	
    	assertTrue("File global-configuration-env.properties not loaded", 
    			getProperties().containsKey("property-only-in-env"));
    	assertTrue("File global-configuration-prj.properties not loaded", 
    			getProperties().containsKey("property-only-in-prj"));
    }
    public void testReadStringNotOverridden() {
    	assertEquals("Invalid value for string-not-overridden", 
    				 "test_module", 
					 getProperties().getString("string-not-overridden"));
    }
    
    /**
     * Expected property keys and values:
     * property-with-filter[selector1.selector2]
     * long-with-filter[selector1.selector2]=1234
     * short-with-filter[selector1.selector2]=1234
     * int-with-filter[selector1.selector2]=1234
     * byte-with-filter[selector1.selector2]=123
     * biginteger-with-filter[selector1.selector2]=1234
     * bigdecimal-with-filter[selector1.selector2]=1234
     * double-with-filter[selector1.selector2]=1234
     * float-with-filter[selector1.selector2]=1234
     * list-with-filter[selector1.selector2]=1234,5678
     * boolean-with-filter[selector1.selector2]=false
     */
    public void testFilterWithDefault() {
        assertEquals("Invalid string value when specifying two selectors", "selector1-and-selector2", 
					 getProperties().getString("property-with-filter", Filter.by("selector1", "selector2"), "defaultvalue"));
		assertEquals("Invalid string value when specifying two selectors but second does not exists", "selector1", 
				 getProperties().getString("property-with-filter", Filter.by("selector1", "non-existent-selector"), "defaultvalue"));
		assertEquals("Invalid string value when specifying two inexistent selectors but default value in file", "no-selector", 
				 getProperties().getString("property-with-filter", Filter.by("non-existent-selector", "non-existent-selector"), "defaultvalue"));
		assertEquals("Invalid string value when specifying two inexistent selectors", "defaultvalue", 
				 getProperties().getString("property-with-filter2", Filter.by("non-existent-selector", "non-existent-selector"), "defaultvalue"));
        assertEquals("Invalid long value when specifying two selectors", 1234, 
				 	  getProperties().getLong("long-with-filter", Filter.by("selector1", "selector2"), 0l));
        assertEquals("Invalid short value when specifying two selectors", 1234, 
			 	  getProperties().getShort("short-with-filter", Filter.by("selector1", "selector2"), (short)0));
        assertEquals("Invalid int value when specifying two selectors", 1234, 
			 	  getProperties().getInt("int-with-filter", Filter.by("selector1", "selector2"), 0));
        assertEquals("Invalid byte value when specifying two selectors", 123, 
			 	  getProperties().getByte("byte-with-filter", Filter.by("selector1", "selector2"), (byte)0));
        assertEquals("Invalid BigInteger value when specifying two selectors", new BigInteger("1234"), 
			 	  getProperties().getBigInteger("biginteger-with-filter", 
			 	          Filter.by("selector1", "selector2"), new BigInteger("0")));
        assertEquals("Invalid BigDecimal value when specifying two selectors", new BigDecimal(1234), 
			 	  getProperties().getBigDecimal("bigdecimal-with-filter", 
			 	          Filter.by("selector1", "selector2"), new BigDecimal(0d)));
        assertEquals("Invalid double value when specifying two selectors", 1234d, 
			 	  getProperties().getDouble("double-with-filter", Filter.by("selector1", "selector2"), 0), 0);
        assertEquals("Invalid float value when specifying two selectors", 1234f, 
			 	  getProperties().getFloat("float-with-filter", Filter.by("selector1", "selector2"), 0), 0);
        assertEquals("Invalid boolean value when specifying two selectors", false, 
			 	  getProperties().getBoolean("boolean-with-filter", Filter.by("selector1", "selector2"), true));
        assertEquals("Invalid list value when specifying two selectors", Arrays.asList(new String[] {"1234","5678"}), 
			 	  getProperties().getList("list-with-filter", Filter.by("selector1", "selector2"), null));
        assertEquals("Invalid string array value when specifying two selectors", new String[] {"1234","5678"}, 
			 	  getProperties().getStringArray("list-with-filter", Filter.by("selector1", "selector2"), null));
    }

    public void testFilterWithoutDefault() {
        try {
            getProperties().getString("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getLong("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getShort("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getInt("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getByte("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getBigInteger("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getBigDecimal("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getDouble("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getFloat("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getList("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
        try {
            getProperties().getStringArray("Inexistent property", Filter.by("selector1", "selector2"));
            fail("A NoSuchElementException should have been thrown");
        } catch (NoSuchElementException success) {}
    }
    public void testSetThrowExceptionOnMissingToFalse() {
        getProperties().setThrowExceptionOnMissing(false);
        try {
            getProperties().getString("Inexistent property",
                    Filter.by("selector1", "selector2"));
            assertTrue("A NoSuchElementException should NOT have been thrown",
                    true);
        } finally {
            getProperties().setThrowExceptionOnMissing(true);
        }
    }

    public void testReadStringOverriddenInPrj() {
    	assertEquals("Invalid value for string-overridden-in-prj", 
    				 "prj", 
					 getProperties().getString("string-overridden-in-prj"));
    }
    
    public void testReadStringOverriddenInPrjWithPrefix() {
    	assertEquals("Invalid value for string-overridden-in-prj-with-prefix",
    				 "prj",
					 getProperties().getString("string-overridden-in-prj-with-prefix"));
    }

    public void testReadStringOverriddenInEnv() {
    	assertEquals("Invalid value for string-overridden-in-env", 
    				 "env", 
					 getProperties().getString("string-overridden-in-env"));
    }
    
    public void testReadStringOverriddenInPrjAndEnv() {
    	assertEquals("Invalid value for string-overridden-in-prj-and-env", 
    				 "env", 
					 getProperties().getString("string-overridden-in-prj-and-env"));
    }
    
    public void testReadListNotOverridden() {
    	List expected = Arrays.asList(new String[]{"test_module1", "test_module2"});
		assertEquals("Invalid value for list-not-overridden", 
		        expected, 
		        getProperties().getList("list-not-overridden"));
    }
    
    public void testReadListOverriddenInPrj() {
    	List expected = Arrays.asList(new String[]{"prj1", "prj2"});
    	assertEquals("Invalid value for list-overridden-in-prj", 
    				expected, 
					getProperties().getList("list-overridden-in-prj"));
    }
   
    public void testDefaultConfigurationObject() {
        DatabaseConf configuration = getConfigurationObject();
		DatabaseAssert.assertContents(configuration);
        DatabaseConf dbConf = (DatabaseConf) configuration;
        assertEquals("Incorrect number of tables. The default configuration was not correclty read", 
        		2, dbConf.getTables().size());
    }
    
    public void testNamedConfigurationObject() {
    	DatabaseConf conf = (DatabaseConf) componentConf.getConfigurationObject("myname");
        DatabaseAssert.assertContents(getConfigurationObject());
        DatabaseConf dbConf = (DatabaseConf) conf;
        assertEquals("Incorrect number of tables. The named configuration was not correclty read", 
        		3, dbConf.getTables().size());
    }
    
    public void testVariablesInObjectConfiguration() {
        Table table1 = (Table) getConfigurationObject().getTables().get(0);
        assertEquals("The table type is not the one specified as a property",
                getProperties().getString("default.table.type"),
                table1.getTableType());
    }

    public void testComponentWithoutProperties() {
        ComponentConfiguration conf = EasyConf.getConfiguration("module_without_properties");
        try {
            assertNotNull("The properties should not be null",
                conf.getProperties());
            fail("An exception should have been thrown because the base file " +
                 "does not exist");
        } catch (ConfigurationNotFoundException ok) {
        }
        try {
            conf.getConfigurationObject();
        } catch (ConfigurationNotFoundException e) {
            fail("When the getProperties method is not called explicitly, " +
                 "the base properties file should not be mandatory");
        }

    }

    public void testComponentWithoutXML() {
        ComponentConfiguration conf = EasyConf.getConfiguration("module_without_xml");
        try {
            conf.getConfigurationObject();
            fail("A Configuration exception should have been thrown");
        } catch (ConfigurationException ok) {}
    }
    
    public void testComponentWithoutDigesterRules() {
        String name = "module_without_digesterRules";
        try {
            EasyConf.getConfiguration(name);
        } catch (DigesterRulesNotFoundException expected) {
            assertNotNull("The exception should contain the name of the missing file",
                    expected.getDigesterRulesFileName());
            assertEquals("Invalid component name in the exception",
                    name, expected.getComponentName());
        }
    }

    public void testUsingSystemProperties() {
//        System.setProperty("easyconf-environment", "local");
//        assertEquals("The environment was not correctly read from the system property",
//                     "local",
//                     getProperties().getString("test_module_environment"));
//        System.setProperty("easyconf-environment", "");
        
        System.setProperty("sysproperty-without-prefix-and-default-value", 
		                   "value-of-sysproperty-without-prefix-and-default-value");
        assertEquals("A system property without prefix should not be read if there is a default value",
        		     "defaultValue",
        			 getProperties().getString("sysproperty-without-prefix-and-default-value"));

        System.setProperty("sysproperty-without-prefix",
                           "value-of-sysproperty-without-prefix");
        assertEquals("The value of the sysproperty should be returned if there isn't a default value",
   		     "value-of-sysproperty-without-prefix",
   			 getProperties().getString("sysproperty-without-prefix"));
		
        System.setProperty("test_module:sysproperty-with-prefix-and-default-value", 
		"value-of-sysproperty-with-prefix");
        assertEquals("The value of the sysproperty should be returned if a prefix is used",
		            "value-of-sysproperty-with-prefix",
			         getProperties().getString("sysproperty-with-prefix-and-default-value"));

    }

    public void testUsingSystemPropertiesInIncludes() {
        assertContains("The file with a sysproperty in the name was not loaded",
                "test_module-local.properties", getProperties().getLoadedSources());
        assertContains("The file with a prefixed sysproperty in the name was not loaded",
                "test_module-local2.properties", getProperties().getLoadedSources());
                
    }

    public void testPropertiesASPModel() {
        String companyId = "exampleCompany";
        ComponentProperties props = EasyConf.getConfiguration(companyId, 
                "test_module").getProperties();
        assertContains("The company-specific file was not read",
                "test_module-"+companyId+Conventions.PROPERTIES_EXTENSION,
                props.getLoadedSources());
        assertEquals("The property was not read from the company specific file",
                companyId,
                props.getString("company-name"));
        assertEquals("The property should have the default value if it is not" +
        		" overridden by the company specific file",
        		"test_module",
        		props.getString("string-not-overridden"));
        assertEquals("The property was not read from the company specific global file",
                "exampleCompanyGlobal",
                props.getString("global-company-name"));
        
    }

    public void testJavaUtilPropertiesASPModel() {
        String companyId = "exampleCompany";
        Properties props = EasyConf.getConfiguration(companyId, 
                "test_module").getProperties().getProperties();
        assertEquals("The property was not read from the company specific file",
                companyId,
                props.getProperty("company-name"));
        assertEquals("The property should have the default value if it is not" +
        		" overridden by the company specific file",
        		"test_module",
        		props.getProperty("string-not-overridden"));
        assertEquals("The property was not read from the company specific global file",
                "exampleCompanyGlobal",
                props.getProperty("global-company-name"));
        
    }
	public void testXMLASPModel() {
        DatabaseConf conf = (DatabaseConf) EasyConf.
        		getConfiguration("exampleCompany", "test_module").
        		getConfigurationObject();
        assertEquals("The company specific conf should have only 1 table",
                1, conf.getTables().size());        
    }
    
    public void testSpecifyingVariables() {
        String result = getProperties().getString("property-with-variable", 
                Filter.usingVariables("exampleVariable", "my-value"));
        assertEquals("The variable has not been substituted successfully",
                "my-value",
                result);
    }
    
    public void testSettingPropertyAsDefaultVariableValue() {
        String result = getProperties().getString("property-with-variable");
        assertEquals("The variable has not been substituted successfully",
                "default-exampleVariable-value",
                result);    	
    }

    public void testSetProperty() {
        getProperties().setProperty("new-property", "new-value");
        assertEquals("The property has not been stored",
                "new-value",
                getProperties().getProperty("new-property"));
        getProperties().setProperty("string-not-overridden", "new-value");
        assertEquals("The new value for an existent property has not been stored",
                "new-value",
                getProperties().getProperty("string-not-overridden"));
    }
	
	public void testStringListAsString() {
		String value = getProperties().getString("database-configuration-classes");
		assertEquals("Invalid string value",
				"com.germinus.easyconf.DatabaseConf,com.germinus.easyconf.Table",
				value);
	}

	public void testNumberListAsString() {
		String value = getProperties().getString("some-numbers");
		assertEquals("Invalid string value",
				"1,2,3,4,5",
				value);
	}

	/**
     * Does not work due to a bug in digester (TODO: confirm)
     */
    public void bugtestXmlThatUsesNonExistentProperty() {
    	ComponentConfiguration p2;
    	
        String name = "module_with_xml_that_uses_non_existent_property";
        try {
        	ComponentConfiguration conf = EasyConf.getConfiguration(name);
        } catch (InvalidPropertyException expected) {
            assertEquals("Invalid component name in the exception",
                    name, expected.getComponentName());
        }
        
    }

    // .............. Helper methods ...................

    ComponentProperties getProperties() {
        return componentConf.getProperties();
    }

    DatabaseConf getConfigurationObject() {
        Object configurationObject = componentConf.getConfigurationObject();
        assertEquals("Invalid configurationObject class",
        		DatabaseConf.class, configurationObject.getClass());
		return (DatabaseConf) configurationObject;
    }

    private void assertEquals(String msg, String[] expected, String[] obtained) {
        if (expected.length != obtained.length) {
            fail(msg + ". Expected and obtained arrays length differ");
        }
        for (int i = 0; i < expected.length; i++) {
            assertEquals(msg + ". (" + i + "th element)", expected[i], obtained[i]);
        }
    }
    
    
    private void assertContains(String msg, String item, List list) {
        boolean contained = list.contains(item);
        if (!contained) {
            for (Iterator it = list.iterator(); it.hasNext();) {
                String url = (String) it.next();
                if (url.endsWith("/" + item)) {
                    contained = true;
                    break;
                }
            }
        }
        assertTrue(msg + ". " + item + " is not included in " + list,
                contained);
    }

}
