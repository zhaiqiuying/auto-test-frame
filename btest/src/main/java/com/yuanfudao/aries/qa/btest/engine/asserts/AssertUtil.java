package com.yuanfudao.aries.qa.btest.engine.asserts;

import com.yuanfudao.aries.qa.btest.model.Executor;
import org.testng.Assert;

public class AssertUtil extends MethodBasedAssert {

    public static void assertTrue(Executor t, String message) {
        try {
            Assert.assertTrue((Boolean) t.run(), message);
        } catch (Throwable e) {
            Assert.assertNull(e, e.getMessage() + "(" + message + ")");
        }
    }

    public static void assertFalse(Executor t, String message) {
        try {
            Assert.assertFalse((Boolean) t.run(), message);
        } catch (Throwable e) {
            Assert.assertNull(e, e.getMessage() + "(" + message + ")");
        }
    }
}
