package com.yuanfudao.aries.qa.btest.engine.asserts;

import org.testng.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodBasedAssert {
    
    private static ThreadLocal<StringBuffer> threadLocalMsg = new ThreadLocal<StringBuffer>();

    static {
        threadLocalMsg.set(new StringBuffer());
    }

    public static void assertInt(int i, int j) {
        try {
            Assert.assertEquals(i, j);
        } catch (Throwable e) {
            StringBuffer sb = threadLocalMsg.get();
            sb = sb == null ? new StringBuffer() : sb;
            sb.append(e.toString()).append("\n");
            threadLocalMsg.set(sb);
        }
    }

    public static void assertTrue(boolean b) {
        assertTrue(b, "");
    }
    public static void assertTrue(boolean b, String desc) {
        try {
            Assert.assertTrue(b);
        } catch (Throwable e) {
            StringBuffer sb = threadLocalMsg.get();
            sb = sb == null ? new StringBuffer() : sb;
            sb.append(desc).append("\n");
            threadLocalMsg.set(sb);
        }
    }
    public static String printMsg() {
        String m = "";
        if (threadLocalMsg.get() != null) {
            m = threadLocalMsg.get().toString();
        }
        threadLocalMsg.set(new StringBuffer());
        return m;
    }
    public static void assertEquals(String actual, String expect){
        try {
            Assert.assertEquals(actual, expect);
        } catch (Throwable e){
            threadLocalMsg.set(new StringBuffer().append(e.toString()).append("\n"));
        }
    }

    /**
     * 正则匹配
     * @param actual
     * @param pat
     */
    public static void assertRegexEquals(String actual, String pat){
        Pattern pattern = Pattern.compile(pat);
        Matcher matcher = pattern.matcher(actual);
        if(matcher.matches()){
            try {
                Assert.assertEquals(actual, actual);
            } catch (Throwable e){
                threadLocalMsg.set(new StringBuffer().append(e.toString()).append("\n"));
            }
        }else{
            try{
                Assert.assertNotEquals(actual, actual);
            } catch (Throwable e){
                threadLocalMsg.set(new StringBuffer().append(e.toString()).append("\n"));
            }
        }
    }
}

