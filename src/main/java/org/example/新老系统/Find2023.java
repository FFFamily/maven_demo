package org.example.新老系统;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.Step6Result1;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyConstant;
import org.example.utils.CoverNewDate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
public class Find2023 {
    @Resource
    private Step6 step6Test;
    @Resource
    private FindUtil findUtil;
    @Resource
    private CoverNewDate coverNewDate;

    public List<OracleData> find(Map<String, List<Step6OldDetailExcel>> companyMap, String newCompanyName){
        Map<String, List<NewBalanceExcelResult>> listMap = initBalance(newCompanyName);
//        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        List<NewBalanceExcelResult> finalExcel = new ArrayList<>();
        List<OracleData> xsList = null;
//        for (String fileName : Objects.requireNonNull(file.list())) {
//            String name = fileName.replace(".xlsx", "");
//            if (!name.equals(path)){
//                continue;
//            }
//            System.out.println("2023-当前文件："+name);
            // 旧系统
//            List<Step6OldDetailExcel> excels = findUtil.readPropertyExcel(fileName);
//            Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(item -> {
//                String companyName = item.getCompanyName().split("-")[0];
//                return CompanyConstant.getNewCompanyByOldCompany(companyName);
//            }));
//            for (String newCompanyName : companyMap.keySet()) {
//                if (!newCompanyName.equals(selectCompanyName)){
//                    continue;
//                }
                System.out.println("2023-当前公司为： "+newCompanyName);
                Step6.Step6TestResult step6TestResult = step6Test.step6Test(newCompanyName, companyMap);
                if (step6TestResult == null){
                    return new ArrayList<>();
                }
                try (ExcelWriter excelWriter = EasyExcel.write(newCompanyName+"-第六步数据.xlsx").build()) {
                    // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
                    WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "模板").head(Step6Result1.class).build();
                    excelWriter.write(step6TestResult.getResult1s(), writeSheet1);
                    WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "新系统").head(OracleData.class).build();
                    excelWriter.write(step6TestResult.getResult2s(), writeSheet2);
                    WriteSheet writeSheet3 = EasyExcel.writerSheet(2, "旧系统").head(Step6OldDetailExcel.class).build();
                    excelWriter.write(step6TestResult.getResult3s(), writeSheet3);
                }

                // 旧系统处理后数据
                List<Step6OldDetailExcel> oldDataList = step6TestResult.getResult3s()
                        .stream()
                        .filter(item ->  "匹配成功".equals(item.getRemark()))
                        .collect(Collectors.toList());
                // 旧系统
                List<OracleData> list3 = new ArrayList<>();
                for (Step6OldDetailExcel data : oldDataList) {
                    coverNewDate.cover("2023-7-12",data);
                    OracleData oracleData = new OracleData();
                    oracleData.setForm("23年7-12月序时账");
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
                for (OracleData oracleData : step6TestResult.getOracleDataList()) {
//                    oracleData.setForm("23年7-12月新系统序时账");
                    list3.add(oracleData);
                }
                EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/company_2023_6_12/"+newCompanyName+"-2023-1-6-组合序时账.xlsx",
                        OracleData.class,
                        new PageReadListener<OracleData>(dataList -> {
                            //coverNewDate.cover("2023-1-6",data);
                            list3.addAll(dataList);
                        })
                ).sheet("组合结果").doRead();
                List<NewBalanceExcelResult> result = new ArrayList<>();
                List<OracleData> list1 = new ArrayList<>();
                List<OracleData> list2 = new ArrayList<>();
                xsList = Stream.of(list1, list2, list3).flatMap(Collection::stream).collect(Collectors.toList());
                Map<String, List<OracleData>> group = xsList.stream().collect(Collectors.groupingBy(item -> item.get账户组合() + getStr(item.get交易对象())));
                for (String key : group.keySet()) {
                    List<OracleData> all = group.get(key);
                    OracleData one = all.get(0);
                    NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
                    newBalanceExcelResult.setForm("2023年");
                    newBalanceExcelResult.setCompanyName(newCompanyName);
                    newBalanceExcelResult.setProjectCode(one.get账户组合());
                    newBalanceExcelResult.setProjectName(one.get账户描述()+".");
                    newBalanceExcelResult.setProject(one.get科目段描述());
                    newBalanceExcelResult.setAuxiliaryAccounting(one.get交易对象名称());
                    newBalanceExcelResult.setV(all.stream().map(OracleData::get输入借方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    newBalanceExcelResult.setW(all.stream().map(OracleData::get输入贷方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    result.add(newBalanceExcelResult);
                }
                List<NewBalanceExcelResult> results = Stream.of(result, listMap.getOrDefault(newCompanyName,new ArrayList<>())).flatMap(Collection::stream).collect(Collectors.toList());
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
//                    re.setBalance(results1.stream().map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setPreBalance(results1.stream().filter(item -> item.getForm().equals("2022年")).map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setBalance(re.getPreBalance().add(re.getV()).subtract(re.getW()));
                    finalExcel.add(re);
                }
//            }
//        }

        EasyExcel.write( "src/main/java/org/example/excel/zhong_nan/merge/"+ newCompanyName +"最终组合结果-2023-余额表.xlsx", NewBalanceExcelResult.class).sheet("余额表").doWrite(finalExcel);
        return xsList;
    }

    public Map<String, List<NewBalanceExcelResult>>  initBalance(String str){
        Map<String, List<NewBalanceExcelResult>> listMap = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/"+str+"-最终组合结果-2022-余额表.xlsx", NewBalanceExcelResult.class, new PageReadListener<NewBalanceExcelResult>(dataList -> {
            for (NewBalanceExcelResult data : dataList) {
                List<NewBalanceExcelResult> orDefault = listMap.getOrDefault(data.getCompanyName(), new ArrayList<>());
                data.setForm("2022年");
                data.setV(null);
                data.setW(null);
                orDefault.add(data);
                listMap.put(data.getCompanyName(), orDefault);
            }
        })).sheet("余额表").doRead();
        return listMap;
    }

    private static String getStr(String str){
        return str == null ?"":str;
    }
}
