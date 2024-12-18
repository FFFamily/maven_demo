package org.example.新老系统;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.ListUtils;
import org.example.enitty.OracleData;
import org.example.utils.CommonUtil;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class Step5 {
    @Resource
    private JdbcTemplate jdbcTemplate;
    public List<OracleData> step5Test(String newCompanyName){
        String findPiSQL = "SELECT  * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+newCompanyName+"' AND z.\"期间\" >= '2023-07'";
        List<OracleData> sqlList = jdbcTemplate.query(findPiSQL, new BeanPropertyRowMapper<>(OracleData.class));
        List<OracleData> collect = sqlList.stream()
                .peek(this::data)
                .filter(item -> item.get交易对象名称().contains("中南物业虚拟"))
                .filter(item -> {
                    String form = item.get科目段描述();
                    return form.startsWith("应付账款")
                            || form.startsWith("预付账款")
                            || form.startsWith("合同负债")
                            || form.startsWith("预收账款")
                            || form.startsWith("应收账款")
                            || form.startsWith("其他应付款")
                            || form.startsWith("其他应收款");
                })
                .collect(Collectors.toList());
        Map<String, List<OracleData>> map = collect.stream().collect(Collectors.groupingBy(OracleData::get批名));
        Map<String, List<OracleData>> sqlMap = sqlList.stream().collect(Collectors.groupingBy(OracleData::get批名));
        // 拿到所有的行说明
        for (String pi : map.keySet()) {
            System.out.println(pi);
            boolean flag = true;
            List<OracleData> mapList = sqlMap.get(pi);
            BigDecimal sum = BigDecimal.ZERO;
            for (OracleData data : mapList) {
                String form = data.get科目段描述();
                boolean isProject = form.startsWith("应付账款")
                        || form.startsWith("预付账款")
                        || form.startsWith("合同负债")
                        || form.startsWith("预收账款")
                        || form.startsWith("应收账款")
                        || form.startsWith("其他应付款")
                        || form.startsWith("其他应收款");
                if (isProject){
                    flag = false;
                    break;
                }
                BigDecimal v =  data.get输入借方() == null ? null : data.get输入借方();
                BigDecimal w =  data.get输入贷方() == null ? null : data.get输入贷方();
                sum = sum.add(CommonUtil.getBigDecimalValue(v)).subtract(CommonUtil.getBigDecimalValue(w));
            }
            if (flag && sum.compareTo(BigDecimal.ZERO) == 0) {
                mapList.forEach(item -> item.set额外字段("客商拆分"));
            }

//            BigDecimal sum = mapList.stream().reduce(BigDecimal.ZERO, (prev, curr) -> {
//                BigDecimal v =  curr.get输入借方() == null ? null : curr.get输入借方();
//                BigDecimal w =  curr.get输入贷方() == null ? null : curr.get输入贷方();
//                return prev.add(CommonUtil.getBigDecimalValue(v)).subtract(CommonUtil.getBigDecimalValue(w));
//            }, (l,r) -> l);
//            if (sum.compareTo(BigDecimal.ZERO) != 0) {
//                // 不为0则跳过
//                continue;
//            }
//            boolean flag = true;
//            Map<String, List<OracleData>> group = mapList.stream().collect(Collectors.groupingBy(OracleData::get科目代码));
//            // 遍历同一批次下不同科目
//            for (String key : group.keySet()) {
//                List<OracleData> itemList = group.get(key);
//                BigDecimal itemSum = BigDecimal.ZERO;
//                for (OracleData item : itemList) {
//                    BigDecimal v =  item.get输入借方() == null ? null : item.get输入借方();
//                    BigDecimal w =  item.get输入贷方() == null ? null : item.get输入贷方();
//                    itemSum = itemSum.add(CommonUtil.getBigDecimalValue(v)).subtract(CommonUtil.getBigDecimalValue(w));
//                }
//                if (itemSum.compareTo(BigDecimal.ZERO) != 0) {
//                    // 单个科目下的余额都应该为0
//                    flag = false;
//                    break;
//                }
//                // 同一批次，筛选每个科目，交易对象都一样,则不标记
//                long count = itemList.stream().map(item -> item.get交易对象名称()).distinct().count();
//                if (count <= 1){
//                    flag = false;
//                    break;
//                }
//            }
//            if (flag) {
//                List<String> collect = mapList.stream().map(item -> item.get交易对象名称()).distinct().collect(Collectors.toList());
//                if (collect.stream().allMatch(item -> item != null && !item.contains("虚拟"))){
//
//                }else {
//                    long size = collect.size();
//                    if (size != 1 // 交易对象全部都一样才需要标记
//                            && collect.stream().anyMatch(item -> item == null || !item.contains("公司"))
//                    ){
//
//                        mapList.forEach(item -> item.set额外字段("客商拆分"));
//                    }
//                }
//            }
//                    res.addAll(data(mapList));
        }
        return sqlList;
    }


    private void data(OracleData result) {
        Object projectDesc = result.get科目段描述();
        if (projectDesc != null) {
            String p = ((String) projectDesc).split("-")[0];
            result.set科目(p);
        }
        BigDecimal v =  result.get输入借方() == null ? null : result.get输入借方();
        BigDecimal w =  result.get输入贷方() == null ? null : result.get输入贷方();
        result.set借正贷负(CommonUtil.getBigDecimalValue(v).subtract(CommonUtil.getBigDecimalValue(w)));
    }
}
