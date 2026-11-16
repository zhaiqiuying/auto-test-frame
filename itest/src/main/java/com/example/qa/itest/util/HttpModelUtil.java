package com.example.qa.itest.util;

import com.example.qa.btest.model.HttpModel;
import com.example.qa.btest.util.HttpUtil;
import com.example.qa.itest.model.UserContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.entity.ContentType;

import java.util.Map;
import java.util.Set;

public class HttpModelUtil {

    private final static String SIGN_SALT = "wdi4n2t8edr";

    public static String format(String url, UserContext userContext, Object param) {
        String u = url.replace("{server}", userContext.getEnv().getHost())
                .replace("{client}", userContext.getDevice().getClient())
                .replace("{app_version}", userContext.getApp().getVersion());

        if (!u.startsWith("http:") && !u.startsWith("https:")) {
            u = userContext.getEnv().getHost() + "/" + u;
        }

        if (param != null && param instanceof Map) {
            for (Map.Entry entry : (Set<Map.Entry>)((Map) param).entrySet()) {
                u = u.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }

        if (u.indexOf('?') > 0) {
            String sign = getSign(buildContent(u.substring(u.indexOf('?') + 1)), 0);
            u = u + "&aries_sign=" + "xysttest";
        }
        return u;
    }

    public static String getSign(String content, int offset) {
        long now = System.currentTimeMillis();
        long seconds = now / 1000;
        seconds = (seconds + offset) / 60;
        return DigestUtils.md5Hex(content + seconds);
    }
    public static String buildContent(String url) {
        return url + SIGN_SALT;
    }

    public static HttpModel create(String url, String method, UserContext userContext, Object param) {
        if (HttpUtil.METHOD_GET.equals(method)) {
            url = HttpUtil.buildUrl(url, (Map<String, Object>) param);
        }
        url = format(url, userContext, param);
        HttpModel.Request request = HttpModel.Request.builder()
                .needSso(true)
                .url(url)
                .user(userContext.getUser().getYtkUserId())
                .cookie(userContext.getUser().getCookie())
                .method(method)
                .param(param)
                .build();
        // 默认传递json格式
        if (HttpUtil.METHOD_POST.equals(method)) {
            request.setContentType(ContentType.APPLICATION_JSON);
        } else if (HttpUtil.METHOD_GET.equals(method)) {
            request.setParam(null);
        }

        HttpModel httpModel = new HttpModel();
        httpModel.setRequest(request);
        return httpModel;
    }

    public static HttpModel create(String url, String method, UserContext userContext) {
        return create(url, method, userContext, null);
    }
}
