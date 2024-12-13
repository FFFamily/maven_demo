package org.example.test_demo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo4 {
    public static void main(String[] args) {
        String input = "【项目：南京禹洲吉庆里】【供应商档案：南京先禾园林绿化工程有限公司】";
        String regex = "(?<=：)[^【】]+";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            System.out.println("匹配的内容: " + matcher.group().trim());
        }
    }
}
