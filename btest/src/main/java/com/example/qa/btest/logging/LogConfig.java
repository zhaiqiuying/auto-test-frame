package com.example.qa.btest.logging;

import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfig {

    static {
        String envType = System.getProperty("ENV_TYPE", "test");
        // System.setProperty("logback.configurationFile", envType + "/logback.xml");
        System.setProperty("logback.configurationFile",
                LogConfig.class.getResource("/" + envType + "/config/common/logback.xml").getPath());
    }

}
