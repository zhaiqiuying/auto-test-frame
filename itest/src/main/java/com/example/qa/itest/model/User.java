package com.example.qa.itest.model;

import com.example.qa.btest.util.StringUtil;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String tag;
    private String name;
    private String desc;
    private String ytkUserId;
    private String cookie;
    private long phone;
    private int sex; // 1: 男 2:女
    private boolean virtual; // is vertual user, 非真实手机号注册的用户为虚拟用户
    private boolean trial; // 是否体验用户
    private boolean vip; // 是否vip用户

    public boolean isValid() {
        return !StringUtil.isEmpty(ytkUserId) && !StringUtil.isEmpty(cookie);
    }
//    private Device device; //  使用设备
//    private App app; // APP
//    private Env env; // 环境

    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Device {

        private String tag;
        private String client;           // 设备类型
        private String id;         // 设备ID
        private String sysVersion; // 系统版本
        private String sysType;
        private String sysName;

        public boolean isValid() {
            return !StringUtil.isEmpty(sysType);
        }
    }
    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class App {
        private String tag;
        private String version; // app版本
        private String name; // app名称

        public boolean isValid() {
            return !StringUtil.isEmpty(version);
        }
    }

    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Env {
        private String laneId; // 泳道标识
        private String host; // 环境域名
        private String type; // 环境 test preonline product
        private String tag;  // 环境标识

        public boolean isValid() {
            return !StringUtil.isEmpty(host);
        }
    }

}
