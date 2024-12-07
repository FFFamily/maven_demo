package org.example.寻找等级;

import lombok.Getter;
import org.example.utils.CommonUtil;
import org.example.utils.ExcelDataUtil;
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
public class FindNccLangJiLevel {
    HashMap<String,Set<MappingNccToFmsExcel>> mappingNccToFmsExcels;
    HashMap<String,Set<MappingProjectExcel>> mappingProjectExcels;
    HashMap<String, MappingCustomerExcel> mappingCustomerExcelHashMap;
    @Getter
    List<OtherInfo3> oldCachedDataList;

    @PostConstruct
    public void init(){
        mappingNccToFmsExcels = new HashMap<>();
        mappingProjectExcels = new HashMap<>();
        mappingCustomerExcelHashMap = new HashMap<>();
        ExcelDataUtil.findMappingNccToFmsExcel(mappingNccToFmsExcels,mappingCustomerExcelHashMap,mappingProjectExcels);
        oldCachedDataList = OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/成都朗逸物业服务有限公司.xlsx", "朗逸物业NCC序时簿");
    }



    public Set<OtherInfo3> findNccLangJiList(OtherInfo3 parentItem){
        // 科目段
        String code;
        // 子目段
        String childCode;
        // 项目段
        String projectCode;
        // 交易对象编码
        String transactionCode;
        // 客商
        String customerCode;
        // 拿到账户组合进行拆分
        String[] z = parentItem.getZ().split("\\.");
        // 科目段
        code = z[2];
        // 子目段
        childCode = z[3];
        // 项目段
        projectCode = z[8];
        // 交易对象编码
        transactionCode = parentItem.getTransactionCode();
        if (transactionCode != null) {
            String regex = "(?<=:)[^:]+(?=:)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(transactionCode);
            if (matcher.find()) {
                // 找到客商编码
                customerCode = matcher.group();
            } else {
                customerCode = null;
            }
        } else {
            customerCode = null;
        }

        // 先去老系统重找对应的数据
        // 新系统的数据可能由老系统的几笔构成
        // 通过科目段+子目段找到 NCC 的 科目段
        Set<MappingNccToFmsExcel> nccCodeList = mappingNccToFmsExcels.getOrDefault(code + "." + childCode,new HashSet<>());
        parentItem.setNccProjectCode(nccCodeList.stream().map(MappingNccToFmsExcel::getD).collect(Collectors.joining("、")));
        // 拿到NCC项目段
        Set<MappingProjectExcel> mappingProjectExcel = mappingProjectExcels.getOrDefault(projectCode,new HashSet<>());
        // 供应商名称
        MappingCustomerExcel mappingCustomerExcel = mappingCustomerExcelHashMap.getOrDefault(customerCode,new MappingCustomerExcel());
        String customerName = mappingCustomerExcel.getC();
        List<OtherInfo3> collectNccBalanceList = new ArrayList<>();
        // 遍历科目段
        for (MappingNccToFmsExcel mappingNccToFmsExcel : nccCodeList) {
            // 遍历 项目段
            for (MappingProjectExcel projectExcel : mappingProjectExcel) {
                // NCC 科目段
                String nccCode = mappingNccToFmsExcel.getD();
                // NCC 项目段
                String nccProjectName = projectExcel.getA();
                // 拼接辅助核算
                parentItem.setNccAssistantCode(CommonUtil.appendErrorMsg(parentItem.getNccProjectCode(),nccProjectName));
                // 去老系统找对应的值
                List<OtherInfo3> nccBalanceList = findList(oldCachedDataList, nccCode, nccProjectName, customerName);
                collectNccBalanceList.addAll(nccBalanceList);
            }
        }
        // ncc 余额
        BigDecimal nccSum = collectNccBalanceList.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.getV())).subtract(CommonUtil.getBigDecimalValue(curr.getW())), (l, r) -> l);
        // FMS 余额
        BigDecimal fmsSum = CommonUtil.getBigDecimalValue(parentItem.getV()).subtract(CommonUtil.getBigDecimalValue(parentItem.getW()));
        // 借贷相抵
        if (nccSum.compareTo(fmsSum) == 0){
            // 找一级的余额组成
            // 余额相等证明找到了
            return new HashSet<>(LevelUtil.FindFirstLevel(collectNccBalanceList, CommonUtil.getZ(nccSum)));
        }else {
            return new HashSet<>();
        }
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
