package org.example.enitty;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CompanyTypeExcel {
    @ExcelProperty("机构名称")
    private String c;
    @ExcelProperty("收并购分类")
    private String d;
}
