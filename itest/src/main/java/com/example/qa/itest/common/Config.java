package com.example.qa.itest.common;

import com.example.qa.btest.db.DataSourceFactory;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.util.YamlUtil;
import com.example.qa.itest.model.CommonContext;
import com.example.qa.itest.model.User;
import com.example.qa.itest.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration("itestConfig")
public class Config {

    private static final String KEY_ENV_TYPE = "ENV_TYPE";
    private static final String DEFAULT_ENV_TYPE = "test";
    private static final String KEY_USERS = "users";
    private static final String KEY_DEVICES = "devices";
    private static final String KEY_ENVS = "envs";
    private static final String KEY_APPS = "apps";
    private static final String KEY_MYSQLS = "mysqls";
    private static final String KEY_REDISES = "redises";
    private static final String KEY_TAG = "tag";

    @Autowired
    @Qualifier("commonService")
    private CommonService commonService;

    @Bean
    public CommonContext context() {
        CommonContext commonContext = new CommonContext();

        // 载入context.yml文件
        String envType = System.getProperty(KEY_ENV_TYPE, DEFAULT_ENV_TYPE);
        String contextFile = this.getClass().getResource("/" + envType + "/data/common/context.yml").getPath();
        loadContext(commonContext, contextFile);
        return commonContext;
    }

    private void loadContext(CommonContext commonContext, String contextFile) {
        Map<String, Object> map = YamlUtil.load(contextFile);
        if (map == null || map.isEmpty()) {
            return;
        }
        if (map.containsKey(KEY_USERS)) {
            Map<String, User> userMap = commonService.toUsers((List<?>) map.get(KEY_USERS));
            userMap.putAll(commonContext.getUserMap());
            commonContext.setUserMap(userMap);
        }
        if (map.containsKey(KEY_DEVICES)) {
            Map<String, User.Device> deviceMap = commonService.toDevices((List<?>) map.get(KEY_DEVICES));
            deviceMap.putAll(commonContext.getDeviceMap());
            commonContext.setDeviceMap(deviceMap);
        }
        if (map.containsKey(KEY_ENVS)) {
            Map<String, User.Env> envMap = commonService.toEnvs((List<?>) map.get(KEY_ENVS));
            envMap.putAll(commonContext.getEnvMap());
            commonContext.setEnvMap(envMap);
        }
        if (map.containsKey(KEY_APPS)) {
            Map<String, User.App> appMap = commonService.toApps((List<?>) map.get(KEY_APPS));
            appMap.putAll(commonContext.getAppMap());
            commonContext.setAppMap(appMap);
        }
        loadDataSources(KEY_MYSQLS, map, commonContext.getMysqlDataSourceMap());
        loadDataSources(KEY_REDISES, map, commonContext.getRedisDataSourceMap());
    }

    private void loadDataSources(String key, Map<String, Object> map, Map<String, IDataSource> targetMap) {
        if (!map.containsKey(key)) {
            return;
        }
        Object value = map.get(key);
        if (!(value instanceof List)) {
            return;
        }
        ((List<?>) value).forEach(item -> {
            if (!(item instanceof Map)) {
                return;
            }
            Map<?, ?> rawMap = (Map<?, ?>) item;
            Map<String, Object> itemMap = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                itemMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            IDataSource dataSource = DataSourceFactory.getDataSource(itemMap);
            Object tag = itemMap.get(KEY_TAG);
            if (tag != null) {
                targetMap.put(String.valueOf(tag), dataSource);
            }
        });
    }
}
