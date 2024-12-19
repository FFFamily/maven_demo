package org.example.enitty;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class LevelFileExcel {
    @ExcelProperty("单据编号")
    private Integer q;
    @ColumnWidth(20)
    @ExcelProperty("来源")
    private String s;
    @ExcelProperty("有效日期")
    private Date n;
    @ExcelProperty("输入借方")
    private BigDecimal v;
    @ExcelProperty("输入贷方")
    private BigDecimal w;
    @ColumnWidth(50)
    @ExcelProperty("账户组合")
    private String z;
    @ColumnWidth(200)
    @ExcelProperty("账户描述")
    private String zDesc;
    // 交易对象
    @ColumnWidth(20)
    @ExcelProperty("交易对象")
    private String transactionId;
    @ColumnWidth(20)
    @ExcelProperty("交易对象名称")
    private String transactionName;
    @ColumnWidth(20)
    @ExcelProperty("交易对象编码处理")
    private String transactionCodeCopy;
    @ColumnWidth(50)
    @ExcelProperty("日记账说明")
    private String journalExplanation;
}
