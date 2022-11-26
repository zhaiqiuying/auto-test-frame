package com.yuanfudao.aries.qa.btest.engine.monitor.api;

import com.yuanfudao.aries.qa.btest.model.TestContext;

/**
 * 消息接收器
 */
public interface Receiver {

    void notice(TestContext testContext);
}
