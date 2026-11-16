package com.example.qa.btest.util;

import com.google.gson.Gson;
import com.example.qa.btest.model.HttpModel;
import com.example.qa.btest.model.HttpModel.Request;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpUtil {

    public static final String METHOD_GET = "get";
    public static final String METHOD_POST = "post";
    private static final String TOKEN = "^YJCFTY&*((*UHG4rtghnFGY";
    private static final Gson gson = new Gson();

    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(5000)
            .setConnectTimeout(5000)
            .setSocketTimeout(50000)
            .build();

    public static String execute(HttpUriRequest t) throws IOException {

        try (final CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            try (final CloseableHttpResponse response = httpClient.execute(t)) {

                final HttpEntity httpEntity = response.getEntity();

                if (httpEntity != null) {
                    return  EntityUtils.toString(httpEntity, Charset.forName("UTF-8"));
                }
            }
        }

        return "";
    }

    private static BasicNameValuePair getSsoHeader(Request request) {

        if (!ObjectUtils.isEmpty(request.getUser()) && !ObjectUtils.isEmpty(request.getCookie())) {
            return new BasicNameValuePair("Cookie", request.getCookie());
        }
        return null;
    }

    private static void addHeader(HttpUriRequest request, HttpModel httpModel) {
        if (!ObjectUtils.isEmpty(httpModel.getRequest().getHeaders())) {
            for (BasicNameValuePair header : httpModel.getRequest().getHeaders()) {
                request.addHeader(header.getName(), header.getValue());
            }
        }

        if (httpModel.getRequest().isNeedSso()) {
            BasicNameValuePair header = getSsoHeader(httpModel.getRequest());
            if (header != null) {
                request.addHeader(header.getName(), header.getValue());
            }
        }
    }

    private static final String URL_PARAM_INDEX_TAG = "?";
    public static String buildUrl(String url, Map<String, Object> param) {
        if (!ObjectUtils.isEmpty(param)) {

            List<BasicNameValuePair> params = new ArrayList();
            for (Map.Entry<String, Object> entry : param.entrySet()) {

                if (ObjectUtils.isArray(entry.getValue())) {
                    for (Object o : (Object[]) entry.getValue()) {
                        params.add(new BasicNameValuePair(entry.getKey(), o.toString()));
                    }
                } else {
                    params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
                }
            }
            if (!ObjectUtils.isEmpty(params)) {
                url = (StringUtils.contains(url, URL_PARAM_INDEX_TAG) ? url + "&" : url + URL_PARAM_INDEX_TAG)
                        + URLEncodedUtils.format((List<? extends NameValuePair>) params, "UTF-8");
            }
        }
        return url;
    }

    private static HttpUriRequest postRequest(HttpModel httpModel) {

        HttpPost request = new HttpPost(httpModel.getRequest().getUrl());
        if (ContentType.DEFAULT_TEXT.equals(httpModel.getRequest().getContentType())) {

        }
        else if (ContentType.APPLICATION_JSON.equals(httpModel.getRequest().getContentType())) {

            StringEntity entity;
            if (httpModel.getRequest().getParam().getClass() == String.class) {
                entity = new StringEntity((String) httpModel.getRequest().getParam(), "UTF-8");
            } else {
                entity = new StringEntity(gson.toJson(httpModel.getRequest().getParam()), "UTF-8");
            }
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            request.setEntity(entity);
        }
        else if (ContentType.APPLICATION_FORM_URLENCODED.equals(httpModel.getRequest().getContentType())) {
            StringEntity entity = new StringEntity(gson.toJson(httpModel.getRequest().getParam()), "UTF-8");
            request.setEntity(entity);
        }
        return request;
    }

    private static HttpUriRequest getRequest(HttpModel httpModel) {

        String url = buildUrl(httpModel.getRequest().getUrl(), (Map<String, Object>) httpModel.getRequest().getParam());
        HttpGet request = new HttpGet(url);
        return request;
    }

    public static String request(HttpModel httpModel) throws HttpException, IOException {

        HttpUriRequest request = null;

        switch (httpModel.getRequest().getMethod()) {
            case METHOD_GET:
                request = getRequest(httpModel);
                break;
            case METHOD_POST:
                request = postRequest(httpModel);
                break;
            default:
                throw new HttpException("not support");
        }

        addHeader(request, httpModel);
        String resp = execute(request);
        httpModel.getResponse().setResult(resp);
        return httpModel.getResponse().toString();
    }

    public static String buildUrl(HttpModel httpModel) {
        String result = "";
        try {
            switch (httpModel.getRequest().getMethod()) {
                case METHOD_GET:
                    result = buildUrl(httpModel.getRequest().getUrl(),
                            (Map)httpModel.getRequest().getParam());
                    break;
                case METHOD_POST:
                    break;
                default:
                    log.error("not support method, method:" + httpModel.getRequest().getMethod());
            }
            return result;
        } catch (Exception e) {
            log.error("Build url fail,", e);
            return httpModel.getRequest().getUrl();
        }
    }
}
