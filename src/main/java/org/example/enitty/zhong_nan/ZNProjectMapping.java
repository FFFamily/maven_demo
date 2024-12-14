package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ZNProjectMapping {
    @ExcelProperty("NCC科目编码")
    private String  nccProjectCode;
    @ExcelProperty("NCC科目名称")
    private String  nccProjectName;
    @ExcelProperty("FMS科目代码")
    private String fmsProjectCode;
    @ExcelProperty("FMS子目代码")
    private String fmsChildProjectCode;
}
