package org.example.拼接excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.拼接excel.entity.ABCDExcelData;
import org.example.拼接excel.entity.TemplateExcelData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class 拼接ABCDExcel {
    public static void main(String[] args) {

    }

    public static void doMerge(String file1,String file2,String targetFile){
        Map<String, ABCDExcelData> list1 = new HashMap<>();
        Map<String, TemplateExcelData> list2 = new HashMap<>();
        List<Object> result = new ArrayList<>();
        EasyExcel.read(file1, new PageReadListener<ABCDExcelData>(dataList -> {
            dataList.forEach(item -> {
                list1.put(item.getC().replaceAll("\\.","-"),item);
            });
        })).sheet("已匹配").doRead();
        EasyExcel.read(file2, new PageReadListener<TemplateExcelData>(dataList -> {
            dataList.forEach(item -> {
                list2.put(item.getA(),item);
            });
        })).sheet("明细原版").doRead();



        try (ExcelWriter excelWriter = EasyExcel.write("merge/组合ABCD字段.xlsx").build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "sheet1").build();
            excelWriter.write(result, writeSheet1);
        }
    }
}
