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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;

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
        String pluginWebInfDir = pluginWebDir + "WEB-INF/ext-web/docroot/WEB-INF";
        String zipName = portalWebDir + "WEB-INF/lib/ext-" + servletContextName + "-webinf.jar";


        /** Zip the contents of the directory, and save it in the zipfile */
        // Check that the directory is a directory, and get its contents
        File d = new File(pluginWebInfDir);
        if (!d.isDirectory()) {
            throw new IllegalArgumentException("Not a directory:  "
                    + pluginWebInfDir);
        }
        String[] entries = d.list();
        byte[] buffer = new byte[4096]; // Create a buffer for copying
        int bytesRead;

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName));
        try {
            for (int i = 0; i < entries.length; i++) {
                File f = new File(d, entries[i]);
                if (f.isDirectory()) {
                    continue;//Ignore directory
                }

                if (ExtRegistry.isMergedFile(f.getName())) {
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

		CopyTask.copyDirectory(
			pluginWebDir + "WEB-INF/ext-web/docroot", portalWebDir,
			StringPool.BLANK, "**/WEB-INF/*", true, false);

		CopyTask.copyDirectory(
			pluginWebDir + "WEB-INF/ext-web/docroot/WEB-INF", portalWebDir + "WEB-INF/classes",
			"**/WEB-INF/classes/*", StringPool.BLANK, true, false);


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

	private static Log _log = LogFactoryUtil.getLog(ExtHotDeployListener.class);
}