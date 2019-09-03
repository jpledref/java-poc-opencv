package com.shcompany.java.poc.opencv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enums.APPOptions;

public class ConfigProvider {
	static final Logger LOG = LoggerFactory.getLogger(ConfigProvider.class);
	static final String FILENAME = "config.properties";

	/* SINGLETON */
	private static ConfigProvider INSTANCE = new ConfigProvider();

	public static ConfigProvider getInstance() {
		return INSTANCE;
	}

	/* Properties */
	private Properties prop = new Properties();

	private ConfigProvider() {
		LOG.info("Starting ConfigProvider...");

		File f = FSProvider.getInstance().getCurrentPathNormalizedFile();
		String dst = f.getPath() + File.separator + FILENAME;

		if (!new File(dst).exists()) {
			String src = FILENAME;
			FSProvider.getInstance().extractFile(src, dst);
		}

		InputStream input = getClass().getClassLoader().getResourceAsStream(FILENAME);
		try {
			prop.load(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		load(APPOptions.class, prop);
	}

	public void printConfig() {
		showConfig(APPOptions.class);
	}

	public static void load(Class<?> configClass, Properties prop) {
		try {
			Properties props = prop;
			for (Field field : configClass.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					try {
						field.set(null, getValue(props, field.getName(), field.getType()));
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading configuration: " + e, e);
		}
	}
	
	public static void showConfig(Class<?> configClass) {
		LOG.info("-- Config Set --vv");
		try {			
			for (Field field : configClass.getDeclaredFields()) {
				LOG.info("Key : " + field.getName() + ", Value : " + field.get(field)); 				
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading configuration: " + e, e);
		}
		LOG.info("-- Config Set --^^");
	}

	private static Object getValue(Properties props, String name, Class<?> type) {
		String value = props.getProperty(name);
		if (value == null)
			throw new IllegalArgumentException("Missing configuration value: " + name);
		if (type == String.class)
			return value;
		if (type == boolean.class)
			return Boolean.parseBoolean(value);
		if (type == int.class)
			return Integer.parseInt(value);
		if (type == float.class)
			return Float.parseFloat(value);
		throw new IllegalArgumentException("Unknown configuration value type: " + type.getName());
	}

}
