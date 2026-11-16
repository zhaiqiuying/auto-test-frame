package com.example.qa.btest.engine.monitor.api;

import com.example.qa.btest.model.TestContext;

/**
 * 消息接收器
 */
public interface Receiver {

    void notice(TestContext testContext);
}
