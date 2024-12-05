package org.example.寻找等级;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.example.utils.CommonUtil;
import org.example.寻找等级.old_excel.yu_zhou.CompanyMappingExcel;
import org.example.寻找等级.old_excel.yu_zhou.OldYZExcelTemplate;
import org.example.寻找等级.old_excel.yu_zhou.YZProjectCodeMappingExcel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FindNccYuZhouLevel {
    // 公司
    private HashMap<String, Set<CompanyMappingExcel>> companyMappingExcelHashMap;
    // 科目
    private HashMap<String, Set<YZProjectCodeMappingExcel>> yzProjectCodeMappingExcelHashMap;
    List<OtherInfo3> oldYZData = new ArrayList<>();
    @PostConstruct
    public void init(){
        // 读取excel
        getYZOldExcel();
        companyMappingExcelHashMap = new HashMap<>();
        // 写法1
        try (ExcelReader excelReader = EasyExcel.read("src/main/java/org/example/utils/禹洲映射关系.xlsx").build()) {
            ReadSheet readSheet1 = EasyExcel.readSheet(0).head(CompanyMappingExcel.class).registerReadListener(new PageReadListener<CompanyMappingExcel>(dataList -> {
                for (CompanyMappingExcel mappingNccToFmsExcel : dataList) {
                    // 公司名称
                    String FMSCompanyName = mappingNccToFmsExcel.getA();
                    String NCCCompanyName = mappingNccToFmsExcel.getB();
                    Set<CompanyMappingExcel> set = companyMappingExcelHashMap.getOrDefault(FMSCompanyName, new HashSet<>());
                    if (set.stream().noneMatch(item -> item.getA().equals(NCCCompanyName))){
                        set.add(mappingNccToFmsExcel);
                    }
                    companyMappingExcelHashMap.put(FMSCompanyName,set);
                }
            })).build();
            ReadSheet readSheet2 = EasyExcel.readSheet(0).head(YZProjectCodeMappingExcel.class).registerReadListener(new PageReadListener<YZProjectCodeMappingExcel>(dataList -> {
                for (YZProjectCodeMappingExcel yzProjectCodeMappingExcel : dataList) {
                    // 公司名称
                    String nccCode = yzProjectCodeMappingExcel.getB();
                    String fmsCode = yzProjectCodeMappingExcel.getD();
                    Set<YZProjectCodeMappingExcel> set = yzProjectCodeMappingExcelHashMap.getOrDefault(fmsCode, new HashSet<>());
                    if (set.stream().noneMatch(item -> item.getB().equals(nccCode))){
                        set.add(yzProjectCodeMappingExcel);
                    }
                    yzProjectCodeMappingExcelHashMap.put(fmsCode,set);
                }
            })).build();
            // 这里注意 一定要把sheet1 sheet2 一起传进去，不然有个问题就是03版的excel 会读取多次，浪费性能
            excelReader.read(readSheet1, readSheet2);
        }
    }

    /**
     *
     * @param parentItem 新系统明细
     * @return
     */
    public Set<OtherInfo3> findNccYuZhouList(OtherInfo3 parentItem){
        String z = parentItem.getZ();
        String[] split = z.split("\\.");
        // 新系统 科目编码
        String fmsProjectCode = split[2];
        // 新系统 机构名称
        String companyName = parentItem.getCompanyName();
        // 计算余额
        // TODO 余额的计算规则
        BigDecimal balance = CommonUtil.getBigDecimalValue(parentItem.getV()).subtract(CommonUtil.getBigDecimalValue(parentItem.getW()));
        // 匹配旧系统的机构
        Set<CompanyMappingExcel> companyset = companyMappingExcelHashMap.getOrDefault(companyName, new HashSet<>());
        // 匹配旧系统的科目
        Set<YZProjectCodeMappingExcel> codeSet = yzProjectCodeMappingExcelHashMap.getOrDefault(fmsProjectCode, new HashSet<>());
        Set<OtherInfo3> result = new HashSet<>();
        for (YZProjectCodeMappingExcel code : codeSet) {
            for (CompanyMappingExcel company : companyset) {
                String key = code.getB()+company.getB()+ balance;
                List<OtherInfo3> collect = oldYZData.stream().filter(item -> Objects.equals(item.getNccYZBalanceMatch(), key)).collect(Collectors.toList());
                // TODO 万一匹配到多个了怎么办
                result.addAll(collect);
            }
        }
        return result;
    }



    public void getYZOldExcel(){
        EasyExcel.read("src/main/java/org/example/excel/禹洲/2021年科目辅助余额表.xlsx", OldYZExcelTemplate.class, new PageReadListener<OldYZExcelTemplate>(dataList -> {
            for (OldYZExcelTemplate oldExcelTemplate : dataList) {
                OtherInfo3 otherInfo3 = new OtherInfo3();
                String b = oldExcelTemplate.getB();
                String[] split = b.split("\\\\");
                String subjectName = split[2];
                // 余额
                BigDecimal money = oldExcelTemplate.getL();
                // ncc 科目编码
                String nccProjectCode = oldExcelTemplate.getA();
                // 机构
                Integer nccCompanyName = oldExcelTemplate.getD();
                if (subjectName.startsWith("应付账款") || subjectName.startsWith("其他应付款") || subjectName.startsWith("合同负债")){
                    money = BigDecimal.ZERO.subtract(money);
                }
                otherInfo3.setNccYZBalanceMatch(nccProjectCode+nccCompanyName+money);
                oldYZData.add(otherInfo3);
            }
        })).sheet("新的工作表-公司原表").doRead();
        System.out.println("读取禹洲旧系统数据完成");
    }
}
