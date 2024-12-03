package org.example.寻找等级;

import org.example.寻找等级.old_excel.MappingNccToFmsExcel;
import org.example.寻找等级.old_excel.OldExcelTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 寻找旧系统等级
 */
public class OldFindLevel {

    /**
     * 根据新系统的找到老系统中一一对应的余额组成
     */
    public static List<OtherInfo3> findList(List<OtherInfo3> list,
                                            String nccCode,
                                            String nccProjectName,
                                            String customerName,
                                            BigDecimal v,
                                            BigDecimal w){
        // 通过映射找到对应的旧系统的数据
        return  list.stream().filter(item ->
                item.getNccProjectCode().equals(nccCode)
                        && (item.getNccAssistantCode().contains(nccProjectName) && item.getNccAssistantCode().contains(customerName))
//                        && ((v != null && v.equals(item.getV())) || (w != null && w.equals(item.getW())))
        ).collect(Collectors.toList());
    }



}
