package org.example.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.core.entity.SourceFileData;
import org.example.分类.entity.DraftFormatTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelDataUtil {
    public static List<SourceFileData> getExcelData(String filePath, String sheetName){
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
                        getValue(i.getSEGMENT10_NAME());
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

    /**
     *
     * @param subjectName 科目段描述
     * @param money 余额
     */
    public static BigDecimal getMoney(String subjectName,BigDecimal money){
        if (subjectName.startsWith("应付账款") || subjectName.startsWith("其他应付款") || subjectName.startsWith("合同负债")){
            return BigDecimal.ZERO.subtract(money);
        }
        return money;
    }

    public static BigDecimal getBalance(List<SourceFileData>  curr){
        return curr.stream().reduce(
                BigDecimal.ZERO,
                (iprev, icurr) -> iprev.add(icurr.getYEAR_BEGIN_DR().subtract(icurr.getYEAR_BEGIN_CR()).add(icurr.getYTD_DR()).subtract(icurr.getYTD_CR())),
                (l, r) -> l);
    }

    public static String getZ(BigDecimal money){
        return money == null ? "" : money.compareTo(BigDecimal.ZERO) < 0 ? "("+ money +")" : money.toString();
    }

    public static Map<String,DraftFormatTemplate> getDraftFormatTemplateExcelData(String filePath, String sheetName){
        Map<String,DraftFormatTemplate> sourceFileDataList = new HashMap<>();
        EasyExcel.read(filePath, DraftFormatTemplate.class, new PageReadListener<DraftFormatTemplate>(dataList -> {
            dataList.forEach(i -> {
                // 科目代码
                String a = i.getA().replaceAll("-",".");
                // 辅助核算字段
                String c = i.getC();
                String regex = ":(.*?)\\s";
                Pattern pattern = Pattern.compile(regex);
                if (c != null){
                    Matcher matcher = pattern.matcher(c);
                    if (matcher.find()) {
                        String group = matcher.group(1);
                        String key = a + group;
                        sourceFileDataList.put(key,i);
                    }
                }else {
                    sourceFileDataList.put(a,i);
                }

            });
        })).sheet(sheetName).doRead();
        return sourceFileDataList;
    }



    private static String getValue(String str){
        return str == null ? "" : str;
    }
}
