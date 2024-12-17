package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.unit.DataUnit;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import org.example.enitty.OracleData;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyTypeConstant;
import org.example.utils.SqlUtil;
import org.example.新老系统.Step5;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class Step5Test {
    @Resource
    private Step5 step5;
    @Resource
    private SqlUtil sqlUtil;
    @Test
    void test1() {
        List<String> allCompany = sqlUtil.findAllCompany();
        for (String company : allCompany) {
            String type = CompanyTypeConstant.mapping.get(company);
            if (type.equals(CompanyTypeConstant.ZHONG_NAN)){
                if (!company.equals("唐山中南国际旅游度假物业服务有限责任公司")){
                    continue;
                }
                System.out.println("当前公司："+company);
                List<OracleData> sqlList = step5.step5Test(company);
                String fileName = "第五步数据-"+company + ".xlsx";
                EasyExcel.write(fileName, OracleData.class).sheet("模板").doWrite(sqlList);
            }
        }
    }


}
