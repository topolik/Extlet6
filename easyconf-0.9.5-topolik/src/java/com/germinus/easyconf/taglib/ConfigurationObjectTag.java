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
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.JspException;
import org.apache.struts.util.RequestUtils;



/**
 * Read a configuration property and expose it as a page variable and attribute
 * Examples of use:
 *
 * &gt;%@ taglib uri="/WEB-INF/tld/easyconf.tld" prefix="easyconf" %>
 *
 * &gt;easyconf:configurationObject id="dbConf"
 *                  component="test_module"
 *                  type="com.germinus.easyconf.example.DatabaseConf"/>
 * &gt;bean:write name="dbConf" property="tables"/>
 * 
 * @jsp.tag name="configurationObject" body-content="empty" 
 *          tei-class="com.germinus.easyconf.taglib.ConfigurationObjectTei"
 */
public class ConfigurationObjectTag extends BodyTagSupport {

    protected String id = null;
    protected String component = null;
    protected String type = "java.lang.Object";

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
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        this.type = type;
    }

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
        Object confObj = EasyConf.getConfiguration(component).
        	getConfigurationObject();
       
        if (confObj == null) {
            JspException e = new JspException("The value of the configuration object is null. " +
            		"Check the configuration files");
            RequestUtils.saveException(pageContext, e);
            throw e;
        }
        pageContext.setAttribute(id, confObj);
        return (EVAL_PAGE);
    }

    public void release() {
        super.release();
        id = null;
        component = null;
        type = null;
    }


}
