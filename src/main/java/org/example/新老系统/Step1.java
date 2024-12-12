package org.example.新老系统;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.OracleData;
import org.example.enitty.SourceFileData;
import org.example.utils.CompanyTypeConstant;
import org.example.utils.SqlUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Step1 {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SqlUtil sqlUtil;
    /**
     * 从新系统中查询出
     */
    public void find(){
        // 查询所有的公司
        String sql = "SELECT z.\"公司段描述\" FROM ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\"";
        List<String> companyList = jdbcTemplate.queryForList(sql, String.class);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String company : companyList) {
            System.out.println(company);
            String companyType = CompanyTypeConstant.mapping.get(company);
            if (companyType.equals(CompanyTypeConstant.ZHONG_NAN)){
                // 中南
//                String findSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+company+"'and z.\"科目段描述\" LIKE '应收账款%'and z.\"行说明\" LIKE '%预估收缴率调整%'AND z.\"期间\" LIKE '2022%'";
//                String findSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"公司段描述\" = '"+company+"'and z.\"科目段描述\" LIKE '应收账款%'and z.\"行说明\" LIKE '%预估收缴率调整%'AND z.\"期间\" >= '2023-01' and z.\"期间\" <= '2023-06'";
                String findSql =  "SELECT * FROM ZDPROD_EXPDP_20241120 z " +
                        "WHERE z.\"公司段描述\" = '"+company+"' " +
//                        "AND z.\"科目段描述\" LIKE '应收账款%' " +
//                        "or z.\"科目段描述\" LIKE '应付账款%' " +
//                        "or z.\"科目段描述\" LIKE '合同负债%' " +
//                        "or z.\"科目段描述\" LIKE '预收账款%' " +
//                        "or z.\"科目段描述\" LIKE '预付账款%' " +
//                        "or z.\"科目段描述\" LIKE '其他应收款%' " +
//                        "or z.\"科目段描述\" LIKE '其他应付款%' " +
                        "AND (z.\"期间\" = '2022-13' OR z.\"期间\" = '2022-ADJ2')";
                List<Map<String, Object>> list = jdbcTemplate.queryForList(findSql);
                List<Map<String, Object>> filterLst = new ArrayList<>();
                for (Map<String, Object> stringObjectMap : list) {
                    String code = stringObjectMap.get("科目段描述").toString();
                    if (code.startsWith("应收账款")
                    || code.startsWith("应付账款")
                            || code.startsWith("合同负债")
                            || code.startsWith("预收账款")
                            || code.startsWith("预付账款")
                            || code.startsWith("其他应收款")
                            || code.startsWith("其他应付款")
                    ){
                        filterLst.add(stringObjectMap);
                    }
                }
                result.addAll(filterLst);
            }else {
                System.out.println("非中南公司");
            }
        }
//        String fileName = "2022-" + System.currentTimeMillis() + ".xlsx";
//        String fileName = "2023-01~2023-06" + System.currentTimeMillis() + ".xlsx";
        String fileName = "2022期末" + System.currentTimeMillis() + ".xlsx";
        EasyExcel.write(fileName, OracleData.class).sheet("模板").doWrite(data(result));
    }

    private List<OracleData> data(List<Map<String, Object>> result) {
        List<OracleData> dataList = ListUtils.newArrayList();
        for (Map<String, Object> map : result) {
            dataList.add(JSONUtil.parse(map).toBean(OracleData.class));
        }
        return dataList;
    }


}
