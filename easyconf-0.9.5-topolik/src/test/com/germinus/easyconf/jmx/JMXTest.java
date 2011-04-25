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
package com.germinus.easyconf.jmx;

import java.io.File;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.germinus.easyconf.ComponentConfiguration;
import com.germinus.easyconf.Conventions;
import com.germinus.easyconf.EasyConf;
import com.germinus.easyconf.FileUtil;
import com.germinus.easyconf.jmx.ComponentConfigurationDynamicMBean;

import junit.framework.TestCase;

/**
 * <a href="JMXTest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Alvaro Gonzalez
 * @version $Revision: 1.5 $
 *
 */
public class JMXTest extends TestCase {
	private static final String CONFIGURATION_DIR = "target/test-classes";
	private static final String JMX_NAME_PREFIX = "easyconf:component=";
	private static final String JMX_TEST_MODULE = "jmx_module";
	private static final String TEST_MODULE = "test_module";
	private static final String RELOAD_MODULE = "jmx_reload_module";
	private static final String STRING_NOT_OVERRIDEN = "string-not-overridden";
	private static final String RELOADED_PROPERTY = "reloaded";
	private static final String RELOADED_PROPERTY_VALUE1 = "beforeReloaded";
	private static final String RELOADED_PROPERTY_VALUE2 = "afterReloaded";
	private static final String STRING_NOT_OVERRIDEN_VALUE = "jmx_module";
	private static final String STRING_OVERRIDEN_IN_PRJ = "string-overridden-in-prj";
	private static final String STRING_OVERRIDEN_IN_PRJ_VALUE = "prj";
	private static final String NON_EXISTENT_ATTRIBUTE = "com.germinus.easyconf.JMXTest.NON_EXISTENT_ATTRIBUTE";
	private static final String NEW_ATTRIBUTE = "com.germinus.easyconf.JMXTest.NEW_ATTRIBUTE";
	private static final String WHATEVER_VALUE = "WHATEVER";
	private MBeanServer mbeanServer;
	private ObjectName testMBeanName;
	private ObjectName testMBeanName2;
	private ObjectName testMBeanName3;
	private File toReloadFile;
	
		
	
	public void testDynamicMBeanGetAttribute() throws Exception{		
		try {			
			Object attribute=mbeanServer.getAttribute(testMBeanName,STRING_NOT_OVERRIDEN);
			assertNotNull("Attribute not retrieved properly. Is null.",attribute);
			String attributeString=(String)attribute;
			assertEquals("Incorrect attribute value.",STRING_NOT_OVERRIDEN_VALUE,attributeString);
		} catch (AttributeNotFoundException e2) {
			fail("Attribute not found");
		} catch (InstanceNotFoundException e2) {
			fail("Mbean not found");
		} catch (MBeanException e2) {
			throw e2.getTargetException();
		}
	}
	
	public void testDynamicMBeanGetAttributes() throws Exception{
		AttributeList attributes=mbeanServer.getAttributes(testMBeanName,new String[]{STRING_NOT_OVERRIDEN});
		assertEquals("Incorrect size of AttributeList.",1,attributes.size());
		Attribute attribute=(Attribute)attributes.iterator().next();
		assertAttribute("Attribute obtained form \"getAttributes\" doesn't match expected.",STRING_NOT_OVERRIDEN,STRING_NOT_OVERRIDEN_VALUE,attribute);
		attributes=mbeanServer.getAttributes(testMBeanName,new String[]{STRING_NOT_OVERRIDEN,STRING_OVERRIDEN_IN_PRJ});
		assertEquals("Incorrect size of AttributeList.",2,attributes.size());
		attribute=(Attribute)attributes.get(1);
		assertAttribute("Attribute obtained form \"getAttributes\" doesn't match expected.",STRING_OVERRIDEN_IN_PRJ,STRING_OVERRIDEN_IN_PRJ_VALUE,attribute);
		attributes=mbeanServer.getAttributes(testMBeanName,new String[]{STRING_NOT_OVERRIDEN,STRING_NOT_OVERRIDEN});
		assertEquals("Incorrect size of AttributeList with one attribute name repeated.",1,attributes.size());
		attribute=(Attribute)attributes.get(0);
		assertAttribute("Attribute obtained form \"getAttributes\" with one attribute name repeated doesn't match expected.",STRING_NOT_OVERRIDEN,STRING_NOT_OVERRIDEN_VALUE,attribute);
	}
	
	public void testDynamicMBeanSetAttribute() throws Exception{
		final String STRING_NOT_OVERRIDEN_NEW_VALUE="jmx_module_new_value";
		Attribute oldAttribute=new Attribute(STRING_NOT_OVERRIDEN,STRING_NOT_OVERRIDEN_NEW_VALUE);
		mbeanServer.setAttribute(testMBeanName,oldAttribute);
		Object newValue=mbeanServer.getAttribute(testMBeanName,STRING_NOT_OVERRIDEN);
		assertEquals("Incorrect new attribute value.",STRING_NOT_OVERRIDEN_NEW_VALUE,newValue);
		Attribute newAttribute=new Attribute(NON_EXISTENT_ATTRIBUTE,"whatever");
		try {
			mbeanServer.setAttribute(testMBeanName,newAttribute);
			fail("Must throw an exception when trying to set a inexistent property");
		}  catch (AttributeNotFoundException e) {			
		}
		oldAttribute=new Attribute(STRING_NOT_OVERRIDEN,null);
		mbeanServer.setAttribute(testMBeanName,oldAttribute);
		newValue=mbeanServer.getAttribute(testMBeanName,STRING_NOT_OVERRIDEN);
		assertNull("New value of property might be null",newValue);		
	}
		
	public void testDynamicMBeanAddProperty() throws Exception{				
		mbeanServer.invoke(
				testMBeanName,
				ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_NAME,
				new Object[]{NON_EXISTENT_ATTRIBUTE},
				ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_SIGNATURE_1);
		Attribute newAttribute=new Attribute(NON_EXISTENT_ATTRIBUTE,WHATEVER_VALUE);
		try {
			mbeanServer.setAttribute(testMBeanName,newAttribute);
		} catch (AttributeNotFoundException e){
			fail("Property not added correctly");
		}
		Attribute actuallyNewPorperty=(Attribute)
			mbeanServer.getAttributes(testMBeanName,new String[]{NON_EXISTENT_ATTRIBUTE}).get(0);
		assertAttribute("Incorrect value for new property",NON_EXISTENT_ATTRIBUTE,WHATEVER_VALUE,actuallyNewPorperty);
		mbeanServer.invoke(
				testMBeanName,
				ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_NAME,
				new Object[]{NEW_ATTRIBUTE,WHATEVER_VALUE},
				ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_SIGNATURE_2);
		Object value=mbeanServer.getAttribute(testMBeanName,NEW_ATTRIBUTE);
		assertEquals("New Property value is incorrect with two arguments operation",WHATEVER_VALUE,value);		
		try {
			mbeanServer.invoke(
					testMBeanName,
					ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_NAME,
					new Object[]{STRING_NOT_OVERRIDEN},
					ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_SIGNATURE_1);
			fail("Adding existing property must throw an exception");
		} catch (Exception e1) {			
		} 
		try {
			mbeanServer.invoke(
					testMBeanName,
					ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_NAME,
					new Object[]{STRING_NOT_OVERRIDEN,WHATEVER_VALUE},
					ComponentConfigurationDynamicMBean.NEW_PROPERTY_OPERATION_SIGNATURE_2);
			fail("Adding existing property must throw an exception");
		} catch (Exception e1) {			
		}
		value=mbeanServer.getAttribute(testMBeanName,STRING_NOT_OVERRIDEN);
		assertEquals("Adding an existing property must not change its value",STRING_NOT_OVERRIDEN_VALUE,value);		
	}
	
	public void testDynamicMBeanOperationInfo() throws Exception{
		MBeanInfo info=mbeanServer.getMBeanInfo(testMBeanName);
		MBeanOperationInfo[] operations=info.getOperations();
		assertEquals("Incorrect number of operations returned by getMBeanInfo",3,operations.length);
	}
	
	public void testDynamicMBeanReload() throws Exception{
//		Properties props=new Properties();
//		props.setProperty(RELOADED_PROPERTY,RELOADED_PROPERTY_VALUE2);
//		boolean b=toReloadFile.delete();
//		assertEquals("No se pudo borrar",true,b);
//		setReloadedFile(props);
//		try {
//			mbeanServer.invoke(testMBeanName3,
//					ComponentConfigurationDynamicMBean.RELOAD_OPERATION_NAME,
//					new Object[]{},new String[]{});
//		} catch (InstanceNotFoundException e) {
//			fail("Reloaded module MBEan not found");
//		} catch (MBeanException e) {
//			fail("Mbean problem: "+e);
//		} catch (ReflectionException e) {
//			fail("reflection problem: "+e);
//		}
//		try {			
//			Object attribute=mbeanServer.getAttribute(testMBeanName3,RELOADED_PROPERTY);
//			assertNotNull("Attribute not retrieved properly. Is null.",attribute);
//			String attributeString=(String)attribute;
//			assertEquals("Incorrect reloaded attribute value.",RELOADED_PROPERTY_VALUE2,attributeString);
//		} catch (AttributeNotFoundException e2) {
//			fail("Attribute not found");
//		} catch (InstanceNotFoundException e2) {
//			fail("Mbean not found");
//		} catch (MBeanException e2) {
//			throw e2.getTargetException();
//		}
	}
	
	protected void assertAttribute(String attributeName,Object attributeValue,Attribute attribute){
		assertAttribute(null,attributeName,attributeValue,attribute);
	}
	
	protected void assertAttribute(String message,String attributeName,Object attributeValue,Attribute attribute){
		String actuallyName=attribute.getName();
		Object actuallyValue=attribute.getValue();
		assertEquals(message+" Incorrect Name",attributeName,actuallyName);
		assertEquals(message+" Incorrect Value",attributeValue,actuallyValue);
	}
	

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {		
		mbeanServer=MBeanServerFactory.newMBeanServer();
		Properties props = new Properties();
        props.setProperty(RELOADED_PROPERTY, RELOADED_PROPERTY_VALUE1);
		setReloadedFile(props);
		registerMBeans();		
	}
	
	protected void setReloadedFile(Properties props) throws Exception{		
		toReloadFile = new File(CONFIGURATION_DIR+ "/" + RELOAD_MODULE + ".properties");                        
        FileUtil.write(toReloadFile, props);		
	}
	
	protected void registerMBeans() {
		
		ComponentConfiguration componentConfiguration =
			EasyConf.getConfiguration(JMX_TEST_MODULE);		
		ComponentConfigurationDynamicMBean confMBean = 
			new ComponentConfigurationDynamicMBean(componentConfiguration);
		ComponentConfigurationDynamicMBean confMBean2 = 
			new ComponentConfigurationDynamicMBean(JMX_TEST_MODULE);
		ComponentConfigurationDynamicMBean confMBean3 = 
			new ComponentConfigurationDynamicMBean(RELOAD_MODULE);
		assertNotNull("MBean not created properly with ComponentConfiguration constructor", confMBean);
		assertNotNull("MBean not created properly with String constructor", confMBean2);
		assertNotNull("Error creating ComponentConfiguration", confMBean2
				.getComponentConfiguration());		
		assertNotNull("MBean not created properly with ComponentConfiguration.", confMBean3);
		try {
			testMBeanName = new ObjectName(JMX_NAME_PREFIX + "jmx_module");
		} catch (MalformedObjectNameException e1) {
			fail("Malformed JMX name: " + JMX_NAME_PREFIX + "jmx_module");
		}
		try {
			testMBeanName2 = new ObjectName(JMX_NAME_PREFIX + "test_module");
		} catch (MalformedObjectNameException e1) {
			fail("Malformed JMX name: " + JMX_NAME_PREFIX + "test_module");
		}
		try {
			testMBeanName3 = new ObjectName(JMX_NAME_PREFIX + RELOAD_MODULE);
		} catch (MalformedObjectNameException e1) {
			fail("Malformed JMX name: " + JMX_NAME_PREFIX + RELOAD_MODULE);
		}
		assertNotNull("JMX ObjectName not created porperly", testMBeanName);
		assertNotNull("JMX ObjectName not created porperly", testMBeanName2);
		assertNotNull("JMX ObjectName not created porperly", testMBeanName3);
		try {
			ObjectInstance instance = mbeanServer.registerMBean(confMBean,
					testMBeanName);
			assertNotNull("Object Instance not created properly", instance);
		} catch (InstanceAlreadyExistsException e) {
		} catch (MBeanRegistrationException e) {
			fail("Mbean not registerd properly");
		} catch (NotCompliantMBeanException e) {
			fail("Not Mbean compliant(confMBean): " + e.getLocalizedMessage());
		}
		try {
			ObjectInstance instance = mbeanServer.registerMBean(confMBean2,
					testMBeanName2);
			assertNotNull("Object Instance not created properly", instance);
		} catch (InstanceAlreadyExistsException e) {
		} catch (MBeanRegistrationException e) {
			fail("Mbean not registerd properly");
		} catch (NotCompliantMBeanException e) {
			fail("Not Mbean compliant(confMBean2): " + e.getLocalizedMessage());
		}
		try {
			ObjectInstance instance = mbeanServer.registerMBean(confMBean3,
					testMBeanName3);
			assertNotNull("Object Instance not created properly", instance);
		} catch (InstanceAlreadyExistsException e) {
		} catch (MBeanRegistrationException e) {
			fail("Mbean not registerd properly");
		} catch (NotCompliantMBeanException e) {
			fail("Not Mbean compliant(confMBean2): " + e.getLocalizedMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		this.toReloadFile.delete();
	}
}
