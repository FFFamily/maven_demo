package org.example.func_three;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OtherInfo2 {
    // 层级
//    @ExcelProperty("阶级")
    @ExcelProperty("编号")
    private String no;
    @ExcelProperty("当前层级")
    private Integer level;
    @ExcelProperty("异常")
    private String errorMsg;
    @ExcelProperty("编号")
    private String a;
//    @ExcelProperty("区域")
//    private String a;
//    @ExcelProperty("科目编码")
//    private String b;
//    @ExcelProperty("科目1")
//    private String c;
//    @ExcelProperty("机构")
//    private String d;
//    @ExcelProperty("成本中心")
//    private String e;
//    @ExcelProperty("科目2")
//    private String f;
//    @ExcelProperty("子目")
//    private String g;
//    @ExcelProperty("产品")
//    private String h;
//    @ExcelProperty("地区")
//    private String i;
//    @ExcelProperty("SBU")
//    private String j;
//    @ExcelProperty("ICP")
//    private String k;
//    @ExcelProperty("项目")
//    private String l;
//    @ExcelProperty("客商")
//    private String m;
    @ExcelProperty("总账日期")
    private Date n;
//    @ExcelProperty("会计科目")
//    private String o;
//    @ExcelProperty("会计科目说明")
//    private String p;
    @ExcelProperty("凭证编号")
    private Integer q;
    @ColumnWidth(40)
    @ExcelProperty("凭证号规则")
    private String r;
    @ExcelProperty("来源")
    private String s;
//    @ExcelProperty("摘要")
//    private String t;
//    @ExcelProperty("往来单位")
//    private String u;
    @ExcelProperty("借方金额")
    private BigDecimal v;
    @ExcelProperty("贷方金额")
    private BigDecimal w;
    @ExcelProperty("方向")
    private String x;
//    @ExcelProperty("余额")
//    private String y;
    @ColumnWidth(200)
    @ExcelProperty("合并段值")
    private String z;

    @ColumnWidth(200)
    @ExcelProperty("最初的合并段值")
    private String originZ;
//    @ExcelProperty("AA")
//    private String aa;
//    @ExcelProperty("AB")
//    private String ab;


}
