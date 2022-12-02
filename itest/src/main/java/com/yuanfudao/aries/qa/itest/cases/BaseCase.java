package com.yuanfudao.aries.qa.itest.cases;

import com.yuanfudao.aries.qa.btest.db.IDBConnection;
import com.yuanfudao.aries.qa.btest.db.IDataSource;
import com.yuanfudao.aries.qa.btest.logging.LogConfig;
import com.yuanfudao.aries.qa.btest.util.StringUtil;
import com.yuanfudao.aries.qa.itest.model.CommonContext;
import com.yuanfudao.aries.qa.itest.model.User;
import com.yuanfudao.aries.qa.itest.model.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.ObjectUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

@ContextConfiguration(locations = { "classpath:common/ApplicationContext.xml" })
public class BaseCase extends AbstractTestNGSpringContextTests {

    public UserContext userContext;
    @Autowired
    private CommonContext commonContext;

    static {
        // 设置logback配置文件
        String envType = System.getProperty("ENV_TYPE", "test");
        System.setProperty("logback.configurationFile",
                LogConfig.class.getResource("/" + envType + "/config/common/logback.xml").getPath());
    }

    @BeforeSuite(alwaysRun = true)
    @BeforeTest(alwaysRun = true)
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        super.springTestContextPrepareTestInstance();
    }

    @BeforeSuite(dependsOnMethods = {"springTestContextPrepareTestInstance"}, alwaysRun = true)
    @BeforeTest(dependsOnMethods = {"springTestContextPrepareTestInstance"}, alwaysRun = true)
    public void beforeSuite(ITestContext context) {

        User.Device device = User.Device.builder()
                .tag(context.getSuite().getParameter("execute_device_tag"))
                .client(context.getSuite().getParameter("execute_device_client"))
                .id(context.getSuite().getParameter("execute_device_id"))
                .build();
        User.App app = User.App.builder()
                .tag(context.getSuite().getParameter("execute_app_tag"))
                .version(context.getSuite().getParameter("execute_app_version"))
                .name(context.getSuite().getParameter("app_name"))
                .build();
        User.Env env = User.Env.builder()
                .tag(context.getSuite().getParameter("execute_env_tag"))
                .host(context.getSuite().getParameter("execute_env_host"))
                .laneId(context.getSuite().getParameter("execute_env_laneId"))
                .type(context.getSuite().getParameter("execute_env_type"))
                .build();
        User user = User.builder()
                .tag(context.getSuite().getParameter("execute_user_tag"))
                .ytkUserId(context.getSuite().getParameter("execute_user_ytkUserId"))
                .phone(Long.parseLong(StringUtil.isEmpty(context.getSuite().getParameter("execute_user_phone")) ?
                        "0" : context.getSuite().getParameter("execute_user_phone")))
                .cookie(context.getSuite().getParameter("execute_user_cookie"))
                .build();

        userContext = new UserContext(user, env, device, app);
        userContext.setUser(user.isValid() ? user :
                user.getTag() == null ? new User() : commonContext.getUserMap().get(user.getTag()));
        userContext.setEnv(env.isValid() ? env :
                env.getTag() == null ? new User.Env() : commonContext.getEnvMap().get(env.getTag()));
        userContext.setDevice(device.isValid() ? device :
                device.getTag() == null ? new User.Device() : commonContext.getDeviceMap().get(device.getTag()));
        userContext.setApp(app.isValid() ? app :
                app.getTag() == null ? new User.App() : commonContext.getAppMap().get(app.getTag()));
        context.getSuite().setAttribute("userContext", userContext);
    }

    public static final int THOUSAND = 1000;
    public void sleepSeconds(int second) {
        try {
            Thread.sleep(second * THOUSAND);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUser(String userTag) {
        if (userContext.getUser() != null && userContext.getUser().getTag().equals(userTag)) {
            return userContext.getUser();
        }
        if (commonContext != null && !ObjectUtils.isEmpty(commonContext.getUserMap())
                && commonContext.getUserMap().containsKey(userTag)) {
            return commonContext.getUserMap().get(userTag);
        }
        User user = new User();
        user.setTag(userTag);
        return user;
    }

    public User.Device getDevice(String deviceTag) {
        return null;
    }

    public IDataSource getDataSource(String dbTag) {
        if (commonContext.getMysqlDataSourceMap() != null
                && !ObjectUtils.isEmpty(commonContext.getMysqlDataSourceMap())
        ) {
            return commonContext.getMysqlDataSourceMap().getOrDefault(dbTag, null);
        }
        return null;
    }

    public IDBConnection getConnection(String dbTag) {
        IDataSource dataSource = getDataSource(dbTag);
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        return null;
    }
}
