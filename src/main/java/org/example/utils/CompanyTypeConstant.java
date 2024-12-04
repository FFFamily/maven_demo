package org.example.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.Data;
import org.example.enitty.CompanyTypeExcel;
import org.example.寻找等级.OtherInfo3;
import org.example.寻找等级.old_excel.OldExcelTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CompanyTypeConstant {
    public static final String LANG_JI = "朗基物业";
    public static final String ZHONG_NAN = "中南物业";
    public static final String YU_ZHOU = "禹洲物业";
   ;

    public static HashMap<String,String> mapping = new HashMap<>();

    static {
        EasyExcel.read("src/main/java/org/example/excel/润楹物业进度统计表V2.0.xlsx", CompanyTypeExcel.class, new PageReadListener<CompanyTypeExcel>(dataList -> {
            for (CompanyTypeExcel companyTypeConstant : dataList) {
                mapping.put(companyTypeConstant.getC(), companyTypeConstant.getD());
            }
        })).sheet("进度统计").headRowNumber(1).doRead();
    }

    public static void main(String[] args) {
        System.out.println(CompanyTypeConstant.mapping);
    }
}
