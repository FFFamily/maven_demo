package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
// 项目映射
@Data
public class ZNEventMapping {
    @ExcelProperty("核算账簿名称")
    private String nccCompanyName;
    @ExcelProperty("项目段")
    private String nccEventName;
    @ExcelProperty("FMS项目段编码")
    private String fmsEventCode;
    @ExcelProperty("FMS项目段")
    private String fmsEventName;
    @ExcelProperty("产品段代码")
    private String fmsProductCode;
    @ExcelProperty("产品段")
    private String fmsProductName;
}
