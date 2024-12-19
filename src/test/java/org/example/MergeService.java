package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.tomcat.Jar;
import org.example.新老系统.Find2022;
import org.example.新老系统.Find2023;
import org.example.新老系统.Find2024;
import org.example.新老系统.FindAllBalance;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    @Data
    @AllArgsConstructor
    public static class Item{
        private String selectPath;
        // 江苏中南物业服务有限公司
        private String selectCompanyName;
    }
    @Test
    void mergeAll(){
        // 8.江苏中南物业服务有限公司泰兴分公司  物业上海公司3
        // 9.江苏中南物业服务有限公司东台分公司  物业上海公司3
        // 10.青岛中南物业管理有限公司烟台分公司 物业济南公司
        // 11.江苏中南物业服务有限公司淮安分公司 物业南京公司
        // 12.江苏中南物业服务有限公司南京分公司  物业南京公司
        // 13.江苏中南物业服务有限公司蚌埠分公司  物业合肥公司
        // 14.江苏中南物业服务有限公司厦门分公司  物业厦门公司
        // 15.江苏中南物业服务有限公司晋江分公司  物业厦门公司
        // 16.江苏中南物业服务有限公司南充分公司  物业成都公司
        // 17.江苏中南物业服务有限公司成都分公司  物业成都公司
        // 18.江苏中南物业服务有限公司天津分公司  物业北京公司
        // 19.江苏中南物业服务有限公司太仓分公司  物业南京公司
        // 20.江苏中南物业服务有限公司梅州分公司  物业深圳公司
        // 21.江苏中南物业服务有限公司西安分公司  物业成都公司
        List<Item> list = new ArrayList<>();
//        list.add(new Item("物业合肥公司","江苏中南物业服务有限公司蚌埠分公司"));
//        list.add(new Item("物业厦门公司","江苏中南物业服务有限公司厦门分公司"));
//        list.add(new Item("物业厦门公司","江苏中南物业服务有限公司晋江分公司"));
        list.add(new Item("物业上海公司1","江苏中南物业服务有限公司"));
//        list.add(new Item("物业成都公司","江苏中南物业服务有限公司南充分公司"));
//        list.add(new Item("物业成都公司","江苏中南物业服务有限公司成都分公司"));
//        list.add(new Item("物业北京公司","江苏中南物业服务有限公司天津分公司"));
//        list.add(new Item("物业南京公司","江苏中南物业服务有限公司太仓分公司"));
//        list.add(new Item("物业深圳公司","江苏中南物业服务有限公司梅州分公司"));
//
//        list.add(new Item("物业成都公司","江苏中南物业服务有限公司西安分公司"));
//        list.add(new Item("物业深圳公司","江苏中南物业服务有限公司惠州分公司"));

        for (Item item : list) {
            //  物业上海公司2
            String selectPath = item.selectPath;
            // 江苏中南物业服务有限公司
            String selectCompanyName = item.selectCompanyName; //  江苏中南物业服务有限公司嘉兴分公司
            Boolean findAll = false;
            // 查询所有的
            List<String> allCompany = findAllCompany();
            for (String newCompanyName : allCompany) {
                if (!newCompanyName.equals(selectCompanyName)){
                    continue;
                }
                System.out.println("开始- 当前公司为："+newCompanyName);
                find2022.find(findAll,newCompanyName);
                find2023.find(findAll,selectPath,newCompanyName);
                find2024.find(findAll,selectPath,newCompanyName);
                findAllBalance.find(findAll,selectPath,newCompanyName);
            }
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
