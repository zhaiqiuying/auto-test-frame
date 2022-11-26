package com.yuanfudao.aries.qa.btest.util;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;


public class YamlUtil {
    private final static Yaml yaml = new Yaml();

    public static Map load(String path) {
        try {
            InputStream inputStream = new FileInputStream(path);
            return (Map<String, Object>) yaml.load(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
