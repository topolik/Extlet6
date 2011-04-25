/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.germinus.easyconf.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.germinus.easyconf.EasyConf;

/**
 * <a href="RefreshConfigurationServlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision$
 *
 */
public class RefreshConfigurationServlet extends HttpServlet {

    
    /* 
     * Refresh the configuration
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String componentName = request.getParameter("componentName");
        if (StringUtils.isBlank(componentName)) {
            EasyConf.refreshAll();
        } else {
            EasyConf.refreshComponent(componentName);
        }
        writeSuccessResponse(response, componentName);
    }

    protected void writeSuccessResponse(HttpServletResponse response, String componentName) 
    	throws IOException {
        String msg;
        if (StringUtils.isBlank(componentName)) {
            msg = "The configuration of " + componentName + " has been reloaded";
        } else {
            msg = "The configuration of all components has been reloaded";
        }
        StringBuffer html = new StringBuffer();
        html.append("<html><head><title>");
        html.append(msg);
        html.append("</title></head><body>");
        html.append("<p align='center'>");
        html.append(msg);
        html.append("</p></body></html>");
        response.getWriter().write(html.toString());        
    }
}
