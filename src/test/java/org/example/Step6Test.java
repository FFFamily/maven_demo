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
            List<OracleData> oracleData = jdbcTemplate.query(findSql, new BeanPropertyRowMapper<>(OracleData.class));
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
                Map<String, List<Step6OldDetailExcel>> projectOldMap = timeGroupOld.stream().collect(Collectors.groupingBy(item -> item.getProjectName().split("－")[0]));
                Map<String, List<OracleData>> projectNewMap = timeGroupNew.stream().collect(Collectors.groupingBy(item -> item.get科目段描述().split("-")[0]));
                List<String> allProjectKey = Stream.of(projectOldMap.keySet(), projectNewMap.keySet()).flatMap(Collection::stream).distinct().collect(Collectors.toList());
                for (String projectKey : allProjectKey) {
                    List<Step6OldDetailExcel>  projectOld = projectOldMap.getOrDefault(projectKey,new ArrayList<>());
                    List<OracleData> projectNew = projectNewMap.getOrDefault(projectKey,new ArrayList<>());
                    BigDecimal oldSum = projectOld.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.getV()).subtract(CommonUtil.getBigDecimalValue(curr.getW()))), (l, r) -> l);
                    BigDecimal newSum = projectNew.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.get输入借方()).subtract(CommonUtil.getBigDecimalValue(curr.get输入贷方()))), (l, r) -> l);
                    if (oldSum.compareTo(newSum) != 0) {
                        // 两个余额不相等
                        Step6Result1 step6Result1 = new Step6Result1();
                        step6Result1.setCompanyName(companyName);
                        step6Result1.setOldProject(projectKey);
                        step6Result1.setNewProject(projectKey);
                        step6Result1.setOldMoney(oldSum);
                        step6Result1.setNewMoney(newSum);
                        step6Result1.setTime(timeKey);
                        step6Result1.setSubMoney(oldSum.subtract(newSum));
                        result1s.add(step6Result1);
                        // 找到造成差额的明细账
                        int oldSize = projectOld.size();
                        int newSize = projectNew.size();
                        if (oldSize > newSize) {
                            Map<String, Step6OldDetailExcel> collect = projectOld.stream().collect(Collectors.toMap(Step6OldDetailExcel::getMatch, item -> item,(l,r) -> l));
                            for (int i = 0; i < newSize; i++) {
                                OracleData newData = projectNew.get(i);
                                Step6OldDetailExcel oldData = collect.get(newData.get行说明());
                                // todo 获取余额
                                BigDecimal oldBalance = CommonUtil.getBigDecimalValue(oldData.getV()).subtract(CommonUtil.getBigDecimalValue(oldData.getW()));
                                BigDecimal newBalance = CommonUtil.getBigDecimalValue(newData.get输入借方()).subtract(CommonUtil.getBigDecimalValue(newData.get输入贷方()));
                                if (oldBalance.compareTo(newBalance) != 0) {
                                    // 余额不相等
                                    result2s.add(newData);
                                    result3s.add(oldData);
                                }
                            }
                            for (int i = newSize; i < oldSize; i++) {
                                // TODO 打标记
                                Step6OldDetailExcel data = projectOld.get(i);
                                result3s.add(data);
                            }
                        }else if (oldSize < newSize) {
                            Map<String, OracleData> collect = projectNew.stream().collect(Collectors.toMap(OracleData::get行说明, item -> item,(l,r) -> l));
                            for (int i = 0; i < oldSize; i++) {
                                Step6OldDetailExcel oldData = projectOld.get(i);
                                OracleData newData = collect.get(oldData.getMatch());
                                // todo 获取余额
                                BigDecimal oldBalance = CommonUtil.getBigDecimalValue(oldData.getV()).subtract(CommonUtil.getBigDecimalValue(oldData.getW()));
                                BigDecimal newBalance = CommonUtil.getBigDecimalValue(newData.get输入借方()).subtract(CommonUtil.getBigDecimalValue(newData.get输入贷方()));
                                if (oldBalance.compareTo(newBalance) != 0) {
                                    // 余额不相等
                                    result2s.add(newData);
                                    result3s.add(oldData);
                                }
                            }
                            for (int i = oldSize; i < newSize; i++) {
                                // TODO 打标记
                                OracleData data = projectNew.get(i);
                                result2s.add(data);
                            }
                        }else {
                            Map<String, Step6OldDetailExcel> collect = projectOld.stream().collect(Collectors.toMap(Step6OldDetailExcel::getMatch, item -> item,(l,r) -> l));
                            for (int i = 0; i < newSize; i++) {
                                OracleData newData = projectNew.get(i);
                                Step6OldDetailExcel oldData = collect.get(newData.get行说明());
                                // todo 获取余额
                                BigDecimal oldBalance = CommonUtil.getBigDecimalValue(oldData.getV()).subtract(CommonUtil.getBigDecimalValue(oldData.getW()));
                                BigDecimal newBalance = CommonUtil.getBigDecimalValue(newData.get输入借方()).subtract(CommonUtil.getBigDecimalValue(newData.get输入贷方()));
                                if (oldBalance.compareTo(newBalance) != 0) {
                                    // 余额不相等
                                    result2s.add(newData);
                                    result3s.add(oldData);
                                }
                            }
                        }
                    }
                }
            }

            // 方法3 如果写到不同的sheet 不同的对象
            String fileName =  "第六步数据-" + companyName + ".xlsx";
            // 这里 指定文件
            try (ExcelWriter excelWriter = EasyExcel.write(fileName).build()) {
                // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "模板" + 0).head(Step6Result1.class).build();
                excelWriter.write(result1s, writeSheet1);
                WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "模板" + 1).head(OracleData.class).build();
                excelWriter.write(result2s, writeSheet2);
                WriteSheet writeSheet3 = EasyExcel.writerSheet(2, "模板" + 2).head(Step6OldDetailExcel.class).build();
                excelWriter.write(result3s, writeSheet3);
            }
        }
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
                                    if (!(projectName.startsWith("应付账款")
                                            || projectName.startsWith("预付账款")
                                            || projectName.startsWith("合同负债")
                                            || projectName.startsWith("预收账款")
                                            || projectName.startsWith("应收账款")
                                            || projectName.startsWith("其他应付款")
                                            || projectName.startsWith("其他应收款")
                                            || projectName.startsWith("其他货币基金"))){
                                        // 只需要7大往来
                                        continue;
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
