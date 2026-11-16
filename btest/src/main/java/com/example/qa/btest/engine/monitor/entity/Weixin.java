package com.example.qa.btest.engine.monitor.entity;

import com.example.qa.btest.engine.monitor.api.Receiver;
import com.example.qa.btest.model.TestContext;

public class Weixin implements Receiver {

    @Override
    public void notice(TestContext testContext) {
        System.out.println("weixin received");
    }
}
