package org.example.分类;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.core.entity.SourceFileData;
import org.example.func_three.Assistant3;
import org.example.func_three.Main3;
import org.example.func_three.OtherInfo3;
import org.example.utils.ExcelDataUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分裂
 */
@Service
public class FindABCD {
    @Resource
    private JdbcTemplate jdbcTemplate;

    public String getValue(String str){
        return str == null ? "" : str;
    }



    public void doFindABDC(String sourceFile){
        List<AssistantResult> excelExcelData = new ArrayList<>();
        List<SourceFileData> sourceFileDataList = ExcelDataUtil.getExcelData(sourceFile,"Sheet1");
        List<AssistantResult> dataList = sourceFileDataList
                .stream()
                .collect(Collectors.groupingBy(i -> i.getMatch() + "."+ i.getTransactionObjectCode()))
                .values()
                .stream()
                .reduce(new ArrayList<>(),(prev, curr) ->{
                    AssistantResult assistantResult = new AssistantResult();
                    SourceFileData sourceFileData = curr.get(0);
                    assistantResult.setFieldCode(sourceFileData.getMatch());
                    assistantResult.setSubjectName(sourceFileData.getSEGMENT3_NAME());
//                    assistantResult.setForm(sourceFileData.getSEGMENT3_NAME());
                    assistantResult.setTransactionObjectCode(sourceFileData.getTransactionObjectCode());
                    assistantResult.setTransactionObjectName(sourceFileData.getTransactionObjectName());
                    assistantResult.setField(sourceFileData.getMatchName());
                    BigDecimal money = curr.stream().reduce(
                            BigDecimal.ZERO,
                            (iprev, icurr) -> icurr.getYEAR_BEGIN_DR().subtract(icurr.getYEAR_BEGIN_CR()).add(icurr.getYTD_DR()).subtract(icurr.getYTD_CR()),
                            (l, r) -> l);
                    assistantResult.setMoney(money);
                    prev.add(assistantResult);
                    return prev;
                },(l,r) -> l);
        List<Assistant3> cachedDataList = new ArrayList<>();
        for (AssistantResult assistantResult : dataList) {
            Assistant3 assistant3 = new Assistant3();
            // 左前缀匹配
            String subjectName = assistantResult.getSubjectName();
            if (subjectName.startsWith("应付账款") || subjectName.startsWith("其他应付款") || subjectName.startsWith("合同负债")){
                BigDecimal money = BigDecimal.ZERO.subtract(assistantResult.getMoney());
                assistantResult.setMoney(money);
            }
            assistant3.setZ(assistantResult.getMoney() == null
                    ? ""
                    : assistantResult.getMoney().compareTo(BigDecimal.ZERO) < 0
                    ? "("+ assistantResult.getMoney() +")"
                    : assistantResult.getMoney().toString());
            assistant3.setR(assistantResult.getFieldCode());
            cachedDataList.add(assistant3);
        }
        for (int i = 0; i < dataList.size(); i++) {
            Assistant3 assistant = cachedDataList.get(i);
            AssistantResult assistantResult = dataList.get(i);
            assistantResult.setIndex(String.valueOf(i+1));
            String z = assistant.getZ();
            if (z == null) {
                continue;
            }
            String projectName = assistant.getR();
            String sql =  "select * from ZDPROD_EXPDP_20241120 z where z.\"账户组合\" = '" + assistantResult.getFieldCode()+"'";
            if (assistantResult.getTransactionObjectCode() != null) {
                sql +=  "and z.\"交易对象\" = '" + assistantResult.getTransactionObjectCode() +"'";
            }else {
                sql +=  "and z.\"交易对象\" is null";
            }
            List<OtherInfo3> startCollect = jdbcTemplate.query(sql, (rs, rowNum) -> {
                OtherInfo3 info = new OtherInfo3();
                info.setA(String.valueOf(rowNum));
                // 年 + 月 + 凭证
                DateTime date = DateUtil.date(rs.getDate("有效日期"));
                int year = date.year();
                int month = date.month();
                int code = rs.getInt("单据编号");
                info.setQ(code);
                info.setR(year+"-"+month+"-"+code);
                info.setV(rs.getBigDecimal("输入借方"));
                info.setW(rs.getBigDecimal("输入贷方"));
                // 有效日期
                info.setN(date);
                info.setS(rs.getString("来源"));
                // 有借就是 借方向
                info.setX(info.getV() != null ? "借" : "贷");
                info.setZ(rs.getString("账户组合"));
                return info;
            });
            List<OtherInfo3> result = Main3.doMain(
                    false,
                    null,
                    startCollect,
                    assistant.getZ(),
                    projectName
            );
            if (result.isEmpty()) {
                // 证明全部匹配
                 findABCD(startCollect, assistantResult,assistant);
            } else {
                 findABCD(result, assistantResult,assistant);
            }
            excelExcelData.add(assistantResult);
            System.out.println("目前进度："+ i/dataList.size() * 100);
        }
        String resultFileName = "ABCD分类-"+System.currentTimeMillis() + ".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(AssistantResult.class).build();
            excelWriter.write(excelExcelData, writeSheet1);
        }
        System.out.println("结束");
    }


    public static BigDecimal getZValue(String z) {
        BigDecimal balance;
        try {
            balance = new BigDecimal(z.replace(",", "").replace("(", "").replace(")", ""));
        } catch (Exception e) {
            balance = BigDecimal.ZERO;
        }
        if (z.contains("(") || z.contains(")")) {
            // 负值
            return BigDecimal.ZERO.subtract(balance);
        }
        return balance;
    }

    public static AssistantResult findABCD(List<OtherInfo3> result,AssistantResult assistantResult, Assistant3 assistant) {
        // 通过总账日期进行分类
//        AssistantResult assistantResult = new AssistantResult();
//        assistantResult.setField(assistant.getR());
//        assistantResult.setIndex(assistant.getA());
//        assistantResult.setMoney(getZValue(assistant.getZ()));
        String z = assistant.getZ();
        // 期初
        List<OtherInfo3> up = new ArrayList<>();
        // 本期
        List<OtherInfo3> low = new ArrayList<>();
        result.forEach(item -> {
            Date time = item.getN();
            Date date = DateUtil.parse("2022-04-30", "yyyy-MM-dd");
            if (DateUtil.date(time).toInstant().compareTo(date.toInstant()) <= 0) {
                // 时间 在 2022年4月30日之前
                up.add(item);
            } else {
                low.add(item);
            }
        });
        // 如果全部都在期初，那么就是归属D类
        if (!up.isEmpty() && low.isEmpty()) {
            assistantResult.setIsIncludeUp(1);
            assistantResult.setType("D");
        } else if (!up.isEmpty()) {
            assistantResult.setIsIncludeUp(1);
            // 最终余额
            BigDecimal totalSum = getZValue(z);
            // 期初余额
            BigDecimal upSum = up.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(curr.getV() != null ? curr.getV() : BigDecimal.ZERO).subtract(curr.getW() != null ? curr.getW() : BigDecimal.ZERO), (l, r) -> l);
            if (upSum.compareTo(BigDecimal.ZERO) > 0 && totalSum.compareTo(upSum) <= 0) {
                // 如果期初余额为正 && 最终余额小于 期初，证明本期发生了扣款
                assistantResult.setType("D");
            } else if (upSum.compareTo(BigDecimal.ZERO) < 0 && totalSum.compareTo(upSum) >= 0) {
                // 如果期初余额为负 && 最终余额大于 期初，证明本期发生了加款
                assistantResult.setType("D");
            } else if (upSum.compareTo(BigDecimal.ZERO) == 0 && totalSum.compareTo(upSum) == 0){
                assistantResult.setType("无法判断");
            } else {
                // 期初为0也会到达
                findABC(low, assistantResult);
            }
        } else {
            // 都是本期的
            findABC(low, assistantResult);
        }
        return assistantResult;
    }


    /**
     * 判断是否属于ABC类
     */
    public static void findABC(List<OtherInfo3> list, AssistantResult assistantResult) {
        Map<String, List<OtherInfo3>> collect = list.stream().collect(Collectors.groupingBy(OtherInfo3::getS));
        int systemSize = 0;
        int personalSize = 0;
        // 遍历来源字段
        for (String form : collect.keySet()) {
//            if (form.equals("物业收费系统") || form.equals("EMS") || form.equals("TMS资金接口") || form.equals("PS人力资源系统") || form.equals("物业ERP")) {
//                systemSize += 1;
//            } else if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
//                personalSize += 1;
//            } else {
//                assistantResult.setType("E");
////                throw new RuntimeException("额外的来源类型："+ form);
//            }
            if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
                personalSize += 1;
            }else {
                systemSize += 1;
            }
        }
        if (systemSize != 0 && personalSize != 0) {
            // 人工 + 系统
            assistantResult.setType("C");
        } else if (systemSize != 0) {
            assistantResult.setType("A");
        } else if (personalSize != 0) {
            assistantResult.setType("B");
        }else {
            assistantResult.setType("所有数据借贷抵消");
        }
    }


}
