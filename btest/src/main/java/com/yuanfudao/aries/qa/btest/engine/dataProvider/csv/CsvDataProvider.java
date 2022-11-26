package com.yuanfudao.aries.qa.btest.engine.dataProvider.csv;

import com.github.superproxy.testdataprovider.MethodUtils;
import com.yuanfudao.aries.qa.btest.engine.dataProvider.common.DataProvider;
import com.yuanfudao.aries.qa.btest.engine.dataProvider.common.MethodContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CsvDataProvider implements DataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvDataProvider.class);

    public CsvDataProvider() {
    }

    public Class getDataType() {
        return Csv.class;
    }

    /**
     * 从csv获取数据
     * @param methodContext
     * @return
     */
    public Object[][] getObjects(MethodContext methodContext) {
        Annotation annotation = methodContext.getAnnotation();
        Method method = methodContext.getMethod();
        if (annotation instanceof Csv) {
            String path = ((Csv)annotation).value();
            if (StringUtils.isNoneEmpty(new CharSequence[]{path})) {
                boolean isRelativePath = ((Csv)annotation).isRelativePath();
                methodContext.setPath(
                        isRelativePath ? "/" + System.getProperty("ENV_TYPE", "") + "/" + path : path);
                LOGGER.debug("csv path is {} of method {}", methodContext.getPath(), method.getName());
            }

            String encoding = ((Csv)annotation).encoding();
            methodContext.setEncoding(encoding);
        }

        try {
            return getObjects(methodContext, method);
        } catch (Exception var6) {
            throw new RuntimeException(var6);
        }
    }

    private static Object[][] getObjects(MethodContext methodContext, Method method) throws Exception {
        String filePath = methodContext.getPath();
        String encoding = methodContext.getEncoding();
        LOGGER.debug("path:{}", (new File(".")).getAbsolutePath());
        Scanner scanner;
        if (StringUtils.isEmpty(encoding)) {
            scanner = new Scanner(com.github.superproxy.testdataprovider.support.csv.CsvDataProvider.class.getResourceAsStream(filePath));
        } else {
            scanner = new Scanner(com.github.superproxy.testdataprovider.support.csv.CsvDataProvider.class.getResourceAsStream(filePath), encoding);
        }

        String head = scanner.nextLine();
        LOGGER.debug("head:{}", head);
        String[] paramNames = MethodUtils.getParameterNames(method);
        int paramCount = paramNames.length;
        Class[] paraTypes = method.getParameterTypes();
        List<Object[]> list = new ArrayList();

        while(scanner.hasNext()) {
            String data = scanner.nextLine();
            String[] array = data.split(",");
            LOGGER.debug("data:{}", data);
            Object[] objects = new Object[paramCount];

            for(int i = 0; i < paramCount; ++i) {
                objects[i] = convert(paraTypes[i], array[i]);
            }

            list.add(objects);
        }

        return (Object[][])list.toArray(new Object[0][0]);
    }

    private static Object convert(Class paraType, String s) {
        Object object;
        if (!paraType.equals(Byte.TYPE) && !paraType.equals(Byte.class)) {
            if (!paraType.equals(Integer.TYPE) && !paraType.equals(Integer.class)) {
                if (!paraType.equals(Short.TYPE) && !paraType.equals(Short.class)) {
                    if (!paraType.equals(Long.TYPE) && !paraType.equals(Long.class)) {
                        if (!paraType.equals(Float.TYPE) && !paraType.equals(Float.class)) {
                            if (!paraType.equals(Double.TYPE) && !paraType.equals(Double.class)) {
                                if (!paraType.equals(Boolean.TYPE) && !paraType.equals(Boolean.class)) {
                                    if (!paraType.equals(Character.TYPE) && !paraType.equals(Character.class)) {
                                        if (paraType.equals(String.class)) {
                                            object = s;
                                        } else {
                                            object = s;
                                        }
                                    } else {
                                        object = s.charAt(0);
                                    }
                                } else {
                                    object = Boolean.valueOf(s);
                                }
                            } else {
                                object = Double.valueOf(s);
                            }
                        } else {
                            object = Float.valueOf(s);
                        }
                    } else {
                        object = Long.valueOf(s);
                    }
                } else {
                    object = Short.valueOf(s);
                }
            } else {
                object = Integer.valueOf(s);
            }
        } else {
            object = Byte.valueOf(s);
        }

        return object;
    }
}
