package com.example.qa.itest.cases;

import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.logging.LogConfig;
import com.example.qa.itest.model.CommonContext;
import com.example.qa.itest.model.User;
import com.example.qa.itest.model.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.ObjectUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import java.util.Map;

@ContextConfiguration(locations = { "classpath:common/ApplicationContext.xml" })
public class BaseCase extends AbstractTestNGSpringContextTests {

    private static final String KEY_ENV_TYPE = "ENV_TYPE";
    private static final String DEFAULT_ENV_TYPE = "test";
    private static final String KEY_LOGBACK_CONFIG = "logback.configurationFile";
    private static final String KEY_EXECUTE_DEVICE_TAG = "execute_device_tag";
    private static final String KEY_EXECUTE_DEVICE_CLIENT = "execute_device_client";
    private static final String KEY_EXECUTE_DEVICE_ID = "execute_device_id";
    private static final String KEY_EXECUTE_APP_TAG = "execute_app_tag";
    private static final String KEY_EXECUTE_APP_VERSION = "execute_app_version";
    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_EXECUTE_ENV_TAG = "execute_env_tag";
    private static final String KEY_EXECUTE_ENV_HOST = "execute_env_host";
    private static final String KEY_EXECUTE_ENV_LANE_ID = "execute_env_laneId";
    private static final String KEY_EXECUTE_ENV_TYPE = "execute_env_type";
    private static final String KEY_EXECUTE_USER_TAG = "execute_user_tag";
    private static final String KEY_EXECUTE_USER_YTK_USER_ID = "execute_user_ytkUserId";
    private static final String KEY_EXECUTE_USER_PHONE = "execute_user_phone";
    private static final String KEY_EXECUTE_USER_COOKIE = "execute_user_cookie";
    private static final String ATTR_USER_CONTEXT = "userContext";

    private static final int MILLIS_PER_SECOND = 1000;

    public UserContext userContext;
    @Autowired
    private CommonContext commonContext;

    static {
        // 设置logback配置文件
        String envType = System.getProperty(KEY_ENV_TYPE, DEFAULT_ENV_TYPE);
        System.setProperty(KEY_LOGBACK_CONFIG,
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
        User.Device device = buildDevice(context);
        User.App app = buildApp(context);
        User.Env env = buildEnv(context);
        User user = buildUser(context);

        userContext = new UserContext(user, env, device, app);
        userContext.setUser(user.isValid() ? user :
                user.getTag() == null ? new User() : commonContext.getUserMap().get(user.getTag()));
        userContext.setEnv(env.isValid() ? env :
                env.getTag() == null ? new User.Env() : commonContext.getEnvMap().get(env.getTag()));
        userContext.setDevice(device.isValid() ? device :
                device.getTag() == null ? new User.Device() : commonContext.getDeviceMap().get(device.getTag()));
        userContext.setApp(app.isValid() ? app :
                app.getTag() == null ? new User.App() : commonContext.getAppMap().get(app.getTag()));
        context.getSuite().setAttribute(ATTR_USER_CONTEXT, userContext);
    }

    private User.Device buildDevice(ITestContext context) {
        return User.Device.builder()
                .tag(context.getSuite().getParameter(KEY_EXECUTE_DEVICE_TAG))
                .client(context.getSuite().getParameter(KEY_EXECUTE_DEVICE_CLIENT))
                .id(context.getSuite().getParameter(KEY_EXECUTE_DEVICE_ID))
                .build();
    }

    private User.App buildApp(ITestContext context) {
        return User.App.builder()
                .tag(context.getSuite().getParameter(KEY_EXECUTE_APP_TAG))
                .version(context.getSuite().getParameter(KEY_EXECUTE_APP_VERSION))
                .name(context.getSuite().getParameter(KEY_APP_NAME))
                .build();
    }

    private User.Env buildEnv(ITestContext context) {
        return User.Env.builder()
                .tag(context.getSuite().getParameter(KEY_EXECUTE_ENV_TAG))
                .host(context.getSuite().getParameter(KEY_EXECUTE_ENV_HOST))
                .laneId(context.getSuite().getParameter(KEY_EXECUTE_ENV_LANE_ID))
                .type(context.getSuite().getParameter(KEY_EXECUTE_ENV_TYPE))
                .build();
    }

    private User buildUser(ITestContext context) {
        String phoneStr = context.getSuite().getParameter(KEY_EXECUTE_USER_PHONE);
        return User.builder()
                .tag(context.getSuite().getParameter(KEY_EXECUTE_USER_TAG))
                .ytkUserId(context.getSuite().getParameter(KEY_EXECUTE_USER_YTK_USER_ID))
                .phone(Long.parseLong(StringUtils.isEmpty(phoneStr) ? "0" : phoneStr))
                .cookie(context.getSuite().getParameter(KEY_EXECUTE_USER_COOKIE))
                .build();
    }

    public void sleepSeconds(int second) {
        if (second < 0) {
            throw new IllegalArgumentException("second must not be negative: " + second);
        }
        try {
            Thread.sleep(second * MILLIS_PER_SECOND);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public User getUser(String userTag) {
        if (userContext.getUser() != null && userTag != null && userTag.equals(userContext.getUser().getTag())) {
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
        throw new UnsupportedOperationException("getDevice not implemented");
    }

    private IDataSource getDataSource(Map<String, IDataSource> map, String tag) {
        if (map != null && !ObjectUtils.isEmpty(map)) {
            return map.getOrDefault(tag, null);
        }
        return null;
    }

    private IDBConnection getConnection(Map<String, IDataSource> map, String tag) {
        IDataSource dataSource = getDataSource(map, tag);
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        return null;
    }

    public IDataSource getDataSource(String dbTag) {
        return getDataSource(commonContext.getMysqlDataSourceMap(), dbTag);
    }

    public IDBConnection getConnection(String dbTag) {
        return getConnection(commonContext.getMysqlDataSourceMap(), dbTag);
    }

    public IDataSource getRedisDataSource(String tag) {
        return getDataSource(commonContext.getRedisDataSourceMap(), tag);
    }

    public IDBConnection getRedisConnection(String tag) {
        return getConnection(commonContext.getRedisDataSourceMap(), tag);
    }
}
