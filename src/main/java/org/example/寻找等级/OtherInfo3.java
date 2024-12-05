package org.example.寻找等级;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OtherInfo3 {
    @ExcelProperty("排序编号(Excel专属)")
    private String a;
    @ExcelProperty("层级编号")
    private String no;
    @ExcelProperty("当前层级")
    private Integer level;
    @ExcelProperty("异常")
    private String errorMsg;
    @ExcelProperty("公司名称")
    private String companyName;
    @ExcelProperty("系统来源")
    private String systemForm;
    @ExcelProperty("唯一标识")
    private String onlySign;
    @ExcelProperty("总账日期")
    private Date n;
    @ExcelProperty("凭证编号")
    private Integer q;
    @ColumnWidth(20)
    @ExcelProperty("凭证号规则")
    private String r;
    @ExcelProperty("来源")
    private String s;
    @ExcelProperty("借方金额")
    private BigDecimal v;
    @ExcelProperty("贷方金额")
    private BigDecimal w;
    @ExcelProperty("方向")
    private String x;
    @ExcelProperty("余额")
    private BigDecimal balanceSum;
    @ColumnWidth(50)
    @ExcelProperty("账户组合")
    private String z;
    @ColumnWidth(50)
    @ExcelProperty("账户组合(已处理)")
    private String zCopy;
    @ColumnWidth(200)
    @ExcelProperty("账户描述")
    private String zDesc;
    // 交易对象
    @ColumnWidth(20)
    @ExcelProperty("交易对象")
    private String transactionId;
    @ColumnWidth(20)
    @ExcelProperty("交易对象编码")
    private String transactionCode;
    @ColumnWidth(20)
    @ExcelProperty("交易对象名称")
    private String transactionName;
    @ColumnWidth(20)
    @ExcelProperty("交易对象编码(已处理)")
    private String transactionCodeCopy;
    @ColumnWidth(50)
    @ExcelProperty("最初账户组合")
    private String originZ;
    @ColumnWidth(50)
    @ExcelProperty("最初账户组合(已处理)")
    private String originZCopy;
    @ColumnWidth(50)
    @ExcelProperty("NCC科目段")
    private String nccProjectCode;
    @ColumnWidth(50)
    @ExcelProperty("NCC辅助核算段")
    private String nccAssistantCode;
    @ColumnWidth(50)
    @ExcelProperty("NCC核算账簿名称")
    private String nccCompanyName;
    @ColumnWidth(50)
    @ExcelProperty("日记账说明")
    private String journalExplanation;
    @ColumnWidth(50)
    @ExcelProperty("合并段")
    private String mergeValue;
}
