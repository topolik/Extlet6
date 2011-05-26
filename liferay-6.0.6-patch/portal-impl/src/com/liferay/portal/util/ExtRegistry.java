/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.util;

import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import java.io.File;
import java.util.Arrays;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;

/**
 * @author Brian Wing Shun Chan
 */
public class ExtRegistry {

	public static Set<String> getFiles(String servletContextName){
		return Collections.unmodifiableSet(_extMap.get(servletContextName).getFiles());
	}

	public static void unregisterExt(String servletContextName){
		_extMap.remove(servletContextName);
	}

	public static Map<String, Set<String>> getConflicts(
			ServletContext servletContext)
		throws Exception {

		String servletContextName = servletContext.getServletContextName();

		Set<String> files = _readExtFiles(
			servletContext, "/WEB-INF/ext-" + servletContextName + ".xml");

		Map<String, Set<String>> conflicts = new HashMap<String, Set<String>>();

		for(ExtRegistryInfo regInfo : _extMap.values()){
			String curServletContextName = regInfo.getServletContext().getServletContextName();
			Set<String> curFiles = regInfo.getFiles();

			for (String file : files) {
				if (!curFiles.contains(file)) {
					continue;
				}

				Set<String> conflictFiles = conflicts.get(
					curServletContextName);

				if (conflictFiles == null) {
					conflictFiles = new TreeSet<String>();

					conflicts.put(curServletContextName, conflictFiles);
				}

				conflictFiles.add(file);
			}
		}

		return conflicts;
	}

	public static Set<String> getServletContextNames() {
		return Collections.unmodifiableSet(_extMap.keySet());
	}
	public static Set<ServletContext> getServletContexts() {
		Set<ServletContext> result = new HashSet<ServletContext>(_extMap.size());
		for(ExtRegistryInfo info : _extMap.values()){
			result.add(info.getServletContext());
		}
		return Collections.unmodifiableSet(result);
	}

	public static ServletContext getServletContext(String servletContextName) {
		if(!isRegistered(servletContextName)){
			return null;
		}
		return _extMap.get(servletContextName).getServletContext();
	}

	public static boolean isRegistered(String servletContextName) {
		if (_extMap.containsKey(servletContextName)) {
			return true;
		}
		else {
			return false;
		}
	}

	public static void updateRegisteredServletContext(ServletContext ctx){
		String servletContextName = ctx.getServletContextName();
		if(isRegistered(servletContextName)){
			_extMap.get(servletContextName).setServletContext(ctx);
		}
	}

	public static void registerExt(ServletContext servletContext)
		throws Exception {

		String servletContextName = servletContext.getServletContextName();

		Set<String> files = _readExtFiles(
			servletContext, "/WEB-INF/ext-" + servletContextName + ".xml");

		_extMap.put(servletContextName, new ExtRegistryInfo(servletContext, files));
	}

	public static void registerPortal(ServletContext servletContext)
		throws Exception {

		Set<String> resourcePaths = servletContext.getResourcePaths(
			"/WEB-INF");

		for (String resourcePath : resourcePaths) {
			if (resourcePath.startsWith("/WEB-INF/ext-") &&
				resourcePath.endsWith("-ext.xml")) {

				String servletContextName = resourcePath.substring(
					13, resourcePath.length() - 4);

				Set<String> files = _readExtFiles(
					servletContext, resourcePath);

				_extMap.put(servletContextName, new ExtRegistryInfo(null, files));
			}
		}
	}

	private static Set<String> _readExtFiles(
			ServletContext servletContext, String resourcePath)
		throws Exception {

		Set<String> files = new TreeSet<String>();

		Document document = SAXReaderUtil.read(
			servletContext.getResourceAsStream(resourcePath));

		Element rootElement = document.getRootElement();

		Element filesElement = rootElement.element("files");

		List<Element> fileElements = filesElement.elements("file");

		for (Element fileElement : fileElements) {
			String fileName = fileElement.getText();
			if (!isMergedFile(fileName)) {
				files.add(fileName);
			}
		}

		return files;
	}

	public static boolean isMergedFile(String name) {
		for (String mergedFile : SUPPORTED_MERGING_FILES) {
			if (name.contains(mergedFile)) {
				return true;
			}
		}
		return false;
	}

	private static Map<String, ExtRegistryInfo> _extMap =
		new HashMap<String, ExtRegistryInfo>();
	public static final List<String> SUPPORTED_MERGING_FILES = 
		Arrays.asList(new String[]{
			"tiles-defs-ext.xml",
			"struts-config-ext.xml",
			"ext-model-hints.xml",
			"ext-spring.xml",
			"ext-hbm.xml",
			"liferay-portlet-ext.xml",
			"liferay-look-and-feel-ext.xml",
			"liferay-layout-templates-ext.xml",
			"portlet-ext.xml",
			"liferay-display-ext.xml",
			"remoting-servlet-ext.xml",
			"portal-log4j-ext.xml",
			"log4j.dtd",
			"content"+File.separator+"Language-ext",
			"ext-orm.xml",
			"web.xml",
			"service.xml",
			"portal-ext.properties",
			"sql"+File.separator,
			"html"+File.separator+"js"+File.separator+"liferay"+
				File.separator+"service.js"
		});
	public static final List<String> EXT_PLUGIN_JARS_GLOBAL_CL = 
		Arrays.asList(new String[]{
			"ext-service"
		});
	public static final List<String> EXT_PLUGIN_JARS_PORTAL_CL = 
		Arrays.asList(new String[]{
			"ext-impl",
			"ext-util-bridges",
			"ext-util-java",
			"ext-util-taglib"
		});
}

class ExtRegistryInfo {
	private ServletContext servletContext;
	private Set<String> files;

	public ExtRegistryInfo(ServletContext servletContext, Set<String> files) {
		this.servletContext = servletContext;
		this.files = files;
	}

	public Set<String> getFiles() {
		return files;
	}

	public void setFiles(Set<String> files) {
		this.files = files;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

}

