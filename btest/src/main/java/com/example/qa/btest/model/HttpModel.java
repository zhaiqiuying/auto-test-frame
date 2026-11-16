package com.example.qa.btest.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class HttpModel {

    private int rank;
    private Request request;
    private Response response;

    public HttpModel() {
        this.request = Request.builder().build();
        this.request.setNeedSso(true);
        this.request.setParam(new HashMap<>());
        this.response = Response.builder().build();
        this.response.setResultMap(new HashMap<>());
    }

    @Getter
    @Setter
    @Builder
    public static class Request {
        private boolean needSso;
        private List<BasicNameValuePair> headers;
        private ContentType contentType;
        private String user;
        private String cookie;
        private String url;
        private String method;
        private Object param;

        @Override
        public String toString() {
            return "url:" + url + "\nparam:" + param.toString() + "\n";
        }
    }

    @Getter
    @Setter
    @Builder
    public static class Response {
        private int code;
        private String msg;
        private int num;
        private Object result;
        private Map<String, Object> resultMap;
    }

}
