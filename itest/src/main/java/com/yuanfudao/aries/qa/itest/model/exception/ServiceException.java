package com.yuanfudao.aries.qa.itest.model.exception;

public class ServiceException extends Exception {

    public ServiceException(String desc, Exception e) {
        super(desc, e);
    }
}
