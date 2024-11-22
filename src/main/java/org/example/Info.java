package org.example;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Info {
    // 区域
    private String regin;
    // 科目编码
    private String accountCode;
    // 科目1
    private String accountOne;
    // 机构
    private String org;
    // 成本中心
    private String costCenter;
    // 科目2
    private String accountTwo;
    // 子目
    private String childAccount;
    // 产品
    private String projectOne;
    // 地区
    private String area;
    private String SBU;
    private String ICP;
    private String projectTwo;
    // 客商
    private String merchant;
    // 总账日期
    private String GeneralLedgerDate;
    private String A;
    private String B;
    private String C;
    private String D;
    private String E;
    private String F;
    private String G;
    private String H;
    private String I;
    private String J;
    private String K;

    @Override
    public String toString() {
        return "Info{" +
                "regin='" + regin + '\'' +
                ", accountCode='" + accountCode + '\'' +
                ", accountOne='" + accountOne + '\'' +
                ", org='" + org + '\'' +
                ", costCenter='" + costCenter + '\'' +
                ", accountTwo='" + accountTwo + '\'' +
                ", childAccount='" + childAccount + '\'' +
                ", projectOne='" + projectOne + '\'' +
                ", area='" + area + '\'' +
                ", SBU='" + SBU + '\'' +
                ", ICP='" + ICP + '\'' +
                ", projectTwo='" + projectTwo + '\'' +
                ", merchant='" + merchant + '\'' +
                ", GeneralLedgerDate='" + GeneralLedgerDate + '\'' +
                ", A='" + A + '\'' +
                ", B='" + B + '\'' +
                ", C='" + C + '\'' +
                ", D='" + D + '\'' +
                ", E='" + E + '\'' +
                ", F='" + F + '\'' +
                ", G='" + G + '\'' +
                ", H='" + H + '\'' +
                ", I='" + I + '\'' +
                ", J='" + J + '\'' +
                ", K='" + K + '\'' +
                '}';
    }
}
