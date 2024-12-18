package org.example;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.utils.CommonUtil;
import org.example.utils.CoverNewDate;
import org.example.新老系统.Step6;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
public class ZMMerge2024 {
    @Resource
    private Step6 step6Test;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private CoverNewDate coverNewDate;
    @Test
    void test(){
        Map<String, List<NewBalanceExcelResult>> listMap = new HashMap<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/merge/最终组合结果-2023-余额表.xlsx", NewBalanceExcelResult.class, new PageReadListener<NewBalanceExcelResult>(dataList -> {
            for (NewBalanceExcelResult data : dataList) {
                List<NewBalanceExcelResult> orDefault = listMap.getOrDefault(data.getCompanyName(), new ArrayList<>());
                data.setForm("2023期末");
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
            System.out.println("当前文件："+name);
            if (!name.equals("物业北京公司")){
                continue;
            }
            List<String> companyList = jdbcTemplate.queryForList(
                    "select z.\"公司段描述\" from ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\" ",
                    String.class
            );
            for (String newCompanyName : companyList) {
                System.out.println("当前公司："+newCompanyName);
                if (!newCompanyName.equals("唐山中南国际旅游度假物业服务有限责任公司")){
                    continue;
                }
                List<OracleData> list3 = new ArrayList<>();
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
                    newBalanceExcelResult.setForm("2024年");
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
//            if (company.equals("青岛中南物业管理有限公司")){
//                EasyExcel.write(company + "-2023-1-6-组合序时账" + ".xlsx", OracleData.class).sheet("组合结果").doWrite(xsList);
//            }
//                File excelFile = new File(newCompanyName + "-2024-组合序时账" + ".xlsx");
//                if (excelFile.exists()){
//                    System.out.println("文件存在");
//                    List<OracleData> list = new ArrayList<>();
//                    EasyExcel.read(excelFile, Step6OldDetailExcel.class,
//                            new PageReadListener<OracleData>(list::addAll));
//                    list.addAll(xsList);
//                    EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(list);
//                }else {
//                    EasyExcel.write(excelFile.getName(), OracleData.class).sheet("组合结果").doWrite(xsList);
//                }
                List<NewBalanceExcelResult> results = Stream.of(result, listMap.getOrDefault(newCompanyName,new ArrayList<>())).flatMap(Collection::stream).collect(Collectors.toList());
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
//                    re.setBalance(results1.stream().map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setPreBalance(results1.stream().filter(item -> item.getForm().equals("2023期末")).map(NewBalanceExcelResult::getBalance).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
                    re.setBalance(re.getPreBalance().add(re.getV()).subtract(re.getW()));
                    finalExcel.add(re);
                }
            }
        }
        EasyExcel.write( "src/main/java/org/example/excel/zhong_nan/merge/最终组合结果-2024-余额表.xlsx", NewBalanceExcelResult.class).sheet("余额表").doWrite(finalExcel);
    }

    private static String getStr(String str){
        return str == null ?"":str;
    }
}