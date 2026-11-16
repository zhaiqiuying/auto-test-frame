package com.example.qa.itest.engine.listener;

import com.example.qa.itest.cases.BaseCase;
import com.example.qa.itest.model.User;
import com.example.qa.itest.model.UserContext;
import org.testng.*;

@Deprecated
public class ItestTestListener implements ITestListener, IInvokedMethodListener {

    public void onStart(ITestContext context) {

        try {
            UserContext userContext = (UserContext) context.getSuite().getAttribute("userContext");

            User user = userContext.getUser();
            User.Env env = userContext.getEnv();
            User.Device device = userContext.getDevice();
            User.App app = userContext.getApp();

//            userContext.setUser(user.isValid() ? user : Config.COMMON_CONTEXT.getUserMap().get(user.getTag()));
//            userContext.setEnv(env.isValid() ? env : Config.COMMON_CONTEXT.getEnvMap().get(env.getTag()));
//            userContext.setDevice(device.isValid() ? device : Config.COMMON_CONTEXT.getDeviceMap().get(device.getTag()));
//            userContext.setApp(app.isValid() ? app : Config.COMMON_CONTEXT.getAppMap().get(app.getTag()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
        if (method.isTestMethod() && ((BaseCase) method.getTestMethod().getInstance()).userContext == null) {
            ((BaseCase) method.getTestMethod().getInstance()).userContext =
                    (UserContext) context.getSuite().getAttribute("userContext");
        }
    }
}
