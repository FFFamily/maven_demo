package org.example.test_demo;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo1 {
    public static void main(String[] args) {
        String input = ".CS:BFWWO-A000001:BFWW0:天津.业主客商 天津";
        String regex = ":(.*?)\\s";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            System.out.println("匹配结果: " + matcher.group(1));
        } else {
            System.out.println("未匹配到内容");
        }
    }
}
