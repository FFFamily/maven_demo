package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
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
    @Test
    void test1() {
        List<Step6OldDetailExcel> excels = readPropertyExcel();
        Map<String, List<Step6OldDetailExcel>> collect = excels.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getCompanyName));
        for (String companyName : collect.keySet()) {
            String nowCompanyName = companyName.split("-")[0];
            if (!nowCompanyName.equals("江苏中南物业服务有限公司温州分公司")){
                continue;
            }
            System.out.println(nowCompanyName);
            List<NewBalanceExcelResult> results = new ArrayList<>();
            List<Step6OldDetailExcel> allCompanyList = collect.get(companyName);
//            String findPiSQL = "SELECT  * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段代码\" = '"+allCompanyList.get(0).getCompanyCode()+"' ";
//                        "companyAND z.\"期间\" >= '2023-07' AND z.\"期间\" <= '2023-12' ";
//            List<Step6OldDetailExcel> sqlData = jdbcTemplate.query(findPiSQL, (row, c) -> {
//                Step6OldDetailExcel data = new Step6OldDetailExcel();
//                data.setOnlySign(row.getString("账户组合"));
//                data.setV(row.getBigDecimal("输入借方"));
//                data.setW(row.getBigDecimal("输入贷方"));
//                return data;
//            });
//            allCompanyList.addAll(sqlData);
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
            String fileName = "组合余额表-"+companyName + ".xlsx";
            EasyExcel.write(fileName, NewBalanceExcelResult.class).sheet("旧系统").doWrite(results);
            String fileName2 = "组合余额表-总账-"+companyName + ".xlsx";
            EasyExcel.write(fileName2, Step6OldDetailExcel.class).sheet("总账").doWrite(allCompanyList);
        }
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
                                    Date time = data.getTime();
                                    DateTime date = DateUtil.date(time);
                                    if (date.isBefore(DateUtil.parse("2022-01-01")) || date.isAfter(DateUtil.parse("2022-12-31"))) {
                                        continue;
                                    }
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
                                    // 3-科目代码
                                    String projectCode = data.getProjectCode();
                                    ZNProjectMapping znProjectMapping = findNccZhongNanLevel.znProjectMapping.get(projectCode);
                                    String fmsProjectCode =  znProjectMapping.getFmsProjectCode();
                                    builder.append(appendStr(fmsProjectCode) ).append(".");
                                    // 4-子目
                                    String fmsChildProjectCode = znProjectMapping.getFmsChildProjectCode();
                                    builder.append(appendStr(fmsChildProjectCode) ).append(".");
                                    // 5-产品代码
                                    String eventName = data.getEventName();
                                    ZNEventMapping znEventMapping = findNccZhongNanLevel.znEventMapping.get(companyName + eventName);
                                    String fmsProductCode = znEventMapping == null ? "0": znEventMapping.getFmsProductCode();
                                    builder.append(appendStr(fmsProductCode) ).append(".");
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
                                        auxiliaryAccounting += data.getCustomerName() == null ? "" : data.getPersonalName() == null ? "-" : data.getPersonalName();
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

    public String appendStr(String str){
        return  str == null ? "0" : str;
    }
}
