<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="/WEB-INF/tld/easyconf.tld" prefix="easyconf" %>

<easyconf:configurationObject id="cmsConfig"
                   component="xpression-cms"
                   type="com.germinus.linde.cms.ContentTypeDefinitions"/>

<easyconf:property id="advertisement_enabled"
                   component="xpression-ui"
                   property="login.advertisement.enabled"/>

<html>
  <head><title>EasyConf taglibs: usage examples</title></head>
  <body>
  <ul>
    <li>configuration object: <%=cmsConfig%></li>
    <li>advertisement enabled? <%=advertisement_enabled%></li>
  </ul>

</body>
</html>