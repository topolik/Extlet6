/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.deploy.hot;

import com.liferay.portal.kernel.deploy.hot.BaseHotDeployListener;
import com.liferay.portal.kernel.deploy.hot.HotDeployEvent;
import com.liferay.portal.kernel.deploy.hot.HotDeployException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.WebDirDetector;
import com.liferay.portal.kernel.servlet.taglib.FileAvailabilityUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.tools.WebXMLBuilder;
import com.liferay.portal.util.ExtRegistry;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.SystemProperties;
import com.liferay.util.ant.CopyTask;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.wsdl.extensions.ExtensionRegistry;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.UrlResource;

/**
 * @author Brian Wing Shun Chan
 */
public class ExtHotDeployListener extends BaseHotDeployListener {

	public void invokeDeploy(HotDeployEvent event) throws HotDeployException {
		try {
			doInvokeDeploy(event);
		}
		catch (Throwable t) {
			throwHotDeployException(
				event, "Error registering extension environment for ", t);
		}
	}

	public void invokeUndeploy(HotDeployEvent event) throws HotDeployException {
		try {
			doInvokeUndeploy(event);
		}
		catch (Throwable t) {
			throwHotDeployException(
				event, "Error unregistering extension environment for ", t);
		}
	}

	protected void copyJar(
			ServletContext servletContext, String dir, String jarName)
		throws Exception {

		String servletContextName = servletContext.getServletContextName();

		String jarFullName = "/WEB-INF/" + jarName + "/" + jarName + ".jar";

		InputStream is = servletContext.getResourceAsStream(jarFullName);

		if (is == null) {
			throw new HotDeployException(jarFullName + " does not exist");
		}

		String newJarFullName =
			dir + "ext-" + servletContextName + jarName.substring(3) + ".jar";

		StreamUtil.transfer(is, new FileOutputStream(new File(newJarFullName)));
	}

	protected void createWebInfJar(String portalWebDir, String pluginWebDir, String servletContextName)
			throws Exception {
		String zipName = portalWebDir + "WEB-INF/lib/ext-" + servletContextName + "-webinf.jar";

		File dir = new File(pluginWebDir + "WEB-INF/ext-web/docroot/WEB-INF");
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Not a directory:  " + dir);
		}

		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return ExtRegistry.isMergedFile(pathname.getPath());
			}
		});
		
		zipWebInfJar(zipName, files);
	}

	private void zipWebInfJar(String zipName, File[] files) throws Exception{
		byte[] buffer = new byte[4096]; // Create a buffer for copying
		int bytesRead;

		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName));
		try {
			for (int i = 0; i < files.length; i++) {
				File f = files[i];
				if (f.isDirectory()) {
					continue;
				}

				String fileName = "WEB-INF/" + f.getName();
				FileInputStream in = new FileInputStream(f); // Stream to read file
				try {
					ZipEntry entry = new ZipEntry(fileName); // Make a ZipEntry
					out.putNextEntry(entry); // Store entry
					while ((bytesRead = in.read(buffer)) != -1) {
						out.write(buffer, 0, bytesRead);
					}
				} finally {
					in.close();
				}
			}
		} finally {
			out.close();
		}
	}

	protected void installExt(
			ServletContext servletContext, ClassLoader portletClassLoader)
		throws Exception {

		String servletContextName = servletContext.getServletContextName();

		String globalLibDir = PortalUtil.getGlobalLibDir();
		String portalWebDir = PortalUtil.getPortalWebDir();
		String portalLibDir = PortalUtil.getPortalLibDir();
		String pluginWebDir = WebDirDetector.getRootDir(portletClassLoader);

		for(String jarName : ExtRegistry.EXT_PLUGIN_JARS_GLOBAL_CL){
			copyJar(servletContext, globalLibDir, jarName);
		}
		for(String jarName : ExtRegistry.EXT_PLUGIN_JARS_PORTAL_CL){
			copyJar(servletContext, portalLibDir, jarName);
		}

		mergeWebXml(portalWebDir, pluginWebDir);

		mergePortalProperties(portalWebDir, servletContext);

		copyWebDirectory(pluginWebDir+"WEB-INF/ext-web/docroot/", portalWebDir);

		mergeServiceJS(portalWebDir, servletContext);

		createWebInfJar(portalWebDir, pluginWebDir, servletContextName);

		FileUtil.copyFile(
			pluginWebDir + "WEB-INF/ext-" + servletContextName + ".xml",
			portalWebDir + "WEB-INF/ext-" + servletContextName + ".xml");

		ExtRegistry.registerExt(servletContext);
	}

	protected void doInvokeDeploy(HotDeployEvent event) throws Exception {
		ServletContext servletContext = event.getServletContext();

		String servletContextName = servletContext.getServletContextName();

		if (_log.isDebugEnabled()) {
			_log.debug("Invoking deploy for " + servletContextName);
		}

		String xml = HttpUtil.URLtoString(
			servletContext.getResource(
				"/WEB-INF/ext-" + servletContextName + ".xml"));

		if (xml == null) {
			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Registering extension environment for " + servletContextName);
		}

		if (ExtRegistry.isRegistered(servletContextName)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Extension environment for " + servletContextName +
						" has been applied.");
			}

			return;
		}

		Map<String, Set<String>> conflicts = ExtRegistry.getConflicts(
			servletContext);

		if (!conflicts.isEmpty()) {
			StringBundler sb = new StringBundler();

			sb.append(
				"Extension environment for " + servletContextName +
					" cannot be applied because of detected conflicts:");

			Iterator<Map.Entry<String, Set<String>>> itr =
				conflicts.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry<String, Set<String>> entry = itr.next();

				String conflictServletContextName = entry.getKey();
				Set<String> conflictFiles = entry.getValue();

				sb.append("\n\t");
				sb.append(conflictServletContextName);
				sb.append(":");

				for (String conflictFile : conflictFiles) {
					sb.append("\n\t\t");
					sb.append(conflictFile);
				}
			}

			_log.error(sb.toString());

			return;
		}

		installExt(servletContext, event.getContextClassLoader());

		FileAvailabilityUtil.reset();

		if (_log.isInfoEnabled()) {
			_log.info(
				"Extension environment for " + servletContextName +
					" has been applied. You must reboot the server and " +
						"redeploy all other plugins.");
		}
	}

	protected void doInvokeUndeploy(HotDeployEvent event) throws Exception {
		ServletContext servletContext = event.getServletContext();

		String servletContextName = servletContext.getServletContextName();

		if (_log.isDebugEnabled()) {
			_log.debug("Invoking undeploy for " + servletContextName);
		}

		String xml = HttpUtil.URLtoString(
			servletContext.getResource(
				"/WEB-INF/ext-" + servletContextName + ".xml"));

		if (xml == null) {
			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Extension environment for " +
					servletContextName + " will not be undeployed");
		}
	}

	protected void mergeWebXml(String portalWebDir, String pluginWebDir) {
		if (!FileUtil.exists(
				pluginWebDir + "WEB-INF/ext-web/docroot/WEB-INF/web.xml")) {

			return;
		}

		String tmpDir =
			SystemProperties.get(SystemProperties.TMP_DIR) + StringPool.SLASH +
				Time.getTimestamp();

		WebXMLBuilder.main(
			new String[] {
				portalWebDir + "WEB-INF/web.xml",
				pluginWebDir + "WEB-INF/ext-web/docroot/WEB-INF/web.xml",
				tmpDir + "/web.xml"
			});

		File portalWebXml = new File(portalWebDir + "WEB-INF/web.xml");
		File tmpWebXml = new File(tmpDir + "/web.xml");

		tmpWebXml.setLastModified(portalWebXml.lastModified());

		CopyTask.copyFile(
			tmpWebXml, new File(portalWebDir + "WEB-INF"), true, true);

		FileUtil.deltree(tmpDir);
	}

	protected void mergePortalProperties(String portalWebDir, ServletContext servletContext) throws Exception {
		URL pluginPropsURL = servletContext.getResource("WEB-INF/ext-web/docroot/WEB-INF/classes/portal-ext.properties");
		if (pluginPropsURL == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("Ext Plugin's portal-ext.properties not found");
			}
			return;
		}
		if (_log.isDebugEnabled()) {
			_log.debug("Loading portal-ext.properties from " + pluginPropsURL);
		}
		PropertiesConfiguration pluginProps = new PropertiesConfiguration(pluginPropsURL);

		PropertiesConfiguration portalProps = new PropertiesConfiguration(this.getClass().getClassLoader().getResource("portal.properties"));

		File extPluginPropsFile = new File(portalWebDir + "WEB-INF/classes/portal-ext-plugin.properties");
		PropertiesConfiguration extPluginPortalProps = new PropertiesConfiguration();
		if (extPluginPropsFile.exists()) {
			extPluginPortalProps.load(extPluginPropsFile);
		}

		for (Iterator it = pluginProps.getKeys(); it.hasNext();) {
			String key = (String) it.next();
			List value = pluginProps.getList(key);
			if (key.endsWith("+")) {
				key = key.substring(0, key.length() - 1);
				List newValue = new ArrayList();
				if (extPluginPortalProps.containsKey(key)) {
					// already rewrited
					newValue.addAll(extPluginPortalProps.getList(key));
				} else {
					newValue.addAll(portalProps.getList(key));
				}

				newValue.addAll(value);
				extPluginPortalProps.setProperty(key, newValue);
			} else {
				extPluginPortalProps.setProperty(key, value);
			}
		}

		extPluginPortalProps.save(extPluginPropsFile);
	}

	protected void mergeServiceJS(String portalWebDir, ServletContext servletContext) throws Exception {
		URL pluginJSURL = servletContext.getResource("WEB-INF/ext-web/docroot/html/js/liferay/service.js");
		if (pluginJSURL == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("Ext Plugin's service.js not found");
			}
			return;
		}
		if (_log.isDebugEnabled()) {
			_log.debug("Loading service.js from " + pluginJSURL);
		}
		// append
		FileOutputStream portalJS = new FileOutputStream(portalWebDir + "html/js/liferay/service.js", true);
		try {
			InputStream pluginJS = new UrlResource(pluginJSURL).getInputStream();
			try {
				byte[] buff = new byte[4096];
				int len = 0;
				portalJS.write(new byte[]{13, 10});
				while((len = pluginJS.read(buff)) != -1){
					portalJS.write(buff, 0, len);
				}
				portalJS.write(new byte[]{13, 10});
			} finally {
				pluginJS.close();
			}
		} finally {
			portalJS.close();
		}
		
	}

	protected void copyWebDirectory(String srcDir, String destDir) throws Exception{
		// list all files except merged files
		File[] files = new File(srcDir).listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return ! ExtRegistry.isMergedFile(pathname.getPath());
			}
		});
		// copy files recursively
		for (File file : files) {
			if (file.isDirectory()) {
				copyWebDirectory(file.getPath(), destDir + File.separator + file.getName());
			} else {
				CopyTask.copyFile(file, new File(destDir), true, false);
			}
		}
	}


	protected void removeJar(
			String servletContextName, String dir, String jarName)
			throws Exception {

		String newJarFullName =
				dir + "ext-" + servletContextName + jarName.substring(3) + ".jar";

		FileUtil.delete(newJarFullName);
	}

	protected void resetPortalWebFiles(
		String servletContextName, String portalWebDir) {

		Set<String> files = ExtRegistry.getFiles(servletContextName);
		for(String file : files){
			if(file.startsWith("ext-web/docroot/") && !file.equals(
				"ext-web/docroot/WEB-INF/web.xml")) {

				String relativeFile = file.substring(
					file.indexOf("docroot/") + "docroot/".length());

				File portalFile = new File(portalWebDir, relativeFile);
				File portalBackupFile = new File(
					portalWebDir, relativeFile + ".backup");

				FileUtil.delete(portalFile);
				if(portalBackupFile.exists()){
					FileUtil.move(portalFile, portalBackupFile);
				}
				FileUtil.move(portalBackupFile, portalFile);
			}
		}
	}

	protected void resetWebXml(String servletContextName, String portalWebDir) {
		Set<String> files = ExtRegistry.getFiles(servletContextName);
		if (!files.contains("ext-web/docroot/WEB-INF/web.xml")) {
			return;
		}

		File originalWebXml = new File(portalWebDir + "WEB-INF/web.xml.original");
		File portalWebXml = new File(portalWebDir + "WEB-INF", "web.xml");
		FileUtil.copyFile(originalWebXml, portalWebXml);
		portalWebXml.setLastModified(originalWebXml.lastModified());
	}

	protected void uninstallExt(String servletContextName) throws Exception {

		String globalLibDir = PortalUtil.getGlobalLibDir();
		String portalWebDir = PortalUtil.getPortalWebDir();
		String portalLibDir = PortalUtil.getPortalLibDir();

		removeJar(servletContextName, globalLibDir, "ext-service");
		removeJar(servletContextName, portalLibDir, "ext-impl");
		removeJar(servletContextName, portalLibDir, "ext-util-bridges");
		removeJar(servletContextName, portalLibDir, "ext-util-java");
		removeJar(servletContextName, portalLibDir, "ext-util-taglib");

		resetWebXml(servletContextName, portalWebDir);

		resetPortalWebFiles(servletContextName, portalWebDir);

		FileUtil.delete(
				portalWebDir + "WEB-INF/ext-" + servletContextName + ".xml");

		ExtRegistry.unregisterExt(servletContextName);
	}

	private static Log _log = LogFactoryUtil.getLog(ExtHotDeployListener.class);
	private static List<String> _redeployServletContextNames =
			new ArrayList<String>();

}
