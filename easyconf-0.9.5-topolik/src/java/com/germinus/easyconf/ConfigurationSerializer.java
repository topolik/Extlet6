package com.germinus.easyconf;

/**
 * Factory class which creates serializer subclasses based on 
 * availability of external classes in the classpath
 *  
 * @author Jorge Ferrer
 *
 */
public abstract class ConfigurationSerializer {

	public static ConfigurationSerializer getSerializer() {
		return new XstreamSerializer();
	}

	/**
	 * Deserialize the configuration object from a String
	 * @param serializedConf
	 * @return A configuration object
	 */
	public abstract Object deserialize(String serializedConf);

	/**
	 * Serialize a configuration object to a String
	 * @param configurationObject
	 * @return An string representing the configuration object
	 */
	public abstract String serialize(Object configurationObject);

}
