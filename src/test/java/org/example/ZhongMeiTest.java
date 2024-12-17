package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Builder;
import lombok.Data;
import org.assertj.core.util.Lists;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.*;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyConstant;
import org.example.utils.CoverNewDate;
import org.example.寻找等级.FindNccZhongNanLevel;
import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.awt.image.Kernel;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class ZhongMeiTest {
    @Resource
    private CoverNewDate coverNewDate;

    public List<String> pathList = Lists.newArrayList(
            "src/main/java/org/example/excel/zhong_nan/detail/物业南京公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业北京公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业上海公司1.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业上海公司2.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业上海公司3.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业厦门公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业合肥公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业成都公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业杭州公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业沈阳公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业济南公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业深圳公司.xlsx",
            "src/main/java/org/example/excel/zhong_nan/detail/物业重庆公司.xlsx"
    );
//    public List<String> pathList = Lists.newArrayList(
//            "src/main/java/org/example/excel/zhong_nan/detail/物业上海公司3.xlsx"
//    );
    @Data
    @Builder
    private static class Result{
        List<NewBalanceExcelResult> results;
        List<Step6OldDetailExcel> allCompanyList;
    }
    @Test
    void test2022() {
        for (String path : pathList) {
            System.out.println("当前path"+path);
            List<Step6OldDetailExcel> excels = readPropertyExcel(path,"2022");
            List<NewBalanceExcelResult> pathResult = new ArrayList<>();
            Map<String, List<Step6OldDetailExcel>> collect = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
            for (String companyName : collect.keySet()) {
//                if (!companyName.equals("江苏中南物业服务有限公司温州分公司")){
//                    continue;
//                }
                System.out.println(companyName);
                Result result = doTest(collect, companyName);
                pathResult.addAll(result.getResults());
                String fileName2 = "组合余额表-2022-总账-"+companyName + ".xlsx";
                File file = new File(fileName2);
                if (file.exists()){
                    System.out.println("文件存在");
                    List<Step6OldDetailExcel> list = new ArrayList<>();
                    EasyExcel.read(file, Step6OldDetailExcel.class,
                            new PageReadListener<Step6OldDetailExcel>(list::addAll));
                    list.addAll(result.getAllCompanyList());
                    EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(list);
                }else {
                    EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(result.getAllCompanyList());
                }
            }
            String[] split = path.split("/");
            String fileName ="余额表-"+split[split.length -1];
            EasyExcel.write(fileName, NewBalanceExcelResult.class).sheet("旧系统").doWrite(pathResult);
        }
    }

    @Test
    void test20230106() {
        for (String path : pathList) {
            System.out.println("当前path"+path);
            List<Step6OldDetailExcel> excels = readPropertyExcel(path,"2023-1-6");
            List<NewBalanceExcelResult> pathResult = new ArrayList<>();
            Map<String, List<Step6OldDetailExcel>> collect = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
            for (String companyName : collect.keySet()) {
//                if (!companyName.equals("江苏中南物业服务有限公司温州分公司")){
//                    continue;
//                }
                System.out.println(companyName);
                Result result = doTest(collect, companyName);
                pathResult.addAll(result.getResults());
                String fileName2 = "组合余额表-2023-1-6-总账-"+companyName + ".xlsx";
                File file = new File(fileName2);
                if (file.exists()){
                    System.out.println("文件存在");
                    List<Step6OldDetailExcel> list = new ArrayList<>();
                    EasyExcel.read(file, Step6OldDetailExcel.class,
                            new PageReadListener<Step6OldDetailExcel>(list::addAll));
                    list.addAll(result.getAllCompanyList());
                    EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(list);
                }else {
                    EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(result.getAllCompanyList());
                }
            }
            String[] split = path.split("/");
            String fileName ="余额表-"+split[split.length -1];
            EasyExcel.write(fileName, NewBalanceExcelResult.class).sheet("旧系统").doWrite(pathResult);
        }

    }

    public Result doTest(Map<String, List<Step6OldDetailExcel>> collect,String companyName){
        List<NewBalanceExcelResult> results = new ArrayList<>();
        List<Step6OldDetailExcel> allCompanyList = collect.get(companyName);
        Map<String, List<Step6OldDetailExcel>> result =
                allCompanyList.stream().collect(Collectors.groupingBy(item -> item.getOnlySign()+item.getAuxiliaryAccounting()));
        for (String key : result.keySet()) {
            List<Step6OldDetailExcel> all = result.get(key);
            Step6OldDetailExcel step6OldDetailExcel = all.get(0);
            NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
            newBalanceExcelResult.setCompanyName(companyName);
            String onlySign = step6OldDetailExcel.getOnlySign();
            newBalanceExcelResult.setProjectCode(onlySign);
            newBalanceExcelResult.setProjectName(step6OldDetailExcel.getOnlySignName());
            newBalanceExcelResult.setProject(step6OldDetailExcel.getProject());
            newBalanceExcelResult.setAuxiliaryAccounting(step6OldDetailExcel.getAuxiliaryAccounting());
            newBalanceExcelResult.setV(all.stream().map(Step6OldDetailExcel::getV).reduce(BigDecimal.ZERO, (prev,curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)),(l, r) ->l));
            newBalanceExcelResult.setW(all.stream().map(Step6OldDetailExcel::getW).reduce(BigDecimal.ZERO, (prev,curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)),(l, r) ->l));

            results.add(newBalanceExcelResult);
        }
        return Result.builder().results(results).allCompanyList(allCompanyList).build();
    }

    /**
     * 读取物业excel
     * @return
     */
    public List<Step6OldDetailExcel> readPropertyExcel(String path,String startTime){
        List<Step6OldDetailExcel> excels = new ArrayList<>();
        // 读取旧系统的余额信息 2022年
        EasyExcel.read(path, Step6OldDetailExcel.class,
                        new PageReadListener<Step6OldDetailExcel>(dataList -> {
                            for (Step6OldDetailExcel data : dataList) {
                                coverNewDate.cover(startTime,data);
                                excels.add(data);
                            }
                        }))
                .sheet("综合查询表").doRead();
        return excels;
    }





}
