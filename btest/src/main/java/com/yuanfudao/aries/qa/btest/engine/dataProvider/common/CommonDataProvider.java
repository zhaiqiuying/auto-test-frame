package com.yuanfudao.aries.qa.btest.engine.dataProvider.common;

import com.github.superproxy.testdataprovider.Path;
import com.yuanfudao.aries.qa.btest.engine.dataProvider.csv.CsvDataProvider;
import com.yuanfudao.aries.qa.btest.engine.dataProvider.mysql.MysqlDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CommonDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonDataProvider.class);

    private static Map<Class, DataProvider> dataProviderMap = new HashMap<Class, DataProvider>();

    static {
        register(new MysqlDataProvider());
        register(new CsvDataProvider());
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
        methodContext.getValueMap().put("url", context.getSuite().getXmlSuite().getParameter("mysql_url"));
        methodContext.getValueMap().put("user", context.getSuite().getXmlSuite().getParameter("mysql_user"));
        methodContext.getValueMap().put("password", context.getSuite().getXmlSuite().getParameter("mysql_password"));
        LOGGER.debug("methodContext:{}", methodContext);
        for (Annotation annotation : annotations) {
            Class c = annotation.annotationType();
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
