package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ZNIPCMapping {
    @ExcelProperty("ICP名称")
    private String nccCustomerName;
    @ExcelProperty("ICP代码")
    private String fmsICPCode;
}
