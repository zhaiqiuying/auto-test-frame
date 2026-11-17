package com.example.qa.btest.engine.dataProvider.common;

import com.github.superproxy.testdataprovider.Path;
import com.example.qa.btest.engine.dataProvider.csv.CsvDataProvider;
import com.example.qa.btest.engine.dataProvider.mysql.MysqlDataProvider;
import com.example.qa.btest.engine.dataProvider.redis.RedisDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommonDataProvider {

    private static final String KEY_URL = "url";
    private static final String KEY_USER = "user";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REDIS_HOST = "redis_host";
    private static final String KEY_REDIS_PORT = "redis_port";
    private static final String KEY_REDIS_PASSWORD = "redis_password";
    private static final String KEY_MYSQL_URL = "mysql_url";
    private static final String KEY_MYSQL_USER = "mysql_user";
    private static final String KEY_MYSQL_PASSWORD = "mysql_password";

    private static final Map<Class<?>, DataProvider> dataProviderMap = new HashMap<>();

    static {
        register(new MysqlDataProvider());
        register(new CsvDataProvider());
        register(new RedisDataProvider());
    }

    public static void register(DataProvider dataProvider) {
        dataProviderMap.put(dataProvider.getDataType(), dataProvider);
    }

    /**
     * 自动读取测试文件json或者cvs中的数据
     *
     * @param method
     * @return
     */
    @org.testng.annotations.DataProvider
    public static Object[][] genData(Method method, ITestContext context) {
        Annotation[] annotations = method.getAnnotations();
        MethodContext methodContext = testObject(annotations);
        methodContext.getValueMap().put(KEY_URL, context.getSuite().getXmlSuite().getParameter(KEY_MYSQL_URL));
        methodContext.getValueMap().put(KEY_USER, context.getSuite().getXmlSuite().getParameter(KEY_MYSQL_USER));
        methodContext.getValueMap().put(KEY_PASSWORD, context.getSuite().getXmlSuite().getParameter(KEY_MYSQL_PASSWORD));
        methodContext.getValueMap().put(KEY_REDIS_HOST, context.getSuite().getXmlSuite().getParameter(KEY_REDIS_HOST));
        methodContext.getValueMap().put(KEY_REDIS_PORT, context.getSuite().getXmlSuite().getParameter(KEY_REDIS_PORT));
        methodContext.getValueMap().put(KEY_REDIS_PASSWORD, context.getSuite().getXmlSuite().getParameter(KEY_REDIS_PASSWORD));
        log.debug("methodContext:{}", methodContext);
        for (Annotation annotation : annotations) {
            Class<?> c = annotation.annotationType();
            if (dataProviderMap.containsKey(c)) {
                DataProvider dataProvider = dataProviderMap.get(c);
                methodContext.setAnnotation(annotation);
                methodContext.setMethod(method);
                return dataProvider.getObjects(methodContext);
            }

        }
        return new Object[0][0];
    }

    public static MethodContext testObject(Annotation[] annotations) {
        MethodContext methodContext = new MethodContext();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Path) {
                methodContext.setPath(((Path) annotation).value());
            }
        }
        return methodContext;
    }
}
