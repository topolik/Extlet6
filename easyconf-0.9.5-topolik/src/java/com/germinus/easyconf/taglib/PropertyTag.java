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
package com.germinus.easyconf.taglib;

import com.germinus.easyconf.EasyConf;
import com.germinus.easyconf.ComponentProperties;
import com.germinus.easyconf.Filter;

import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.JspException;
import org.apache.struts.util.RequestUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.ArrayList;


/**
 * Read a configuration property and expose it as a page variable and attribute
 * Examples of use:
 *
 * &gt;%@ taglib uri="/WEB-INF/tld/easyconf.tld" prefix="easyconf" %>
 *
 * &gt;easyconf:property id="registration_list"
 *                  component="registration"
 *                  property="registration.list"
 *                  type="java.util.List"/>
 * &gt;logic:iterate id="item" name="registration_list">
 *   &gt;bean:write name="item"/>    &gt;br/>
 * &gt;/logic:iterate>
 *
 * &gt;easyconf:property id="registration_disabled"
 *                  component="registration"
 *                  property="registration.disabled"/>
 * &gt;logic:equal name="registration_disabled" value="true">
 *   The registration is disabled
 * &gt;/logic:equal>
 * 
 * @jsp.tag name="property" body-content="empty" tei-class="com.germinus.easyconf.taglib.PropertyTei"
 */
public class PropertyTag extends BodyTagSupport {
	private static final long serialVersionUID = 3546082471134573881L;

	private static final String DEFAULT_TYPE = "java.lang.String";

    protected String id = null;
    protected String component = null;
    protected String property = null;
    protected String type = DEFAULT_TYPE;
    protected String selector1 = "";
    protected String selector2 = "";
    protected String selector3 = "";
    protected String selector4 = "";
    protected String selector5 = "";    
    protected String defaultValue;
    private static final List EMPTY_LIST = new ArrayList();

	
    public PropertyTag() {
		super();
		release();
	}

	/**
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public String getId() {
        return (this.id);
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getType() {
        if (StringUtils.isEmpty(type)) {
            type = DEFAULT_TYPE;
        }
        return (this.type);
    }

    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public void setType(String type) {
        if (StringUtils.isNotEmpty(type)) {
            this.type = type;
        }
    }


    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getDefaultValue() {
        return (this.defaultValue);
    }

    /**
     * Note: currently this is only used if type is String
     * @param defaultValue
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    
    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getSelector1() {
        return selector1;
    }
    public void setSelector1(String selector1) {
        this.selector1 = selector1;
    }
    
    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getSelector2() {
        return selector2;
    }
    public void setSelector2(String selector2) {
        this.selector2 = selector2;
    }

    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getSelector3() {
        return selector3;
    }
    public void setSelector3(String selector3) {
        this.selector3 = selector3;
    }

    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getSelector4() {
        return selector4;
    }
    public void setSelector4(String selector4) {
        this.selector4 = selector4;
    }

    /**
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getSelector5() {
        return selector5;
    }
    public void setSelector5(String selector5) {
        this.selector5 = selector5;
    }
    
    private String[] getSelectorArray() {
        List selectors = new ArrayList();
        if (StringUtils.isNotEmpty(selector1)) {
            selectors.add(selector1);
        }
        if (StringUtils.isNotEmpty(selector2)) {
            selectors.add(selector2);
        }
        if (StringUtils.isNotEmpty(selector3)) {
            selectors.add(selector3);
        }
        if (StringUtils.isNotEmpty(selector4)) {
            selectors.add(selector4);
        }
        if (StringUtils.isNotEmpty(selector5)) {
            selectors.add(selector5);
        }
        return (String[]) selectors.toArray(new String[0]);
    }
    // .................. Taglib methods ..................
    
    /**
     * Check if we need to evaluate the body of the tag
     *
     * @exception javax.servlet.jsp.JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        return (EVAL_BODY_BUFFERED);

    }


    /**
     * Save the body content of this tag (if any), or throw a JspException
     * if the value was already defined.
     *
     * @exception JspException if value was defined by an attribute
     */
    public int doAfterBody() throws JspException {
        return (SKIP_BODY);

    }


    /**
     * Retrieve the required property and expose it as a scripting variable.
     *
     * @exception JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        Object value = null;
        
        ComponentProperties conf = EasyConf.getConfiguration(component).
        	getProperties();
        value = readProperty(conf);

       
        if (value == null) {
            JspException e = new JspException("The value of the property is null");
            RequestUtils.saveException(pageContext, e);
            throw e;
        }
        pageContext.setAttribute(id, value);
        return (EVAL_PAGE);

    }

    private Object readProperty(ComponentProperties conf) throws JspException {
        Object value;
        if (getType().equals("java.util.List")) {
            value = conf.getList(property, getPropertyFilter(), EMPTY_LIST);
        } else if (getType().equals("java.lang.Integer")) {
            value = conf.getInteger(property, getPropertyFilter(), new Integer(0));
        } else if (getType().equals("java.lang.String[]")) {
            value = conf.getStringArray(property, getPropertyFilter(), new String[0]);
        } else if (getType().equals("java.lang.String")) {
            if (defaultValue != null) {
                value = conf.getString(property, getPropertyFilter(), defaultValue);
            } else {
                value = conf.getString(property, getPropertyFilter());
            }
        } else if (getType().equals("java.lang.Double")) {
            value = new Double(conf.getDouble(property, getPropertyFilter()));
        } else if (getType().equals("java.lang.Float")) {
            value = new Float(conf.getFloat(property, getPropertyFilter()));
        } else if (getType().equals("java.lang.Byte")) {
            value = new Byte(conf.getByte(property, getPropertyFilter()));
        } else if (getType().equals("java.math.BigDecimal")) {
            value = conf.getBigDecimal(property, getPropertyFilter());
        } else if (getType().equals("java.lang.BigInteger")) {
            value = conf.getBigInteger(property, getPropertyFilter());
        } else if (getType().equals("java.lang.Boolean")) {
            value = new Boolean(conf.getBoolean(property, getPropertyFilter()));
        } else if (getType().equals("java.lang.Short")) {
            value = new Short(conf.getShort(property, getPropertyFilter()));
        } else if (getType().equals("java.lang.Long")) {
            value = new Long(conf.getLong(property, getPropertyFilter()));
        } else {
                JspException e = new JspException("Unsupported type: " +type);
                RequestUtils.saveException(pageContext, e);
                throw e;
        }
        return value;
    }

    private Filter getPropertyFilter() {
        return Filter.by(getSelectorArray());
    }

    public void release() {
        super.release();
        id = null;
        component = null;
        property = null;
        type = null;
        defaultValue = null;
    }


}
