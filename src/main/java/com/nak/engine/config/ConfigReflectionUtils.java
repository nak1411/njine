package com.nak.engine.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Properties;

public class ConfigReflectionUtils {

    public static <T> T createFromProperties(Class<T> configClass, Properties properties) {
        try {
            T instance = configClass.getDeclaredConstructor().newInstance();

            for (Field field : configClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                String key = field.getName();
                String value = properties.getProperty(key);

                if (value != null) {
                    setFieldValue(field, instance, value);
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create config from properties", e);
        }
    }

    public static Properties convertToProperties(Object config) {
        Properties properties = new Properties();

        try {
            Class<?> configClass = config.getClass();

            for (Field field : configClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                String key = field.getName();
                Object value = field.get(config);

                if (value != null) {
                    if (value instanceof Map) {
                        // Special handling for Map fields (like key bindings)
                        Map<?, ?> map = (Map<?, ?>) value;
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            properties.setProperty(key + "." + entry.getKey(), entry.getValue().toString());
                        }
                    } else {
                        properties.setProperty(key, value.toString());
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert config to properties", e);
        }

        return properties;
    }

    private static void setFieldValue(Field field, Object instance, String value) throws Exception {
        Class<?> fieldType = field.getType();

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(instance, Boolean.parseBoolean(value));
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(instance, Integer.parseInt(value));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(instance, Long.parseLong(value));
        } else if (fieldType == float.class || fieldType == Float.class) {
            field.set(instance, Float.parseFloat(value));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(instance, Double.parseDouble(value));
        } else if (fieldType == String.class) {
            field.set(instance, value);
        } else if (fieldType == Map.class) {
            // Special handling for Map fields would go here
            // This is simplified - a full implementation would need to handle Map serialization
            System.out.println("Map field deserialization not fully implemented: " + field.getName());
        } else {
            throw new UnsupportedOperationException("Unsupported field type: " + fieldType);
        }
    }
}
