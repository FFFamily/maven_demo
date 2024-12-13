package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
// 项目映射
@Data
public class ZNEventMapping {
    @ExcelProperty("NCC项目段")
    private String  nccEventName;
    @ExcelProperty("FMS机构")
    private String fmsOrg;
    @ExcelProperty("FMS项目段编码")
    private String fmsEventCode;
}
