package com.yuanfudao.aries.qa.itest.service;

import com.yuanfudao.aries.qa.btest.model.HttpModel;
import com.yuanfudao.aries.qa.btest.util.HttpUtil;
import com.yuanfudao.aries.qa.itest.model.User;
import com.yuanfudao.aries.qa.itest.model.UserContext;
import com.yuanfudao.aries.qa.itest.model.exception.ServiceException;
import com.yuanfudao.aries.qa.itest.util.HttpModelUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface CommonService {

     public final static Logger LOGGER = LoggerFactory.getLogger(CommonService.class);
     public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    default String sendAndGetHttpResult(String url, String method, UserContext user) {
        return sendAndGetHttpResult(url, method, user, null);
    }
     default String sendAndGetHttpResult(String url, String method, UserContext user, Object param) {
         HttpModel httpModel = HttpModelUtil.create(url, method, user, param); // 构造一个http对象
         sendHttpRequest(httpModel); // 执行http请求
         return (String) httpModel.getResponse().getResult();
     }

    /**
     * 发送http请求
     * @param httpModel
     */
    default void sendHttpRequest(HttpModel httpModel) {
        try {
            HttpUtil.request(httpModel);
        } catch (Exception e) {
            Exception ee = new ServiceException(Thread.currentThread().getStackTrace()[2].getMethodName(), e);
            LOGGER.error("", ee);
            httpModel.getResponse().setResult(e.toString());
        }
    }

    default Map<String, User> toUsers(List list) {
        if (ObjectUtils.isEmpty(list)) {
            return new ConcurrentHashMap<>();
        }
        Map result = new ConcurrentHashMap<>();
        for (Object item : list) {
            try {
                User user = OBJECT_MAPPER.convertValue(item, User.class);
                result.put(user.getTag(), user);
            } catch (Exception e) {
                LOGGER.error("toUsers error", e);
            }
        }
        return result;
    }

    default Map<String, User.Device> toDevices(List list) {
        if (ObjectUtils.isEmpty(list)) {
            return new ConcurrentHashMap<>();
        }
        Map result = new ConcurrentHashMap<>();
        for (Object item : list) {
            try {
                User.Device device = OBJECT_MAPPER.convertValue(item, User.Device.class);
                result.put(device.getTag(), device);
            } catch (Exception e) {
                LOGGER.error("toDevices error", e);
            }
        }
        return result;
    }

    default Map<String, User.App> toApps(List list) {
        if (ObjectUtils.isEmpty(list)) {
            return new ConcurrentHashMap<>();
        }
        Map result = new ConcurrentHashMap<>();
        for (Object item : list) {
            try {
                User.App app = OBJECT_MAPPER.convertValue(item, User.App.class);
                result.put(app.getTag(), app);
            } catch (Exception e) {
                LOGGER.error("toApps error", e);
            }
        }
        return result;
    }

    default Map<String, User.Env> toEnvs(List list) {
        if (ObjectUtils.isEmpty(list)) {
            return new ConcurrentHashMap<>();
        }
        Map result = new ConcurrentHashMap<>();
        for (Object item : list) {
            try {
                User.Env env = OBJECT_MAPPER.convertValue(item, User.Env.class);
                result.put(env.getTag(), env);
            } catch (Exception e) {
                LOGGER.error("toEnvs error", e);
            }
        }
        return result;
    }
}
