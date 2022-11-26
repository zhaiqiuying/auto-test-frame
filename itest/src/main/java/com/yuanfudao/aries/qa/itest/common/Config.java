package com.yuanfudao.aries.qa.itest.common;

import com.yuanfudao.aries.qa.btest.util.YamlUtil;
import com.yuanfudao.aries.qa.itest.model.CommonContext;
import com.yuanfudao.aries.qa.itest.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration("itestConfig")
public class Config {

    public final static CommonContext COMMON_CONTEXT = new CommonContext();
    @Autowired
    @Qualifier("commonService")
    private CommonService commonService;

    @Bean
    public CommonContext context() {

        String envType = System.getProperty("ENV_TYPE");
        String contextFile = this.getClass().getResource("/" + envType + "/context.yml").getPath();
        Map map = YamlUtil.load(contextFile);

        if (map == null || map.size() == 0) {
            return COMMON_CONTEXT;
        }
        if (map.containsKey("users")) {
            Map userMap = commonService.toUsers((List) map.get("users"));
            userMap.putAll(COMMON_CONTEXT.getUserMap());
            COMMON_CONTEXT.setUserMap(userMap);
        }
        if (map.containsKey("devices")) {
            Map deviceMap = commonService.toDevices((List) map.get("devices"));
            deviceMap.putAll(COMMON_CONTEXT.getDeviceMap());
            COMMON_CONTEXT.setDeviceMap(deviceMap);
        }
        if (map.containsKey("envs")) {
            Map envMap = commonService.toEnvs((List) map.get("envs"));
            envMap.putAll(COMMON_CONTEXT.getEnvMap());
            COMMON_CONTEXT.setEnvMap(envMap);
        }
        if (map.containsKey("apps")) {
            Map appMap = commonService.toApps((List) map.get("apps"));
            appMap.putAll(COMMON_CONTEXT.getAppMap());
            COMMON_CONTEXT.setAppMap(appMap);
        }
        return COMMON_CONTEXT;
    }
}
