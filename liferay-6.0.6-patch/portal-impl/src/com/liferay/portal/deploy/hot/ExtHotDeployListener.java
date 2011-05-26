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
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.HttpUtil;
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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import org.apache.commons.configuration.PropertiesConfiguration;
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

		if (_redeployServletContextNames.contains(servletContextName) && 
			ExtRegistry.isRegistered(servletContextName)) {

			if (_log.isInfoEnabled()) {
				_log.info(
					"Redeploying Ext Plugin for " + servletContextName);
			}
			uninstallExt(servletContextName);
			if (_log.isInfoEnabled()) {
				_log.info(
					"Ext Plugin " + servletContextName + " has been undeployed.");
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Registering extension environment for " + servletContextName);
		}

		if (ExtRegistry.isRegistered(servletContextName)) {
			ExtRegistry.updateRegisteredServletContext(servletContext);
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

		try {
			installExt(servletContext, event.getContextClassLoader());
		} catch (Exception e) {
			// rollback
			uninstallExt(servletContextName);

			throw e;
		}


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

		if(ExtRegistry.isRegistered(servletContextName)){
			_redeployServletContextNames.add(servletContextName);
			if (_log.isInfoEnabled()) {
				_log.info(
					"Redeploying Ext Plugin for"
						+ servletContextName +
						" ... waiting for deploy");
			}
		} else {
			_log.error(
				"Ext Plugin for " + servletContextName +
					" is not registered!");
		}
	}

	protected void installExt(
			ServletContext servletContext, ClassLoader portletClassLoader)
		throws Exception {

		String servletContextName = servletContext.getServletContextName();

		String portalWebDir = PortalUtil.getPortalWebDir();
		String pluginWebDir = WebDirDetector.getRootDir(portletClassLoader);

		ExtRegistry.registerExt(servletContext);

		installJars(servletContext);
		installWebInfJar(portalWebDir, pluginWebDir, servletContextName);
		installWebFiles(portalWebDir, pluginWebDir, servletContextName);

		rebuildPortalExtPluginProperties();
		rebuildServiceJS();
		rebuildWebXml();

		FileUtil.copyFile(
			pluginWebDir + "WEB-INF/ext-" + servletContextName + ".xml",
			portalWebDir + "WEB-INF/ext-" + servletContextName + ".xml");
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
	protected void installJars(ServletContext servletContext)
			throws Exception {
		String globalLibDir = PortalUtil.getGlobalLibDir();
		String portalLibDir = PortalUtil.getPortalLibDir();

		for (String jarName : ExtRegistry.EXT_PLUGIN_JARS_GLOBAL_CL) {
			copyJar(servletContext, globalLibDir, jarName);
		}
		for (String jarName : ExtRegistry.EXT_PLUGIN_JARS_PORTAL_CL) {
			copyJar(servletContext, portalLibDir, jarName);
		}
	}
	protected void installWebFiles(
		String portalWebDir, String pluginWebDir,
		String servletContextName) throws Exception {

		HookHotDeployListener hookListener = new HookHotDeployListener();
		Set<String> files = ExtRegistry.getFiles(servletContextName);
		for(String file : files){
			if(file.startsWith("ext-web/docroot/") &&
				!ExtRegistry.isMergedFile(file)) {

				String relativeFile = file.substring(
					file.indexOf("docroot/") + "docroot/".length());

				File portalFile = new File(portalWebDir, relativeFile);
				File pluginFile = new File(pluginWebDir + "WEB-INF/", file);

				File hookPortalFile = hookListener.getPortalJspBackupFile(portalFile);
				if(hookPortalFile.exists()){
					resetPortalFileBackup(portalFile.getAbsolutePath(), hookPortalFile.getAbsolutePath());
					if (_log.isDebugEnabled()) {
						_log.debug("Copying [from, to]: [" + pluginFile + ", " + hookPortalFile + "]");
					}
					FileUtil.copyFile(pluginFile, hookPortalFile);
				} else {
					resetPortalFileBackup(portalFile.getAbsolutePath());
					if (_log.isDebugEnabled()) {
						_log.debug("Copying [from, to]: [" + pluginFile + ", " + portalFile + "]");
					}
					FileUtil.copyFile(pluginFile, portalFile);
				}
			}
		}
	}
	protected void installWebInfJar(String portalWebDir, String pluginWebDir, String servletContextName)
			throws Exception {
		String zipName = portalWebDir + "WEB-INF/lib/ext-" + servletContextName + "-webinf" + ".jar";

		File dir = new File(pluginWebDir + "WEB-INF/ext-web/docroot/WEB-INF");
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Not a directory: " + dir);
		}

		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return ExtRegistry.isMergedFile(pathname.getPath());
			}
		});

		zipWebInfJar(zipName, files);
	}

	protected void rebuildWebXml() {
		String portalWebDir = PortalUtil.getPortalWebDir();
		resetPortalFileBackup(portalWebDir + "WEB-INF/web.xml");

		Set<ServletContext> ctxs = ExtRegistry.getServletContexts();
		for (ServletContext servletContext : ctxs) {
			String pluginWebXML = servletContext.getRealPath("WEB-INF/ext-web/docroot/WEB-INF/web.xml");
			if (!FileUtil.exists(pluginWebXML)) {
				if (_log.isDebugEnabled()) {
					_log.debug("Ext Plugin's web.xml not found for " + servletContext.getServletContextName());
				}
				return;
			}
			if (_log.isDebugEnabled()) {
				_log.debug("Rebuilding portal's web.xml using " + pluginWebXML);
			}

			rebuildWebXml(pluginWebXML);
		}
	}


	protected void rebuildWebXml(String pluginWebXMLFileName) {
		String portalWebDir = PortalUtil.getPortalWebDir();
		String tmpDir =
			SystemProperties.get(SystemProperties.TMP_DIR) + StringPool.SLASH +
				Time.getTimestamp();

		WebXMLBuilder.main(
			new String[] {
				portalWebDir + "WEB-INF/web.xml",
				pluginWebXMLFileName,
				tmpDir + "/web.xml"
			});

		File portalWebXml = new File(portalWebDir + "WEB-INF/web.xml");
		File tmpWebXml = new File(tmpDir + "/web.xml");

		tmpWebXml.setLastModified(portalWebXml.lastModified());

		File originalWebXml = new File(portalWebDir + "WEB-INF", "web.xml.original");
		FileUtil.copyFile(portalWebXml, originalWebXml);
		originalWebXml.setLastModified(portalWebXml.lastModified());

		CopyTask.copyFile(
			tmpWebXml, new File(portalWebDir + "WEB-INF"), true, true);

		FileUtil.deltree(tmpDir);
	}



	protected void rebuildPortalExtPluginProperties() throws Exception {
		File extPluginPropsFile = new File(PortalUtil.getPortalWebDir() + "WEB-INF/classes/portal-ext-plugin.properties");
		extPluginPropsFile.delete();
		extPluginPropsFile.createNewFile();
		Set<ServletContext> ctxs = ExtRegistry.getServletContexts();
		for (ServletContext servletContext : ctxs) {
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
			rebuildPortalExtPluginProperties(pluginPropsURL);
		}
	}

	private void rebuildPortalExtPluginProperties(URL pluginPropsURL) throws Exception {
		PropertiesConfiguration pluginProps = new PropertiesConfiguration(pluginPropsURL);

		PropertiesConfiguration portalProps = new PropertiesConfiguration(this.getClass().getClassLoader().getResource("portal.properties"));

		File extPluginPropsFile = new File(PortalUtil.getPortalWebDir() + "WEB-INF/classes/portal-ext-plugin.properties");
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

	protected void rebuildServiceJS() throws Exception {
		String portalWebDir = PortalUtil.getPortalWebDir();
		resetPortalFileBackup(portalWebDir + "html/js/liferay/service.js");

		for (ServletContext servletContex : ExtRegistry.getServletContexts()) {
			rebuildServiceJS(portalWebDir, servletContex);
		}
	}

	private void rebuildServiceJS(String portalWebDir, ServletContext servletContext) throws Exception {
		URL pluginJSURL = servletContext.getResource("WEB-INF/ext-web/docroot/html/js/liferay/service.js");
		if (pluginJSURL == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("Ext Plugin's service.js not found for "
					+ servletContext.getServletContextName());
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

	private void resetPortalFileBackup(String portalFileName) {
		resetPortalFileBackup(portalFileName, portalFileName);
	}
	private void resetPortalFileBackup(String portalFileName, String currentFileName) {
		File backupFile = new File(portalFileName + BACKUP_EXT);
		File currentFile = new File(currentFileName);
		if(!currentFile.exists()){
			return;
		}
		if (!backupFile.exists()) {
			FileUtil.copyFile(currentFile, backupFile);
			backupFile.setLastModified(currentFile.lastModified());
		} else {
			FileUtil.copyFile(backupFile, currentFile);
			currentFile.setLastModified(backupFile.lastModified());
		}
	}

	protected void uninstallExt(String servletContextName) throws Exception {
		uninstallJars(servletContextName);
		uninstallWebInfJar(servletContextName);
		uninstallWebFiles(servletContextName);

		ExtRegistry.unregisterExt(servletContextName);

		rebuildPortalExtPluginProperties();
		rebuildServiceJS();
		rebuildWebXml();

		FileUtil.delete(PortalUtil.getPortalWebDir() +
			"WEB-INF/ext-" + servletContextName + ".xml");
	}

	protected void uninstallJars(String servletContextName)
			throws Exception {
		String globalLibDir = PortalUtil.getGlobalLibDir();
		String portalLibDir = PortalUtil.getPortalLibDir();

		for (String jarName : ExtRegistry.EXT_PLUGIN_JARS_GLOBAL_CL) {
			removeJar(servletContextName, globalLibDir, jarName);
		}
		for (String jarName : ExtRegistry.EXT_PLUGIN_JARS_PORTAL_CL) {
			removeJar(servletContextName, portalLibDir, jarName);
		}
	}
	protected void removeJar(
			String servletContextName, String dir, String jarName)
			throws Exception {

		String newJarFullName =
				dir + "ext-" + servletContextName + jarName.substring(3) + ".jar";

		FileUtil.delete(newJarFullName);
	}
	protected void uninstallWebFiles(String servletContextName) {
		String portalWebDir = PortalUtil.getPortalWebDir();
				HookHotDeployListener hookListener = new HookHotDeployListener();

		Set<String> files = ExtRegistry.getFiles(servletContextName);
		for(String file : files){
			if(file.startsWith("ext-web/docroot/") &&
				!ExtRegistry.isMergedFile(file)) {

				String relativeFile = file.substring(
					file.indexOf("docroot/") + "docroot/".length());

				File portalFile = new File(portalWebDir, relativeFile);
				File hookPortalFile = hookListener.getPortalJspBackupFile(portalFile);
				if(hookPortalFile.exists()){
					resetPortalFileBackup(portalFile.getAbsolutePath(), hookPortalFile.getAbsolutePath());
				} else {
					resetPortalFileBackup(portalFile.getAbsolutePath());
					if(!FileUtil.exists(portalFile + BACKUP_EXT)){
						FileUtil.delete(portalFile + BACKUP_EXT);
					}
				}
			}
		}
	}
	protected void uninstallWebInfJar(String servletContextName) throws Exception {
		String portalLibDir = PortalUtil.getPortalLibDir();
		removeJar(servletContextName, portalLibDir, "ext-webinf");
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


	private static Log _log = LogFactoryUtil.getLog(ExtHotDeployListener.class);
	private static List<String> _redeployServletContextNames =
			new ArrayList<String>();
	private static String BACKUP_EXT = ".beforeExt";
}
