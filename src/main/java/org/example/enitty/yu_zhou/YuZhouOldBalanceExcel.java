package org.example.enitty.yu_zhou;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 禹州老系统余额表
 */
@Data
public class YuZhouOldBalanceExcel {
    @ExcelProperty("老系统科目编码")
    private String n;
    @ExcelProperty("老系统科目名称")
    private String o;
    @ExcelProperty("老系统辅助段")
    private String p;
    @ExcelProperty("老系统初始账套")
    private String q;
    @ExcelProperty("2022绝对值")
    private BigDecimal v;
}
