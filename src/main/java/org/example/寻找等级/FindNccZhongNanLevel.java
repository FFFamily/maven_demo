package org.example.寻找等级;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.example.enitty.Assistant;
import org.example.enitty.zhong_nan.*;
import org.example.utils.OldExcelDataUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FindNccZhongNanLevel {
    // 科目映射
    public HashMap<String,ZNProjectMapping> znProjectMapping = new HashMap<>();
    // 公司
    public HashMap<String, ZNCompanyMapping> znCompanyMapping = new HashMap<>();
    // 部门
    public HashMap<String, ZNOrgMapping> znOrgMapping = new HashMap<>();
    // 项目
    public HashMap<String, ZNEventMapping> znEventMapping = new HashMap<>();
    // 客商
    public HashMap<String,ZNCompanyMapping> znCustomerMapping = new HashMap<>();
    // ICP
    public HashMap<String, ZNIPCMapping> znipcMapping = new HashMap<>();
    // 原中南关联方
    public HashMap<String,ZNRelationMapping> znRelationMapping = new HashMap<>();
    // 原中南关联方科目映射
    public HashMap<String,ZNRelationProjectMapping> znRelationProjectMapping = new HashMap<>();

    @PostConstruct
    public void init(){
        try (ExcelReader excelReader = EasyExcel.read("src/main/java/org/example/excel/zhong_nan/2-中南NCC与FMS映射表.xlsx").build()) {
            initZnProjectMapping(excelReader);
            initZnCompanyMapping(excelReader);
            initZnOrgMapping(excelReader);
            initZnEventMapping(excelReader);
            initZnRelationProjectMapping(excelReader);
        }
        initZnipcMapping();
        initZnRelationMapping();
    }

    private void initZnRelationProjectMapping(ExcelReader excelReader) {
        ReadSheet readSheet1 = EasyExcel.readSheet("11-原关联方中南集团映射").head(ZNRelationProjectMapping.class).registerReadListener(new PageReadListener<ZNRelationProjectMapping>(dataList -> {
            for (ZNRelationProjectMapping data : dataList) {
                String project = data.getProject();
                znRelationProjectMapping.put(project,data);
            }
        })).build();
        excelReader.read(readSheet1);
        System.out.println("11-原关联方中南集团映射 读取完成");
    }

    private void initZnRelationMapping() {
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/原中南关联方.xlsx", ZNRelationMapping.class, new PageReadListener<ZNRelationMapping>(dataList -> {
            for (ZNRelationMapping data : dataList) {
                znRelationMapping.put(data.getSupplierName(),data);
            }
        })).sheet("新的工作表").doRead();
        System.out.println("原中南关联方 读取完成");

    }

    private void initZnipcMapping() {
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/ICP名单.xlsx", ZNIPCMapping.class, new PageReadListener<ZNIPCMapping>(dataList -> {
            for (ZNIPCMapping data : dataList) {
                znipcMapping.put(data.getNccCustomerName(),data);
            }
        })).sheet("ICP名单").doRead();
        System.out.println("ICP名单 读取完成");
    }

    private void initZnEventMapping(ExcelReader excelReader) {
        ReadSheet readSheet1 = EasyExcel.readSheet("2-项目段").head(ZNEventMapping.class).registerReadListener(new PageReadListener<ZNEventMapping>(dataList -> {
            for (ZNEventMapping data : dataList) {
                String nccCompanyName = data.getNccCompanyName();
                String nccEventName = data.getNccEventName();
                znEventMapping.put(nccCompanyName+nccEventName,data);
            }
        })).build();
        excelReader.read(readSheet1);
        System.out.println("2-项目段 读取完成");
    }

    private void initZnOrgMapping(ExcelReader excelReader) {
        ReadSheet readSheet1 = EasyExcel.readSheet("3-成本中心").head(ZNOrgMapping.class).registerReadListener(new PageReadListener<ZNOrgMapping>(dataList -> {
            for (ZNOrgMapping data : dataList) {
                String nccCompanyName = data.getNCCCompanyName();
                String nccAssistantName = data.getNCCAssistantName();
                String regex = "【部门：(.*?)】";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(nccAssistantName);
                if (matcher.find()) {
                    String result = matcher.group(1);
                    znOrgMapping.put(nccCompanyName+result,data);
                }
            }
        })).build();
        excelReader.read(readSheet1);
        System.out.println("3-成本中心 读取完成");
    }

    private void initZnCompanyMapping(ExcelReader excelReader) {
        ReadSheet readSheet1 = EasyExcel.readSheet("1-机构").head(ZNCompanyMapping.class).registerReadListener(new PageReadListener<ZNCompanyMapping>(dataList -> {
            for (ZNCompanyMapping data : dataList) {
                znCompanyMapping.put(data.getNCCCompanyName(),data);
                znCustomerMapping.put(data.getNCCCompanyNameCopy(),data);
            }
        })).build();
        excelReader.read(readSheet1);
        System.out.println("1-机构读取完成");
    }

    /**
     * 通过公司名称获取
     */
    public List<OtherInfo3> getOldCachedDataListByCompanyName(String companyName){
        // 读取老系统的序时账
        // 对老系统进行转换而非直接拿
        List<OtherInfo3> result = OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/2023年1-11月序时簿.xlsx", companyName);
        result.addAll(OldExcelDataUtil.getOldExcel("src/main/java/org/example/excel/lang_ji/2022.xlsx", companyName));
        return result;
    }

    public void initZnProjectMapping(ExcelReader excelReader) {
        ReadSheet readSheet1 = EasyExcel.readSheet("0-科目映射").head(ZNProjectMapping.class).registerReadListener(new PageReadListener<ZNProjectMapping>(dataList -> {
            for (ZNProjectMapping data : dataList) {
                znProjectMapping.put(data.getNccProjectCode(),data);
            }
        })).build();
        excelReader.read(readSheet1);
        System.out.println("0-科目映射 读取完成");
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
