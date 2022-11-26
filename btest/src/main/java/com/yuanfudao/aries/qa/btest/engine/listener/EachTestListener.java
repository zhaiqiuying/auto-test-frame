package com.yuanfudao.aries.qa.btest.engine.listener;

import com.yuanfudao.aries.qa.btest.engine.asserts.MethodBasedAssert;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.internal.TestResult;

public class EachTestListener implements IInvokedMethodListener {

    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            String m = MethodBasedAssert.printMsg();
            if (!m.equals("")) {
                testResult.setStatus(TestResult.FAILURE);
                testResult.setThrowable(new AssertionError(m));
            }
        }
    }
}
