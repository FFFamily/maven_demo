//package org.example.controller;
//
//
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.json.JSONUtil;
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.ExcelWriter;
//import com.alibaba.excel.read.listener.PageReadListener;
//import com.alibaba.excel.util.ListUtils;
//import com.alibaba.excel.write.metadata.WriteSheet;
//import org.example.enitty.Assistant;
//import org.example.enitty.OracleData;
//import org.example.enitty.SourceFileData;
//import org.example.utils.ExcelDataUtil;
//import org.example.utils.LevelUtil;
//import org.example.utils.SqlUtil;
//import org.example.分类.AssistantResult;
//import org.example.分类.FindABCD;
//import org.example.分类.entity.DraftFormatTemplate;
//import org.example.寻找等级.FindLevel;
//import org.example.寻找等级.OtherInfo3;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//import static org.example.utils.ExcelDataUtil.getZ;
//
//@RestController
//@RequestMapping("/test")
//public class ExcelController {
//    @Resource
//    private JdbcTemplate jdbcTemplate;
//    @Resource
//    private FindABCD findABCD;
//    @Resource
//    private FindLevel findLevel;
//    @Resource
//    private SqlUtil sqlUtil;
//
//    @GetMapping("/demo1")
//    public void test1(){
//        Integer i = jdbcTemplate.queryForObject("select count(*) from ZDPROD_EXPDP_20241120", Integer.class);
//        System.out.println(i);
//    }
//    @GetMapping("/demo2")
//    public void test2(){
//        doAsync();
//    }
//
//    @GetMapping("/findABCD")
//    public void findABCD(){
//        findABCD.doFindABDC();
//    }
//
//
//
//    @GetMapping("/findLevel")
//    public void findLevel(){
//        List<SourceFileData> sourceFileDataList = ExcelDataUtil.getExcelData("src/main/java/org/example/分类/9月科目辅助余额表.xlsx","Sheet1");
//        Map<String, List<Assistant>> companyMap = ExcelDataUtil.covertAssistant(sourceFileDataList, null, null)
//                .stream()
//                .filter(item -> item.getCompanyCode().equals("NPXS0"))
//                .filter(item -> item.getR().equals("NPXS0.0.2241990101.34.0.0.0.0.30013387.0"))
//                .filter(item -> item.getTransactionObjectId().equals("CS:12901000"))
//                // 根据公司分组
//                .collect(Collectors.groupingBy(Assistant::getCompanyCode));
//        for (String companyCode : companyMap.keySet()) {
//            System.out.println(DateUtil.date()+ " 当前公司："+ companyCode);
//            List<Assistant> realAssistantList = companyMap.get(companyCode);
//            List<OtherInfo3> result1 = new ArrayList<>();
//            System.out.println("共"+realAssistantList.size()+"条");
//            String findCompanySql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段代码\" = '"+companyCode+"'";
//            List<OtherInfo3> cachedDataList = sqlUtil.find(findCompanySql);
//            // TODO 读取旧系统的明细数据
//            List<OtherInfo3> oldCachedDataList = ExcelDataUtil.getOldExcel();
//            System.out.println("整个公司包含数据量："+cachedDataList.size());
//            cachedDataList.forEach(LevelUtil::organizeDataItem);
//            for (int i = 0; i < realAssistantList.size(); i++) {
//                Assistant assistant = realAssistantList.get(i);
//                String z = assistant.getZ();
//                if (z == null) {
//                    continue;
//                }
//                // 账户组合描述
//                String projectName = assistant.getR();
//                String onlySign = assistant.getOnlySign();
//
//                List<OtherInfo3> startCollect = cachedDataList.stream()
//                        .filter(item -> item.getOnlySign().equals(onlySign))
//                        .collect(Collectors.toList());
//                List<OtherInfo3> result = findLevel.doMain(
//                        true,
//                        false,
//                        true,
//                        oldCachedDataList,
//                        cachedDataList,
//                        startCollect,
//                        assistant.getZ(),
//                        onlySign);
//                if (result.isEmpty()){
//                    // 证明所有的都借贷相互抵消了
//                    OtherInfo3 otherInfo3 = new OtherInfo3();
//                    otherInfo3.setA(String.valueOf(i));
//                    otherInfo3.setNo("1");
//                    otherInfo3.setLevel(1);
//                    otherInfo3.setS(assistant.getForm());
//                    otherInfo3.setBalanceSum(new BigDecimal(assistant.getZ()));
//                    otherInfo3.setZ(projectName);
//                    otherInfo3.setZDesc(assistant.getRDesc());
//                    otherInfo3.setTransactionId(assistant.getTransactionObjectId());
//                    otherInfo3.setTransactionName(assistant.getTransactionObjectName());
//                    otherInfo3.setOriginZ(projectName);
//                    result1.add(otherInfo3);
//                } else {
//                    int finalI = i;
//                    result.forEach(item -> {
//                        item.setA(String.valueOf(finalI));
//                        item.setZDesc(assistant.getRDesc());
//                        String transactionObjectCode = assistant.getTransactionObjectCode();
//                        String assistantTransactionObjectCodeCopy = assistant.getTransactionObjectCodeCopy();
//                        // 源-交易对象编码
//                        item.setTransactionCode(transactionObjectCode);
//                        // 处理-交易对象编码
//                        item.setTransactionCodeCopy(assistantTransactionObjectCodeCopy);
//                        item.setOriginZ(projectName);
//                        item.setOriginZCopy(projectName.replaceAll("\\.","-"));
//                        // 处理-账户组合
//                        String zCopy = item.getOriginZCopy().replaceAll("\\.","-");
//                        item.setZCopy(zCopy);
//                        item.setMergeValue(zCopy + (item.getTransactionCodeCopy() == null ? "" : item.getTransactionCodeCopy()));
//                    });
//                    result1.addAll(result);
//                }
//            }
//            Assistant assistant = companyMap.get(companyCode).get(0);
//            String resultFileName = "模版-" + assistant.getE() + "-" + System.currentTimeMillis() + ".xlsx";
//            try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
//                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
//                excelWriter.write(result1, writeSheet1);
//                System.out.println(resultFileName+"导出完成");
//            }
//        }
//        System.out.println("结束");
//    }
//
//
//    public void doAsync(){
//        // 查询所有公司
//        List<String> companyList = jdbcTemplate.queryForList("select z.\"公司段描述\" from ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\"", String.class);
//        for (int i = 0; i < companyList.size(); i++) {
//            String company = companyList.get(i);
//            System.out.println("当前公司为："+company);
//            System.out.println("sql执行开始："+DateUtil.date());
//            String sql = "SELECT * from ZDPROD_EXPDP_20241120 z where z.\"公司段描述\" = " + "'"+company+"'";
//            List<Map<String, Object>> dataList = jdbcTemplate.queryForList(sql);
//            System.out.println("sql执行结束： "+DateUtil.date());
//            System.out.println("需要处理的数据："+dataList.size());
//            exportExcel(dataList,company);
//            System.out.println("已执行："+i+"还剩下："+(companyList.size() - i));
//        }
//
//        System.out.println("处理完成");
//    }
//
//
//    public void exportExcel(List<Map<String, Object>> dataList,String company){
//        if (!dataList.isEmpty()){
//            Map<String, Object> map = dataList.get(0);
//            String resultFileName = company + ".xlsx";
//            EasyExcel.write(resultFileName)
//                    .head(head(map))
//                    .sheet("模板")
//                    .doWrite(dataList.stream().map(item -> JSONUtil.parse(item).toBean(OracleData.class)).collect(Collectors.toList()));
//        }
//        System.out.println("导出完成："+ DateUtil.date());
//    }
//
//    private List<List<String>> head(Map<String, Object> map) {
//        List<List<String>> list = ListUtils.newArrayList();
//        for (String key : map.keySet()) {
//            List<String> head0 = ListUtils.newArrayList();
//            head0.add(key);
//            list.add(head0);
//        }
//        return list;
//    }
//}
