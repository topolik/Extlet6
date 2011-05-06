package com.germinus.easyconf;

import java.io.File;
import java.util.Properties;


import junit.framework.TestCase;

public class MergingTest extends TestCase {

    private static final String CONFIGURATION_DIR = "target"+File.separator+"test-classes";
    private static final String COMPONENT_NAME = "merging-test";
    private static final String COMPONENT_NAME_2 = "merging-test-2";
    private static final String COMPONENT_NAME_FILE = CONFIGURATION_DIR + File.separator + COMPONENT_NAME + Conventions.PROPERTIES_EXTENSION;
    private static final String COMPONENT_NAME_FILE2 = CONFIGURATION_DIR + File.separator + COMPONENT_NAME_2 + Conventions.PROPERTIES_EXTENSION;

    private static final String KEY_SAME_CONFIG_FILE = "key-extended-in-same-conf-file";
    private static final String KEY_DIFFERENT_CONFIG_FILE = "key-extended-in-different-conf-file";
    private static final String KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE = "key-overwriten-in-different-conf-file";
    File confFile;
    File confFile2;
    ComponentConfiguration componentConf;

    protected void setUp() throws Exception {

        confFile = new File(COMPONENT_NAME_FILE);
        confFile2 = new File(COMPONENT_NAME_FILE2);
        StringBuilder sb = new StringBuilder();
        addProperty(sb, KEY_SAME_CONFIG_FILE, "v1");
        addProperty(sb, KEY_SAME_CONFIG_FILE+Conventions.PLUS_SIGN, "v2");
        addProperty(sb, KEY_SAME_CONFIG_FILE+Conventions.PLUS_SIGN, "v3");

        addProperty(sb, KEY_DIFFERENT_CONFIG_FILE, "v1");
        addProperty(sb, KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE, "to-be-overwritten");

        addProperty(sb, Conventions.INCLUDE_PROPERTY, COMPONENT_NAME_2 + Conventions.PROPERTIES_EXTENSION);

        FileUtil.write(confFile, sb.toString());

        sb.setLength(0);

        addProperty(sb, KEY_DIFFERENT_CONFIG_FILE+Conventions.PLUS_SIGN, "v2");
        addProperty(sb, KEY_DIFFERENT_CONFIG_FILE+Conventions.PLUS_SIGN, "v3");

        addProperty(sb, KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE, "v1");
        addProperty(sb, KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE+Conventions.PLUS_SIGN, "v2");
        addProperty(sb, KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE+Conventions.PLUS_SIGN, "v3");

        FileUtil.write(confFile2, sb.toString());

        componentConf = EasyConf.getConfiguration("merging-test");
    }

    protected void tearDown() throws Exception {
        confFile.delete();
        confFile2.delete();
        EasyConf.refreshAll();
    }

    private void addProperty(StringBuilder sb, String key, String value) {
        sb.append(key).append("=").append(value).append("\n");
    }
    public void testMergingInSameFile() throws Exception {
    	assertEquals("Invalid value for " + KEY_SAME_CONFIG_FILE, "v1,v2,v3", componentConf.getProperties().getString(KEY_SAME_CONFIG_FILE));
    }
    public void testMergingInDifferentFile() throws Exception {
    	assertEquals("Invalid value for " + KEY_DIFFERENT_CONFIG_FILE, "v1,v2,v3", componentConf.getProperties().getString(KEY_DIFFERENT_CONFIG_FILE));
    }
    public void testOverwritingInDifferentFile() throws Exception {
    	assertEquals("Invalid value for " + KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE, "v1,v2,v3", componentConf.getProperties().getString(KEY_TO_REWRITE_DIFFERENT_CONFIG_FILE));
    }
}
