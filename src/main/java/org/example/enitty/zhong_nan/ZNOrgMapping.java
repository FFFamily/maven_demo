package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ZNOrgMapping {
    @ExcelProperty("核算账簿名称")
    private String NCCCompanyName;
    @ExcelProperty("辅助核算")
    private String NCCAssistantName;
    @ExcelProperty("FMS成本中心代码")
    private String FMSOrgCode;
}
