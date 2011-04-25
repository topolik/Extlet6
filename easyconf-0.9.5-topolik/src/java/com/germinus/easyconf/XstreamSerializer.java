/*
 * Created on 06-nov-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.germinus.easyconf;

import com.thoughtworks.xstream.XStream;

/**
 * @author jorge
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XstreamSerializer extends ConfigurationSerializer {

	private static final XStream xstream = new XStream();
	public Object deserialize(String serializedConf) {
		return xstream.fromXML(serializedConf);
	}

	public String serialize(Object configurationObject) {
		return xstream.toXML(configurationObject);
	}
	

}
