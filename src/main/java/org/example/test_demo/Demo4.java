package org.example.test_demo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo4 {
    public static void main(String[] args) {
        String input = "【银行类别：01\\中国工商银行】【银行账户：4402939119100049806\\工行府河音乐花园支行9806】";
        String regex = "(?<=：)[^【】]+";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            System.out.println("匹配的内容: " + matcher.group().trim());
        }
    }
}
