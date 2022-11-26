package com.yuanfudao.aries.qa.btest.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {
    public static Object[][] listToArray(List list) {

        if (list == null) {
            return new Object[0][0];
        }

        List<Object> result = new ArrayList();
        for (Object item : list) {
            if (item instanceof List) {
                result.add(((List<?>) item).toArray());
            } else {
                result.add((Object) item);
            }
        }
        return result.toArray(new Object[0][0]);
    }

    public static void main(String[] args) {
        List list = new ArrayList();
        list.add(new ArrayList<Integer>() {{
            add(1);
            add(2);
        }});
        Object[][] o = listToArray(list);
    }
}
