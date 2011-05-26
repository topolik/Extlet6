/**
 * Liferay, Inc. All rights reserved.
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

package com.liferay.portal.service.impl;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.image.SpriteProcessorUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.plugin.PluginPackage;
import com.liferay.portal.kernel.plugin.Version;
import com.liferay.portal.kernel.servlet.ServletContextUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.ServerDetector;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.ColorScheme;
import com.liferay.portal.model.PluginSetting;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.Theme;
import com.liferay.portal.model.impl.ColorSchemeImpl;
import com.liferay.portal.model.impl.ThemeImpl;
import com.liferay.portal.plugin.PluginUtil;
import com.liferay.portal.service.base.ThemeLocalServiceBaseImpl;
import com.liferay.portal.theme.ThemeCompanyId;
import com.liferay.portal.theme.ThemeCompanyLimit;
import com.liferay.portal.theme.ThemeGroupId;
import com.liferay.portal.theme.ThemeGroupLimit;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.ContextReplace;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.UrlResource;

/**
 * @author Brian Wing Shun Chan
 * @author Jorge Ferrer
 */
public class ThemeLocalServiceImpl extends ThemeLocalServiceBaseImpl {

	public ColorScheme getColorScheme(
			long companyId, String themeId, String colorSchemeId,
			boolean wapTheme)
		throws SystemException {

		colorSchemeId = GetterUtil.getString(colorSchemeId);

		Theme theme = getTheme(companyId, themeId, wapTheme);

		Map<String, ColorScheme> colorSchemesMap = theme.getColorSchemesMap();

		ColorScheme colorScheme = colorSchemesMap.get(colorSchemeId);

		if (colorScheme == null) {
			List<ColorScheme> colorSchemes = theme.getColorSchemes();

			if (colorSchemes.size() > 0) {
				for (int i = (colorSchemes.size() - 1); i >= 0; i--) {
					colorScheme = colorSchemes.get(i);

					if (colorScheme.isDefaultCs()) {
						break;
					}
				}
			}
		}

		if (colorScheme == null) {
			if (wapTheme) {
				colorSchemeId = ColorSchemeImpl.getDefaultWapColorSchemeId();
			}
			else {
				colorSchemeId =
					ColorSchemeImpl.getDefaultRegularColorSchemeId();
			}
		}

		if (colorScheme == null) {
			colorScheme = ColorSchemeImpl.getNullColorScheme();
		}

		return colorScheme;
	}

	public Theme getTheme(long companyId, String themeId, boolean wapTheme)
		throws SystemException {

		themeId = GetterUtil.getString(themeId);

		Theme theme = _getThemes(companyId).get(themeId);

		if (theme == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"No theme found for specified theme id " + themeId +
						". Returning the default theme.");
			}

			if (wapTheme) {
				themeId = ThemeImpl.getDefaultWapThemeId(companyId);
			}
			else {
				themeId = ThemeImpl.getDefaultRegularThemeId(companyId);
			}

			theme = _themes.get(themeId);
		}

		if (theme == null) {
			if (_themes.isEmpty()) {
				if (_log.isDebugEnabled()) {
					_log.debug("No themes are installed");
				}

				return null;
			}

			_log.error(
				"No theme found for default theme id " + themeId +
					". Returning a random theme.");

			Iterator<Map.Entry<String, Theme>> itr =
				_themes.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry<String, Theme> entry = itr.next();

				theme = entry.getValue();
			}
		}

		return theme;
	}

	public List<Theme> getThemes(long companyId) {
		List<Theme> themes = ListUtil.fromCollection(
			_getThemes(companyId).values());

		return ListUtil.sort(themes);
	}

	public List<Theme> getThemes(
			long companyId, long groupId, long userId, boolean wapTheme)
		throws SystemException {

		List<Theme> themes = getThemes(companyId);

		themes = (List<Theme>)PluginUtil.restrictPlugins(
			themes, companyId, userId);

		Iterator<Theme> itr = themes.iterator();

		while (itr.hasNext()) {
			Theme theme = itr.next();

			if ((theme.getThemeId().equals("controlpanel")) ||
				(!theme.isGroupAvailable(groupId)) ||
				(theme.isWapTheme() != wapTheme)) {

				itr.remove();
			}
		}

		return themes;
	}

	public List<Theme> getWARThemes() {
		List<Theme> themes = ListUtil.fromCollection(_themes.values());

		Iterator<Theme> itr = themes.iterator();

		while (itr.hasNext()) {
			Theme theme = itr.next();

			if (!theme.isWARFile()) {
				itr.remove();
			}
		}

		return themes;
	}

	public List<String> init(
		ServletContext servletContext, String themesPath,
		boolean loadFromServletContext, String[] xmls,
		PluginPackage pluginPackage) {

		return init(
			null, servletContext, themesPath, loadFromServletContext, xmls,
			pluginPackage);
	}

	public List<String> init(
		String servletContextName, ServletContext servletContext,
		String themesPath, boolean loadFromServletContext, String[] xmls,
		PluginPackage pluginPackage) {

		List<String> themeIds = new ArrayList<String>();

		try {
			for (int i = 0; i < xmls.length; i++) {
				Set<String> themes = _readThemes(
					servletContextName, servletContext, themesPath,
					loadFromServletContext, xmls[i], pluginPackage);

				Iterator<String> itr = themes.iterator();

				while (itr.hasNext()) {
					String themeId = itr.next();

					if (!themeIds.contains(themeId)) {
						themeIds.add(themeId);
					}
				}
			}

			Set<String> themes = new HashSet<String>();
			ClassLoader classLoader = getClass().getClassLoader();
			// load xmls
			String resourceName = "WEB-INF/liferay-look-and-feel-ext.xml";
			Enumeration<URL> resources = classLoader.getResources(resourceName);
			if (_log.isDebugEnabled() && !resources.hasMoreElements()) {
				_log.debug("No " + resourceName + " has been found");
			}
			while (resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				if (_log.isDebugEnabled()) {
					_log.debug("Loading " + resourceName + " from: " + resource);
				}

				if (resource == null) {
					continue;
				}

				InputStream is = new UrlResource(resource).getInputStream();
				try {
					String xmlExt = IOUtils.toString(is, "UTF-8");
					themes.addAll(_readThemes(
						servletContextName, servletContext, themesPath,
						loadFromServletContext, xmlExt, pluginPackage));
				} catch (Exception e) {
					_log.error("Problem while loading file " + resource, e);
				} finally {
					is.close();
				}
			}

			for (String themeId : themes) {
				if (!themeIds.contains(themeId)) {
					themeIds.add(themeId);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		_themesPool.clear();

		return themeIds;
	}

	public void uninstallThemes(List<String> themeIds) {
		for (int i = 0; i < themeIds.size(); i++) {
			String themeId = themeIds.get(i);

			_themes.remove(themeId);

			layoutTemplateLocalService.uninstallLayoutTemplates(themeId);
		}

		_themesPool.clear();
	}

	private List<ThemeCompanyId> _getCompanyLimitExcludes(Element el) {
		List<ThemeCompanyId> includes = new ArrayList<ThemeCompanyId>();

		if (el != null) {
			List<Element> companyIds = el.elements("company-id");

			for (int i = 0; i < companyIds.size(); i++) {
				Element companyIdEl = companyIds.get(i);

				String name = companyIdEl.attributeValue("name");
				String pattern = companyIdEl.attributeValue("pattern");

				ThemeCompanyId themeCompanyId = null;

				if (Validator.isNotNull(name)) {
					themeCompanyId = new ThemeCompanyId(name, false);
				}
				else if (Validator.isNotNull(pattern)) {
					themeCompanyId = new ThemeCompanyId(pattern, true);
				}

				if (themeCompanyId != null) {
					includes.add(themeCompanyId);
				}
			}
		}

		return includes;
	}

	private List<ThemeCompanyId> _getCompanyLimitIncludes(Element el) {
		return _getCompanyLimitExcludes(el);
	}

	private List<ThemeGroupId> _getGroupLimitExcludes(Element el) {
		List<ThemeGroupId> includes = new ArrayList<ThemeGroupId>();

		if (el != null) {
			List<Element> groupIds = el.elements("group-id");

			for (int i = 0; i < groupIds.size(); i++) {
				Element groupIdEl = groupIds.get(i);

				String name = groupIdEl.attributeValue("name");
				String pattern = groupIdEl.attributeValue("pattern");

				ThemeGroupId themeGroupId = null;

				if (Validator.isNotNull(name)) {
					themeGroupId = new ThemeGroupId(name, false);
				}
				else if (Validator.isNotNull(pattern)) {
					themeGroupId = new ThemeGroupId(pattern, true);
				}

				if (themeGroupId != null) {
					includes.add(themeGroupId);
				}
			}
		}

		return includes;
	}

	private List<ThemeGroupId> _getGroupLimitIncludes(Element el) {
		return _getGroupLimitExcludes(el);
	}

	private Map<String, Theme> _getThemes(long companyId) {
		Map<String, Theme> themes = _themesPool.get(companyId);

		if (themes == null) {
			themes = new ConcurrentHashMap<String, Theme>();

			Iterator<Map.Entry<String, Theme>> itr =
				_themes.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry<String, Theme> entry = itr.next();

				String themeId = entry.getKey();
				Theme theme = entry.getValue();

				if (theme.isCompanyAvailable(companyId)) {
					themes.put(themeId, theme);
				}
			}

			_themesPool.put(companyId, themes);
		}

		return themes;
	}

	private Version _getVersion(String version) {
		if (version.equals("${current-version}")) {
			version = ReleaseInfo.getVersion();
		}

		return Version.getInstance(version);
	}

	private void _readColorSchemes(
		Element theme, Map<String, ColorScheme> colorSchemes,
		ContextReplace themeContextReplace) {

		Iterator<Element> itr = theme.elements("color-scheme").iterator();

		while (itr.hasNext()) {
			Element colorScheme = itr.next();

			ContextReplace colorSchemeContextReplace =
				(ContextReplace)themeContextReplace.clone();

			String id = colorScheme.attributeValue("id");

			colorSchemeContextReplace.addValue("color-scheme-id", id);

			ColorScheme colorSchemeModel = colorSchemes.get(id);

			if (colorSchemeModel == null) {
				colorSchemeModel = new ColorSchemeImpl(id);
			}

			String name = GetterUtil.getString(
				colorScheme.attributeValue("name"), colorSchemeModel.getName());

			name = colorSchemeContextReplace.replace(name);

			boolean defaultCs = GetterUtil.getBoolean(
				colorScheme.elementText("default-cs"),
				colorSchemeModel.isDefaultCs());

			String cssClass = GetterUtil.getString(
				colorScheme.elementText("css-class"),
				colorSchemeModel.getCssClass());

			cssClass = colorSchemeContextReplace.replace(cssClass);

			colorSchemeContextReplace.addValue("css-class", cssClass);

			String colorSchemeImagesPath = GetterUtil.getString(
				colorScheme.elementText("color-scheme-images-path"),
				colorSchemeModel.getColorSchemeImagesPath());

			colorSchemeImagesPath = colorSchemeContextReplace.replace(
				colorSchemeImagesPath);

			colorSchemeContextReplace.addValue(
				"color-scheme-images-path", colorSchemeImagesPath);

			colorSchemeModel.setName(name);
			colorSchemeModel.setDefaultCs(defaultCs);
			colorSchemeModel.setCssClass(cssClass);
			colorSchemeModel.setColorSchemeImagesPath(colorSchemeImagesPath);

			colorSchemes.put(id, colorSchemeModel);
		}
	}

	private Set<String> _readThemes(
			String servletContextName, ServletContext servletContext,
			String themesPath, boolean loadFromServletContext, String xml,
			PluginPackage pluginPackage)
		throws Exception {

		Set<String> themeIds = new HashSet<String>();

		if (xml == null) {
			return themeIds;
		}

		Document doc = SAXReaderUtil.read(xml, true);

		Element root = doc.getRootElement();

		Version portalVersion = _getVersion(ReleaseInfo.getVersion());

		boolean compatible = false;

		Element compatibilityEl = root.element("compatibility");

		if (compatibilityEl != null) {
			Iterator<Element> itr = compatibilityEl.elements(
				"version").iterator();

			while (itr.hasNext()) {
				Element versionEl = itr.next();

				Version version = _getVersion(versionEl.getTextTrim());

				if (version.includes(portalVersion)) {
					compatible = true;

					break;
				}
			}
		}

		if (!compatible) {
			_log.error(
				"Themes in this WAR are not compatible with " +
					ReleaseInfo.getServerInfo());

			return themeIds;
		}

		ThemeCompanyLimit companyLimit = null;

		Element companyLimitEl = root.element("company-limit");

		if (companyLimitEl != null) {
			companyLimit = new ThemeCompanyLimit();

			Element companyIncludesEl =
				companyLimitEl.element("company-includes");

			if (companyIncludesEl != null) {
				companyLimit.setIncludes(
					_getCompanyLimitIncludes(companyIncludesEl));
			}

			Element companyExcludesEl =
				companyLimitEl.element("company-excludes");

			if (companyExcludesEl != null) {
				companyLimit.setExcludes(
					_getCompanyLimitExcludes(companyExcludesEl));
			}
		}

		ThemeGroupLimit groupLimit = null;

		Element groupLimitEl = root.element("group-limit");

		if (groupLimitEl != null) {
			groupLimit = new ThemeGroupLimit();

			Element groupIncludesEl = groupLimitEl.element("group-includes");

			if (groupIncludesEl != null) {
				groupLimit.setIncludes(_getGroupLimitIncludes(groupIncludesEl));
			}

			Element groupExcludesEl =
				groupLimitEl.element("group-excludes");

			if (groupExcludesEl != null) {
				groupLimit.setExcludes(_getGroupLimitExcludes(groupExcludesEl));
			}
		}

		long timestamp = ServletContextUtil.getLastModified(servletContext);

		Iterator<Element> itr1 = root.elements("theme").iterator();

		while (itr1.hasNext()) {
			Element theme = itr1.next();

			ContextReplace themeContextReplace = new ContextReplace();

			themeContextReplace.addValue("themes-path", themesPath);

			String themeId = theme.attributeValue("id");

			if (servletContextName != null) {
				themeId =
					themeId + PortletConstants.WAR_SEPARATOR +
						servletContextName;
			}

			themeId = PortalUtil.getJsSafePortletId(themeId);

			themeContextReplace.addValue("theme-id", themeId);

			themeIds.add(themeId);

			Theme themeModel = _themes.get(themeId);

			if (themeModel == null) {
				themeModel = new ThemeImpl(themeId);

				_themes.put(themeId, themeModel);
			}

			themeModel.setTimestamp(timestamp);

			PluginSetting pluginSetting =
				pluginSettingLocalService.getDefaultPluginSetting();

			themeModel.setPluginPackage(pluginPackage);
			themeModel.setDefaultPluginSetting(pluginSetting);

			themeModel.setThemeCompanyLimit(companyLimit);
			themeModel.setThemeGroupLimit(groupLimit);

			if (servletContextName != null) {
				themeModel.setServletContextName(servletContextName);
			}

			themeModel.setLoadFromServletContext(loadFromServletContext);

			String name = GetterUtil.getString(
				theme.attributeValue("name"), themeModel.getName());

			String rootPath = GetterUtil.getString(
				theme.elementText("root-path"), themeModel.getRootPath());

			rootPath = themeContextReplace.replace(rootPath);

			themeContextReplace.addValue("root-path", rootPath);

			String templatesPath = GetterUtil.getString(
				theme.elementText("templates-path"),
				themeModel.getTemplatesPath());

			templatesPath = themeContextReplace.replace(templatesPath);
			templatesPath = StringUtil.safePath(templatesPath);

			themeContextReplace.addValue("templates-path", templatesPath);

			String cssPath = GetterUtil.getString(
				theme.elementText("css-path"), themeModel.getCssPath());

			cssPath = themeContextReplace.replace(cssPath);
			cssPath = StringUtil.safePath(cssPath);

			themeContextReplace.addValue("css-path", cssPath);

			String imagesPath = GetterUtil.getString(
				theme.elementText("images-path"),
				themeModel.getImagesPath());

			imagesPath = themeContextReplace.replace(imagesPath);
			imagesPath = StringUtil.safePath(imagesPath);

			themeContextReplace.addValue("images-path", imagesPath);

			String javaScriptPath = GetterUtil.getString(
				theme.elementText("javascript-path"),
				themeModel.getJavaScriptPath());

			javaScriptPath = themeContextReplace.replace(javaScriptPath);
			javaScriptPath = StringUtil.safePath(javaScriptPath);

			themeContextReplace.addValue("javascript-path", javaScriptPath);

			String virtualPath = GetterUtil.getString(
				theme.elementText("virtual-path"), themeModel.getVirtualPath());

			String templateExtension = GetterUtil.getString(
				theme.elementText("template-extension"),
				themeModel.getTemplateExtension());

			themeModel.setName(name);
			themeModel.setRootPath(rootPath);
			themeModel.setTemplatesPath(templatesPath);
			themeModel.setCssPath(cssPath);
			themeModel.setImagesPath(imagesPath);
			themeModel.setJavaScriptPath(javaScriptPath);
			themeModel.setVirtualPath(virtualPath);
			themeModel.setTemplateExtension(templateExtension);

			Element settingsEl = theme.element("settings");

			if (settingsEl != null) {
				Iterator<Element> itr2 = settingsEl.elements(
					"setting").iterator();

				while (itr2.hasNext()) {
					Element settingEl = itr2.next();

					String key = settingEl.attributeValue("key");
					String value = settingEl.attributeValue("value");

					themeModel.setSetting(key, value);
				}
			}

			themeModel.setWapTheme(GetterUtil.getBoolean(
				theme.elementText("wap-theme"), themeModel.isWapTheme()));

			Element rolesEl = theme.element("roles");

			if (rolesEl != null) {
				Iterator<Element> itr2 = rolesEl.elements(
					"role-name").iterator();

				while (itr2.hasNext()) {
					Element roleNameEl = itr2.next();

					pluginSetting.addRole(roleNameEl.getText());
				}
			}

			_readColorSchemes(
				theme, themeModel.getColorSchemesMap(), themeContextReplace);
			_readColorSchemes(
				theme, themeModel.getColorSchemesMap(), themeContextReplace);

			Element layoutTemplatesEl = theme.element("layout-templates");

			if (layoutTemplatesEl != null) {
				Element standardEl = layoutTemplatesEl.element("standard");

				if (standardEl != null) {
					layoutTemplateLocalService.readLayoutTemplate(
						servletContextName, servletContext, null,
						standardEl, true, themeId, pluginPackage);
				}

				Element customEl = layoutTemplatesEl.element("custom");

				if (customEl != null) {
					layoutTemplateLocalService.readLayoutTemplate(
						servletContextName, servletContext, null,
						customEl, false, themeId, pluginPackage);
				}
			}

			if (!themeModel.isWapTheme()) {
				_setSpriteImages(servletContext, themeModel, imagesPath);
			}
		}

		return themeIds;
	}

	private void _setSpriteImages(
			ServletContext servletContext, Theme theme, String resourcePath)
		throws Exception {

		Set<String> resourcePaths = servletContext.getResourcePaths(
			resourcePath);

		if (resourcePaths == null) {
			return;
		}

		List<File> images = new ArrayList<File>(resourcePaths.size());

		for (String curResourcePath : resourcePaths) {
			if (curResourcePath.endsWith(StringPool.SLASH)) {
				_setSpriteImages(servletContext, theme, curResourcePath);
			}
			else if (curResourcePath.endsWith(".png")) {
				String realPath = ServletContextUtil.getRealPath(
					servletContext, curResourcePath);

				if (realPath != null) {
					images.add(new File(realPath));
				}
				else {
					if (ServerDetector.isTomcat()) {
						if (_log.isInfoEnabled()) {
							_log.info(ServletContextUtil.LOG_INFO_SPRITES);
						}
					}
					else {
						_log.error(
							"Real path for " + curResourcePath + " is null");
					}
				}
			}
		}

		String spriteFileName = ".sprite.png";
		String spritePropertiesFileName = ".sprite.properties";
		String spritePropertiesRootPath = ServletContextUtil.getRealPath(
			servletContext, theme.getImagesPath());

		Properties spriteProperties = SpriteProcessorUtil.generate(
			images, spriteFileName, spritePropertiesFileName,
			spritePropertiesRootPath, 16, 16, 10240);

		if (spriteProperties == null) {
			return;
		}

		spriteFileName =
			resourcePath.substring(
				theme.getImagesPath().length(), resourcePath.length()) +
			spriteFileName;

		theme.setSpriteImages(spriteFileName, spriteProperties);
	}

	private static Log _log = LogFactoryUtil.getLog(
		ThemeLocalServiceImpl.class);

	private static Map<String, Theme> _themes =
		new ConcurrentHashMap<String, Theme>();
	private static Map<Long, Map<String, Theme>> _themesPool =
		new ConcurrentHashMap<Long, Map<String, Theme>>();

}
