package org.example.分类;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅助
 */
@Data
public class AssistantResult {
    // 索引
    @ExcelProperty("编号")
    private String index;

    @ExcelProperty("公司名称")
    private String companyName;

    @ExcelProperty("科目段描述")
    private String subjectName;
    // 匹配字段
    @ColumnWidth(180)
    @ExcelProperty("匹配字段")
    private String field;
    // 匹配字段编码
    @ExcelProperty("匹配字段编码")
    private String fieldCode;
    // 金额
    @ExcelProperty("余额")
    private BigDecimal money;
    // 类型
    @ColumnWidth(20)
    @ExcelProperty("根据所有组成分类")
    private String type;
    @ColumnWidth(20)
    @ExcelProperty("根据1级组成分类")
    private String oneLevelType;

    // 交易对象编码
    @ColumnWidth(40)
    @ExcelProperty("交易对象编码")
    private String transactionObjectCode;
    // 交易对象编码名称
    @ColumnWidth(40)
    @ExcelProperty("交易对象编码名称")
    private String transactionObjectName;
    private Integer isIncludeUp;

    @ColumnWidth(40)
    @ExcelProperty("来源汇总")
    private String form;

    @ExcelProperty("是否合同范围内")
    private String isOrigin;

    @ExcelProperty("客商分类")
    private String customerType;
}
