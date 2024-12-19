package org.example;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.tomcat.Jar;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.CompanyConstant;
import org.example.新老系统.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private FindUtil findUtil;
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
//        File file = new File();
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");

        for (String fileName : file.list()) {
            String name = fileName.replace(".xlsx", "");
            System.out.println("2023-当前文件："+name);
            // 老系统数据
            List<Step6OldDetailExcel> excels = findUtil.readPropertyExcel(fileName);
            Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
                String companyName = item.getCompanyName().split("-")[0];
                return CompanyConstant.getNewCompanyByOldCompany(companyName);
            }));
            for (String newCompanyName : companyMap.keySet()) {
                System.out.println("开始- 当前公司为："+newCompanyName);
                List<OracleData> list1 = find2022.find(newCompanyName);
                List<OracleData> list2 = find2023.find(companyMap, newCompanyName);
                List<OracleData> list3 = find2024.find(newCompanyName);
                List<OracleData> xsList = new ArrayList<>();
                xsList.addAll(list1);
                xsList.addAll(list2);
                xsList.addAll(list3);
//                findAllBalance.find(selectPath,newCompanyName);
                File excelFile = new File(newCompanyName + "-总序时账" + ".xlsx");
                if (excelFile.exists()){
                    System.out.println("文件存在");
                    List<OracleData> oldList = new ArrayList<>();
                    EasyExcel.read(excelFile, Step6OldDetailExcel.class,
                            new PageReadListener<OracleData>(oldList::addAll));
                    oldList.addAll(xsList);
                    EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(oldList);
                }else {
                    EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(xsList);
                }
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
