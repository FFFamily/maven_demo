package org.example.enitty.yu_zhou;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

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
    @ExcelProperty("科目编码")
    private String h;
}
