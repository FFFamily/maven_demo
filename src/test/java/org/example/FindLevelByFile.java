package org.example;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.enitty.SourceFileData;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.*;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.FindNccLangJiLevel;
import org.example.寻找等级.OtherInfo3;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class FindLevelByFile {
    @Resource
    private FindLevel findLevel;
    @Resource
    private FindNccLangJiLevel findNccLangJiLevel;
    @Resource
    private SqlUtil sqlUtil;
    @Test
    void findLevel() {
        List<SourceFileData> sourceFileDataList = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/江苏中南物业服务有限公司常德分公司-CRC_B00_GL_辅助核算余额 _161224.xls",
                new AnalysisEventListener<Map<Integer,String>>() {
                    @Override
                    public void invoke(Map<Integer,String> o, AnalysisContext analysisContext) {
                        SourceFileData sourceFileData = new SourceFileData();
                        sourceFileDataList.add(sourceFileData);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

                    }
                }).excelType(ExcelTypeEnum.XLS).sheet("江苏中南物业服务有限公司常德分公司-CRC_B00_GL_辅助").doRead();
        Map<String, List<Assistant>> companyMap = ExcelDataUtil.covertAssistant(sourceFileDataList, null, null)
                .stream()
//                .filter(item -> item.getCompanyCode().equals("WCRC0"))
//                .filter(item -> item.getR().equals("WCRC0.0.1122010101.05.999999.0.0.0.30017821.0"))
//                .filter(item -> item.getTransactionObjectId().equals("SS:72747717"))
                // 根据公司分组
                .collect(Collectors.groupingBy(Assistant::getCompanyCode));
        for (String companyCode : companyMap.keySet()) {
            System.out.println(DateUtil.date()+ " 当前公司："+ companyCode);
            // 读取旧系统的序时账
            Assistant Firstassistant = companyMap.get(companyCode).get(0);
            String companyName = Firstassistant.getE();
//            String companyType = CompanyTypeConstant.mapping.get(companyName);
//            if (!companyType.equals(CompanyTypeConstant.LANG_JI)){
//                System.out.println("不是朗基的公司，跳过");
//                continue;
//            }
//            List<OtherInfo3> oldCachedDataList = findNccLangJiLevel.getOldCachedDataListByCompanyName(companyName);
//            oldCachedDataList.forEach(LevelUtil::organizeDataItem);
            List<Assistant> realAssistantList = companyMap.get(companyCode);
            List<OtherInfo3> result1 = new ArrayList<>();
            System.out.println("共"+realAssistantList.size()+"条");
            List<OtherInfo3> cachedDataList = new ArrayList<>();
            EasyExcel.read("",
                    new PageReadListener<Map<Integer, String>>(dataList -> {
                        for (Map<Integer, String> data : dataList) {
                            OtherInfo3 otherInfo3 = new OtherInfo3();
                            cachedDataList.add(otherInfo3);
                        }
                    }));
            cachedDataList.forEach(item -> {
                LevelUtil.organizeDataItem(item);
                item.setSystemForm("新系统");
            });
            for (int i = 0; i < realAssistantList.size(); i++) {
                Assistant assistant = realAssistantList.get(i);
                String z = assistant.getZ();
                if (z == null) {
                    continue;
                }
                // 账户组合描述
                String projectName = assistant.getR();
                String onlySign = assistant.getOnlySign();
                List<OtherInfo3> startCollect = cachedDataList.stream()
                        .filter(item -> item.getOnlySign().equals(onlySign))
//                        .peek(item -> {
//                            item.setTransactionCode(assistant.getTransactionObjectCode());
//                        })
                        .collect(Collectors.toList());
                List<OtherInfo3> result = findLevel.doMain(
                        true,
                        false,
                        true,
                        null,
                        cachedDataList,
                        startCollect,
                        assistant.getZ(),
                        assistant);
                if (result.isEmpty()){
                    // 证明所有的都借贷相互抵消了
                    OtherInfo3 otherInfo3 = new OtherInfo3();
                    otherInfo3.setA(String.valueOf(i));
                    otherInfo3.setNo("1");
                    otherInfo3.setLevel(1);
                    otherInfo3.setS(assistant.getForm());
                    otherInfo3.setBalanceSum(new BigDecimal(assistant.getZ()));
                    otherInfo3.setZ(projectName);
                    otherInfo3.setZDesc(assistant.getRDesc());
                    otherInfo3.setTransactionId(assistant.getTransactionObjectId());
                    otherInfo3.setTransactionName(assistant.getTransactionObjectName());
                    otherInfo3.setOriginZ(projectName);
                    result1.add(otherInfo3);
                } else {
                    HashMap<Integer,BigDecimal> lastLevelMap = new HashMap<>();
                    for (int i1 = 0; i1 < result.size(); i1++) {
                        OtherInfo3 item = result.get(i1);
                        item.setA(String.valueOf(i));
                        if (!Objects.equals(item.getSystemForm(),"老系统")) {
                            item.setZDesc(assistant.getRDesc());
                            String transactionObjectCode = assistant.getTransactionObjectCode();
                            String assistantTransactionObjectCodeCopy = assistant.getTransactionObjectCodeCopy();
                            // 源-交易对象编码
                            item.setTransactionCode(transactionObjectCode);
                            // 处理-交易对象编码
                            item.setTransactionCodeCopy(assistantTransactionObjectCodeCopy);
                            item.setOriginZ(projectName);
                            item.setOriginZCopy(projectName.replaceAll("\\.","-"));
                            // 处理-账户组合
                            String zCopy = item.getZ().replaceAll("\\.","-");
                            item.setZCopy(zCopy);
                            item.setMergeValue(zCopy + (item.getTransactionCodeCopy() == null ? "" : item.getTransactionCodeCopy()));
                        }
                        // 计算余额
                        // 计算余额
                        OtherInfo3 lastOne = i1 == 0 ? null : result.get(i1-1);
                        BigDecimal lastBalance;
                        if (lastOne == null){
                            lastBalance = BigDecimal.ZERO;
                        }else {
                            if (lastOne.getLevel().equals(item.getLevel())){
                                // 等级相等,
                                lastBalance = lastOne.getBalanceSum();
                            }else if (lastOne.getLevel() < item.getLevel()){
                                lastLevelMap.put(lastOne.getLevel(),lastOne.getBalanceSum());
                                lastBalance = BigDecimal.ZERO;
                            }else {
                                lastBalance = lastLevelMap.getOrDefault(item.getLevel(),BigDecimal.ZERO);
                            }
                        }
                        item.setBalanceSum(lastBalance.add(CommonUtil.getBigDecimalValue(item.getV()).subtract(CommonUtil.getBigDecimalValue(item.getW()))));
                    }
                    result1.addAll(result);
                }
            }

            String resultFileName = "模版-" + companyName + "-" + System.currentTimeMillis() + ".xlsx";
            try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
                excelWriter.write(result1, writeSheet1);
                System.out.println(resultFileName+"导出完成");
            }
        }
        System.out.println("结束");
    }

}
