package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

// 新的辅助余额表
@Data
public class NewBalanceExcelResult {
    @ExcelProperty("匹配段")
    private String onlySign;
    @ExcelProperty("辅助核算段")
    private String auxiliaryAccounting;
    @ExcelProperty("借")
    private BigDecimal v;
    @ExcelProperty("贷")
    private BigDecimal w;
}
