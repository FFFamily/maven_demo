package org.example.enitty;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class OracleData {
    private String 帐套;
    private String 批名;
    private String 批说明;
    private String 日记账名;
    private String 公司段代码;
    private String 公司段描述;
    private String 科目代码;
    private String 科目段描述;
    private String 报账单号;
    private String 交易对象;
    private String 交易对象名称;
    private String 记账人ID;
    private String 过账人;
    private String 日记账说明;
    private String 期间;
    private String 行号;
    private String 行说明;
    private String 记账日期;
    private String 有效日期;
    private String 类别;
    private String 来源;
    private String 单据编号;
    private String 冲销状态;
    private String 冲销期间;
    private String 过账时间;
    private String 审批人ID;
    private String 审批状态;
    private String 日记账状态;
    private String 本位币币种;
    private String 原币币种;
    private BigDecimal 输入借方;
    private BigDecimal 输入贷方;
    private String 本位币借方;
    private String 本位币贷方;
    private String 关联方;
    private String 关联方描述;
    private String 项目;
    private String 项目段描述;
    private String 增减变动;
    private String 交易参考;
    private String 结算号;
    private String 账户组合;
    private String 账户描述;

    private String 额外字段;
    private String 科目;
    private String 借正贷负;
}
