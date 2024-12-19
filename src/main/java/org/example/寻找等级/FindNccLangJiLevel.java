package org.example.寻找等级;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import lombok.Getter;
import org.example.enitty.Assistant;
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
    // 科目映射
    HashMap<String,Set<MappingNccToFmsExcel>> mappingNccToFmsExcels;
    // 项目映射
    HashMap<String,Set<MappingProjectExcel>> mappingProjectExcels;
    // 客商映射
    HashMap<String, MappingCustomerExcel> mappingCustomerExcelHashMap;


    @PostConstruct
    public void init(){
        mappingNccToFmsExcels = new HashMap<>();
        mappingProjectExcels = new HashMap<>();
        mappingCustomerExcelHashMap = new HashMap<>();
        getMappingNccToFmsExcels();
        getMappingProjectExcels();
        getMappingCustomerExcelHashMap();

    }

    /**
     * 通过公司名称获取
     */
    public List<OtherInfo3> getOldCachedDataListByCompanyName(String companyName){
        List<OtherInfo3> result = OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/2023年1-11月序时簿.xlsx", companyName);
        result.addAll(OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/2022.xlsx", companyName));
        return result;
    }

    public void getMappingNccToFmsExcels() {
        try (ExcelReader excelReader = EasyExcel.read("src/main/java/org/example/excel/lang_ji/朗逸物业映射关系.xlsx").build()) {
            ReadSheet readSheet1 = EasyExcel.readSheet(0).head(MappingNccToFmsExcel.class).registerReadListener(new PageReadListener<MappingNccToFmsExcel>(dataList -> {
                for (MappingNccToFmsExcel mappingNccToFmsExcel : dataList) {
                    String j = mappingNccToFmsExcel.getJ();
                    String k = mappingNccToFmsExcel.getK();
                    String key = j+"."+k;
                    Set<MappingNccToFmsExcel> list = mappingNccToFmsExcels.getOrDefault(key, new HashSet<>());
                    if (list.stream().noneMatch(item -> item.getD().equals(mappingNccToFmsExcel.getD()))){
                        list.add(mappingNccToFmsExcel);
                    }
                    mappingNccToFmsExcels.put(key,list);
                }
            })).build();
            excelReader.read(readSheet1);
        }
    }
    public void getMappingProjectExcels() {
        EasyExcel.read("src/main/java/org/example/excel/lang_ji/客商.xlsx", MappingCustomerExcel.class,new PageReadListener<MappingCustomerExcel>(dataList -> {
            for (MappingCustomerExcel mappingNccToFmsExcel : dataList) {
                String key = mappingNccToFmsExcel.getB();
                if (key == null){
                    continue;
                }
                mappingCustomerExcelHashMap.put(key,mappingNccToFmsExcel);
            }
        })).doReadAll();
    }
    public void getMappingCustomerExcelHashMap() {
        EasyExcel.read("src/main/java/org/example/excel/lang_ji/项目段.xlsx",MappingProjectExcel.class,new PageReadListener<MappingProjectExcel>(dataList -> {
            for (MappingProjectExcel mappingNccToFmsExcel : dataList) {
                String key = mappingNccToFmsExcel.getC();
                if (key == null){
                    continue;
                }
                Set<MappingProjectExcel> list = mappingProjectExcels.getOrDefault(key, new HashSet<>());
                if (list.stream().noneMatch(item -> item.getA().equals(mappingNccToFmsExcel.getA()))){
                    list.add(mappingNccToFmsExcel);
                }
                mappingProjectExcels.put(key,list);
            }
        })).doReadAll();
    }



    public Set<OtherInfo3> findNccLangJiList(List<OtherInfo3> oldCachedDataList,OtherInfo3 parentItem, Assistant assistant){
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
        // 余额表中的交易对象编码 交易对象编码
//        transactionCode = parentItem.getTransactionCode();
        transactionCode = assistant.getTransactionObjectCode();
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
