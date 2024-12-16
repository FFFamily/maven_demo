package org.example;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Data;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Merge22Result;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.ZNIPCMapping;
import org.example.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ZMMergeTest {
    public static void main(String[] args) {
        merge2022();
    }

    static void merge2022(){
        List<Merge22Result> res = new ArrayList<>();
        Map<String,List<OracleData>> map1 = new HashMap<>();
        // 22 调整
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/22年调整.xlsx", OracleData.class, new PageReadListener<OracleData>(dataList -> {
            for (OracleData data : dataList) {
                List<OracleData> list = map1.getOrDefault(data.get公司段描述(), new ArrayList<>());
                list.add(data);
                map1.put(data.get公司段描述(),list);
            }
        })).sheet("模板").doRead();
        // 22年应收账款预估
        Map<String,List<OracleData>> map2 = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/22年应收账款预估收缴率调整.xlsx", OracleData.class, new PageReadListener<OracleData>(dataList -> {
            for (OracleData data : dataList) {
                List<OracleData> list = map2.getOrDefault(data.get公司段描述(), new ArrayList<>());
                list.add(data);
                map2.put(data.get公司段描述(),list);
            }
        })).sheet("模板").doRead();
        List<String> companyList = Stream.of(map1.keySet(), map2.keySet()).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        // 中南2022期初
        List<OracleData> list3 = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/组合余额表-2022-总账-江苏中南物业服务有限公司温州分公司-人民币账簿.xlsx", Step6OldDetailExcel.class, new PageReadListener<Step6OldDetailExcel>(dataList -> {
            for (Step6OldDetailExcel data : dataList) {
                OracleData oracleData = new OracleData();
                oracleData.set账户组合(data.getOnlySign());
                oracleData.set账户描述(data.getOnlySignName());
                oracleData.set交易对象(data.getAuxiliaryAccounting());
                oracleData.set输入借方(data.getV());
                oracleData.set输入贷方(data.getW());
                oracleData.set科目段描述(data.getProject());
                list3.add(oracleData);
            }
        })).sheet("总账").doRead();

        for (String company : companyList) {
            if (!company.equals("江苏中南物业服务有限公司温州分公司")) {
                continue;
            }
            List<NewBalanceExcelResult> result = new ArrayList<>();
            List<OracleData> list1 = map1.getOrDefault(company, new ArrayList<>());
            List<OracleData> list2 = map2.getOrDefault(company, new ArrayList<>());
            Map<String, List<OracleData>> group = Stream.of(list1, list2,list3).flatMap(Collection::stream).collect(Collectors.groupingBy(item -> item.get账户组合() + getStr(item.get交易对象())));
            for (String key : group.keySet()) {
                List<OracleData> all = group.get(key);
                OracleData one = all.get(0);
                NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
                newBalanceExcelResult.setForm("2022");
                newBalanceExcelResult.setCompanyName(company);
                newBalanceExcelResult.setProjectCode(one.get账户组合());
                newBalanceExcelResult.setProjectName(one.get账户描述());
                newBalanceExcelResult.setProject(one.get科目段描述());
                newBalanceExcelResult.setAuxiliaryAccounting(one.get交易对象());
                newBalanceExcelResult.setV(all.stream().map(OracleData::get输入借方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                newBalanceExcelResult.setW(all.stream().map(OracleData::get输入贷方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                newBalanceExcelResult.setBalance(newBalanceExcelResult.getV().subtract(newBalanceExcelResult.getW()));
                result.add(newBalanceExcelResult);
            }
            String fileName = "组合结果-"+company + ".xlsx";
            EasyExcel.write(fileName, NewBalanceExcelResult.class).sheet("旧系统").doWrite(result);


            List<NewBalanceExcelResult> list4 = new ArrayList<>();
            EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/中南22期初.xlsx", OldBalance.class, new PageReadListener<OldBalance>(dataList -> {
                for (OldBalance data : dataList) {
                    NewBalanceExcelResult oracleData = new NewBalanceExcelResult();
                    oracleData.setForm("期初");
                    oracleData.setCompanyName(data.getCompanyName());
                    oracleData.setProjectCode(data.getOnlySign());
//                    oracleData.setProjectName(data.getOnlySign());
                    oracleData.setAuxiliaryAccounting(data.getAuxiliaryAccounting());
                    oracleData.setBalance(data.getBalance());
                    list4.add(oracleData);
                }
            })).sheet("期初").doRead();

            List<NewBalanceExcelResult> finalRes = new ArrayList<>();
            Map<String, List<NewBalanceExcelResult>> collect = Stream.of(result, list4).flatMap(Collection::stream).collect(Collectors.groupingBy(NewBalanceExcelResult::getCompanyName));
            for (String companyName : collect.keySet()) {
                if (!companyName.equals("江苏中南物业服务有限公司温州分公司")){
                    continue;
                }
                List<NewBalanceExcelResult> results = collect.get(companyName);
                Map<String, List<NewBalanceExcelResult>> cGroup = results.stream().collect(Collectors.groupingBy(item -> item.getProjectCode() + item.getAuxiliaryAccounting()));
                for (String s : cGroup.keySet()) {
                    List<NewBalanceExcelResult> results1 = cGroup.get(s);
                    NewBalanceExcelResult re = new NewBalanceExcelResult();
                    re.setForm(results1.stream().map(NewBalanceExcelResult::getForm).distinct().collect(Collectors.joining("、")));
                    re.setCompanyName(results1.stream().map(NewBalanceExcelResult::getCompanyName).distinct().collect(Collectors.joining("、")));
                    re.setProjectCode(results1.stream().map(NewBalanceExcelResult::getProjectCode).distinct().collect(Collectors.joining("、")));
                    re.setProjectName(results1.stream().map(NewBalanceExcelResult::getProjectName).distinct().collect(Collectors.joining("、")));
                    re.setAuxiliaryAccounting(results1.stream().map(NewBalanceExcelResult::getAuxiliaryAccounting).distinct().collect(Collectors.joining("、")));
                    re.setV(results1.stream().map(NewBalanceExcelResult::getV).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setW(results1.stream().map(NewBalanceExcelResult::getW).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setBalance(results1.stream().map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setPreBalance(results1.stream().filter(item -> item.getForm().equals("期初")).map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    finalRes.add(re);
                }
            }
            EasyExcel.write("最终组合结果-"+company + ".xlsx", NewBalanceExcelResult.class).sheet("组合").doWrite(finalRes);
        }
    }
    @Data
    public static class OldBalance{
        @ExcelProperty("主体")
        private String companyName;
        @ExcelProperty("科目编码")
        private String onlySign;
//        @ExcelProperty("科目编码")
//        private String onlySignName;
        @ExcelProperty("辅助核算段")
        private String auxiliaryAccounting;
        @ExcelProperty("旧系统22期初余额")
        private BigDecimal balance;
    }
    private static String getStr(String str){
        return str == null ?"":str;
    }
}
