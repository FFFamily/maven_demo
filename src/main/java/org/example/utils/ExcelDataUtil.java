package org.example.utils;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.example.enitty.Assistant;
import org.example.enitty.SourceFileData;
import org.example.分类.AssistantResult;
import org.example.分类.entity.DraftFormatTemplate;
import org.example.寻找等级.OtherInfo3;
import org.example.寻找等级.old_excel.MappingCustomerExcel;
import org.example.寻找等级.old_excel.MappingNccToFmsExcel;
import org.example.寻找等级.old_excel.MappingProjectExcel;
import org.example.寻找等级.old_excel.OldExcelTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExcelDataUtil {
    public static List<SourceFileData> getExcelData(String filePath, String sheetName){
        List<SourceFileData> sourceFileDataList = new ArrayList<>();
        EasyExcel.read(filePath, SourceFileData.class, new PageReadListener<SourceFileData>(dataList -> {
            dataList.forEach(i -> {
                if (i.getSEGMENT3_NAME().startsWith("应付账款")
                || i.getSEGMENT3_NAME().startsWith("预付账款")
                        || i.getSEGMENT3_NAME().startsWith("合同负债")
                        || i.getSEGMENT3_NAME().startsWith("预收账款")
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
            });
        })).sheet(sheetName).doRead();
        System.out.println("9月科目辅助余额表 读取完成");
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

    /**
     * 不明客商表

     */
    public static Map<String,DraftFormatTemplate> getDraftFormatTemplateExcelData(String filePath, String sheetName){
        Map<String,DraftFormatTemplate> sourceFileDataList = new HashMap<>();
        EasyExcel.read(filePath, DraftFormatTemplate.class, new PageReadListener<DraftFormatTemplate>(dataList -> {
            dataList.forEach(i -> {
                // 科目代码
                String a = i.getA().replaceAll("-",".");
                // 辅助核算字段
                String c = i.getC();
//                String regex = ":(.*?)\\s";
//                Pattern pattern = Pattern.compile(regex);
                if (c != null){
                    int startIndex = c.indexOf(":");
                    int endIndex = c.lastIndexOf(":");
                    if (startIndex != endIndex){
                        String key = a+ c.substring(startIndex+1,endIndex-1);
                        sourceFileDataList.put(key,i);
                    }
//                    Matcher matcher = pattern.matcher(c);
//                    if (matcher.find()) {
//                        String group = matcher.group(1);
//                        String key = a + group;
//                        sourceFileDataList.put(key,i);
//                    }

                }else {
                    sourceFileDataList.put(a,i);
                }

            });
        })).sheet(sheetName).doRead();
        return sourceFileDataList;
    }

    public static List<AssistantResult> covertAssistantResult(List<SourceFileData> sourceFileDataList,Map<String, DraftFormatTemplate> mapping){
        List<AssistantResult> dataList = sourceFileDataList
                .stream()
                .collect(Collectors.groupingBy(i -> i.getMatch() + "."+ i.getTransactionObjectId()))
                .values()
                .stream()
                .reduce(new ArrayList<>(),(prev, curr) ->{
                    AssistantResult assistantResult = new AssistantResult();
                    SourceFileData sourceFileData = curr.get(0);
                    assistantResult.setCompanyName(sourceFileData.getSEGMENT1_NAME());
                    assistantResult.setFieldCode(sourceFileData.getMatch());
                    assistantResult.setSubjectName(sourceFileData.getSEGMENT3_NAME());
//                    assistantResult.setForm(sourceFileData.getSEGMENT3_NAME());
                    String transactionObjectId = sourceFileData.getTransactionObjectId();
                    String transactionObjectCode = sourceFileData.getTransactionObjectCode();
                    String transactionObjectName = sourceFileData.getTransactionObjectName();
                    String transactionObjectCodeCopy = sourceFileData.getTransactionObjectCodeCopy();
                    assistantResult.setTransactionObjectId(transactionObjectId);
                    assistantResult.setTransactionObjectCode(transactionObjectCode);
                    assistantResult.setTransactionObjectName(transactionObjectName);
                    assistantResult.setTransactionObjectCodeCopy(transactionObjectCodeCopy);
                    assistantResult.setField(sourceFileData.getMatchName());
                    BigDecimal money = ExcelDataUtil.getBalance(curr);
                    assistantResult.setSEGMENT1(sourceFileData.getSEGMENT1());
                    assistantResult.setSEGMENT1_NAME(sourceFileData.getSEGMENT1_NAME());
                    assistantResult.setSEGMENT2_NAME(sourceFileData.getSEGMENT2_NAME());
                    assistantResult.setSEGMENT3_NAME(sourceFileData.getSEGMENT3_NAME());
                    assistantResult.setSEGMENT4_NAME(sourceFileData.getSEGMENT4_NAME());
                    assistantResult.setSEGMENT5_NAME(sourceFileData.getSEGMENT5_NAME());
                    assistantResult.setSEGMENT6_NAME(sourceFileData.getSEGMENT6_NAME());
                    assistantResult.setSEGMENT7_NAME(sourceFileData.getSEGMENT7_NAME());
                    assistantResult.setSEGMENT8_NAME(sourceFileData.getSEGMENT8_NAME());
                    assistantResult.setSEGMENT9_NAME(sourceFileData.getSEGMENT9_NAME());
                    assistantResult.setSEGMENT10_NAME(sourceFileData.getSEGMENT10_NAME());
                    assistantResult.setMoney(money);
                    if (mapping != null){
                        String key;
                        if (transactionObjectCode != null){
                            int startIndex = transactionObjectCode.indexOf(":");
                            int endIndex = transactionObjectCode.lastIndexOf(":");
                            if (startIndex != -1 && startIndex != endIndex){
                                key = assistantResult.getFieldCode()+transactionObjectCode.substring(startIndex+1,endIndex-1);
                            }else {
                                key = assistantResult.getFieldCode()+transactionObjectCode;
                            }
                        }else {
                            key = assistantResult.getFieldCode();
                        }
                        DraftFormatTemplate draftFormatTemplate = mapping.get(key);
                        if (draftFormatTemplate != null) {
                            assistantResult.setIsOrigin(draftFormatTemplate.getO());
                            assistantResult.setCustomerType(draftFormatTemplate.getT());
                            assistantResult.setMergeFile(draftFormatTemplate.getC());
                        }
                    }
                    prev.add(assistantResult);
                    return prev;
                },(l,r) -> l);
        return  dataList;
    }

    public static List<Assistant> covertAssistant(List<SourceFileData> sourceFileDataList,List<AssistantResult> dataList,Map<String, DraftFormatTemplate> mapping){
        dataList = dataList == null ? covertAssistantResult(sourceFileDataList, mapping) : dataList;
        List<Assistant> cachedDataList = new ArrayList<>();
        for (AssistantResult assistantResult : dataList) {
            Assistant assistant3 = new Assistant();
            BigDecimal money = ExcelDataUtil.getMoney(assistantResult.getSubjectName(),assistantResult.getMoney());
            assistantResult.setMoney(money);
            // 左前缀匹配
            assistant3.setZ(getZ(assistantResult.getMoney()));

            assistant3.setR(assistantResult.getFieldCode());
            assistant3.setE(assistantResult.getCompanyName());
            assistant3.setTransactionObjectId(assistantResult.getTransactionObjectId());
            assistant3.setTransactionObjectCode(assistantResult.getTransactionObjectCode());
            assistant3.setTransactionObjectName(assistantResult.getTransactionObjectName());
            assistant3.setTransactionObjectCodeCopy(assistantResult.getTransactionObjectCodeCopy());
            assistant3.setRDesc(assistantResult.getField());
            assistant3.setCompanyCode(assistantResult.getSEGMENT1());
            assistant3.setForm(assistantResult.getForm());
            cachedDataList.add(assistant3);
            // 唯一标识：账户组合+交易Id
            assistant3.setOnlySign(assistant3.getR()+assistant3.getTransactionObjectId());
        }
        System.out.println("解析Assistant完成");
        return cachedDataList;
    }


    private static String getValue(String str){
        return str == null ? "" : str;
    }

    public static void main(String[] args) {
        getOldExcel();
    }

    public static List<OtherInfo3> getOldExcel(){
        List<OtherInfo3> data = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/朗逸物业NCC序时簿.xlsx", OldExcelTemplate.class, new PageReadListener<OldExcelTemplate>(dataList -> {
            for (OldExcelTemplate oldExcelTemplate : dataList) {
                OtherInfo3 otherInfo3 = new OtherInfo3();
                String year = oldExcelTemplate.getA();
                String month = oldExcelTemplate.getB();
                String day = oldExcelTemplate.getC();
                String dateStr = year+"-"+month+"-"+day;
                // 总账日期
                otherInfo3.setN(DateUtil.parse(dateStr));
                // 凭证号
                otherInfo3.setQ(oldExcelTemplate.getE());
                // 拼接凭证号
                otherInfo3.setR(year+"-"+month+otherInfo3.getQ());
                // TODO 来源
                // 借
                otherInfo3.setV(oldExcelTemplate.getL());
                // 贷
                otherInfo3.setW(oldExcelTemplate.getN());
                // TODO 余额
                String regex = "(?<=：)[^【】]+";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(oldExcelTemplate.getG());
                // 唯一标识
                otherInfo3.setOnlySign(oldExcelTemplate.getF());
                while (matcher.find()) {
                    otherInfo3.setOnlySign(otherInfo3.getOnlySign()+"-"+matcher.group().trim());
                }
                // ncc 科目
                otherInfo3.setNccProjectCode(oldExcelTemplate.getF());
                // ncc 辅助核算
                otherInfo3.setNccAssistantCode(oldExcelTemplate.getG());
                data.add(otherInfo3);
            }
        })).sheet("朗逸物业NCC序时簿").doRead();
        return data;
    }

    public static void findMappingNccToFmsExcel(HashMap<String,List<MappingNccToFmsExcel>> mappingNccToFmsExcelHashMap,
                                                                      HashMap<String, MappingCustomerExcel> mappingCustomerExcelHashMap,
                                                                      HashMap<String, MappingProjectExcel> mappingProjectExcels
                                                                      ){
        // 写法1
        try (ExcelReader excelReader = EasyExcel.read("src/main/java/org/example/utils/朗逸物业映射关系.xlsx").build()) {
            // 这里为了简单 所以注册了 同样的head 和Listener 自己使用功能必须不同的Listener
            ReadSheet readSheet1 = EasyExcel.readSheet(0).head(MappingNccToFmsExcel.class).registerReadListener(new PageReadListener<MappingNccToFmsExcel>(dataList -> {
                for (MappingNccToFmsExcel mappingNccToFmsExcel : dataList) {
                    String j = mappingNccToFmsExcel.getJ();
                    String k = mappingNccToFmsExcel.getK();
                    String key = j+"."+k;
                    List<MappingNccToFmsExcel> list = mappingNccToFmsExcelHashMap.getOrDefault(key, new ArrayList<>());
                    list.add(mappingNccToFmsExcel);
                    mappingNccToFmsExcelHashMap.put(key,list);
                }
            })).build();
            ReadSheet readSheet2 =
                    EasyExcel.readSheet(1).head(MappingCustomerExcel.class).registerReadListener(new PageReadListener<MappingCustomerExcel>(dataList -> {
                        for (MappingCustomerExcel mappingNccToFmsExcel : dataList) {
                            String key = mappingNccToFmsExcel.getB();
                            mappingCustomerExcelHashMap.put(key,mappingNccToFmsExcel);
                        }
                    })).build();
            ReadSheet readSheet3 =
                    EasyExcel.readSheet(2).head(MappingProjectExcel.class).registerReadListener(new PageReadListener<MappingProjectExcel>(dataList -> {
                        for (MappingProjectExcel mappingNccToFmsExcel : dataList) {
                            String key = mappingNccToFmsExcel.getC();
                            mappingProjectExcels.put(key,mappingNccToFmsExcel);
                        }
                    })).build();
            // 这里注意 一定要把sheet1 sheet2 一起传进去，不然有个问题就是03版的excel 会读取多次，浪费性能
            excelReader.read(readSheet1, readSheet2,readSheet3);
        }

    }

}
