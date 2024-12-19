package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import org.example.enitty.Assistant;
import org.example.enitty.LevelFileExcel;
import org.example.enitty.SourceFileData;
import org.example.utils.*;
import org.example.分类.AssistantResult;
import org.example.分类.FindABCD;
import org.example.分类.entity.DraftFormatTemplate;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.FindNccLangJiLevel;
import org.example.寻找等级.OtherInfo3;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.utils.ExcelDataUtil.covertAssistantResult;
import static org.example.utils.ExcelDataUtil.getDraftFormatTemplateExcelData;

@SpringBootTest
public class FindABCDByFile {
    @Resource
    private FindLevel findLevel;
    @Resource
    private FindNccLangJiLevel findNccLangJiLevel;
    @Resource
    private FindABCD findABCD;
    @Resource
    private SqlUtil sqlUtil;
    @Test
    void test(){
        List<String> list = new ArrayList<>();
        list.add("江苏中南物业服务有限公司武汉分公司");
        list.add("江苏中南物业服务有限公司常德分公司");
        list.add("江苏中南物业服务有限公司潜江分公司");
        list.add("江苏中南物业服务有限公司长沙分公司");
        list.add("禹洲物业服务有限公司武汉分公司");
        for (String company : list) {
            findABCD(company);
        }
    }


    void findABCD(String company) {
//        String company = "江苏中南物业服务有限公司常德分公司";
        List<Assistant> assistants = FindFileUtil.redaBalance(company);
        Map<String, List<Assistant>> companyMap = assistants.stream().collect(Collectors.groupingBy(Assistant::getE));
        for (String companyCode : companyMap.keySet()) {
            System.out.println(DateUtil.date()+ " 当前公司："+ companyCode);
            // 读取旧系统的序时账
            Assistant Firstassistant = companyMap.get(companyCode).get(0);
            String companyName = Firstassistant.getE();
            List<Assistant> realAssistantList = companyMap.get(companyCode);
//            List<SourceFileData> sourceFileDataList = readExcel(realAssistantList);
//            List<AssistantResult> dataList = ExcelDataUtil.covertAssistantResult(sourceFileDataList, null);
            System.out.println("共"+realAssistantList.size()+"条");
            List<OtherInfo3> cachedDataList = FindFileUtil.readDetailExcel(company);
            cachedDataList.forEach(item -> {
                LevelUtil.organizeDataItem(item);
                item.setSystemForm("新系统");
            });
//            if (realAssistantList.size() != dataList.size()){
//                throw new RuntimeException("为什么不对");
//            }
            List<AssistantResult> excelExcelData = new ArrayList<>();
            for (int i = 0; i < realAssistantList.size(); i++) {
                Assistant assistant = realAssistantList.get(i);
//                if (!assistant.getR().equals("WRMB0-0-1002010101-ZD001398-0-0-0-0-0-0")){
//                    continue;
//                }
                String onlySign = assistant.getOnlySign();
//                AssistantResult assistantResult = dataList.get(i);
                AssistantResult assistantResult = new AssistantResult();
                assistantResult.setCompanyName(companyName);
                String rDesc = assistant.getRDesc();
                String[] splitRDesc = rDesc.split("\\.");
                String[] splitR = assistant.getR().split("-");
                assistantResult.setTransactionObjectId(assistant.getTransactionObjectId());
                assistantResult.setTransactionObjectName(assistant.getTransactionObjectName());
                assistantResult.setTransactionObjectCode(assistant.getTransactionObjectCode());
                assistantResult.setTransactionObjectCodeCopy(assistant.getTransactionObjectCodeCopy());
                assistantResult.setFieldCode( assistant.getR());
                assistantResult.setField(rDesc);
                assistantResult.setSubjectName(splitRDesc[2]);

                assistantResult.setSEGMENT1(splitR[0]);
                assistantResult.setSEGMENT1_NAME(splitRDesc[0]);
                assistantResult.setSEGMENT2_NAME(splitRDesc[1]);
                assistantResult.setSEGMENT3_NAME(splitRDesc[2]);
                assistantResult.setSEGMENT4_NAME(splitRDesc[3]);
                assistantResult.setSEGMENT5_NAME(splitRDesc[4]);
                assistantResult.setSEGMENT6_NAME(splitRDesc[5]);
                assistantResult.setSEGMENT7_NAME(splitRDesc[6]);
                assistantResult.setSEGMENT8_NAME(splitRDesc[7]);
                assistantResult.setSEGMENT9_NAME(splitRDesc[8]);
                assistantResult.setSEGMENT10_NAME(splitRDesc.length == 10 ? splitRDesc[9] : "");

                assistantResult.setMoney(getZValue(assistant.getZ()));

                assistantResult.setIndex(String.valueOf(i+1));
                String z = assistant.getZ();
                if (z == null) {
                    continue;
                }

                List<OtherInfo3> startCollect = cachedDataList.stream()
                        .filter(item -> item.getOnlySign().equals(onlySign))
                        .collect(Collectors.toList());
                if (startCollect.isEmpty()) {
//                    excelExcelData.add(assistantResult);
                    continue;
                }
                startCollect.forEach(LevelUtil::organizeDataItem);
                String form = startCollect.stream().map(OtherInfo3::getS).distinct().collect(Collectors.joining("、"));
                assistantResult.setForm(form);
                findABCD.doFind(startCollect,assistant,assistantResult,true);
                List<OtherInfo3> oneLevel = findABCD.doFind(startCollect, assistant, assistantResult, false);
                String oneLevelForm = oneLevel.stream().map(OtherInfo3::getS).distinct().collect(Collectors.joining("、"));
                assistantResult.setOneLevelForm(oneLevelForm);
                excelExcelData.add(assistantResult);
            }
            String resultFileName = "ABCD分类-"+companyName+ ".xlsx";
            try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(AssistantResult.class).build();
                excelWriter.write(excelExcelData, writeSheet1);
            }
        }
    }


    public List<SourceFileData> readExcel(List<Assistant> realAssistantList){
        List<SourceFileData> sourceFileDataList = new ArrayList<>();
        for (Assistant assistant : realAssistantList) {
            SourceFileData data = new SourceFileData();
            String rDesc = assistant.getRDesc();
            String[] splitRDesc = rDesc.split("\\.");
            String[] splitR = assistant.getR().split("-");
            data.setSEGMENT1_NAME(assistant.getE());
            data.setMatch(assistant.getR());
            data.setMatchName(rDesc);
            data.setTransactionObjectId(assistant.getTransactionObjectId());
            data.setTransactionObjectName(assistant.getTransactionObjectName());
            data.setTransactionObjectCode(assistant.getTransactionObjectCode());
            data.setTransactionObjectCodeCopy(assistant.getTransactionObjectCodeCopy());
            data.setSEGMENT1(splitR[0]);
            data.setSEGMENT1_NAME(splitRDesc[0]);
            data.setSEGMENT2_NAME(splitRDesc[1]);
            data.setSEGMENT3_NAME(splitRDesc[2]);
            data.setSEGMENT4_NAME(splitRDesc[3]);
            data.setSEGMENT5_NAME(splitRDesc[4]);
            data.setSEGMENT6_NAME(splitRDesc[5]);
            data.setSEGMENT7_NAME(splitRDesc[6]);
            data.setSEGMENT8_NAME(splitRDesc[7]);
            data.setSEGMENT9_NAME(splitRDesc[8]);
            data.setSEGMENT10_NAME(splitRDesc.length == 10 ? splitRDesc[9] : "");
            data.setYEAR_BEGIN_CR(getZValue(assistant.getZ()));
            data.setYEAR_BEGIN_DR(BigDecimal.ZERO);
            data.setYTD_CR(BigDecimal.ZERO);
            data.setYTD_DR(BigDecimal.ZERO);
            sourceFileDataList.add(data);
        }
        return sourceFileDataList;
    }

    public  BigDecimal getZValue(String z) {
        BigDecimal balance;
        try {
            balance = new BigDecimal(z.replace(",", "").replace("(", "").replace(")", ""));
        } catch (Exception e) {
            balance = BigDecimal.ZERO;
        }
        if (z.contains("(") || z.contains(")")) {
            // 负值
            return BigDecimal.ZERO.subtract(balance);
        }
        return balance;
    }




}
