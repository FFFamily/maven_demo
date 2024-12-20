package org.example;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.hash.Hash;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.tomcat.Jar;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyConstant;
import org.example.utils.CompanyTypeConstant;
import org.example.新老系统.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
//    @Resource
//    private FindAllBalance findAllBalance;
    @Data
    @AllArgsConstructor
    public static class Item{
        private String selectPath;
        // 江苏中南物业服务有限公司
        private String selectCompanyName;
    }
    private Map<String,String> initMap(){
        Map<String,String> map = new HashMap<>();
        return map;
    };
    @Test
    void init(){
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        for (String fileName : file.list()) {
            String name = fileName.replace(".xlsx", "");
            System.out.println("2023-当前文件："+name);
            try {
                // 老系统数据
                List<Step6OldDetailExcel> excels = findUtil.readPropertyExcel(fileName);
                Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
                    String companyName = item.getCompanyName().split("-")[0];
                    return CompanyConstant.getNewCompanyByOldCompany(companyName);
                }));
                for (String newCompanyName : companyMap.keySet()) {
                    System.out.println("map.put(\"" + newCompanyName + "\", \""+name+"\");");
                }
            }catch (Exception e){
                System.out.println("异常- 当前公司为：" + DateUtil.date());
            }
        }
    }
    @Test
    void  test(){
        List<String> allCompany = findAllCompany();
        for (String company : allCompany) {
            String type = CompanyTypeConstant.mapping.get(company);
            System.out.println("当前公司："+company);
            System.out.println("当前公司分类："+type);
            if (type.equals(CompanyTypeConstant.ZHONG_NAN)){
                // 只有中南的才跑
                mergeAll(company);
            }
        }
    }

    void mergeAll(String company){
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        for (String fileName : file.list()) {
            String name = fileName.replace(".xlsx", "");
            System.out.println("2023-当前文件："+name);
            if (!name.equals("物业杭州公司")){
                continue;
            }
            try {
                // 老系统数据
                List<Step6OldDetailExcel> excels = findUtil.readPropertyExcel(fileName);
                Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
                    String companyName = item.getCompanyName().split("-")[0];
                    return CompanyConstant.getNewCompanyByOldCompany(companyName);
                }));
                for (String newCompanyName : companyMap.keySet()) {
                    System.out.println("开始- 当前公司为：" + newCompanyName + ": " + DateUtil.date());
                    if (!newCompanyName.equals("江苏中南物业服务有限公司余杭分公司")){
                        continue;
                    }
                    List<OracleData> list1 = find2022.find(newCompanyName);
                    List<OracleData> list2 = find2023.find(companyMap, newCompanyName);
                    List<OracleData> list3 = find2024.find(newCompanyName);
                    List<OracleData> xsList = new ArrayList<>();
                    xsList.addAll(list1);
                    xsList.addAll(list2);
                    xsList.addAll(list3);
//                findAllBalance.find(selectPath,newCompanyName);
                    File excelFile = new File(newCompanyName + "-总序时账" + ".xlsx");
                    if (excelFile.exists()) {
                        System.out.println("文件存在");
                        List<OracleData> oldList = new ArrayList<>();
                        EasyExcel.read(excelFile, Step6OldDetailExcel.class,
                                new PageReadListener<OracleData>(oldList::addAll));
                        oldList.addAll(xsList);
                        EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(oldList);
                    } else {
                        EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(xsList);
                    }
                    System.out.println("结束- 当前公司为：" + newCompanyName + ": " + DateUtil.date());
                }
            }catch (Exception e){
                System.out.println("异常- 当前公司为：" + DateUtil.date());
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
