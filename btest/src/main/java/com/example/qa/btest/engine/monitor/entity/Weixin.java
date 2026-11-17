package com.example.qa.btest.engine.monitor.entity;

import com.example.qa.btest.engine.monitor.api.Receiver;
import com.example.qa.btest.model.TestContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Weixin implements Receiver {

    @Override
    public void notice(TestContext testContext) {
        log.info("weixin received");
    }
}
