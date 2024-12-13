package org.example.test_demo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo4 {
    public static void main(String[] args) {
        String input = "【项目：1231/南京禹洲吉庆里】【供应商档案：南京先禾园林绿化工程有限公司】";
        String regex = "(?<=：)([^/【】]+)(?:/([^【】]+))?";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            System.out.println("匹配的内容: " + matcher.group().trim());
        }
    }


//    public static void main(String[] args) {
//        String input = "【A: 123/你好】 【B:123】";
//        String regex = "(?<=：)([^/【】]+)(?:/([^【】]+))?";
//
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(input);
//
//        while (matcher.find()) {
//            System.out.println("部分1: " + matcher.group(1)); // 提取冒号后 `/` 前的部分
//            System.out.println("部分2: " + (matcher.group(2) != null ? matcher.group(2) : "无")); // 提取 `/` 后的部分（如果有）
//        }
//    }
}
