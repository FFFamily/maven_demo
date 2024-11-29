package org.example.enitty;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SourceFileData {
    private String NAME;
    private String LEDGER_ID;
    private String PERIOD_NAME;
    private String CURRENCY_CODE;
    private BigDecimal YEAR_BEGIN_CR;
    private BigDecimal YEAR_BEGIN_DR;
    private BigDecimal YTD_CR;
    private BigDecimal YTD_DR;
    private String PERIOD_CR;
    private String PERIOD_DR;
    //    @ExcelProperty("机构代码")
    private String SEGMENT1;
    //    @ExcelProperty("机构名")
    private String SEGMENT1_NAME;
    //    @ExcelProperty("成本中心代码")
    private String SEGMENT2;
    //    @ExcelProperty("成本中心名称")
    private String SEGMENT2_NAME;
    //    @ExcelProperty("科目代码")
    private String SEGMENT3;
    //    @ExcelProperty("科目名称")
    private String SEGMENT3_NAME;
    //    @ExcelProperty("子目代码")
    private String SEGMENT4;
    //    @ExcelProperty("子目名称")
    private String SEGMENT4_NAME;
    //    @ExcelProperty("产品段代码")
    private String SEGMENT5;
    //    @ExcelProperty("产品段名称")
    private String SEGMENT5_NAME;
    //    @ExcelProperty("地区代码")
    private String SEGMENT6;
    //    @ExcelProperty("地区名称")
    private String SEGMENT6_NAME;
    //    @ExcelProperty("SBU代码")
    private String SEGMENT7;
    //    @ExcelProperty("SBU名称")
    private String SEGMENT7_NAME;
    //    @ExcelProperty("ICP代码")
    private String SEGMENT8;
    //    @ExcelProperty("ICP名称")
    private String SEGMENT8_NAME;
    //    @ExcelProperty("项目段代码")
    private String SEGMENT9;
    //    @ExcelProperty("项目段名称")
    private String SEGMENT9_NAME;
    //    @ExcelProperty("备用段代码")
    private String SEGMENT10;
    //    @ExcelProperty("备用段名称")
    private String SEGMENT10_NAME;
    @ExcelProperty("交易对象ID")
    private String transactionObjectId;
    @ExcelProperty("交易对象编码")
    private String transactionObjectCode;
    @ExcelProperty("交易对象名称")
    private String transactionObjectName;
    @ExcelProperty("交易对象编码处理")
    private String transactionObjectCodeCopy;
    // 编码
    @ExcelProperty("匹配字段")
    private String match;
    // 名称
    private String matchName;
}
