package org.example.enitty.yu_zhou;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 旧系统序时账
 */
@Data
public class YuZhouOldDetailExcel {
    // 年
    @ExcelProperty(index = 0)
    private String a;
    // 月
    @ExcelProperty(index = 1)
    private String b;
    // 日
    @ExcelProperty(index = 2)
    private String c;

    // 凭证号
    @ExcelProperty(index = 3)
    private String d;
    // 科目编码
    @ExcelProperty(index = 6)
    private String g;
    // 科目名称
    @ExcelProperty(index = 7)
    private String h;
    // 辅助项
    @ExcelProperty(index = 8)
    private String i;
    // 借
    @ExcelProperty(index = 11)
    private BigDecimal l;
    // 贷
    @ExcelProperty(index = 13)
    private BigDecimal n;
}
