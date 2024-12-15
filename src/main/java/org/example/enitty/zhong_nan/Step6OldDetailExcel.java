package org.example.enitty.zhong_nan;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Step6OldDetailExcel {
    @ExcelProperty("账套名称")
    private String companyName;

    @ExcelProperty("部门－名称")
    private String orgName;
    @ExcelProperty("日期")
    private Date time;
    @ExcelProperty("科目代码")
    private String projectCode;
    @ExcelProperty("科目名称")
    private String projectName;
    @ExcelProperty("实际科目")
    private String actualProject;
    @ExcelProperty("匹配科目")
    private String matchProject;
    @ExcelProperty("客商－名称")
    private String customerName;
    @ExcelProperty("人员档案－名称")
    private String personalName;
    @ExcelProperty("项目－名称")
    private String eventName;
    @ExcelProperty("借方金额")
    private BigDecimal v;
    @ExcelProperty("贷方金额")
    private BigDecimal w;
    @ExcelProperty("摘要")
    private String match;
    @ExcelProperty("备注")
    private String remark;

//    @ExcelProperty("组合")
//    private String group;
    // 是否被使用
    @ExcelIgnore
    private Boolean used;
    // 新系统公司段代码
    @ExcelIgnore
    private String companyCode;
    // 辅助核算段
    @ExcelProperty("生成的辅助核算段")
    private String auxiliaryAccounting;
    // 科目
    @ExcelIgnore
    private String project;
    @ExcelProperty("账户组合")
    private String onlySign;
    // 唯一匹配段名称
    @ExcelProperty("账户组合描述")
    private String onlySignName;

    public Boolean getUsed(){
        return used != null && used;
    }

}
