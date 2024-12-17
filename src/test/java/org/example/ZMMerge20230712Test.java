package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.Data;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Merge22Result;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.Step6Result1;
import org.example.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
public class ZMMerge20230712Test {
    @Resource
    private Step6Test step6Test;
    @Test
    void test(){
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        for (String fileName : Objects.requireNonNull(file.list())) {
            String name = fileName.replace(".xlsx", "");
            System.out.println("当前文件："+name);
            if (!name.equals("物业上海公司1")){
                continue;
            }
            // 旧系统
            List<Step6OldDetailExcel> excels = step6Test.readPropertyExcel(fileName);
            Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
            for (String companyName : companyMap.keySet()) {
                Step6Test.Step6TestResult step6TestResult = step6Test.step6Test(companyName, companyMap);
                if (step6TestResult == null){
                    continue;
                }
                List<Step6Result1> result1s = step6TestResult.getResult1s();
                // 新系统处理后数据
                List<OracleData> result2s = step6TestResult.getResult2s().stream().filter(item -> item.getForm() != null).collect(Collectors.toList());
                // 新系统
                List<OracleData> newDataList = step6TestResult.getOracleDataList();
                for (OracleData item : result2s) {
                    newDataList.remove(item);
                }
                // 旧系统处理后数据
                List<Step6OldDetailExcel> result3s = step6TestResult.getResult3s().stream().filter(item -> item.getRemark() != null).collect(Collectors.toList());
                // 旧系统
                List<Step6OldDetailExcel> oldDataList = companyMap.get(companyName);
                for (Step6OldDetailExcel item : result3s) {
                    oldDataList.remove(item);
                }
                // 数据过滤
                try (ExcelWriter excelWriter = EasyExcel.write(name+"-"+companyName+"-第六步数据.xlsx").build()) {
                    // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
                    WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "模板").head(Step6Result1.class).build();
                    excelWriter.write(result1s, writeSheet1);
                    WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "新系统").head(OracleData.class).build();
                    excelWriter.write(newDataList, writeSheet2);
                    WriteSheet writeSheet3 = EasyExcel.writerSheet(2, "旧系统").head(Step6OldDetailExcel.class).build();
                    excelWriter.write(oldDataList, writeSheet3);
                }
            }
        }
    }
    public static void main(String[] args) {
        merge20230612();
    }

    static void merge20230612(){
        List<Merge22Result> res = new ArrayList<>();
        Map<String,List<OracleData>> map1 = new HashMap<>();
        // 22 调整
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/23年1-6月调整.xlsx", OracleData.class, new PageReadListener<OracleData>(dataList -> {
            for (OracleData data : dataList) {
                List<OracleData> list = map1.getOrDefault(data.get公司段描述(), new ArrayList<>());
                data.setForm("23年1-6月调整");
                list.add(data);
                map1.put(data.get公司段描述(),list);
            }
        })).sheet("Sheet1").doRead();
        // 22年应收账款预估
        Map<String,List<OracleData>> map2 = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/23年1-6月应收账款预估收缴率调整.xlsx", OracleData.class, new PageReadListener<OracleData>(dataList -> {
            for (OracleData data : dataList) {
                data.setForm("23年1-6月预估收缴率");
                List<OracleData> list = map2.getOrDefault(data.get公司段描述(), new ArrayList<>());
                list.add(data);
                map2.put(data.get公司段描述(),list);
            }
        })).sheet("模板").doRead();
        Map<String, List<NewBalanceExcelResult>> listMap = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/最终组合结果-2022-余额表.xlsx", NewBalanceExcelResult.class, new PageReadListener<NewBalanceExcelResult>(dataList -> {
            for (NewBalanceExcelResult data : dataList) {
                List<NewBalanceExcelResult> orDefault = listMap.getOrDefault(data.getCompanyName(), new ArrayList<>());
                data.setForm("2022期末");
                data.setV(null);
                data.setW(null);
//                data.setBalance(data.getPreBalance());
//                data.setPreBalance(null);
                orDefault.add(data);
                listMap.put(data.getCompanyName(), orDefault);
            }
        })).sheet("余额表").doRead();
        List<NewBalanceExcelResult> finalExcel = new ArrayList<>();
        File file = new File("src/main/java/org/example/excel/zhong_nan/merge/company_2023_6_12");
        if (!file.isDirectory()){
            throw new RuntimeException("不是目录");
        }
        for (String fileName : file.list()) {
            if (fileName.equals(".DS_Store")){
                continue;
            }
            List<OracleData> list3 = new ArrayList<>();
            String[] split = fileName.split("-");
            String company = split[split.length - 1].replace(".xlsx", "");
            System.out.println("当前公司："+company);
//            if (!company.equals("唐山中南国际旅游度假物业服务有限责任公司")){
//                continue;
//            }
            EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/company_2023_6_12/"+fileName, Step6OldDetailExcel.class, new PageReadListener<Step6OldDetailExcel>(dataList -> {
                for (Step6OldDetailExcel data : dataList) {
                    OracleData oracleData = new OracleData();
                    oracleData.setForm("23年1-6月序时账");
                    oracleData.set公司段描述(data.getCompanyName());
                    oracleData.set账户组合(data.getOnlySign());
                    oracleData.set账户描述(data.getOnlySignName());
                    oracleData.set交易对象(data.getAuxiliaryAccountingCode());
                    oracleData.set交易对象名称(data.getAuxiliaryAccounting());
                    oracleData.set输入借方(data.getV());
                    oracleData.set输入贷方(data.getW());
                    oracleData.set单据编号(data.getVoucherCode());
                    oracleData.set有效日期(data.getTime());
                    DateTime parse = DateUtil.parse(data.getTime());
                    oracleData.set期间(parse.year()+"-"+(parse.month()+1));
                    oracleData.set科目代码(data.getProjectCode());
                    oracleData.set科目段描述(data.getProjectName());
                    oracleData.set对方科目(data.getOtherProjectCode());
                    oracleData.set对方科目名称(data.getOtherProjectName());
                    oracleData.set行说明(data.getMatch());
                    oracleData.set项目(data.getEventCode());
                    oracleData.set项目段描述(data.getEventName());
                    oracleData.set部门代码(data.getOrgCode());
                    oracleData.set部门名称(data.getOrgName());
                    list3.add(oracleData);
                }
            })).sheet("总账").doRead();

            List<NewBalanceExcelResult> result = new ArrayList<>();
            List<OracleData> list1 = map1.getOrDefault(company, new ArrayList<>());
            List<OracleData> list2 = map2.getOrDefault(company, new ArrayList<>());
            List<OracleData> xsList = Stream.of(list1, list2, list3).flatMap(Collection::stream).collect(Collectors.toList());
            Map<String, List<OracleData>> group = xsList.stream().collect(Collectors.groupingBy(item -> item.get账户组合() + getStr(item.get交易对象())));
            for (String key : group.keySet()) {
                List<OracleData> all = group.get(key);
                OracleData one = all.get(0);
                NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
                newBalanceExcelResult.setForm("2023年1-6月");
                newBalanceExcelResult.setCompanyName(company);
                newBalanceExcelResult.setProjectCode(one.get账户组合());
                newBalanceExcelResult.setProjectName(one.get账户描述()+".");
                newBalanceExcelResult.setProject(one.get科目段描述());
                newBalanceExcelResult.setAuxiliaryAccounting(one.get交易对象名称());
                newBalanceExcelResult.setV(all.stream().map(OracleData::get输入借方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                newBalanceExcelResult.setW(all.stream().map(OracleData::get输入贷方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                newBalanceExcelResult.setBalance(newBalanceExcelResult.getV().subtract(newBalanceExcelResult.getW()));
                result.add(newBalanceExcelResult);
            }
//            if (company.equals("青岛中南物业管理有限公司")){
//                EasyExcel.write(company + "-2023-1-6-组合序时账" + ".xlsx", OracleData.class).sheet("组合结果").doWrite(xsList);
//            }
//            EasyExcel.write(company + "-2023-1-6-组合序时账" + ".xlsx", OracleData.class).sheet("组合结果").doWrite(xsList);
            List<NewBalanceExcelResult> results = Stream.of(result, listMap.getOrDefault(company,new ArrayList<>())).flatMap(Collection::stream).collect(Collectors.toList());
            Map<String, List<NewBalanceExcelResult>> cGroup = results.stream().collect(Collectors.groupingBy(item -> item.getProjectCode() + item.getAuxiliaryAccounting()));
            for (String s : cGroup.keySet()) {
                List<NewBalanceExcelResult> results1 = cGroup.get(s);
                NewBalanceExcelResult re = new NewBalanceExcelResult();
                re.setForm(results1.stream().map(NewBalanceExcelResult::getForm).distinct().collect(Collectors.joining("、")));
                re.setCompanyName(results1.stream().map(NewBalanceExcelResult::getCompanyName).distinct().collect(Collectors.joining("、")));
                re.setProjectCode(results1.stream().map(NewBalanceExcelResult::getProjectCode).distinct().collect(Collectors.joining("、")));
                // todo 后续有个. 要处理
                re.setProjectName(results1.stream().map(NewBalanceExcelResult::getProjectName).distinct().collect(Collectors.joining("、")));
                re.setAuxiliaryAccounting(results1.stream().map(NewBalanceExcelResult::getAuxiliaryAccounting).distinct().collect(Collectors.joining("、")));
                re.setV(results1.stream().map(NewBalanceExcelResult::getV).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                re.setW(results1.stream().map(NewBalanceExcelResult::getW).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                re.setBalance(results1.stream().map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                re.setPreBalance(results1.stream().filter(item -> item.getForm().equals("2022期末")).map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                finalExcel.add(re);
            }
        }
        EasyExcel.write( "最终组合结果-2023-1-6-余额表.xlsx", NewBalanceExcelResult.class).sheet("余额表").doWrite(finalExcel);
    }
    @Data
    public static class OldBalance{
        @ExcelProperty("主体")
        private String companyName;
        @ExcelProperty("科目编码")
        private String onlySign;
        @ExcelProperty("科目编码名称映射")
        private String onlySignName;
        @ExcelProperty("辅助核算段")
        private String auxiliaryAccounting;
        @ExcelProperty("旧系统22期初余额")
        private BigDecimal balance;
    }
    private static String getStr(String str){
        return str == null ?"":str;
    }
}
