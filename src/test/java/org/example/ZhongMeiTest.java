package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Builder;
import lombok.Data;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.*;
import org.example.utils.CommonUtil;
import org.example.寻找等级.FindNccZhongNanLevel;
import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.awt.image.Kernel;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class ZhongMeiTest {
    @Resource
    private FindNccZhongNanLevel findNccZhongNanLevel;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Data
    @Builder
    private static class Result{
        List<NewBalanceExcelResult> results;
        List<Step6OldDetailExcel> allCompanyList;
    }
    @Test
    void test2022() {
        List<Step6OldDetailExcel> excels = readPropertyExcel();
        Map<String, List<Step6OldDetailExcel>> collect = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
        for (String companyName : collect.keySet()) {
            String nowCompanyName = companyName.split("-")[0];
            if (!nowCompanyName.equals("江苏中南物业服务有限公司温州分公司")){
                continue;
            }
            System.out.println(nowCompanyName);
            Result result = doTest(collect, companyName);
            String fileName = "组合余额表-"+companyName + ".xlsx";
            EasyExcel.write(fileName, NewBalanceExcelResult.class).sheet("旧系统").doWrite(result.getResults());
            String fileName2 = "组合余额表-总账-"+companyName + ".xlsx";
            EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(result.getAllCompanyList());
        }
    }

    @Test
    void test20230106() {
        List<Step6OldDetailExcel> excels = readPropertyExcel();
        Map<String, List<Step6OldDetailExcel>> collect = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
        for (String companyName : collect.keySet()) {
            String nowCompanyName = companyName.split("-")[0];
            if (!nowCompanyName.equals("江苏中南物业服务有限公司温州分公司")){
                continue;
            }
            System.out.println(nowCompanyName);
            Result result = doTest(collect, companyName);
            String fileName = "组合余额表-23(1-6)-"+companyName + ".xlsx";
            EasyExcel.write(fileName, NewBalanceExcelResult.class).sheet("旧系统").doWrite(result.getResults());
            String fileName2 = "组合余额表-23(1-6)-总账-"+companyName + ".xlsx";
            EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(result.getAllCompanyList());
        }
    }

    public Result doTest(Map<String, List<Step6OldDetailExcel>> collect,String companyName){
        List<NewBalanceExcelResult> results = new ArrayList<>();
        List<Step6OldDetailExcel> allCompanyList = collect.get(companyName);
        Map<String, List<Step6OldDetailExcel>> result =
                allCompanyList.stream().collect(Collectors.groupingBy(item -> item.getOnlySign()+item.getAuxiliaryAccounting()));
        for (String onlySign : result.keySet()) {
            List<Step6OldDetailExcel> all = result.get(onlySign);
            Step6OldDetailExcel step6OldDetailExcel = all.get(0);
            NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
            newBalanceExcelResult.setOnlySign(step6OldDetailExcel.getOnlySign());
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
                                    String projectName = data.getProjectName();
                                    if (!isBackProject2022(projectName)){
                                        continue;
                                    }
                                    Date time = data.getTime();
                                    DateTime date = DateUtil.date(time);
//                                    if (date.isBefore(DateUtil.parse("2022-01-01")) || date.isAfter(DateUtil.parse("2022-12-31"))) {
//                                        continue;
//                                    }
                                    if (date.isBefore(DateUtil.parse("2023-01-01")) || date.isAfter(DateUtil.parse("2022-06-30"))) {
                                        continue;
                                    }
                                    // 年
                                    int year = date.year();
                                    // 月
                                    int month = date.month() + 1;
                                    StringBuilder builder = new StringBuilder();
                                    // 1- 机构代码
                                    String companyName = data.getCompanyName();
                                    ZNCompanyMapping znCompanyMapping = findNccZhongNanLevel.znCompanyMapping.get(companyName);
                                    String fmsCompanyCode = znCompanyMapping.getFMSCompanyCode();
                                    builder.append(fmsCompanyCode).append(".");
                                    data.setCompanyCode(fmsCompanyCode);
                                    // 2- 部门
//                                    String orgName = data.getOrgName();
//                                    ZNOrgMapping znOrgMapping = findNccZhongNanLevel.znOrgMapping.get(orgName);
//                                    String fmsOrgCode = znOrgMapping.getFMSOrgCode();
//                                    builder.append(fmsOrgCode).append(".");
                                    builder.append("0").append(".");

                                    // 5-产品代码
                                    String eventName = data.getEventName();
                                    ZNEventMapping znEventMapping = findNccZhongNanLevel.znEventMapping.get(companyName + eventName);
                                    String fmsProductCode = znEventMapping == null ? "0": znEventMapping.getFmsProductCode();
                                    builder.append(appendStr(fmsProductCode) ).append(".");
                                    // 3-科目代码 4-子目代码
                                    findProjectInfoByTime(data,year,month,builder);
                                    // 6-地区代码
                                    String fmsAreaCode = "0";
                                    builder.append(fmsAreaCode).append(".");
                                    // 7-SBU
                                    String fmsSBU = "0";
                                    builder.append(fmsSBU).append(".");
                                    // 8-ICP
                                    String customerName = data.getCustomerName();
                                    ZNIPCMapping znipcMapping = findNccZhongNanLevel.znipcMapping.get(customerName);
                                    String icp = znipcMapping == null ? null : znipcMapping.getFmsICPCode();
//                                    ZNCompanyMapping znCompanyMapping1 = findNccZhongNanLevel.znCustomerMapping.get(customerName);
//                                    String icp = znCompanyMapping1.getFMSCompanyCode() == null ? "0" : znCompanyMapping1.getFMSCompanyCode();
                                    builder.append(appendStr(icp)).append(".");
                                    // 9-项目代码
                                    String fmsEventCode = znEventMapping == null ? "0" : znEventMapping.getFmsEventCode();
                                    builder.append(appendStr(fmsEventCode) ).append(".");
                                    // 10-备用
                                    String standby  = "0";
                                    builder.append(standby);
                                    String onlySign = builder.toString();
                                    data.setOnlySign(onlySign);
                                    // 辅助核算
                                    String auxiliaryAccounting = "";
                                    if (icp != null){
                                        auxiliaryAccounting += "-";
                                    }else {
                                        auxiliaryAccounting += data.getCustomerName() != null ? data.getCustomerName() : data.getPersonalName() == null ? "-" : data.getPersonalName();
                                    }
                                    data.setAuxiliaryAccounting(auxiliaryAccounting);
                                    excels.add(data);
                                }catch (Exception e){
//                                    System.out.println("解析中南老系统明细数据出错: "+e.getMessage());
                                    System.out.println(data);
                                    e.printStackTrace();
                                }

                            }
                        }))
                .sheet("综合查询表").headRowNumber(3).doRead();
        return excels;
    }

    private void findProjectInfoByTime(Step6OldDetailExcel data ,int year,int month,StringBuilder builder){
        // 3-科目代码
        String projectCode = data.getProjectCode();
        ZNProjectMapping znProjectMapping = findNccZhongNanLevel.znProjectMapping.get(projectCode);
        String fmsProjectCode =  znProjectMapping.getFmsProjectCode();
        // 4-子目
        String fmsChildProjectCode = znProjectMapping.getFmsChildProjectCode();
        if (year == 2022){
            builder.append(appendStr(fmsProjectCode) ).append(".");
            builder.append(appendStr(fmsChildProjectCode) ).append(".");
        }else if (year == 2023 && month >= 1 && month <= 6){
            String customerName = data.getCustomerName();
            ZNRelationMapping znRelationMapping = findNccZhongNanLevel.znRelationMapping.get(customerName);
            if (znRelationMapping != null){
                System.out.println("原中南关联表存在对应的客商");
                ZNRelationProjectMapping znRelationProjectMapping = findNccZhongNanLevel.znRelationProjectMapping.get(fmsProjectCode);
                fmsProjectCode = znRelationProjectMapping.getFmsProjectCode();
                fmsChildProjectCode = znRelationProjectMapping.getFmsChildProjectCode();
                builder.append(appendStr(fmsProjectCode) ).append(".");
                builder.append(appendStr(fmsChildProjectCode) ).append(".");
            }
        }

    }


    public String appendStr(String str){
        return  str == null ? "0" : str;
    }

    private Boolean isBackProject2022(String projectName) {
        return projectName.startsWith("应付账款")
                || projectName.startsWith("预付账款")
                || projectName.startsWith("合同负债")
                || projectName.startsWith("预收账款")
                || projectName.startsWith("应收账款")
                || projectName.startsWith("其他应付款")
                || projectName.startsWith("其他应收款")
                || projectName.startsWith("应收票据");
    }
}
