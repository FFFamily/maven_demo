package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

// 原中南关联方
@Data
public class ZNRelationMapping {
    @ExcelProperty("供应商名称")
    private String supplierName;
}
