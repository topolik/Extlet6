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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import com.germinus.easyconf.ComponentConfiguration;
import com.germinus.easyconf.ComponentProperties;
import com.germinus.easyconf.EasyConf;

/**
 * MBean wrapper for a ComponentConfiguration. <a
 * href="ComponentConfigurationDynamicMBean.java.html"> <b><i>View Source </i>
 * </b> </a>
 *
 * @author Alvaro Gonz�lez
 * @version $Revision: 1.4 $
 *
 */
public class ComponentConfigurationDynamicMBean implements DynamicMBean {
    /**
     * Name of the reloadConfiguration operation. Used when invoking
     * <code>invoke</code> method.
     */
    public static final String RELOAD_OPERATION_NAME = "reloadConfiguration";

    /**
     * Name of the newProperty operations. Used when invoking
     * <code>invoke</code> method.
     */
    public static final String NEW_PROPERTY_OPERATION_NAME = "newProperty";

    /**
     * Signature of the reloadConfiguration operation. Used when invoking
     * <code>invoke</code> method.
     */
    public static final String[] RELOAD_OPERATION_SIGNATURE = new String[0];

    /**
     * Signature of the newProperty operation with one parameter.
     * Used when invoking <code>invoke</code> method.
     */
    public static final String[] NEW_PROPERTY_OPERATION_SIGNATURE_1 = 
    	new String[] { String.class.toString() };

    /**
     * Signature of the newProperty operation with two parameters.
     * Used when invoking <code>invoke</code> method.
     */
    public static final String[] NEW_PROPERTY_OPERATION_SIGNATURE_2 = 
    	new String[] {String.class.toString(), Object.class.toString() };

    private static final String NEW_PROPERTY_OPERATION_DESCRIPTION_1 = 
    	"Add new property to the Configuration Component. " +
    	"The Param is the property name. Must not" +
		" exists another property with the same name.";

	private static final String NEW_PROPERTY_OPERATION_DESCRIPTION_2 = 
		NEW_PROPERTY_OPERATION_DESCRIPTION_1 +
		" The second param is the initial value of the new property";

	private static final String RELOAD_OPERATION_DESCRIPTION = 
		"Reloads the configuration";

	private static final String CONSTRUCTOR_DESCRIPTION_1 = 
		"Constructs new MBean with the configuration of the named Component";

	private String componentName;

	private ComponentConfiguration componentConfiguration;

	private MBeanAttributeInfo[] attributesInfo;

	private Map modifiedProperties;

	private Map newPropeties;

	private MBeanOperationInfo[] operationInfo;

	private MBeanConstructorInfo[] constructorsInfo;

	/**
	 * Create a MBean wraping some loaded ComponentConfiguration 
	 * @param component
	 */
	public ComponentConfigurationDynamicMBean
		(ComponentConfiguration component) {
		super();
		this.componentName=component.getComponentName();
		this.componentConfiguration=component;
		init();
	}

	/**
	 * Loads and wraps a ComponentConfiguration
	 * @param componentName
	 */
	public ComponentConfigurationDynamicMBean(String componentName) {
		super();
		this.componentName = componentName;
		this.componentConfiguration = EasyConf.getConfiguration(componentName);
		init();		
	}

	/**
	 * Some initializations common to both constructors
	 */
	private void init(){
		this.modifiedProperties = new HashMap();
		this.newPropeties = new HashMap();
	}

	/**
	 * Obtains the component attributes from the ComponentCongiguration
	 * 
	 * @see javax.management.DynamicMBean#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String attributeName)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		ComponentProperties prop = componentConfiguration.getProperties();
		if (modifiedProperties.containsKey(attributeName))
			return modifiedProperties.get(attributeName);
		if (prop.containsKey(attributeName))
			return prop.getProperty(attributeName);
		else if (newPropeties.containsKey(attributeName))
			return newPropeties.get(attributeName);
		else
			throw new AttributeNotFoundException(attributeName);
	}

	/**
	 * Sets an attribute
	 * 
	 * @see javax.management.DynamicMBean#setAttribute(javax.management.Attribute)
	 */
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		if (componentConfiguration.getProperties().containsKey(
				attribute.getName()))
			modifiedProperties.put(attribute.getName(), attribute.getValue());
		else if (newPropeties.containsKey(attribute.getName()))
			newPropeties.put(attribute.getName(), attribute.getValue());
		else
			throw new AttributeNotFoundException(attribute.getName());
	}

	/**
	 * List named attributes
	 * 
	 * @see javax.management.DynamicMBean#getAttributes(java.lang.String[])
	 */
	public AttributeList getAttributes(String[] attributesNames) {
		Set includedAttributeNames = new HashSet();
		AttributeList attributeList = new AttributeList();
		for (int i = 0; i < attributesNames.length; i++) {
			String attributeName = attributesNames[i];
			if (!includedAttributeNames.contains(attributeName)) {
				includedAttributeNames.add(attributeName);
				try {
					Object value = getAttribute(attributeName);
					Attribute attribute = new Attribute(attributeName, value);
					attributeList.add(attribute);
				} catch (AttributeNotFoundException e) {
					// Ignoring attribute
				} catch (MBeanException e) {
				} catch (ReflectionException e) {
				}
			}
		}
		return attributeList;
	}

	/**
	 * Set named attributes
	 * 
	 * @see javax.management.DynamicMBean#setAttributes(javax.management.AttributeList)
	 */
	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList modifieds = new AttributeList();
		Iterator it = attributes.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			try {
				setAttribute(attribute);
				modifieds.add(attribute);
			} catch (AttributeNotFoundException e) {
			} catch (InvalidAttributeValueException e) {
			} catch (MBeanException e) {
			} catch (ReflectionException e) {
			}
		}
		return modifieds;
	}

	/**
	 * Invoke one of the operations exposed by the MBeanas. 
	 * This Operations could be one of:
	 * <ul>
	 *   <li><code>void newProperty(String propertyName)</code></li>
	 *   <li>
	 *   	<code>void newProperty(String propertyName, String initialValue)
	 *   	</code>
	 *   </li>
	 *   <li><code>void reloadConfiguration()</code></li>
	 * </ul>
	 * 
	 * @see javax.management.DynamicMBean#invoke(java.lang.String,
	 *      java.lang.Object[], java.lang.String[])
	 */
	public Object invoke(String operationName, Object[] params,
			String[] signature) throws MBeanException, ReflectionException {
		if (operationName.equals(RELOAD_OPERATION_NAME)) {
			reloadConfiguration();
			return null;
		}
		if (operationName.equals(NEW_PROPERTY_OPERATION_NAME)) {
			if (signature.length == 1)
				newProperty((String) params[0]);
			else if (signature.length == 2)
				newProperty((String) params[0], params[1]);
			else
				throw new MBeanException(new IllegalArgumentException(
						"Operation not found in MBEan: " + operationName + "("
								+ signature + ")"));
			return null;
		}
		throw new MBeanException(new IllegalArgumentException(
				"Operation not found in MBEan: " + operationName + "("
						+ signature + ")"));
	}

	// ***** Implementations of the MBean's operations *****//

	private void reloadConfiguration() {
//		System.out.println("Old Properties: "+componentConfiguration.getProperties().toMap());
		EasyConf.refreshComponent(componentName);
		this.componentConfiguration = EasyConf.getConfiguration(componentName);
//		System.out.println("New Properties: "+componentConfiguration.getProperties().toMap());
	}

	private void newProperty(String propertyName) throws MBeanException {
		newProperty(propertyName, null);
	}

	private void newProperty(String propertyName, Object value)
			throws MBeanException {
		ComponentProperties prop = componentConfiguration.getProperties();
		if ((newPropeties.containsKey(propertyName))
				|| prop.containsKey(propertyName))
			throw new MBeanException(new IllegalArgumentException(
					"Cannot add new propert whith name: " + propertyName
							+ ". There is a property whith that name yet"));
		newPropeties.put(propertyName, value);
	}

	/**
	 * Return the Information exposed by the MBean: Attributes, Operations,
	 * Constructors and Notifications.
	 * 
	 * @see javax.management.DynamicMBean#getMBeanInfo()
	 */
	public MBeanInfo getMBeanInfo() {
		return new MBeanInfo(this.getClass().toString(), "Easyconf component: "
				+ this.componentName, getAttributeInfo(),
				getConsturctorsInfo(), getOperationInfo(), null);
	}

	/**
	 * Constructs the info of the MBean's attributes.
	 * 
	 * @return Array of MBeanAttributeInfo
	 */
	protected MBeanAttributeInfo[] getAttributeInfo() {
		if (attributesInfo == null) {
			ComponentProperties properties = componentConfiguration
					.getProperties();
			List auxList = new ArrayList();
			Iterator it = properties.getKeys();
			while (it.hasNext()) {
				String propertyName = (String) it.next();
				Object value = properties.getProperty(propertyName);
				String type = value.getClass().getName();
				boolean isIs = value.getClass().equals(Boolean.class);
				MBeanAttributeInfo attributeInfo = new MBeanAttributeInfo(
						propertyName, type, "Easyconf " + componentName
								+ " component property: " + propertyName, true,
						true, isIs);
				auxList.add(attributeInfo);
				this.attributesInfo = (MBeanAttributeInfo[]) auxList
						.toArray(new MBeanAttributeInfo[auxList.size()]);
			}
		}
		return this.attributesInfo;
	}

	/**
	 * Constructs the info of the MBean's operations.
	 * 
	 * @return Array of MBeanOperationInfo
	 */
	protected MBeanOperationInfo[] getOperationInfo() {
		if (operationInfo == null) {
			operationInfo = new MBeanOperationInfo[] {
					new MBeanOperationInfo(NEW_PROPERTY_OPERATION_NAME,
							NEW_PROPERTY_OPERATION_DESCRIPTION_1,
							// Parameters
							new MBeanParameterInfo[] { new MBeanParameterInfo(
									"propertyName", String.class.getName(),
									"Name of the new property") }, void.class
									.getName(), MBeanOperationInfo.ACTION),
					new MBeanOperationInfo(
							NEW_PROPERTY_OPERATION_NAME,
							NEW_PROPERTY_OPERATION_DESCRIPTION_2,
							// Parameters
							new MBeanParameterInfo[] {
									new MBeanParameterInfo("propertyName",
											String.class.getName(),
											"Name of the new property"),
									new MBeanParameterInfo("value",
											Object.class.getName(),
											"Initial value of the new property") },
							void.class.getName(), MBeanOperationInfo.ACTION),
					new MBeanOperationInfo(RELOAD_OPERATION_NAME,
							RELOAD_OPERATION_DESCRIPTION,
							// Parameters
							new MBeanParameterInfo[] {}, void.class.getName(),
							MBeanOperationInfo.ACTION) };

		}
		return operationInfo;
	}

	/**
	 * Constructs an array of the MBean's constructors.
	 * 
	 * @return Array of MBeanConstructorInfo
	 */
	protected MBeanConstructorInfo[] getConsturctorsInfo() {
		if (constructorsInfo == null) {
			Constructor constructor = null;
			try {
				constructor = this.getClass().getConstructor(
						new Class[] { String.class });
			} catch (Exception e) {
			}
			constructorsInfo = new MBeanConstructorInfo[] { new MBeanConstructorInfo(
					CONSTRUCTOR_DESCRIPTION_1, constructor) };
		}
		return constructorsInfo;
	}

	/**
	 * Returns the ComponentConfiguration associated with this MBean.
	 * 
	 * @return ComponentConfiguration.
	 */
	protected ComponentConfiguration getComponentConfiguration() {
		return componentConfiguration;
	}

	/**
	 * @param componentConfiguration
	 *            The componentConfiguration to set.
	 */
	protected void setComponentConfiguration(
			ComponentConfiguration componentConfiguration) {
		this.componentConfiguration = componentConfiguration;
	}

	private boolean propertyExists(String property) {
		if (componentConfiguration.getProperties().containsKey(property))
			return true;
		if (newPropeties.containsKey(property))
			return true;
		return false;
	}
}
