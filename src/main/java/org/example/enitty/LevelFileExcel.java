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
    @ExcelProperty("会计科目")
    private String s;
    @ExcelProperty("有效日期")
    private Date n;
    @ExcelProperty("输入借方")
    private BigDecimal v;
    @ExcelProperty("输入贷方")
    private BigDecimal w;
    @ExcelProperty("帐户")
    private String z;
    @ExcelProperty("帐户说明")
    private String zDesc;
    // 交易对象
    @ExcelProperty("交易对象")
    private String transactionId;
    @ExcelProperty("交易对象名称")
    private String transactionName;
    @ExcelProperty("交易对象编码处理")
    private String transactionCodeCopy;
    @ExcelProperty("日记账说明")
    private String journalExplanation;
}
