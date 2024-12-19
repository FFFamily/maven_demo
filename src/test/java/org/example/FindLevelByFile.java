package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.enitty.LevelFileExcel;
import org.example.enitty.SourceFileData;
import org.example.enitty.zhong_nan.OldZNAuxiliaryBalanceSheet;
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
        List<Assistant> assistants = new ArrayList<>();
        String company = "江苏中南物业服务有限公司常德分公司";
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
//                        assistant3.setR(code.replace("-","."));
                        // 机构
                        assistant3.setE(company);
                        assistant3.setTransactionObjectId("");
                        assistant3.setTransactionObjectCode("");
                        assistant3.setTransactionObjectName("");
                        // 辅助核算段
                        String s = o.get(2);
                        String[] split = s.split("\\.");

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
                }).excelType(ExcelTypeEnum.XLSX).sheet(0).headRowNumber(2).doRead();
        Map<String, List<Assistant>> companyMap = assistants
                .stream()
//                .filter(item -> item.getCompanyCode().equals("WCRC0"))
//                .filter(item -> item.getR().equals("WCRC0.0.1122010101.05.999999.0.0.0.30017821.0"))
//                .filter(item -> item.getTransactionObjectId().equals("SS:72747717"))
                .collect(Collectors.groupingBy(Assistant::getE));
        for (String companyCode : companyMap.keySet()) {
            System.out.println(DateUtil.date()+ " 当前公司："+ companyCode);
            // 读取旧系统的序时账
            Assistant Firstassistant = companyMap.get(companyCode).get(0);
            String companyName = Firstassistant.getE();
            List<Assistant> realAssistantList = companyMap.get(companyCode);
            List<OtherInfo3> result1 = new ArrayList<>();
            System.out.println("共"+realAssistantList.size()+"条");
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
                if (startCollect.isEmpty()){
                    System.out.println("找不到对应的值："+onlySign);
                    continue;
                }
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
//                    otherInfo3.setBalanceSum(new BigDecimal(assistant.getZ()));
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

            String resultFileName = "分级-" + companyName + ".xlsx";
            try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
                excelWriter.write(result1, writeSheet1);
                System.out.println(resultFileName+"导出完成");
            }
        }
        System.out.println("结束");
    }

    private static String getStr(String str){
        return str == null ?"":str;
    }

}
