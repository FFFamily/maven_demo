package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.Data;
import org.example.enitty.Assistant;
import org.example.enitty.OracleData;
import org.example.enitty.yu_zhou.YuZhouOldBalanceExcel;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.Step6Result1;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyConstant;
import org.example.utils.CoverNewDate;
import org.example.新老系统.Step1;
import org.example.新老系统.Step6;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
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
    private Step6 step6;
    @Resource
    private CoverNewDate coverNewDate;

    @Test
    void test1() {
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        for (String fileName : Objects.requireNonNull(file.list())) {
            String name = fileName.replace(".xlsx", "");
            System.out.println("当前文件："+name);
            if (!name.equals("物业北京公司")){
                continue;
            }
            List<Step6OldDetailExcel> excels = step6.readPropertyExcel(fileName);
            Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
                String companyName = item.getCompanyName().split("-")[0];
                return CompanyConstant.getNewCompanyByOldCompany(companyName);
            }));
            for (String companyName : companyMap.keySet()) {
                if (!companyName.equals("")){
                    continue;
                }
                Step6.Step6TestResult step6TestResult = step6.step6Test(companyName, companyMap);
                if (step6TestResult == null) {
                    continue;
                }
                List<Step6Result1> result1s = step6TestResult.getResult1s();
                List<OracleData> result2s = step6TestResult.getResult2s();
                List<Step6OldDetailExcel> result3s = step6TestResult.getResult3s();
                // 这里 指定文件
                try (ExcelWriter excelWriter = EasyExcel.write(name+"-"+companyName+"-第六步数据.xlsx").build()) {
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
    }
}
