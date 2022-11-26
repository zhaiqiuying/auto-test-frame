package com.yuanfudao.aries.qa.itest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    private User user;
    private User.Env env;
    private User.Device device;
    private User.App app;
}
