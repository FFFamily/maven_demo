package org.example.controller;


import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.core.entity.SourceFileData;
import org.example.enitty.Assistant;
import org.example.enitty.OracleData;
import org.example.utils.ExcelDataUtil;
import org.example.分类.AssistantResult;
import org.example.分类.FindABCD;
import org.example.分类.entity.DraftFormatTemplate;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.FindLevelBySystem;
import org.example.寻找等级.OtherInfo3;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.utils.ExcelDataUtil.getZ;

@RestController
@RequestMapping("/test")
public class ExcelController {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private FindABCD findABCD;
    @Resource
    private FindLevelBySystem findLevelBySystem;

    @GetMapping("/demo1")
    public void test1(){
        Integer i = jdbcTemplate.queryForObject("select count(*) from ZDPROD_EXPDP_20241120", Integer.class);
        System.out.println(i);
    }

    @GetMapping("/demo2")
    public void test2(){
        doAsync();
    }
    @GetMapping("/findABCD")
    public void findABCD(){
        findABCD.doFindABDC("src/main/java/org/example/分类/9月科目辅助余额表.xlsx");
    }
    @GetMapping("/findLevel")
    public void findLevel(){
        List<SourceFileData> sourceFileDataList = ExcelDataUtil.getExcelData("src/main/java/org/example/分类/9月科目辅助余额表2.xlsx","Sheet1");
        List<Assistant> realAssistantList = ExcelDataUtil.covertAssistant(sourceFileDataList,null, null);
        List<OtherInfo3> result1 = new ArrayList<>();
        int size = 0;
        for (int i = 0; i < realAssistantList.size(); i++) {
            Assistant assistant = realAssistantList.get(i);
            if (!assistant.getE().equals("禹洲物业服务有限公司泉州分公司")){
                continue;
            }
            System.out.println("第"+i+"条，开始"+"共"+realAssistantList.size()+"条："+DateUtil.date());
            String z = assistant.getZ();
            if (z == null) {
                continue;
            }
            String projectName = assistant.getR();
            List<OtherInfo3> result = findLevelBySystem.doMain(
                    assistant.getZ(),
                    projectName,
                    assistant.getTransactionObjectCode());
            result1.addAll(result);
            size++;
            if (size == 100){
                break;
            }
        }
        String resultFileName = "模版" + System.currentTimeMillis()+".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
            excelWriter.write(result1, writeSheet1);
            System.out.println(resultFileName+"导出完成");
        }
    }


    public void doAsync(){
        // 查询所有公司
        List<String> companyList = jdbcTemplate.queryForList("select z.\"公司段描述\" from ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\"", String.class);
        for (int i = 0; i < companyList.size(); i++) {
            String company = companyList.get(i);
            System.out.println("当前公司为："+company);
            System.out.println("sql执行开始："+DateUtil.date());
            String sql = "SELECT * from ZDPROD_EXPDP_20241120 z where z.\"公司段描述\" = " + "'"+company+"'";
            List<Map<String, Object>> dataList = jdbcTemplate.queryForList(sql);
            System.out.println("sql执行结束： "+DateUtil.date());
            System.out.println("需要处理的数据："+dataList.size());
            exportExcel(dataList,company);
            System.out.println("已执行："+i+"还剩下："+(companyList.size() - i));
        }

        System.out.println("处理完成");
    }


    public void exportExcel(List<Map<String, Object>> dataList,String company){
        if (!dataList.isEmpty()){
            Map<String, Object> map = dataList.get(0);
            String resultFileName = company + ".xlsx";
            EasyExcel.write(resultFileName)
                    .head(head(map))
                    .sheet("模板")
                    .doWrite(dataList.stream().map(item -> JSONUtil.parse(item).toBean(OracleData.class)).collect(Collectors.toList()));
        }
        System.out.println("导出完成："+ DateUtil.date());
    }

    private List<List<String>> head(Map<String, Object> map) {
        List<List<String>> list = ListUtils.newArrayList();
        for (String key : map.keySet()) {
            List<String> head0 = ListUtils.newArrayList();
            head0.add(key);
            list.add(head0);
        }
        return list;
    }
}
