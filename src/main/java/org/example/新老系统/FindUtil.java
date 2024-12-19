package org.example.新老系统;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.ZNProjectMapping;
import org.example.utils.CommonUtil;
import org.example.utils.CoverNewDate;
import org.example.寻找等级.FindNccZhongNanLevel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Component
public class FindUtil {
    @Resource
    private CoverNewDate coverNewDate;
    @Resource
    private FindNccZhongNanLevel findNccZhongNanLevel;
    /**
     * 计算明细
     * @param company 公司
     * @param oracleData 明细
     * @return
     */
    public NewBalanceExcelResult caculate(String company,List<OracleData> oracleData){
        OracleData one = oracleData.get(0);
        NewBalanceExcelResult newBalanceExcelResult = new NewBalanceExcelResult();
        newBalanceExcelResult.setForm("2022");
        newBalanceExcelResult.setCompanyName(company);
        newBalanceExcelResult.setProjectCode(one.get账户组合());
        newBalanceExcelResult.setProjectName(one.get账户描述()+".");
        newBalanceExcelResult.setProject(one.get科目段描述());
        newBalanceExcelResult.setAuxiliaryAccounting(one.get交易对象名称());
        newBalanceExcelResult.setV(oracleData.stream().map(OracleData::get输入借方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
        newBalanceExcelResult.setW(oracleData.stream().map(OracleData::get输入贷方).reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr)), (l, r) -> l));
        newBalanceExcelResult.setBalance(newBalanceExcelResult.getV().subtract(newBalanceExcelResult.getW()));
        return newBalanceExcelResult;
    }

    /**
     * 读取物业excel
     * @return
     */
    public List<Step6OldDetailExcel> readPropertyExcel(String fileName){
        List<Step6OldDetailExcel> excels = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/detail/"+fileName, Step6OldDetailExcel.class,
                        new PageReadListener<Step6OldDetailExcel>(dataList -> {
                            for (Step6OldDetailExcel data : dataList) {
                                try {
                                    if (data.getV() == null && data.getW() == null){
                                        throw new RuntimeException("无法计算金额");
                                    }
                                    String time = data.getTime();
                                    DateTime date = DateUtil.parseDate(time);
                                    if (date.isBefore(DateUtil.parse("2023-07-01")) || date.isAfter(DateUtil.parse("2023-12-31"))) {
                                        // 只需要 07-12 月的
                                        continue;
                                    }
                                    // 科目
                                    String projectName = data.getProjectName();
                                    if (!(isBackProject(projectName) || projectName.startsWith("其他货币资金"))){
                                        // 只需要7大往来
                                        continue;
                                    }
                                    // 其他货币基金只取 9-12月
                                    if (projectName.startsWith("其他货币资金") && (date.isBefore(DateUtil.parse("2023-09-01")) || date.isAfter(DateUtil.parse("2023-12-31")))){
                                        continue;
                                    }
                                    // 摘要
                                    String match = data.getMatch();
                                    if (match.contains("资金归集")){
                                        continue;
                                    }

//                                    String oldProject = getOldProject(data);
                                    data.setOldProject(getOldProject(data));
                                    String oldProject = coverNewDate.getProjectName(data).split("-")[0];
                                    data.setActualProject(oldProject);
                                    if (oldProject.startsWith("其他应收款") || oldProject.startsWith("其他货币资金")){
                                        data.setMatchProject("其他应收款");
                                    }else if (oldProject.startsWith("合同负债") || oldProject.startsWith("预收账款")){
                                        data.setMatchProject("合同负债/预收账款");
                                    } else {
                                        data.setMatchProject(oldProject);
                                    }
                                    ZNProjectMapping znProjectMapping = findNccZhongNanLevel.znProjectMapping.get(data.getProjectCode());
                                    data.setProjectName(znProjectMapping.getFmsProjectName());
                                    excels.add(data);
                                }catch (Exception e){
                                    System.out.println("解析中南老系统明细数据出错: "+e.getMessage());
                                    System.out.println(data);
                                }

                            }
                        }))
                .sheet("综合查询表").doRead();
        return excels;
    }


    public Boolean isBackProject(String projectName){
        return projectName.startsWith("应付账款")
                || projectName.startsWith("预付账款")
                || projectName.startsWith("合同负债")
                || projectName.startsWith("预收账款")
                || projectName.startsWith("应收账款")
                || projectName.startsWith("其他应付款")
                || projectName.startsWith("其他应收款");
    }

    private String getOldProject(Step6OldDetailExcel excel){
        return excel.getProjectName().split("－")[0];
    }


}
