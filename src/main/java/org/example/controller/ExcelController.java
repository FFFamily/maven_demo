package org.example.controller;


import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.controller.enitty.OracleData;
import org.example.分类.AssistantResult;
import org.example.分类.FindABCD;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.拼接excel.doMontageExcel.list;

@RestController
@RequestMapping("/test")
public class ExcelController {
    @Resource
    private JdbcTemplate jdbcTemplate;

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
