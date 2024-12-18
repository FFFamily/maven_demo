package org.example.新老系统;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyConstant;
import org.example.utils.CoverNewDate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
public class FindAllBalance {
    @Resource
    private Step6 step6Test;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private CoverNewDate coverNewDate;
    public void find(Boolean isFindAll,String path,String selectCompany){
        Map<String, List<NewBalanceExcelResult>> listMap = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/最终组合结果-2022-余额表.xlsx", NewBalanceExcelResult.class, new PageReadListener<NewBalanceExcelResult>(dataList -> {
            for (NewBalanceExcelResult data : dataList) {
                List<NewBalanceExcelResult> orDefault = listMap.getOrDefault(data.getCompanyName(), new ArrayList<>());
                data.setForm("2022期末");
                data.setV(null);
                data.setW(null);
                orDefault.add(data);
                listMap.put(data.getCompanyName(), orDefault);
            }
        })).sheet("余额表").doRead();
        File file = new File("src/main/java/org/example/excel/zhong_nan/detail");
        List<NewBalanceExcelResult> finalExcel = new ArrayList<>();
        for (String fileName : Objects.requireNonNull(file.list())) {
            String name = fileName.replace(".xlsx", "");
            if (!isFindAll && !name.equals(path)){
                continue;
            }
            System.out.println("明细账-当前文件："+name);
            // 旧系统
            List<Step6OldDetailExcel> excels = step6Test.readPropertyExcel(fileName);
            Map<String, List<Step6OldDetailExcel>> companyMap = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
            for (String oldCompanyName : companyMap.keySet()) {
                String str = oldCompanyName.split("-")[0];
                String newCompanyName = CompanyConstant.getNewCompanyByOldCompany(str);
                if (!isFindAll && !newCompanyName.equals(selectCompany)){
                    continue;
                }
                System.out.println("明细账-当前公司："+newCompanyName);
                Step6.Step6TestResult step6TestResult = step6Test.step6Test(oldCompanyName, companyMap);
                if (step6TestResult == null){
                    continue;
                }
                // 旧系统处理后数据
                List<Step6OldDetailExcel> result3s = step6TestResult.getResult3s()
                        .stream()
                        .filter(item -> item.getRemark() != null)
                        .collect(Collectors.toList());
                // 旧系统
                List<Step6OldDetailExcel> oldDataList = companyMap.get(oldCompanyName);
                for (Step6OldDetailExcel item : result3s) {
                    oldDataList.remove(item);
                }

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

                EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/company/组合余额表-2022-总账-"+newCompanyName+".xlsx",
                        Step6OldDetailExcel.class,
                        new PageReadListener<Step6OldDetailExcel>(dataList -> {
                            for (Step6OldDetailExcel data : dataList) {
//                                coverNewDate.cover("2023-1-6",data);
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
                        })
                ).sheet("总账").doRead();

                EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/company_2023_6_12/"+newCompanyName+"-2023-1-6-组合序时账.xlsx",
                        OracleData.class,
                        new PageReadListener<OracleData>(dataList -> {
                            //coverNewDate.cover("2023-1-6",data);
                            list3.addAll(dataList);
                        })
                ).sheet("组合结果").doRead();

                String findSql = "select * from ZDPROD_EXPDP_20241120 z where z.\"公司段描述\" = '" + newCompanyName + "' and z.\"期间\" >= '2024-01' and z.\"期间\" <= '2024-09'";
                List<OracleData> newDataList = jdbcTemplate.query(findSql, new BeanPropertyRowMapper<>(OracleData.class));
                for (OracleData data : newDataList) {
                    String form = data.get科目段描述();
                    boolean isProject = form.startsWith("应付账款")
                            || form.startsWith("预付账款")
                            || form.startsWith("合同负债")
                            || form.startsWith("预收账款")
                            || form.startsWith("应收账款")
                            || form.startsWith("其他应付款")
                            || form.startsWith("其他应收款");
                    if (isProject){
                        data.setForm("24年1-9月序时账");
                        list3.add(data);
                    }
                }
                List<NewBalanceExcelResult> result = new ArrayList<>();
                List<OracleData> list1 = new ArrayList<>();
                List<OracleData> list2 = new ArrayList<>();
                List<OracleData> xsList = Stream.of(list1, list2, list3).flatMap(Collection::stream).collect(Collectors.toList());
                Map<String, List<OracleData>> group = xsList.stream().collect(Collectors.groupingBy(item -> item.get账户组合() + getStr(item.get交易对象())));
                for (String key : group.keySet()) {
                    List<OracleData> all = group.get(key);
                    OracleData one = all.get(0);
                    NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
                    newBalanceExcelResult.setForm("2023年7-12月");
                    newBalanceExcelResult.setCompanyName(newCompanyName);
                    newBalanceExcelResult.setProjectCode(one.get账户组合());
                    newBalanceExcelResult.setProjectName(one.get账户描述()+".");
                    newBalanceExcelResult.setProject(one.get科目段描述());
                    newBalanceExcelResult.setAuxiliaryAccounting(one.get交易对象名称());
                    newBalanceExcelResult.setV(all.stream().map(OracleData::get输入借方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    newBalanceExcelResult.setW(all.stream().map(OracleData::get输入贷方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
//                    newBalanceExcelResult.setBalance(newBalanceExcelResult.getV().subtract(newBalanceExcelResult.getW()));
                    result.add(newBalanceExcelResult);
                }
                File excelFile = new File(oldCompanyName + "-总序时账" + ".xlsx");
                if (excelFile.exists()){
                    System.out.println("文件存在");
                    List<OracleData> list = new ArrayList<>();
                    EasyExcel.read(excelFile, Step6OldDetailExcel.class,
                            new PageReadListener<OracleData>(list::addAll));
                    list.addAll(xsList);
                    EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(list);
                }else {
                    EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(xsList);
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
                    re.setPreBalance(results1.stream().filter(item -> item.getForm().equals("2023期末")).map(NewBalanceExcelResult::getPreBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setBalance(re.getPreBalance().add(re.getV()).subtract(re.getW()));
                    finalExcel.add(re);
                }
            }
        }
        EasyExcel.write( "最终组合结果-总余额表.xlsx", NewBalanceExcelResult.class).sheet("余额表").doWrite(finalExcel);
    }

    private static String getStr(String str){
        return str == null ?"":str;
    }
}
