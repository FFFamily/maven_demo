package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class Step6Result1 {
    @ExcelProperty("主体")
    private String companyName;
    @ExcelProperty("期间")
    private String time;
    @ExcelProperty("旧系统科目")
    private String oldProject;
    @ExcelProperty("新系统科目")
    private String newProject;
    @ExcelProperty("旧系统借正贷负金额")
    private BigDecimal oldMoney;
    @ExcelProperty("新系统借正贷负金额")
    private BigDecimal newMoney;
    @ExcelProperty("差额")
    private BigDecimal subMoney;
}

