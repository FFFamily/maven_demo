package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 机构映射
 */
@Data
public class ZNCompanyMapping {
    @ExcelProperty("核算账簿名称")
    private String NCCCompanyName;
    @ExcelProperty("公司名称")
    private String NCCCompanyNameCopy;
    @ExcelProperty("机构代码")
    private String FMSCompanyCode;
    @ExcelProperty("FMS机构")
    private String FMSCompanyName;
}

