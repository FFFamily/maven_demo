package org.example.分类;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.Assistant;
import org.example.func_two.Main2;
import org.example.func_two.OtherInfo2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分裂
 */
@Service
public class FindABCDBySystem {
    @Resource
    private JdbcTemplate jdbcTemplate;


    public static void doFindABDC(String sourceFile){
        List<AssistantResult> excelExcelData = new ArrayList<>();
        List<OtherInfo2> cachedDataList = new ArrayList<>();
        List<Assistant> assistantList = new ArrayList<>();

        EasyExcel.read(fileName1, OtherInfo2.class, new PageReadListener<OtherInfo2>(dataList -> {
            Main2.organizeData(dataList);
            cachedDataList.addAll(dataList);
        })).sheet().doRead();
        // 要查询ABCD的字段
        EasyExcel.read(sourceFile, Assistant.class, new PageReadListener<Assistant>(assistantList::addAll)).sheet("往来清理明细表").doRead();


        List<Assistant> realAssistantList = assistantList.stream()
//                .filter(item -> "禹洲物业服务有限公司泉州分公司应付账款-暂估款-物业-外拓项目-住宅--泉州海德堡SS:438846:JODV0:SYZ000311".equals(item.getR()))
                .skip(1)
                .collect(Collectors.toList());
        for (int i = 0; i < realAssistantList.size(); i++) {
            Assistant assistant = realAssistantList.get(i);
            System.out.println("当前行：" + (i + 2));
            String z = assistant.getZ();
            if (z == null) {
                System.out.println("z 为null 当前月无需处理");
                continue;
            }
            String projectName = assistant.getR();
            List<OtherInfo2> startCollect = cachedDataList.stream()
                    .filter(item -> item.getZ().equals(projectName))
                    .collect(Collectors.toList());
            List<OtherInfo2> result = Main2.doMain(false, cachedDataList, startCollect, assistant.getZ(),projectName,0);
            AssistantResult assistantResult;
            if (result.isEmpty()) {
                // 证明全部匹配
                assistantResult = findABCD(startCollect, assistant);
            } else {
                assistantResult = findABCD(result, assistant);
            }
            excelExcelData.add(assistantResult);
        }
        String resultFileName = "ABCD分类-"+System.currentTimeMillis() + ".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(AssistantResult.class).build();
            excelWriter.write(excelExcelData, writeSheet1);
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

    public static AssistantResult findABCD(List<OtherInfo2> result, Assistant assistant) {
        // 通过总账日期进行分类
        AssistantResult assistantResult = new AssistantResult();
        assistantResult.setField(assistant.getR());
        assistantResult.setIndex(assistant.getA());
        String z = assistant.getZ();
        // 期初
        List<OtherInfo2> up = new ArrayList<>();
        // 本期
        List<OtherInfo2> low = new ArrayList<>();
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
            assistantResult.setIsIncludeUp(1);
            assistantResult.setType("D");
        } else if (!up.isEmpty()) {
            assistantResult.setIsIncludeUp(1);
            // 最终余额
            BigDecimal totalSum = getZValue(z);
            // 期初余额
            BigDecimal upSum = up.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(curr.getV() != null ? curr.getV() : BigDecimal.ZERO).subtract(curr.getW() != null ? curr.getW() : BigDecimal.ZERO), (l, r) -> l);
            if (upSum.compareTo(BigDecimal.ZERO) > 0 && totalSum.compareTo(upSum) <= 0) {
                // 如果期初余额为正 && 最终余额小于 期初，证明本期发生了扣款
                assistantResult.setType("D");
            } else if (upSum.compareTo(BigDecimal.ZERO) < 0 && totalSum.compareTo(upSum) >= 0) {
                // 如果期初余额为负 && 最终余额大于 期初，证明本期发生了加款
                assistantResult.setType("D");
            } else if (upSum.compareTo(BigDecimal.ZERO) == 0 && totalSum.compareTo(upSum) == 0){
                assistantResult.setType("");
            } else {
                // 期初为0也会到达
                findABC(low, assistantResult);
            }
        } else {
            // 都是本期的
            findABC(low, assistantResult);
        }
        return assistantResult;
    }


    /**
     * 判断是否属于ABC类
     */
    public static void findABC(List<OtherInfo2> list, AssistantResult assistantResult) {
        Map<String, List<OtherInfo2>> collect = list.stream().collect(Collectors.groupingBy(OtherInfo2::getS));
        int systemSize = 0;
        int personalSize = 0;
        // 遍历来源字段
        for (String form : collect.keySet()) {
            if (form.equals("物业收费系统") || form.equals("EMS") || form.equals("TMS资金接口") || form.equals("PS人力资源系统") || form.equals("物业ERP")) {
                systemSize += 1;
            } else if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
                personalSize += 1;
            } else {
                throw new RuntimeException("额外的来源类型");
            }
        }
        if (systemSize != 0 && personalSize != 0) {
            // 人工 + 系统
            assistantResult.setType("C");
        } else if (systemSize != 0) {
            assistantResult.setType("A");
        } else if (personalSize != 0) {
            assistantResult.setType("B");
        }
    }


}
