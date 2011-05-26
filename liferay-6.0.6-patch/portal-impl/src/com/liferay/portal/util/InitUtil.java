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

package com.liferay.portal.util;

import com.liferay.portal.cache.CacheRegistryImpl;
import com.liferay.portal.configuration.ConfigurationFactoryImpl;
import com.liferay.portal.dao.db.DBFactoryImpl;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.configuration.ConfigurationFactoryUtil;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.JavaProps;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.TimeZoneUtil;
import com.liferay.portal.log.Log4jLogFactoryImpl;
import com.liferay.portal.spring.util.SpringUtil;
import com.liferay.util.SystemProperties;
import com.liferay.util.log4j.Log4JUtil;

import org.apache.commons.lang.time.StopWatch;

import java.net.URL;
import java.util.Enumeration;

/**
 * @author Brian Wing Shun Chan
 */
public class InitUtil {

	public static synchronized void init() {
		if (_initialized) {
			return;
		}

		StopWatch stopWatch = null;

		if (_PRINT_TIME) {
			stopWatch = new StopWatch();

			stopWatch.start();
		}

		// Set the default locale used by Liferay. This locale is no longer set
		// at the VM level. See LEP-2584.

		String userLanguage = SystemProperties.get("user.language");
		String userCountry = SystemProperties.get("user.country");
		String userVariant = SystemProperties.get("user.variant");

		LocaleUtil.setDefault(userLanguage, userCountry, userVariant);

		// Set the default time zone used by Liferay. This time zone is no
		// longer set at the VM level. See LEP-2584.

		String userTimeZone = SystemProperties.get("user.timezone");

		TimeZoneUtil.setDefault(userTimeZone);

		// Shared class loader

		try {
			Thread currentThread = Thread.currentThread();

			PortalClassLoaderUtil.setClassLoader(
				currentThread.getContextClassLoader());
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Log4J

		if (GetterUtil.getBoolean(SystemProperties.get(
				"log4j.configure.on.startup"), true)) {

			ClassLoader classLoader = InitUtil.class.getClassLoader();

			Log4JUtil.configureLog4J(
				classLoader.getResource("META-INF/portal-log4j.xml"));
			try {
				Log _log = LogFactoryUtil.getLog(InitUtil.class);

				String configName = "META-INF/portal-log4j-ext.xml";
				Enumeration<URL> configs = 
					classLoader.getResources(configName);
				if (_log.isDebugEnabled() && !configs.hasMoreElements()) {
					_log.debug("No " + configName + " has been found");
				}
				while (configs.hasMoreElements()) {
					URL config = configs.nextElement();
					Log4JUtil.configureLog4J(config);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Shared log

		try {
			LogFactoryUtil.setLogFactory(new Log4jLogFactoryImpl());
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// Cache registry

		CacheRegistryUtil.setCacheRegistry(new CacheRegistryImpl());

		// Configuration factory

		ConfigurationFactoryUtil.setConfigurationFactory(
			new ConfigurationFactoryImpl());

		// DB factory

		DBFactoryUtil.setDBFactory(new DBFactoryImpl());

		// Java properties

		JavaProps.isJDK5();

		if (_PRINT_TIME) {
			System.out.println(
				"InitAction takes " + stopWatch.getTime() + " ms");
		}

		_initialized = true;
	}

	public synchronized static void initWithSpring() {
		if (_initialized) {
			return;
		}

		init();

		SpringUtil.loadContext();

		_initialized = true;
	}

	private static final boolean _PRINT_TIME = false;

	private static boolean _initialized;

}
