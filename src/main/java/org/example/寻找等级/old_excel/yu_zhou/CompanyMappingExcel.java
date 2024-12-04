package org.example.寻找等级.old_excel.yu_zhou;

import lombok.Data;

/**
 * 账簿名称映射表
 */
@Data
public class CompanyMappingExcel {
    // 新系统账簿机构名称
    private String a;
    // 老机构账簿名称
    private String b;
    // -
    private String c;
    // 老机构账簿核算名称（科目余额表中
    private String d;
}
