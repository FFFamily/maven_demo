package org.example.enitty;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 辅助余额
 */
@Data
public class Assistant {
    // 备用：辅助核算段
    private String a;
    private String b;
    private String c;
    private String d;
    // 机构名称
    private String e;
    private String f;
    private String g;
    private String h;
    private String i;
    private String j;
    private String k;
    private String l;
    private String m;
    private String n;
    private String o;
    private String p;
    private String q;
    // 科目段编码
    private String r;
    private String s;
    // 来源
    private String t;
    private String u;
    private String v;
    private String w;
    private String x;
    private String y;
    // 余额
    private String z;
    // 唯一标识
    private String onlySign;

    // 交易对象编码
    private String transactionObjectId;
    private String transactionObjectCode;
    private String transactionObjectCodeCopy;
    private String transactionObjectName;
    // 编码描述（科目段描述）
    private String rDesc;
    // 机构代码
    private String companyCode;
    // 来源
    private String form;


}
