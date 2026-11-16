package com.example.qa.btest.engine.asserts;

import org.testng.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodBasedAssert extends Assert {

    public static ThreadLocal<StringBuffer> threadLocalMsg = new ThreadLocal<StringBuffer>();

    static {
        threadLocalMsg.set(new StringBuffer());
    }

    /**
     * method内的断言全部执行完毕后再输出，用于一个断言失败，后续断言无法执行的情况
     * @param i
     * @param j
     */
    public static void assertIntByMethod(int i, int j) {
        try {
            Assert.assertEquals(i, j);
        } catch (Throwable e) {
            appendMsg(e, "");
        }
    }

    public static void assertTrueByMethod(boolean b) {
        assertTrue(b, "");
    }

    public static void assertTrueByMethod(boolean b, String desc) {
        try {
            Assert.assertTrue(b);
        } catch (Throwable e) {
            appendMsg(e, desc);
        }
    }

    public static void assertEqualsByMethod(String actual, String expect) {
        try {
            Assert.assertEquals(actual, expect);
        } catch (Throwable e) {
            appendMsg(e, e.getMessage());
        }
    }

    /**
     * 正则匹配
     *
     * @param actual
     * @param pat
     */
    public static void assertRegexEqualsByMethod(String actual, String pat) {
        Pattern pattern = Pattern.compile(pat);
        Matcher matcher = pattern.matcher(actual);
        if (matcher.matches()) {
            try {
                Assert.assertEquals(actual, actual);
            } catch (Throwable e) {
                appendMsg(e, e.getMessage());
            }
        } else {
            try {
                Assert.assertNotEquals(actual, actual);
            } catch (Throwable e) {
                appendMsg(e, e.getMessage());
            }
        }
    }

    private static void appendMsg(Throwable e, String desc) {
        StringBuffer sb = threadLocalMsg.get();
        sb = sb == null ? new StringBuffer() : sb;
        sb.append(desc).append("\n");
        threadLocalMsg.set(sb);
    }

    public static String printMsg() {
        String m = "";
        if (threadLocalMsg.get() != null) {
            m = threadLocalMsg.get().toString();
        }
        threadLocalMsg.set(new StringBuffer());
        return m;
    }
}

