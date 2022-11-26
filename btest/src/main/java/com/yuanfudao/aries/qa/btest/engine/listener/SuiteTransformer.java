package com.yuanfudao.aries.qa.btest.engine.listener;

import com.yuanfudao.aries.qa.btest.engine.annatation.Example;
import com.yuanfudao.aries.qa.btest.util.JavasistUtil;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.testng.IAlterSuiteListener;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.Method;
import java.util.List;

public class SuiteTransformer implements IAlterSuiteListener {

    public void alter(List<XmlSuite> suites) {

        for (XmlSuite xmlSuite : suites) {
            List<XmlTest> tests = xmlSuite.getTests();
            for (XmlTest test : tests) {
                List<XmlClass> classes = test.getClasses();
                for (XmlClass clazz : classes) {
                    Class<?> c = clazz.getSupportClass();
                    Method[] methods = c.getDeclaredMethods();
                    for (Method method : methods) {
                        transformExampleAnnotation(c, method);
                    }
                }
            }
        }
    }

    private void transformExampleAnnotation(Class<?> c, Method method) {
        Test testAnn = method.getAnnotation(Test.class);
        Example exaAnn = method.getAnnotation(Example.class);
        if (testAnn != null && exaAnn == null) {
            JavasistUtil.addAnnotationToMethod(c, method.getName(),
                    Test.class, (annotation, constPool) -> annotation.addMemberValue("enabled",
                            new BooleanMemberValue(constPool.addIntegerInfo(0), constPool)));
        } else if (testAnn == null && exaAnn != null) {
            JavasistUtil.addAnnotationToMethod(c, method.getName(),
                    Test.class, (annotation, constPool) -> annotation.addMemberValue("testName",
                            new StringMemberValue(constPool.addUtf8Info(exaAnn.testName()),
                                    constPool)));
        }
    }

}
