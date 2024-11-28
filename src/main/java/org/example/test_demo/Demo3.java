package org.example.test_demo;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.SourceFileData;

import java.util.ArrayList;
import java.util.List;

public class Demo3 {
    private static String getValue(String str){
        return str == null ? "" : str;
    }
    public static void main(String[] args) {
        List<SourceFileData> sourceFileDataList = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/分类/9月科目辅助余额表.xlsx", SourceFileData.class, new PageReadListener<SourceFileData>(dataList -> {
            for (SourceFileData i : dataList) {
                if (i.getSEGMENT3_NAME().startsWith("应付账款")
                        || i.getSEGMENT3_NAME().startsWith("预付账款")
                        || i.getSEGMENT3_NAME().startsWith("合同负债")
                        || i.getSEGMENT3_NAME().startsWith("应收账款")
                        || i.getSEGMENT3_NAME().startsWith("其他应付款")
                        || i.getSEGMENT3_NAME().startsWith("其他应收款")){
                    String matchField = getValue(i.getSEGMENT1_NAME())  + "." +
                            getValue(i.getSEGMENT2_NAME()) + "." +
                            getValue(i.getSEGMENT3_NAME()) + "." +
                            getValue(i.getSEGMENT4_NAME()) + "." +
                            getValue(i.getSEGMENT5_NAME()) + "." +
                            getValue(i.getSEGMENT6_NAME()) + "." +
                            getValue(i.getSEGMENT7_NAME()) + "." +
                            getValue(i.getSEGMENT8_NAME()) + "." +
                            getValue(i.getSEGMENT9_NAME()) + "." +
                            getValue(i.getSEGMENT10_NAME());
                    String matchFieldCode = getValue(i.getSEGMENT1())  + "." +
                            getValue(i.getSEGMENT2()) + "." +
                            getValue(i.getSEGMENT3()) + "." +
                            getValue(i.getSEGMENT4()) + "." +
                            getValue(i.getSEGMENT5()) + "." +
                            getValue(i.getSEGMENT6()) + "." +
                            getValue(i.getSEGMENT7()) + "." +
                            getValue(i.getSEGMENT8()) + "." +
                            getValue(i.getSEGMENT9()) + "." +
                            getValue(i.getSEGMENT10());
                    i.setMatch(matchFieldCode);
                    i.setMatchName(matchField);
                    sourceFileDataList.add(i);
                }
            }
        })).sheet("Sheet1").doRead();

        List<SourceFileData> sourceFileDataList2 = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/分类/9月科目辅助余额表.xlsx", SourceFileData.class, new PageReadListener<SourceFileData>(dataList -> {
            sourceFileDataList2.addAll(dataList);
        })).sheet("Sheet1").doRead();
        System.out.println("9月科目辅助余额表 读取完成");
    }
}
