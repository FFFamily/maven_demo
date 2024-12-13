package org.example.寻找等级;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.example.enitty.Assistant;
import org.example.enitty.zhong_nan.ZNProjectMapping;
import org.example.utils.CommonUtil;
import org.example.utils.LevelUtil;
import org.example.utils.OldExcelDataUtil;
import org.example.寻找等级.old_excel.MappingCustomerExcel;
import org.example.寻找等级.old_excel.MappingNccToFmsExcel;
import org.example.寻找等级.old_excel.MappingProjectExcel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FindNccZhongNanLevel {
    // 科目映射
    HashMap<String,ZNProjectMapping> znProjectMapping = new HashMap<>();

    @PostConstruct
    public void init(){
        initZnProjectMapping();
    }

    /**
     * 通过公司名称获取
     */
    public List<OtherInfo3> getOldCachedDataListByCompanyName(String companyName){
        List<OtherInfo3> result = OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/2023年1-11月序时簿.xlsx", companyName);
        result.addAll(OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/2022.xlsx", companyName));
        return result;
    }

    public void initZnProjectMapping() {
        try (ExcelReader excelReader = EasyExcel.read("").build()) {
            ReadSheet readSheet1 = EasyExcel.readSheet(0).head(ZNProjectMapping.class).registerReadListener(new PageReadListener<ZNProjectMapping>(dataList -> {
                for (ZNProjectMapping znProjectMapping : dataList) {

                }
            })).build();
            excelReader.read(readSheet1);
        }
    }

    public Set<OtherInfo3> findNccZhongNanList(List<OtherInfo3> oldCachedDataList,OtherInfo3 parentItem, Assistant assistant){
        // 拿到账户组合进行拆分
        String[] z = parentItem.getZ().split("\\.");
        // 科目段
        String code  = z[2];
        // 子目段
        String childCode = z[3];
        // 项目段
        String projectCode  = z[8];
        // 通过科目段+子目段找到 NCC 的 科目段
        ZNProjectMapping projectMapping = znProjectMapping.get(code + "." + childCode);
        // 客商映射
        // 项目映射
        // 老系统 key = 科目+项目+客商
        String key;

        // ncc 余额
//        BigDecimal nccSum = collectNccBalanceList.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.getV())).subtract(CommonUtil.getBigDecimalValue(curr.getW())), (l, r) -> l);
//        // FMS 余额
//        BigDecimal fmsSum = CommonUtil.getBigDecimalValue(parentItem.getV()).subtract(CommonUtil.getBigDecimalValue(parentItem.getW()));
//        // 借贷相抵
//        if (nccSum.compareTo(fmsSum) == 0){
//            // 找一级的余额组成
//            // 余额相等证明找到了
//            return new HashSet<>(LevelUtil.FindFirstLevel(collectNccBalanceList, CommonUtil.getZ(nccSum)));
//        }else {
//            return new HashSet<>();
//        }
        return null;
    }

    /**
     * 根据新系统的找到老系统中一一对应的余额组成
     */
    public static List<OtherInfo3> findList(List<OtherInfo3> list,
                                            String nccCode,
                                            String nccProjectName,
                                            String customerName){
        // 通过映射找到对应的旧系统的数据
        return  list.stream().filter(item ->
                item.getNccProjectCode().equals(nccCode)
                        && ((nccProjectName == null || (item.getNccAssistantCode() != null && item.getNccAssistantCode().contains(nccProjectName)))
                        && (customerName == null || (item.getNccAssistantCode() != null && item.getNccAssistantCode().contains(customerName))))
        ).collect(Collectors.toList());
    }


}
