package com.example.qa.btest.engine.listener;

import org.apache.commons.codec.digest.DigestUtils;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryNum = 0;
    private static Map<Integer, Integer> retryNumMap = new ConcurrentHashMap<>(); // 存储重试次数，对于DDT模式用例会有问题

    @Override
    public boolean retry(ITestResult iTestResult) {
        retryNum += 1;
        Method method = iTestResult.getMethod().getConstructorOrMethod().getMethod();
        int methodMd5 = DigestUtils.md5Hex(method.toGenericString() + method.getParameters().toString()).hashCode();
        if (!retryNumMap.containsKey(methodMd5)) {
            Annotation[] annotations = method.getAnnotations();
            if (annotations != null && annotations.length > 0) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Retry) {
                        int retryNum = ((Retry) annotation).maxNum();
                        retryNumMap.put(methodMd5, retryNum);
                    }
                }
            }
        }
        if (retryNumMap.getOrDefault(methodMd5, 0) >= retryNum) {
            return true;
        }
        return false;
    }
}
