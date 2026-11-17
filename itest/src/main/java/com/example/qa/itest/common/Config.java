package com.example.qa.itest.common;

import com.example.qa.btest.db.DataSourceFactory;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.util.YamlUtil;
import com.example.qa.itest.model.CommonContext;
import com.example.qa.itest.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration("itestConfig")
public class Config {

    @Autowired
    @Qualifier("commonService")
    private CommonService commonService;

    @Bean
    public CommonContext context() {
        CommonContext commonContext = new CommonContext();

        // 载入context.yml文件
        String envType = System.getProperty("ENV_TYPE", "test");
        String contextFile = this.getClass().getResource("/" + envType + "/data/common/context.yml").getPath();
        loadContext(commonContext, contextFile);
        return commonContext;
    }

    private void loadContext(CommonContext commonContext, String contextFile) {
        Map map = YamlUtil.load(contextFile);
        if (map == null || map.size() == 0) {
            return;
        }
        if (map.containsKey("users")) {
            Map userMap = commonService.toUsers((List) map.get("users"));
            userMap.putAll(commonContext.getUserMap());
            commonContext.setUserMap(userMap);
        }
        if (map.containsKey("devices")) {
            Map deviceMap = commonService.toDevices((List) map.get("devices"));
            deviceMap.putAll(commonContext.getDeviceMap());
            commonContext.setDeviceMap(deviceMap);
        }
        if (map.containsKey("envs")) {
            Map envMap = commonService.toEnvs((List) map.get("envs"));
            envMap.putAll(commonContext.getEnvMap());
            commonContext.setEnvMap(envMap);
        }
        if (map.containsKey("apps")) {
            Map appMap = commonService.toApps((List) map.get("apps"));
            appMap.putAll(commonContext.getAppMap());
            commonContext.setAppMap(appMap);
        }
        if (map.containsKey("mysqls")) {
            ((List) map.get("mysqls")).stream().forEach(item -> {
                IDataSource dataSource = DataSourceFactory.getDataSource((Map) item);
                commonContext.getMysqlDataSourceMap().put((String) ((Map<?, ?>) item).get("tag"), dataSource);
            });
        }
        if (map.containsKey("redises")) {
            ((List) map.get("redises")).stream().forEach(item -> {
                IDataSource dataSource = DataSourceFactory.getDataSource((Map) item);
                commonContext.getRedisDataSourceMap().put((String) ((Map<?, ?>) item).get("tag"), dataSource);
            });
        }
    }
}
