package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.enitty.LevelFileExcel;
import org.example.enitty.SourceFileData;
import org.example.utils.CommonUtil;
import org.example.utils.ExcelDataUtil;
import org.example.utils.LevelUtil;
import org.example.utils.SqlUtil;
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
    void findABCD() {
//        Map<String, DraftFormatTemplate> mapping = getDraftFormatTemplateExcelData("src/main/java/org/example/分类/明细分类汇总-总部提供.xlsx", "明细");
        String company = "江苏中南物业服务有限公司常德分公司";
        List<Assistant> assistants = redaBalance(company);
        Map<String, List<Assistant>> companyMap = assistants.stream().collect(Collectors.groupingBy(Assistant::getE));
        for (String companyCode : companyMap.keySet()) {
            System.out.println(DateUtil.date()+ " 当前公司："+ companyCode);
            // 读取旧系统的序时账
            Assistant Firstassistant = companyMap.get(companyCode).get(0);
            String companyName = Firstassistant.getE();
            List<Assistant> realAssistantList = companyMap.get(companyCode);
//            List<SourceFileData> sourceFileDataList = ExcelDataUtil.getExcelData("src/main/java/org/example/分类/9月科目辅助余额表.xlsx","Sheet1");
            List<SourceFileData> sourceFileDataList = readExcel(realAssistantList);
            List<AssistantResult> dataList = ExcelDataUtil.covertAssistantResult(sourceFileDataList, null);
            System.out.println("共"+realAssistantList.size()+"条");
            List<OtherInfo3> cachedDataList = readDetailExcel(company);
            cachedDataList.forEach(item -> {
                LevelUtil.organizeDataItem(item);
                item.setSystemForm("新系统");
            });
            List<AssistantResult> excelExcelData = new ArrayList<>();
            for (int i = 0; i < dataList.size(); i++) {
                Assistant assistant = realAssistantList.get(i);
                String onlySign = assistant.getOnlySign();
                AssistantResult assistantResult = dataList.get(i);
                assistantResult.setIndex(String.valueOf(i+1));
                String z = assistant.getZ();
                if (z == null) {
                    continue;
                }
                List<OtherInfo3> startCollect = cachedDataList.stream()
                        .filter(item -> item.getOnlySign().equals(onlySign))
                        .collect(Collectors.toList());
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

    public List<Assistant> redaBalance(String company){
        List<Assistant> assistants = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/ewai/"+company+"-辅助核算余额.xlsx",
                new AnalysisEventListener<Map<Integer,String>>() {
                    @Override
                    public void invoke(Map<Integer,String> o, AnalysisContext analysisContext) {
                        Assistant assistant3 = new Assistant();

                        // 左前缀匹配
                        BigDecimal v = new BigDecimal(o.get(7).replaceAll(",",""));
                        BigDecimal w = new BigDecimal(o.get(8).replaceAll(",",""));
                        assistant3.setZ(CommonUtil.getZ(CommonUtil.getBigDecimalValue(v).subtract(CommonUtil.getBigDecimalValue(w))));
                        String code = o.get(0);
                        assistant3.setR(code);
                        assistant3.setRDesc(o.get(1));
                        // 机构
                        assistant3.setE(company);
                        assistant3.setTransactionObjectId("");
                        assistant3.setTransactionObjectCode("");
                        assistant3.setTransactionObjectName("");
                        // 辅助核算段
                        String s = o.get(2);
                        String[] split = s.split("\\.");
                        assistant3.setA(s);
                        assistant3.setTransactionObjectCodeCopy(split[1].equals("-") ? "" : split[1]);
                        // 科目段描述
                        String codeName = o.get(1);
                        assistant3.setRDesc(codeName);
//                        assistant3.setCompanyCode(o.get(0));
//                        assistant3.setForm(o.get(0));
                        // 唯一标识：账户组合+交易Id
                        assistant3.setOnlySign(assistant3.getR()+assistant3.getTransactionObjectCodeCopy());
                        assistants.add(assistant3);
                    }
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

                    }
                }).sheet(0).headRowNumber(2).doRead();
        return assistants;
    }
    public List<SourceFileData> readExcel(List<Assistant> realAssistantList){
        List<SourceFileData> sourceFileDataList = new ArrayList<>();
        for (Assistant assistant : realAssistantList) {
            SourceFileData data = new SourceFileData();
            String rDesc = assistant.getRDesc();
            String[] splitRDesc = rDesc.split("\\.");
            String[] splitR = assistant.getR().split("\\.");
            data.setSEGMENT1_NAME(assistant.getE());
            data.setMatch(assistant.getR());
//            data.setSEGMENT3_NAME(splitRDesc[]);
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

    public List<OtherInfo3> readDetailExcel(String company){
        List<OtherInfo3> cachedDataList = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/ewai/总帐凭证行查 _"+company+".xlsx", LevelFileExcel.class,
                new PageReadListener<LevelFileExcel>(dataList -> {
                    for (LevelFileExcel levelFileExcel : dataList) {
//                                    String s = levelFileExcel.getS();
                        String project = levelFileExcel.getProject();
                        if (!(project.startsWith("应付账款")
                                || project.startsWith("预付账款")
                                || project.startsWith("合同负债")
                                || project.startsWith("预收账款")
                                || project.startsWith("应收账款")
                                || project.startsWith("其他应付款")
                                || project.startsWith("其他应收款"))){
                            continue;
                        }
                        OtherInfo3 info = new OtherInfo3();
                        //
                        // 有效日期
                        DateTime date = DateUtil.date(levelFileExcel.getN());
                        int year = date.year();
                        int month = date.month()+1;
                        int code = levelFileExcel.getQ();
                        info.setQ(code);
                        info.setR(year+"-"+month+"-"+code);
                        info.setV(levelFileExcel.getV());
                        info.setW(levelFileExcel.getW());
                        // 有效日期
                        info.setN(date);
                        info.setS(levelFileExcel.getS());
                        // 有借就是 借方向
                        info.setX(info.getV() != null ? "借" : "贷");
                        info.setZ(levelFileExcel.getZ());
                        info.setZCopy(levelFileExcel.getZ().replace(".","-"));
                        info.setZDesc(levelFileExcel.getZDesc());
                        info.setTransactionId(getStr(levelFileExcel.getTransactionId()));
                        info.setTransactionName(getStr( levelFileExcel.getTransactionName()));
                        info.setTransactionCodeCopy(getStr(levelFileExcel.getTransactionCodeCopy()));
                        info.setOnlySign(info.getZCopy()+info.getTransactionCodeCopy());
//                                    info.setOriginZCopy(info.getZCopy()+info.getTransactionCodeCopy());
                        // 公司名称
                        info.setCompanyName(company);
                        // 用于追溯老系统
                        info.setJournalExplanation(levelFileExcel.getJournalExplanation());
                        cachedDataList.add(info);
                    }
                })
        ).sheet(0).doRead();
        return cachedDataList;
    }
    private static String getStr(String str){
        return str == null ?"":str;
    }

}
