package org.example.寻找等级.old_excel.yu_zhou;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 禹洲旧系统余额表
 */
@Data
public class OldYZExcelTemplate {
    // 科目编码
    private String a;
    // 科目名称
    private String b;
    private String c;
    // 核算账簿名称
    private Integer d;
    private Integer e;
    private String f;
    private String g;

    private String h;
    private String i;
    private String j;
    private String k;
    // 期末余额
    private BigDecimal l;
}
