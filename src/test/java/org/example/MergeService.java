package org.example;

import org.apache.tomcat.Jar;
import org.example.新老系统.Find2022;
import org.example.新老系统.Find2023;
import org.example.新老系统.Find2024;
import org.example.新老系统.FindAllBalance;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class MergeService {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private Find2022 find2022;
    @Resource
    private Find2023 find2023;
    @Resource
    private Find2024 find2024;
    @Resource
    private FindAllBalance findAllBalance;
    @Test
    void mergeAll(){
        String selectPath = "物业上海公司1";
        String selectCompanyName = "江苏中南物业服务有限公司";
        // 查询所有的
        List<String> allCompany = findAllCompany();
        for (String newCompanyName : allCompany) {
            System.out.println("当前公司为："+newCompanyName);
            if (!newCompanyName.equals(selectCompanyName)){
                continue;
            }
            find2022.find(false,newCompanyName);
            find2023.find(false,selectPath,newCompanyName);
            find2024.find(false,selectPath,newCompanyName);
            findAllBalance.find(false,selectPath,newCompanyName);
        }
    }

    private List<String> findAllCompany(){
        List<String> companyList = jdbcTemplate.queryForList(
                "select z.\"公司段描述\" from ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\" ",
                String.class
        );
        return companyList;
    }
}
