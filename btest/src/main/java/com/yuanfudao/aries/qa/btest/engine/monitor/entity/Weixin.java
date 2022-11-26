package com.yuanfudao.aries.qa.btest.engine.monitor.entity;

import com.yuanfudao.aries.qa.btest.engine.monitor.api.Receiver;
import com.yuanfudao.aries.qa.btest.model.TestContext;

public class Weixin implements Receiver {

    @Override
    public void notice(TestContext testContext) {
        System.out.println("weixin received");
    }
}
