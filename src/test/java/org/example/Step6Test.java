package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.enitty.OracleData;
import org.example.enitty.yu_zhou.YuZhouOldBalanceExcel;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.Step6Result1;
import org.example.utils.CommonUtil;
import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.utils.CommonUtil.getZ;

@SpringBootTest
public class Step6Test {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    void test1() {
        List<Step6OldDetailExcel> excels = readPropertyExcel();
        Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> item.getCompanyName()));
        for (String companyName : companyMap.keySet()) {
            List<Step6Result1> result1s = new ArrayList<>();
            List<OracleData> result2s = new ArrayList<>();
            List<Step6OldDetailExcel> result3s = new ArrayList<>();
            System.out.println("当前公司为： "+companyName);
            if (!companyName.equals("江苏中南物业服务有限公司温州分公司")){
                continue;
            }
            List<Step6OldDetailExcel> list = companyMap.get(companyName);
            String findSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+companyName+"' AND z.\"期间\" >= '2023-07' AND z.\"期间\" <= '2023-12' AND z.\"批名\" like '%NCC%'";
            List<OracleData> oracleData = jdbcTemplate.query(findSql, new BeanPropertyRowMapper<>(OracleData.class))
                    .stream()
                    .peek(item -> {
                        String newProject = getNewProject(item);
                        item.setActualProject(newProject);
                        if (newProject.contains("合同负债") || newProject.contains("预收账款")){
                            item.setMatchProject("合同负债/预收账款");
                        }else {
                            item.setMatchProject(newProject);
                        }
                    })
                    .filter(item -> isBackProject(item.getActualProject()))
                    .collect(Collectors.toList());
            // 按月进行分组
            Map<String, List<Step6OldDetailExcel>> timeOldCollect = list.stream().collect(Collectors.groupingBy(item -> {
                DateTime date = DateUtil.date(item.getTime());
                int year = date.year();
                int month = date.month() + 1;
                return year + "-" + (month > 9 ? month : "0" + month);
            }));
            Map<String, List<OracleData>> timeNewCollect = oracleData.stream().collect(Collectors.groupingBy(OracleData::get期间));
            List<String> timeOldKeyCollect = new ArrayList<>(timeOldCollect.keySet());
            List<String> timeNewKeyCollect = new ArrayList<>(timeNewCollect.keySet());
            // 所有的时间
            List<String> allTimeKey = Stream.of(timeOldKeyCollect, timeNewKeyCollect).flatMap(Collection::stream).distinct().collect(Collectors.toList());
            for (String timeKey : allTimeKey) {
                List<Step6OldDetailExcel>  timeGroupOld = timeOldCollect.get(timeKey);
                List<OracleData> timeGroupNew = timeNewCollect.get(timeKey);
                Map<String, List<Step6OldDetailExcel>> projectOldMap = timeGroupOld.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getMatchProject));
                Map<String, List<OracleData>> projectNewMap = timeGroupNew.stream().collect(Collectors.groupingBy(OracleData::getMatchProject));
                List<String> allProjectKey = Stream.of(projectOldMap.keySet(), projectNewMap.keySet()).flatMap(Collection::stream).distinct().collect(Collectors.toList());
                for (String projectKey : allProjectKey) {
                    List<Step6OldDetailExcel>  projectOld = projectOldMap.getOrDefault(projectKey,new ArrayList<>());
                    List<OracleData> projectNew = projectNewMap.getOrDefault(projectKey,new ArrayList<>());

                    BigDecimal oldSum = projectOld.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.getV()).subtract(CommonUtil.getBigDecimalValue(curr.getW()))), (l, r) -> l);
                    BigDecimal newSum = projectNew.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.get输入借方()).subtract(CommonUtil.getBigDecimalValue(curr.get输入贷方()))), (l, r) -> l);
                    if (oldSum.compareTo(newSum) != 0) {
                        // 两个余额不相等
                        Step6Result1 step6Result1 = create(
                                companyName,
                                timeKey,
                                projectOld.stream().map(Step6OldDetailExcel::getActualProject).distinct().collect(Collectors.joining("、")),
                                projectNew.stream().map(OracleData::getActualProject).distinct().collect(Collectors.joining("、")),
                                oldSum,
                                newSum);
                        step6Result1.setRemark("余额不相等");
                        result1s.add(step6Result1);
                        // 找到造成差额的明细账
                        int oldSize = projectOld.size();
                        int newSize = projectNew.size();
                        if (oldSize > newSize) {
                            Map<String, List<OracleData>> collect = projectNew.stream().collect(Collectors.groupingBy(item -> item.get行说明()));
                            for (int i = 0; i < newSize; i++) {
                                Step6OldDetailExcel oldData = projectOld.get(i);
                                BigDecimal oldBalance = getOldBalance(oldData);
                                List<OracleData> newDataList = collect.getOrDefault(oldData.getMatch(),new ArrayList<>());
                                if (newDataList.size() == 1){
                                    OracleData newData = newDataList.get(0);
                                    BigDecimal newBalance = getNewBalance(newData);
                                    if (oldBalance.compareTo(newBalance) != 0) {
                                        // 余额不相等
                                        newData.set备注(timeKey+projectKey+"：金额不相等");
                                        oldData.setRemark(timeKey+projectKey+"：金额不相等");
                                    }
                                }else {
                                    for (OracleData newData : newDataList) {
                                        BigDecimal newBalance = getNewBalance(newData);
                                        if (!newData.getUsed() && newBalance.compareTo(oldBalance) == 0){
                                            oldData.setUsed(true);
                                            newData.setUsed(true);
                                            oldData.setRemark(timeKey+projectKey+"：存在多个匹配，但余额相等");
                                            break;
                                        }
                                    }
                                }
                            }
                            for (int i = newSize; i < oldSize; i++) {
                                Step6OldDetailExcel data = projectOld.get(i);
                                data.setRemark(timeKey+projectKey+"：多出的金额");
                            }
                        }else if (oldSize < newSize) {
                            Map<String, List<OracleData>> collect = projectNew.stream().collect(Collectors.groupingBy(item -> item.get行说明()));
                            for (int i = 0; i < oldSize; i++) {
                                Step6OldDetailExcel oldData = projectOld.get(i);
                                BigDecimal oldBalance = getOldBalance(oldData);
                                List<OracleData> newDataList = collect.getOrDefault(oldData.getMatch(),new ArrayList<>());
                                if (newDataList.size() == 1){
                                    OracleData newData = newDataList.get(0);
                                    BigDecimal newBalance = getNewBalance(newData);
                                    if (oldBalance.compareTo(newBalance) != 0) {
                                        // 余额不相等
                                        newData.set备注(timeKey+projectKey+"：金额不相等");
                                        oldData.setRemark(timeKey+projectKey+"：金额不相等");
                                    }
                                }else {
                                    for (OracleData newData : newDataList) {
                                        BigDecimal newBalance = getNewBalance(newData);
                                        if (!newData.getUsed() && newBalance.compareTo(oldBalance) == 0){
                                            oldData.setUsed(true);
                                            newData.setUsed(true);
                                            oldData.setRemark(timeKey+projectKey+"：存在多个匹配，但余额相等");
                                            break;
                                        }
                                    }
                                }
                            }
                            for (int i = oldSize; i < newSize; i++) {
                                OracleData data = projectNew.get(i);
                                data.set备注(timeKey+projectKey+"：多出的金额");
                            }

                        }else {
                            Map<String, List<OracleData>> collect = projectNew.stream().collect(Collectors.groupingBy(item -> item.get行说明()));
                            for (int i = 0; i < oldSize; i++) {
                                Step6OldDetailExcel oldData = projectOld.get(i);
                                BigDecimal oldBalance = getOldBalance(oldData);
                                List<OracleData> newDataList = collect.getOrDefault(oldData.getMatch(),new ArrayList<>());
                                if (newDataList.size() == 1){
                                    OracleData newData = newDataList.get(0);
                                    BigDecimal newBalance = getNewBalance(newData);
                                    if (oldBalance.compareTo(newBalance) != 0) {
                                        // 余额不相等
                                        newData.set备注(timeKey+projectKey+"：金额不相等");
                                        oldData.setRemark(timeKey+projectKey+"：金额不相等");
                                    }
                                }else {
                                    for (OracleData newData : newDataList) {
                                        BigDecimal newBalance = getNewBalance(newData);
                                        if (!newData.getUsed() && newBalance.compareTo(oldBalance) == 0){
                                            oldData.setUsed(true);
                                            newData.setUsed(true);
                                            oldData.setRemark(timeKey+projectKey+"：存在多个匹配，但余额相等");
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }else {
                        Step6Result1 step6Result1 = create(
                                companyName,
                                timeKey,
                                projectOld.stream().map(Step6OldDetailExcel::getActualProject).distinct().collect(Collectors.joining("、")),
                                projectNew.stream().map(OracleData::getActualProject).distinct().collect(Collectors.joining("、")),
                                oldSum,
                                newSum);
                        result1s.add(step6Result1);
                    }
                    result3s.addAll(projectOld);
                    result2s.addAll(projectNew);
                }
            }

            // 方法3 如果写到不同的sheet 不同的对象
            String fileName =  "第六步数据-" + companyName + ".xlsx";
            // 这里 指定文件
            try (ExcelWriter excelWriter = EasyExcel.write(fileName).build()) {
                // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "模板").head(Step6Result1.class).build();
                excelWriter.write(result1s, writeSheet1);
                WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "新系统").head(OracleData.class).build();
                excelWriter.write(result2s, writeSheet2);
                WriteSheet writeSheet3 = EasyExcel.writerSheet(2, "旧系统").head(Step6OldDetailExcel.class).build();
                excelWriter.write(result3s, writeSheet3);
            }
        }
    }


    private BigDecimal getOldBalance(Step6OldDetailExcel oldData){
        return CommonUtil.getBigDecimalValue(oldData.getV()).subtract(CommonUtil.getBigDecimalValue(oldData.getW()));
    }

    private BigDecimal getNewBalance(OracleData newData){
        return CommonUtil.getBigDecimalValue(newData.get输入借方()).subtract(CommonUtil.getBigDecimalValue(newData.get输入贷方()));
    }

    private Step6Result1 create(String companyName,String timeKey,String oldProjectKey,String newProjectKey,BigDecimal oldSum,BigDecimal newSum){
        Step6Result1 step6Result1 = new Step6Result1();
        step6Result1.setCompanyName(companyName);
        step6Result1.setOldProject(oldProjectKey);
        step6Result1.setNewProject(newProjectKey);
        step6Result1.setOldMoney(oldSum);
        step6Result1.setNewMoney(newSum);
        step6Result1.setTime(timeKey);
        step6Result1.setSubMoney(oldSum.subtract(newSum));
        return step6Result1;
    }

    private String getOldProject(Step6OldDetailExcel excel){
        return excel.getProjectName().split("－")[0];
    }

    private String getNewProject(OracleData oracleData){
        return oracleData.get科目段描述().split("-")[0];
    }

    private Boolean isBackProject(String projectName){
        return projectName.startsWith("应付账款")
                || projectName.startsWith("预付账款")
                || projectName.startsWith("合同负债")
                || projectName.startsWith("预收账款")
                || projectName.startsWith("应收账款")
                || projectName.startsWith("其他应付款")
                || projectName.startsWith("其他应收款")
                || projectName.startsWith("其他货币基金");
    }




    /**
     * 读取物业excel
     * @return
     */
    public List<Step6OldDetailExcel> readPropertyExcel(){
        List<Step6OldDetailExcel> excels = new ArrayList<>();
        // 读取旧系统的余额信息 2022年
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/物业杭州公司 - 副本.xlsx", Step6OldDetailExcel.class,
                        new PageReadListener<Step6OldDetailExcel>(dataList -> {
                            for (Step6OldDetailExcel data : dataList) {
                                try {
                                    if (data.getV() == null && data.getW() == null){
                                        throw new RuntimeException("无法计算金额");
                                    }
                                    String companyName = data.getCompanyName();
                                    String realCompanyName = companyName.split("-")[0];
                                    data.setCompanyName(realCompanyName);
                                    Date time = data.getTime();
                                    DateTime date = DateUtil.date(time);
                                    if (date.isBefore(DateUtil.parse("2023-07-01")) || date.isAfter(DateUtil.parse("2023-12-31"))) {
                                        // 只需要 07-12 月的
                                        continue;
                                    }
                                    // 科目
                                    String projectName = data.getProjectName();
                                    if (!isBackProject(projectName)){
                                        // 只需要7大往来
                                        continue;
                                    }
                                    // 其他货币基金只取 9-12月
                                    if (projectName.startsWith("其他货币基金") && !(date.isBefore(DateUtil.parse("2023-09-01")) || date.isAfter(DateUtil.parse("2023-12-31")))){
                                        System.out.println("过滤：");
                                        System.out.println(data);
                                        continue;
                                    }
                                    // 摘要
                                    String match = data.getMatch();
                                    if (match.contains("资金归集")){
                                        continue;
                                    }

                                    String oldProject = getOldProject(data);
                                    data.setActualProject(oldProject);
                                    if (oldProject.startsWith("其他应收款") || oldProject.startsWith("其他货币基金")){
                                        data.setMatchProject("其他应收款");
                                    }else if (oldProject.startsWith("合同负债") || oldProject.startsWith("预收账款")){
                                        data.setMatchProject("合同负债/预收账款");
                                    } else {
                                        data.setMatchProject(oldProject);
                                    }
                                    excels.add(data);
                                }catch (Exception e){
                                    System.out.println("解析中南老系统明细数据出错");
                                    System.out.println(data);
                                }

                            }
                        }))
                .sheet("综合查询表").headRowNumber(3).doRead();
        return excels;
    }
}
