package com.example.qa.btest.engine.dataProvider.mysql;

import com.example.qa.btest.db.DataSourceFactory;
import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.engine.dataProvider.common.DataProvider;
import com.example.qa.btest.engine.dataProvider.common.MethodContext;
import com.example.qa.btest.util.ListUtil;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MysqlDataProvider implements DataProvider {

    @Override
    public Class getDataType() {
        return Mysql.class;
    }

    /**
     * 从mysql中获取数据
     * @param methodContext
     * @return
     */
    @Override
    public Object[][] getObjects(MethodContext methodContext) {
        Annotation annotation = methodContext.getAnnotation();
        Method method = methodContext.getMethod();
        if (annotation instanceof Mysql) {
            String url = ((Mysql) annotation).url();
            String user = ((Mysql) annotation).user();
            String password = ((Mysql) annotation).password();
            String sql = ((Mysql) annotation).sql();

            Map<String, Object> dataSourceConfMap = methodContext.getValueMap();
            dataSourceConfMap.put("maxWait", "500");

            if (StringUtils.isNoneEmpty(url)) {
                dataSourceConfMap.put("url", url);
                dataSourceConfMap.put("user", user);
                dataSourceConfMap.put("password", password);
            }
            if (StringUtils.isNoneEmpty((String) dataSourceConfMap.get("url"))) {
                log.debug("url:{}, user:{}, password:{}, sql:{}", url, user, password, sql);
                try {
                    return getObjects(dataSourceConfMap, sql);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new Object[0][0];
    }

    private static Object[][] getObjects(Map<String, Object> dataSourceConfMap, String sql) throws Exception {
        IDataSource dataSource = DataSourceFactory.createDataSource(dataSourceConfMap);
        List<Object[]> list = new ArrayList<Object[]>();
        if (dataSource != null) {
            IDBConnection connection = dataSource.getConnection();
            try {
                list = (List) connection.execute(sql);
                log.info("get {} data from mysql", list.size());
            } catch (SQLException e) {
                log.error("execute sql error: {}", sql, e);
            } finally {
                connection.close();
            }
        }
        return (Object[][]) ListUtil.listToArray(list);
    }
}
