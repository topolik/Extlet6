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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * CompositeConfiguration that merges values of every loaded configuration. <br />
 * Please be aware that this is uncached version because underlying content could
 * be changed using Java Reflection API.
 * 
 * @author Tomas Polesovsky
 */
public class MergingCompositeConfiguration extends CompositeConfiguration {

    public MergingCompositeConfiguration() {
        super();
    }

    public MergingCompositeConfiguration(Configuration inMemoryConfiguration) {
        super(inMemoryConfiguration);
    }

    /**
     * If loaded configurations contain {@code key+} then try to merge values, otherwise return {@code super.getProperty(key)}.
     * @param key
     * @return
     */
    public Object getProperty(String key) {
        String mergingKey = key + Conventions.PLUS_SIGN;

        if (super.containsKey(mergingKey)) {
            return getMergedProperty(mergingKey, key);
        }

        return super.getProperty(key);
    }

    /**
     * Iterates through all configurations from lowest to highest priority based on following rules (see {@link CompositeConfiguration#getProperty(java.lang.String)}) :<br />
     * <ul>
     * <li>Configuration at index 0 has the highest priority</li>
     * <li>Configuration at index {@code i+1} has lower priority than Configuration at index {@code i}</li>
     * </ul>
     * <br />
     * And merge every configuration based on following rules (see {@link MergingCompositeConfiguration#mergeConfiguration}:<br />
     * <ul>
     * <li>If configuration contains {@code key=newValue1,newValue2} then replace result with new values =&gt; {@code result = [newValue1, newValue2]}</li>
     * <li>If configuration contains {@code key+=newValue1,newValue2} then add value to existing result =&gt; {@code result = [value1, value2, ..., newValue1, newValue2]}</li>
     * </ul>
     *
     * @param mergingKey key+
     * @param simpleKey key
     * @return List of values
     */
    protected Object getMergedProperty(String mergingKey, String simpleKey) {
        int size = getNumberOfConfigurations();
        List result = new ArrayList(size);

        // iterates backwards from lower priority to higher
        for (int i = size-1; i >= 0; i--) {
            Configuration conf = getConfiguration(i);
            mergeConfiguration(conf, mergingKey, simpleKey, result);
        }

        return result;
    }

    protected void mergeConfiguration(Configuration conf, String mergingKey, String simpleKey, List result){
        if (conf.containsKey(simpleKey)) {
            result.clear();
            addPropertyToResult(result, simpleKey, conf);
        }
        if (conf.containsKey(mergingKey)) {
            addPropertyToResult(result, mergingKey, conf);
        }
    }

    protected void addPropertyToResult(List result, String key, Configuration conf) {
        Object value = conf.getProperty(key);
        if(value instanceof List){
            result.addAll((List) value);
        } else {
            result.add(value);
        }
    }
}
