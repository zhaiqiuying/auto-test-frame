package com.yuanfudao.aries.qa.btest.common;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

@Configuration("baseConfig")
public class Config {

    @Bean
    public Yaml yaml() {
        return new Yaml();
    }

    @Bean
    public Gson gson() {
        return new Gson();
    }
}
