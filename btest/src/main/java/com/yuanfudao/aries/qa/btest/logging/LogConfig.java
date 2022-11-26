package com.yuanfudao.aries.qa.btest.logging;

import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfig {

    static {
        String envType = System.getProperty("ENV_TYPE");
        // System.setProperty("logback.configurationFile", envType + "/logback.xml");
        System.setProperty("logback.configurationFile",
                LogConfig.class.getResource("/" + envType + "/logback.xml").getPath());
    }

}
