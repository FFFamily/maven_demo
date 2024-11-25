package org.example.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.core.entity.SourceFileData;

import java.util.ArrayList;
import java.util.List;

public class ExcelDataUtil {
    public static List<SourceFileData> getExcelData(String filePath,String sheetName){
        List<SourceFileData> sourceFileDataList = new ArrayList<>();
        EasyExcel.read(filePath, SourceFileData.class, new PageReadListener<SourceFileData>(dataList -> {
            dataList.forEach(i -> {
                String matchField = getValue(i.getSEGMENT1_NAME())  + "." +
                        getValue(i.getSEGMENT2_NAME()) + "." +
                        getValue(i.getSEGMENT3_NAME()) + "." +
                        getValue(i.getSEGMENT4_NAME()) + "." +
                        getValue(i.getSEGMENT5_NAME()) + "." +
                        getValue(i.getSEGMENT6_NAME()) + "." +
                        getValue(i.getSEGMENT7_NAME()) + "." +
                        getValue(i.getSEGMENT8_NAME()) + "." +
                        getValue(i.getSEGMENT9_NAME()) + "." +
                        getValue(i.getSEGMENT10_NAME()) + ".";
//                        getValue(i.getTransactionObjectCode()) + "." +
//                        getValue(i.getTransactionObjectName());
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
            });
        })).sheet(sheetName).doRead();
        return sourceFileDataList;
    }

    private static String getValue(String str){
        return str == null ? "" : str;
    }
}
