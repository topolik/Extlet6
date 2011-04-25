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
package com.germinus.easyconf.struts;

import org.apache.struts.action.*;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.germinus.easyconf.EasyConf;

/**
 * Refresh the configuration of a given component which uses EasyConf. If
 * no component is specified all components of the current JVM are refreshed.
 * @author jferrer
 */
public class RefreshConfigurationAction extends Action {
    private static final String SUCCESS = "SUCCESS";

    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest req,
                                 HttpServletResponse response) throws Exception {
        DynaActionForm dform = (DynaActionForm) form;
        if (dform != null) {
            String componentName = (String) dform.get("componentName");
            if (StringUtils.isBlank(componentName)) {
                EasyConf.refreshAll();
            } else {
                EasyConf.refreshComponent(componentName);
            }
        } else {
            EasyConf.refreshAll();
        }
        return mapping.findForward(SUCCESS);
    }
}
