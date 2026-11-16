package com.example.qa.btest.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CsvUtil {

    public static String[][] read(String filePath) {

        int count = 0;
        String[] data = null;
        String returnObj[][] = null;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        ArrayList<String> content = new ArrayList<String>();

        try {
            br = new BufferedReader(new FileReader(filePath));
            int datalength = 0;
            int listsize = 0;

            while ((line = br.readLine()) != null) {
                content.add(line);
            }
            listsize = content.size();
            datalength = content.get(0).split(cvsSplitBy).length;
            returnObj = new String[listsize][datalength];

            for (int i = 0; i < listsize; i++) {
                data = content.get(i).split(cvsSplitBy);
                for (int j = 0; j < datalength; j++) {
                    returnObj[i][j] = data[j];
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return returnObj;
    }
}