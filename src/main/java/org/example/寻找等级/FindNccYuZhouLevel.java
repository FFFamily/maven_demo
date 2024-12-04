package org.example.寻找等级;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.example.utils.CommonUtil;
import org.example.utils.ExcelDataUtil;
import org.example.寻找等级.old_excel.MappingCustomerExcel;
import org.example.寻找等级.old_excel.MappingNccToFmsExcel;
import org.example.寻找等级.old_excel.MappingProjectExcel;
import org.example.寻找等级.old_excel.OldExcelTemplate;
import org.example.寻找等级.old_excel.yu_zhou.CompanyMappingExcel;
import org.example.寻找等级.old_excel.yu_zhou.YZProjectCodeMappingExcel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FindNccYuZhouLevel {
    // 公司
    private HashMap<String, Set<CompanyMappingExcel>> companyMappingExcelHashMap;
    // 科目
    private HashMap<String, Set<YZProjectCodeMappingExcel>> yzProjectCodeMappingExcelHashMap;
    @PostConstruct
    public void init(){
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



    public Set<OtherInfo3> findNccYuZhouList(List<OtherInfo3> oldCachedDataList, OtherInfo3 parentItem){
        return new HashSet<>();
    }



    public static List<OtherInfo3> getYZOldExcel(){
        List<OtherInfo3> data = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/禹洲/成都朗逸物业服务有限公司.xlsx", OldExcelTemplate.class, new PageReadListener<OldExcelTemplate>(dataList -> {
            for (OldExcelTemplate oldExcelTemplate : dataList) {

            }
        })).sheet("朗逸物业NCC序时簿").doRead();
        return data;
    }
}
