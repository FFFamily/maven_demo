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
    @ExcelProperty("分类")
    private String type;
    // 交易对象编码
    @ColumnWidth(40)
    @ExcelProperty("交易对象编码")
    private String transactionObjectCode;
    // 交易对象编码名称
    @ColumnWidth(40)
    @ExcelProperty("交易对象编码名称")
    private String transactionObjectName;
    private Integer isIncludeUp;
}
