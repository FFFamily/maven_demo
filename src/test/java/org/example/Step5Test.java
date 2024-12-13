package org.example;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import org.example.enitty.OracleData;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyTypeConstant;
import org.example.utils.SqlUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class Step5Test {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SqlUtil sqlUtil;
    @Test
    void test1() {
        List<String> allCompany = sqlUtil.findAllCompany();
        for (String company : allCompany) {
            String type = CompanyTypeConstant.mapping.get(company);
            if (type.equals(CompanyTypeConstant.ZHONG_NAN)){
//                if (!company.equals("江苏中南物业服务有限公司")){
//                    continue;
//                }
                System.out.println("当前公司："+company);
                List<OracleData> res = new ArrayList<>();
//                String findHangSQL = "SELECT z.\"行说明\"  FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+company+"' AND z.\"期间\" >= '2023-07' AND z.\"期间\" <= '2023-12'  GROUP BY z.\"行说明\" HAVING  COUNT(z.\"行说明\") > 1 ";
                String findPiSQL = "SELECT  * " +
                        "FROM ZDPROD_EXPDP_20241120 z " +
                        "WHERE z.\"公司段描述\" = '"+company+"' " +
                        "AND z.\"期间\" >= '2023-07' " +
                        "AND z.\"期间\" <= '2023-12' ";

                List<Map<String, Object>> sqlList = jdbcTemplate.queryForList(findPiSQL);
//                List<String> collect = sqlList.stream().map(item -> (String)item.get("批名")).distinct().collect(Collectors.toList());
                Map<String, List<Map<String, Object>>> map = sqlList.stream().collect(Collectors.groupingBy(item -> (String) item.get("批名")));
                // 拿到所有的行说明
//                List<String> list = jdbcTemplate.queryForList(findPiSQL,String.class);
                for (String pi : map.keySet()) {
                    System.out.println(pi);
//                    if (!pi.equals("BKOZ0_GUANYANYAN5202310021731")){
//                        continue;
//                    }
//                    String sql = "SELECT *" + "FROM ZDPROD_EXPDP_20241120 z " + "WHERE z.\"公司段描述\" = '"+company+"' " + "AND z.\"期间\" >= '2023-07' AND z.\"期间\" <= '2023-12' AND z.\"批名\" = '"+pi+"' ";
                    List<Map<String, Object>> mapList = map.get(pi).stream().filter(item -> {
                        String form = (String) item.get("科目段描述");
                        return form.startsWith("应付账款")
                                || form.startsWith("预付账款")
                                || form.startsWith("合同负债")
                                || form.startsWith("预收账款")
                                || form.startsWith("应收账款")
                                || form.startsWith("其他应付款")
                                || form.startsWith("其他应收款");
                    }).collect(Collectors.toList());

                    BigDecimal sum = mapList.stream().reduce(BigDecimal.ZERO, (prev, curr) -> {
                        BigDecimal v =  curr.get("输入借方") == null ? null : (BigDecimal)curr.get("输入借方");
                        BigDecimal w =  curr.get("输入贷方") == null ? null : (BigDecimal)curr.get("输入贷方");
                        return prev.add(CommonUtil.getBigDecimalValue(v)).subtract(CommonUtil.getBigDecimalValue(w));
                    }, (l,      r) -> l);
                    if (sum.compareTo(BigDecimal.ZERO) != 0) {
                        // 不为0则跳过
                        res.addAll(data(mapList));
                        continue;
                    }
                    boolean flag = true;
                    Map<String, List<Map<String, Object>>> group = mapList.stream().collect(Collectors.groupingBy(item -> (String)item.get("科目代码")));
                    // 遍历同一批次下不同科目
                    for (String key : group.keySet()) {
                        List<Map<String, Object>> itemList = group.get(key);
                        BigDecimal itemSum = BigDecimal.ZERO;
                        for (Map<String, Object> item : itemList) {
                            BigDecimal v =  item.get("输入借方") == null ? null : (BigDecimal)item.get("输入借方");
                            BigDecimal w =  item.get("输入贷方") == null ? null : (BigDecimal)item.get("输入贷方");
                            itemSum = itemSum.add(CommonUtil.getBigDecimalValue(v)).subtract(CommonUtil.getBigDecimalValue(w));
                        }
                        if (itemSum.compareTo(BigDecimal.ZERO) != 0) {
                            // 单个科目下的余额都应该为0
                            flag = false;
                            break;
                        }
                        // 同一批次，筛选每个科目，交易对象都一样,则不标记
                        long count = itemList.stream().map(item -> (String) item.get("交易对象名称")).distinct().count();
                        if (count <= 1){
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        List<String> collect = mapList.stream().map(item -> (String)item.get("交易对象名称")).distinct().collect(Collectors.toList());
                        if (collect.stream().allMatch(item -> item != null && !item.contains("虚拟"))){

                        }else {
                            long size = collect.size();
                            if (size != 1 // 交易对象全部都一样才需要标记
                                    && collect.stream().anyMatch(item -> item == null || !item.contains("公司"))
                            ){

                                mapList.forEach(item -> item.put("额外字段","客商拆分"));
                            }
                        }
                    }
                    res.addAll(data(mapList));
                }
                String fileName = "第五步数据-"+company + System.currentTimeMillis() + ".xlsx";
                EasyExcel.write(fileName, OracleData.class).sheet("模板").doWrite(res);
            }
        }
    }

    private List<OracleData> data(List<Map<String, Object>> result) {
        List<OracleData> dataList = ListUtils.newArrayList();
        for (Map<String, Object> map : result) {
            Object projectDesc = map.get("科目段描述");
            if (projectDesc != null) {
                String p = ((String) projectDesc).split("-")[0];
                map.put("科目",p);
            }
            BigDecimal v =  map.get("输入借方") == null ? null : (BigDecimal)map.get("输入借方");
            BigDecimal w =  map.get("输入贷方") == null ? null : (BigDecimal)map.get("输入贷方");
            map.put("借正贷负",CommonUtil.getBigDecimalValue(v).subtract(CommonUtil.getBigDecimalValue(w)));
            dataList.add(JSONUtil.parse(map).toBean(OracleData.class));
        }
        return dataList;
    }
}
