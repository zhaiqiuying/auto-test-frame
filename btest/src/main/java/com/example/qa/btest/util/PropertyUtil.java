package com.example.qa.btest.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {

    public static Properties loadProperty(String properyName) {

        Properties properties = new Properties();
        InputStream inputStream = PropertyUtil.class.getClassLoader().getResourceAsStream(properyName);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
