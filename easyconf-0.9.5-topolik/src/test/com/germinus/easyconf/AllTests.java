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
package com.germinus.easyconf;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Launches all the tests of EasyConf
 *
 * @version $Revision: 1.1 $
 */
public class AllTests extends TestCase {

    private static final Log log = LogFactory.getLog(AllTests.class);

    public AllTests(String name) {
        super(name);
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static TestSuite suite () {
        TestSuite suite = new TestSuite("All EasyConf Tests");
        suite.addTest(UnitTests.suite());
        suite.addTest(SystemTests.suite());
        return suite;
    }

    public void testSuccess() {
        assertTrue(true);
    }

    public void testFail() {
        fail("This test fails on purpose to check that junit is working. " 
             + "You can now safely remove it from UnitTests.java");
    }
}
