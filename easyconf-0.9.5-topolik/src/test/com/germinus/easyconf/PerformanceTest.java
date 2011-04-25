package com.germinus.easyconf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

import junit.framework.TestCase;

public class PerformanceTest extends TestCase {

    private static final int REPETITIONS = 10000;
	private static final String CONFIGURATION_DIR = "target/test-classes";
    private static final String COMPONENT_NAME = "performance-test";
    private static final String COMPONENT_NAME_2 = "performance-test-2";

    File confFile; 
    File reloadedConfFile; 

	protected void setUp() throws Exception {
        confFile = new File(CONFIGURATION_DIR+ "/" + COMPONENT_NAME + ".properties");
        reloadedConfFile = new File(CONFIGURATION_DIR+ "/" + COMPONENT_NAME_2 + ".properties");
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");
        props.setProperty("key3", "value3");

        FileUtil.write(confFile, props);
		
		props.setProperty(Conventions.RELOAD_DELAY_PROPERTY, "1");
        FileUtil.write(reloadedConfFile, props);
}
    protected void tearDown() throws Exception {
        confFile.delete();
		EasyConf.refreshAll();
    }
	
	public void testPerformance() throws Exception {
		long javaUtilProperties = measureJavaUtilProperties();
		long commons = measurePropertiesConfiguration(confFile, false);
		long commonsReloading = measurePropertiesConfiguration(reloadedConfFile, true);
		long withoutReloading = measureGetString(COMPONENT_NAME);
		long withReloading = measureGetString(COMPONENT_NAME_2);
		System.out.println("####### Performance test summary ######## ");
		System.out.println("java.utilProperties takes:           " + javaUtilProperties + " miliseconds");
		System.out.println("Commons takes:                       " + commons + " miliseconds");
		System.out.println("Commons with reloading takes:        " + commonsReloading + " miliseconds");
		System.out.println("Using EasyConf takes:                " + withoutReloading + " miliseconds");
		System.out.println("Using EasyConf with reloading takes: " + withReloading + " miliseconds");
	}
	public long measureJavaUtilProperties() throws Exception {
		setUp();
		Properties props = new Properties();
		props.load(new FileInputStream(confFile));
		long start = System.currentTimeMillis();
		String value;
		for (int i = 0; i < REPETITIONS; i++) {
			value = props.getProperty("key1");
		}
		long end = System.currentTimeMillis();
		long period = end - start;
		tearDown();
		return period;
	}
	public long measurePropertiesConfiguration(File file, boolean reload) throws Exception {
		setUp();
		PropertiesConfiguration props = new PropertiesConfiguration(file);
		if (reload) {
			props.setReloadingStrategy(new FileChangedReloadingStrategy());
		}
		long start = System.currentTimeMillis();
		String value;
		for (int i = 0; i < REPETITIONS; i++) {
			value = props.getString("key1");
		}
		long end = System.currentTimeMillis();
		long period = end - start;
		tearDown();
		return period;
	}
	public long measureGetString(String componentName) throws Exception {
		setUp();
		EasyConf.getConfiguration(componentName).getProperties();
		long start = System.currentTimeMillis();
		String value;
		for (int i = 0; i < REPETITIONS; i++) {
			value = EasyConf.getConfiguration(componentName).getProperties().
				getString("key1");
		}
		long end = System.currentTimeMillis();
		long period = end - start;
		tearDown();
		return period;
	}

}
