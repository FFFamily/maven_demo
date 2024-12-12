package org.example.寻找等级.old_excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import org.example.寻找等级.OtherInfo3;

import java.math.BigDecimal;

/**
 * 旧系统excel文件内容
 */
@Data
public class OldExcelTemplate {
    // 年度
//    private String year;
    // 公司名称
    @ExcelProperty("企业名称")
    private String companyName;
    // 年
    @ExcelProperty("年度")
    private String a;
    // 月
    @ExcelProperty("月")
    private String b;
    // 日
    @ExcelProperty("日")
    private String c;
    // 凭证号
    @ExcelProperty("凭证号")
    private String d;
    // 分录号
//    private Integer e;
//    private String f;
    // 科目编码
    @ExcelProperty("科目编码")
    private String g;

    //    private String h;
    // 辅助项
    // 客商辅助核算
    // 项目名称辅助核算
    @ExcelProperty("辅助项")
    private String i;
//    private String j;
//    private String k;
    // 借方 本币
    @ExcelProperty("借方")
    private BigDecimal l;
//    private String m;
    // 贷方 本币
    @ExcelProperty("贷方")
    private BigDecimal n;
//    private String o;
//    private String p;
//    private String q;
//    private String r;
//    private String s;
//    private String t;
//    private String u;
//    private String v;
//    private String w;
//    private String x;
//    private String y;
//    private String z;

}
