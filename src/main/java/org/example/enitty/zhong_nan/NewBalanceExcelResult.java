package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

// 新的辅助余额表
@Data
public class NewBalanceExcelResult {
    @ExcelProperty("数据来源")
    private String form;
    @ExcelProperty("主体")
    private String companyName;
    @ExcelProperty("科目")
    private String project;
    @ExcelProperty("科目代码")
    private String projectCode;
    @ExcelProperty("科目名称")
    private String projectName;
    @ExcelProperty("辅助核算段")
    private String auxiliaryAccounting;
    @ExcelProperty("本期借方")
    private BigDecimal v;
    @ExcelProperty("本期贷方")
    private BigDecimal w;
    @ExcelProperty("期初余额")
    private BigDecimal preBalance;
    @ExcelProperty("期末余额")
    private BigDecimal balance;
}
