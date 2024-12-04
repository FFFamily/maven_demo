package org.example;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.enitty.SourceFileData;
import org.example.utils.ExcelDataUtil;
import org.example.utils.SqlUtil;
import org.example.分类.FindABCD;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.OtherInfo3;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class TestDemo {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private FindABCD findABCD;
    @Resource
    private FindLevel findLevel;
    @Resource
    private SqlUtil sqlUtil;
    @Test
    void findLevel() {
        List<SourceFileData> sourceFileDataList = ExcelDataUtil.getExcelData("src/main/java/org/example/分类/9月科目辅助余额表.xlsx","Sheet1");
        Map<String, List<Assistant>> companyMap = ExcelDataUtil.covertAssistant(sourceFileDataList, null, null)
                .stream()
                .filter(item -> item.getCompanyCode().equals("WCRC0"))
                .filter(item -> item.getR().equals("WCRC0.0.1123190101.0.999999.0.0.0.30017800.0"))
                .filter(item -> item.getTransactionObjectId().equals("SS:71683924"))
                // 根据公司分组
                .collect(Collectors.groupingBy(Assistant::getCompanyCode));
        for (String companyCode : companyMap.keySet()) {
            System.out.println(DateUtil.date()+ " 当前公司："+ companyCode);
            List<Assistant> realAssistantList = companyMap.get(companyCode);
            List<OtherInfo3> result1 = new ArrayList<>();
            System.out.println("共"+realAssistantList.size()+"条");
            String findCompanySql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段代码\" = '"+companyCode+"'";
            List<OtherInfo3> cachedDataList = sqlUtil.find(findCompanySql);
            // TODO 读取旧系统的明细数据
            List<OtherInfo3> oldCachedDataList = ExcelDataUtil.getOldExcel();
            System.out.println("整个公司包含数据量："+cachedDataList.size());
            cachedDataList.forEach(item -> findLevel.organizeDataItem(item));
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
//                        .filter(item -> item.getZ().equals(projectName) && Objects.equals(item.getTransactionId(),assistant.getTransactionObjectId()))
                        .collect(Collectors.toList());
                List<OtherInfo3> result = findLevel.doMain(
                        true,
                        false,
                        true,
                        oldCachedDataList,
                        cachedDataList,
                        startCollect,
                        assistant.getZ(),
                        onlySign);
                if (result.isEmpty()){
                    // 证明所有的都借贷相互抵消了
                    OtherInfo3 otherInfo3 = new OtherInfo3();
                    otherInfo3.setA(String.valueOf(i));
                    otherInfo3.setNo("1");
                    otherInfo3.setLevel(1);
                    otherInfo3.setS(assistant.getForm());
                    otherInfo3.setBalanceSum(assistant.getZ());
                    otherInfo3.setZ(projectName);
                    otherInfo3.setZDesc(assistant.getRDesc());
                    otherInfo3.setTransactionId(assistant.getTransactionObjectId());
                    otherInfo3.setTransactionName(assistant.getTransactionObjectName());
                    otherInfo3.setOriginZ(projectName);
                    result1.add(otherInfo3);
                } else {
                    int finalI = i;
                    result.forEach(item -> {
                        item.setA(String.valueOf(finalI));
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
                        String zCopy = item.getOriginZCopy().replaceAll("\\.","-");
                        item.setZCopy(zCopy);
                        item.setMergeValue(zCopy + (item.getTransactionCodeCopy() == null ? "" : item.getTransactionCodeCopy()));
                    });
                    result1.addAll(result);
                }
            }
            Assistant assistant = companyMap.get(companyCode).get(0);
            String resultFileName = "模版-" + assistant.getE() + "-" + System.currentTimeMillis() + ".xlsx";
            try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
                excelWriter.write(result1, writeSheet1);
                System.out.println(resultFileName+"导出完成");
            }
        }
        System.out.println("结束");
    }

}
