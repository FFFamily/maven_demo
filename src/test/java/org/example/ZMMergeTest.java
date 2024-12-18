package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Data;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Merge22Result;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.CommonUtil;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ZMMergeTest {
    public static void main(String[] args) {
        merge2022();
    }

    static void merge2022(){
        Map<String,List<OracleData>> map1 = new HashMap<>();
        // 22 调整
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/22年调整.xlsx", OracleData.class, new PageReadListener<OracleData>(dataList -> {
            for (OracleData data : dataList) {
                List<OracleData> list = map1.getOrDefault(data.get公司段描述(), new ArrayList<>());
                data.setForm("22年调整");
                list.add(data);
                map1.put(data.get公司段描述(),list);
            }
        })).sheet("模板").doRead();
        // 22年应收账款预估
        Map<String,List<OracleData>> map2 = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/22年应收账款预估收缴率调整.xlsx", OracleData.class, new PageReadListener<OracleData>(dataList -> {
            for (OracleData data : dataList) {
                data.setForm("22年预估收缴率");
                List<OracleData> list = map2.getOrDefault(data.get公司段描述(), new ArrayList<>());
                list.add(data);
                map2.put(data.get公司段描述(),list);
            }
        })).sheet("模板").doRead();
        Map<String, List<NewBalanceExcelResult>> listMap = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/中南22期初.xlsx", OldBalance.class, new PageReadListener<OldBalance>(dataList -> {
            for (OldBalance data : dataList) {
                List<NewBalanceExcelResult> orDefault = listMap.getOrDefault(data.getCompanyName(), new ArrayList<>());
                NewBalanceExcelResult oracleData = new NewBalanceExcelResult();
                oracleData.setForm("期初");
                oracleData.setCompanyName(data.getCompanyName());
                oracleData.setProjectCode(data.getOnlySign());
                oracleData.setProjectName(data.getOnlySignName());
                oracleData.setAuxiliaryAccounting(data.getAuxiliaryAccounting());
                oracleData.setBalance(data.getBalance());
                orDefault.add(oracleData);
                listMap.put(data.getCompanyName(), orDefault);
            }
        })).sheet("期初").doRead();
        List<NewBalanceExcelResult> finalExcel = new ArrayList<>();
        File file = new File("src/main/java/org/example/excel/zhong_nan/merge/company");
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

            EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/company/"+fileName, Step6OldDetailExcel.class, new PageReadListener<Step6OldDetailExcel>(dataList -> {
                for (Step6OldDetailExcel data : dataList) {
                    OracleData oracleData = new OracleData();
                    oracleData.setForm("22年序时账");
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
                newBalanceExcelResult.setForm("2022");
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
//            EasyExcel.write(company + "-组合序时账" + ".xlsx", OracleData.class).sheet("组合结果").doWrite(xsList);
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
                re.setPreBalance(results1.stream().filter(item -> item.getForm().equals("期初")).map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                finalExcel.add(re);
            }
        }
        EasyExcel.write( "src/main/java/org/example/excel/zhong_nan/merge/最终组合结果-2022-余额表.xlsx", NewBalanceExcelResult.class).sheet("余额表").doWrite(finalExcel);
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
