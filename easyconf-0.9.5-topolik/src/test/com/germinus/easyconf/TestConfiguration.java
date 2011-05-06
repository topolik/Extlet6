/*
 * Copyright 2011 IBA CZ, s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.germinus.easyconf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.configuration.AbstractConfiguration;

/**
 *
 * @author Tomas Polesovsky
 */
public class TestConfiguration extends AbstractConfiguration{
    private Map props= new HashMap();

    public TestConfiguration(){
        props.put("string-overridden-in-prj-and-testConfiguration", "testConfiguration");
    }
    protected void addPropertyDirect(String key, Object value) {
        props.put(key, value);
    }

    public boolean isEmpty() {
        return props.isEmpty();
    }

    public boolean containsKey(String key) {
        return props.containsKey(key);
    }

    public void clearProperty(String key) {
        props.remove(key);
    }

    public Iterator getKeys() {
        return props.keySet().iterator();
    }

    public Object getProperty(String key) {
        return props.get(key);
    }

}
