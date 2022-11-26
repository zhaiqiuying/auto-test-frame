package com.yuanfudao.aries.qa.btest.model;

import lombok.Data;

@Data
public class Executor<T> {

    private Runner runner;

    public Executor(Runner runner) {
        this.runner = runner;
    }

    public <T> T run() {
        return (T) this.runner.run();
    }
}
