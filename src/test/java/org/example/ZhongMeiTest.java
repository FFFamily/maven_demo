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
            if (!companyName.equals("江苏中南物业服务有限公司温州分公司")){
                continue;
            }
            List<NewBalanceExcelResult> results = new ArrayList<>();
            List<Step6OldDetailExcel> allCompanyList = collect.get(companyName);
            String findPiSQL = "SELECT  * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+companyName+"' ";
//                        "companyAND z.\"期间\" >= '2023-07' AND z.\"期间\" <= '2023-12' ";
            List<Step6OldDetailExcel> sqlData = jdbcTemplate.query(findPiSQL, (row, c) -> {
                Step6OldDetailExcel data = new Step6OldDetailExcel();
                data.setOnlySign(row.getString("账户组合"));
                data.setV(row.getBigDecimal("输入借方"));
                data.setW(row.getBigDecimal("输入贷方"));
                return data;
            });
            allCompanyList.addAll(sqlData);
            Map<String, List<Step6OldDetailExcel>> result =
                    allCompanyList.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getOnlySign));
            for (String onlySign : result.keySet()) {
                List<Step6OldDetailExcel> all = result.get(onlySign);
                NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
                newBalanceExcelResult.setOnlySign(onlySign);
                newBalanceExcelResult.setV(all.stream().map(Step6OldDetailExcel::getV).reduce(BigDecimal.ZERO, (prev,curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)),(l, r) ->l));
                newBalanceExcelResult.setW(all.stream().map(Step6OldDetailExcel::getW).reduce(BigDecimal.ZERO, (prev,curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)),(l, r) ->l));
                results.add(newBalanceExcelResult);
            }
            String fileName = "组合余额表-"+companyName + ".xlsx";
            EasyExcel.write(fileName, OracleData.class).sheet("旧系统").doWrite(results);
        }
    }

    /**
     * 读取物业excel
     * @return
     */
    public List<Step6OldDetailExcel> readPropertyExcel(){
        List<Step6OldDetailExcel> excels = new ArrayList<>();
        Map<String,String> companyMapping = new HashMap<>();
        companyMapping.put("江苏中南物业服务有限公司（总部）","江苏中南物业服务有限公司");
        companyMapping.put("江苏中南物业服务有限公司（商管）","江苏中南物业服务有限公司");
        companyMapping.put("江苏中南物业服务有限公司（住宅）","江苏中南物业服务有限公司");
        companyMapping.put("江苏中南物业服务有限公司平湖分公司","江苏中南物业服务有限公司");
        // 读取旧系统的余额信息 2022年
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/物业上海公司.xlsx", Step6OldDetailExcel.class,
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
                                    builder.append(fmsProjectCode).append(".");
                                    // 4-子目
                                    String fmsChildProjectCode = znProjectMapping.getFmsChildProjectCode();
                                    builder.append(fmsChildProjectCode).append(".");
                                    // 5-产品代码
                                    String eventName = data.getEventName();
                                    ZNEventMapping znEventMapping = findNccZhongNanLevel.znEventMapping.get(companyName + eventName);
                                    String fmsProductCode = znEventMapping.getFmsProductCode();
                                    builder.append(fmsProductCode).append(".");
                                    // 6-地区代码
                                    String fmsAreaCode = "0";
                                    builder.append(fmsAreaCode).append(".");
                                    // 7-SBU
                                    String fmsSBU = "0";
                                    builder.append(fmsSBU).append(".");
                                    // 8-ICP
                                    String customerName = data.getCustomerName();
                                    ZNIPCMapping znipcMapping = findNccZhongNanLevel.znipcMapping.get(customerName);
                                    String icp = znipcMapping == null ? "0" : znipcMapping.getFmsICPCode();
//                                    ZNCompanyMapping znCompanyMapping1 = findNccZhongNanLevel.znCustomerMapping.get(customerName);
//                                    String icp = znCompanyMapping1.getFMSCompanyCode() == null ? "0" : znCompanyMapping1.getFMSCompanyCode();
                                    builder.append(icp).append(".");
                                    // 9-项目代码
                                    String fmsEventCode = znEventMapping.getFmsEventCode();
                                    builder.append(fmsEventCode).append(".");
                                    // 10-备用
                                    String standby  = "0";
                                    builder.append(standby);
                                    String onlySign = builder.toString();
                                    data.setOnlySign(onlySign);
                                    excels.add(data);
                                }catch (Exception e){
                                    System.out.println("解析中南老系统明细数据出错: "+e.getMessage());
                                    System.out.println(data);
                                }

                            }
                        }))
                .sheet("综合查询表").headRowNumber(3).doRead();
        return excels;
    }
}
