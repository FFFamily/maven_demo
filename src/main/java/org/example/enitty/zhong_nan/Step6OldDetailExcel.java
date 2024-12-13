package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Step6OldDetailExcel {
    @ExcelProperty("账套名称")
    private String companyName;
    @ExcelProperty("日期")
    private Date time;
    @ExcelProperty("科目名称")
    private String projectName;
    @ExcelProperty("借方金额")
    private BigDecimal v;
    @ExcelProperty("贷方金额")
    private BigDecimal w;
    @ExcelProperty("摘要")
    private String match;
}
