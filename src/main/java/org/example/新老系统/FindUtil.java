package org.example.新老系统;

import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.NewBalanceExcelResult;
import org.example.utils.CommonUtil;

import java.math.BigDecimal;
import java.util.List;

public class FindUtil {
    /**
     * 计算明细
     * @param company 公司
     * @param oracleData 明细
     * @return
     */
    public static NewBalanceExcelResult caculate(String company,List<OracleData> oracleData){
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


}
