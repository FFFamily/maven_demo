package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.Assistant;
import org.example.enitty.OracleData;
import org.example.enitty.yu_zhou.YuZhouOldBalanceExcel;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            System.out.println("当前公式为： "+companyName);
            List<Step6OldDetailExcel> list = companyMap.get(companyName);
            String findSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+companyName+"' AND z.\"期间\" >= '2023-07' AND z.\"期间\" <= '2023-12' AND z.\"批名\" like '%NCC%'";
            List<OracleData> oracleData = jdbcTemplate.queryForList(findSql, OracleData.class);
            // 按月进行分组
            Map<String, List<Step6OldDetailExcel>> collect = list.stream().collect(Collectors.groupingBy(item -> {
                DateTime date = DateUtil.date(item.getTime());
                int year = date.year();
                int month = date.month() + 1;
                return year + "-" + month;
            }));

        }
    }




    /**
     * 读取物业excel
     * @return
     */
    public List<Step6OldDetailExcel> readPropertyExcel(){
        List<Step6OldDetailExcel> excels = new ArrayList<>();
        // 读取旧系统的余额信息 2022年
        EasyExcel.read("", YuZhouOldBalanceExcel.class,
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
                .sheet("").headRowNumber(2).doRead();
        return excels;
    }
}
