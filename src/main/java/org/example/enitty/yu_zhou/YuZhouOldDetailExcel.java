package org.example.enitty.yu_zhou;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 旧系统序时账
 */
@Data
public class YuZhouOldDetailExcel {
    @ExcelProperty("科目编码")
    private String h;
}
