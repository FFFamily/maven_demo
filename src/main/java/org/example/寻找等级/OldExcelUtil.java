package org.example.寻找等级;

import org.example.寻找等级.old_excel.MappingNccToFmsExcel;
import org.example.寻找等级.old_excel.OldExcelTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OldExcelUtil {
    // NCC  与 FMS 映射
    List<MappingNccToFmsExcel> mappingList1 = new ArrayList<>();
    // 客商
    List<Object> mappingList2 = new ArrayList<>();
    // 项目段映射
    List<Object> mappingList3 = new ArrayList<>();

    /**
     *
     * @param oldExcelTemplate
     * @return
     */
    public OtherInfo3 covert(OldExcelTemplate oldExcelTemplate){
        OtherInfo3 otherInfo3 = new OtherInfo3();
        // 科目编码
        String f = oldExcelTemplate.getF();
        Optional<MappingNccToFmsExcel> optionalD = mappingList1.stream().filter(item -> item.getD().equals(f)).findFirst();
        // 从映射表中找到对应的科目编码
        if (optionalD.isPresent()) {
            // 映射
            MappingNccToFmsExcel mappingNccToFmsExcel = optionalD.get();
            // 机构代码
            // 科目代码
            String j = mappingNccToFmsExcel.getJ();
            // 子目代码
            String k = mappingNccToFmsExcel.getK();
            // 产品段
            // 地区代码
            // SUB代码
            String n = mappingNccToFmsExcel.getN();
            // ICP 代码
            // 项目段
            // 备用
            // 成本中心代码
        }
        return otherInfo3;
    }
}
