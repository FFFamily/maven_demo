package org.example;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo1 {
    public static void main(String[] args) {
        String input = ".SS:446432:IYJU0:总部.润楹物业服务（成都）有限公司重庆分公司工会委员会 总部";
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
