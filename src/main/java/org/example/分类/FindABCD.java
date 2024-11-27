package org.example.分类;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.core.entity.SourceFileData;
import org.example.enitty.Assistant;
import org.example.utils.SqlUtil;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.OtherInfo3;
import org.example.utils.ExcelDataUtil;
import org.example.分类.entity.DraftFormatTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.utils.ExcelDataUtil.getDraftFormatTemplateExcelData;
import static org.example.utils.ExcelDataUtil.getZ;

/**
 * 分裂
 */
@Service
public class FindABCD {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SqlUtil sqlUtil;

    public String getValue(String str){
        return str == null ? "" : str;
    }



    public void doFindABDC(String sourceFile){
        List<AssistantResult> excelExcelData = new ArrayList<>();
        List<SourceFileData> sourceFileDataList = ExcelDataUtil.getExcelData(sourceFile,"Sheet1");
        Map<String, DraftFormatTemplate> mapping = getDraftFormatTemplateExcelData("src/main/java/org/example/分类/明细分类汇总-总部提供.xlsx", "明细");
        List<AssistantResult> dataList = ExcelDataUtil.covertAssistantResult(sourceFileDataList, mapping);
        List<Assistant> cachedDataList = ExcelDataUtil.covertAssistant(sourceFileDataList,dataList, mapping);
        for (int i = 0; i < dataList.size(); i++) {
            Assistant assistant = cachedDataList.get(i);
            AssistantResult assistantResult = dataList.get(i);
            assistantResult.setIndex(String.valueOf(i+1));
            String z = assistant.getZ();
            if (z == null) {
                continue;
            }
            String projectName = assistant.getR();
            String sql =  "select * from ZDPROD_EXPDP_20241120 z where z.\"账户组合\" = '" + assistantResult.getFieldCode()+"'";
            if (assistantResult.getTransactionObjectName() != null) {
                sql +=  "and z.\"交易对象名称\" like '" + assistantResult.getTransactionObjectName() +"%'";
            }else {
                sql +=  "and z.\"交易对象名称\" is null";
            }
            List<OtherInfo3> startCollect = sqlUtil.find(sql);
            String form = startCollect.stream().map(OtherInfo3::getS).distinct().collect(Collectors.joining("、"));
            assistantResult.setForm(form);
            doFind(startCollect,assistant,projectName,assistantResult,true);
            doFind(startCollect,assistant,projectName,assistantResult,false);
            excelExcelData.add(assistantResult);
//            if (i == 1000){
//                break;
//            }
            System.out.println("当前位置："+i +" 一共有： "+dataList.size());
        }
        String resultFileName = "ABCD分类-"+System.currentTimeMillis() + ".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(AssistantResult.class).build();
            excelWriter.write(excelExcelData, writeSheet1);
        }
        System.out.println("结束");
    }

    public static void doFind(List<OtherInfo3> startCollect,Assistant assistant,String projectName,AssistantResult assistantResult,Boolean isFindAll){
        List<OtherInfo3> result = FindLevel.doMain(
                false,
                isFindAll,
                false,
                null,
                startCollect,
                assistant.getZ(),
                projectName
        );
        String type;
        if (result.isEmpty()) {
            // 证明全部匹配
            type = findABCD(startCollect, assistantResult,assistant);
        } else {
            type = findABCD(result, assistantResult,assistant);
        }
        if (isFindAll){
            assistantResult.setType(type);
        }else {
            assistantResult.setOneLevelType(type);
        }
    }




    public static BigDecimal getZValue(String z) {
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

    public static String findABCD(List<OtherInfo3> result,AssistantResult assistantResult, Assistant assistant) {
        // 通过总账日期进行分类
//        AssistantResult assistantResult = new AssistantResult();
//        assistantResult.setField(assistant.getR());
//        assistantResult.setIndex(assistant.getA());
//        assistantResult.setMoney(getZValue(assistant.getZ()));
        String z = assistant.getZ();
        // 期初
        List<OtherInfo3> up = new ArrayList<>();
        // 本期
        List<OtherInfo3> low = new ArrayList<>();
        result.forEach(item -> {
            Date time = item.getN();
            Date date = DateUtil.parse("2022-04-30", "yyyy-MM-dd");
            if (DateUtil.date(time).toInstant().compareTo(date.toInstant()) <= 0) {
                // 时间 在 2022年4月30日之前
                up.add(item);
            } else {
                low.add(item);
            }
        });
        // 如果全部都在期初，那么就是归属D类
        if (!up.isEmpty() && low.isEmpty()) {
//            assistantResult.setIsIncludeUp(1);
//            assistantResult.setType("D");
            return "D";
        } else if (!up.isEmpty()) {
//            assistantResult.setIsIncludeUp(1);
            // 最终余额
            BigDecimal totalSum = getZValue(z);
            // 期初余额
            BigDecimal upSum = up.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(curr.getV() != null ? curr.getV() : BigDecimal.ZERO).subtract(curr.getW() != null ? curr.getW() : BigDecimal.ZERO), (l, r) -> l);
            if (upSum.compareTo(BigDecimal.ZERO) > 0 && totalSum.compareTo(upSum) <= 0) {
                // 如果期初余额为正 && 最终余额小于 期初，证明本期发生了扣款
//                assistantResult.setType("D");
                return "D";
            } else if (upSum.compareTo(BigDecimal.ZERO) < 0 && totalSum.compareTo(upSum) >= 0) {
                // 如果期初余额为负 && 最终余额大于 期初，证明本期发生了加款
//                assistantResult.setType("D");
                return "D";
            } else if (upSum.compareTo(BigDecimal.ZERO) == 0 && totalSum.compareTo(upSum) == 0){
//                assistantResult.setType("无法判断");
                return "无法判断";
            } else {
                // 期初为0也会到达
                return findABC(low, assistantResult);
            }
        } else {
            // 都是本期的
            return findABC(low, assistantResult);
        }
    }


    /**
     * 判断是否属于ABC类
     */
    public static String findABC(List<OtherInfo3> list, AssistantResult assistantResult) {
        Map<String, List<OtherInfo3>> collect = list.stream().collect(Collectors.groupingBy(OtherInfo3::getS));
        int systemSize = 0;
        int personalSize = 0;
        // 遍历来源字段
        for (String form : collect.keySet()) {
//            if (form.equals("物业收费系统") || form.equals("EMS") || form.equals("TMS资金接口") || form.equals("PS人力资源系统") || form.equals("物业ERP")) {
//                systemSize += 1;
//            } else if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
//                personalSize += 1;
//            } else {
//                assistantResult.setType("E");
////                throw new RuntimeException("额外的来源类型："+ form);
//            }
            if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
                personalSize += 1;
            }else {
                systemSize += 1;
            }
        }
        if (systemSize != 0 && personalSize != 0) {
            // 人工 + 系统
//            assistantResult.setType("C");
            return "C";
        } else if (systemSize != 0) {
//            assistantResult.setType("A");
            return "A";
        } else if (personalSize != 0) {
//            assistantResult.setType("B");
            return "B";
        }else {
//            assistantResult.setType("所有数据借贷抵消");
            return "所有数据借贷抵消";
        }
    }


}
