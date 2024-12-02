package org.example.寻找等级.old_excel;

import lombok.Data;
import org.example.寻找等级.OtherInfo3;

@Data
public class OldExcelTemplate {
    private String a;
    private String b;
    private String c;
    private String d;
    private String e;
    // 科目编码
    private String f;
    // 辅助项
    // 客商辅助核算
    // 项目名称辅助核算
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
    private String r;
    private String s;
    private String t;
    private String u;
    private String v;
    private String w;
    private String x;
    private String y;
    private String z;

    public OtherInfo3 coverToInfo(){
        OtherInfo3 otherInfo3 = new OtherInfo3();

        return otherInfo3;
    }
}
