package org.example.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.example.寻找等级.OtherInfo3;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
@Component
public class SqlUtil {
    @Resource
    private JdbcTemplate jdbcTemplate;
    public List<OtherInfo3> find(String sql,Object... args){
        if (args == null){
            return jdbcTemplate.query(sql, this::cover);
        }else {
            return jdbcTemplate.query(sql, this::cover,args);
        }
    }

    public List<String> findAllCompany(){
        return jdbcTemplate.queryForList("SELECT z.\"公司段描述\" FROM ZDPROD_EXPDP_20241120 z GROUP BY z.\"公司段描述\"", String.class);
    }

    public OtherInfo3 cover(ResultSet rs, int rowNum) throws SQLException {
        OtherInfo3 info = new OtherInfo3();
//        info.setA(String.valueOf(rowNum));
        // 年 + 月 + 凭证
        DateTime date = DateUtil.date(rs.getDate("有效日期"));
        int year = date.year();
        int month = date.month()+1;
        int code = rs.getInt("单据编号");
        info.setQ(code);
        info.setR(year+"-"+month+"-"+code);
        info.setV(rs.getBigDecimal("输入借方"));
        info.setW(rs.getBigDecimal("输入贷方"));
        // 有效日期
        info.setN(date);
        info.setS(rs.getString("来源"));
        // 有借就是 借方向
        info.setX(info.getV() != null ? "借" : "贷");
        info.setZ(rs.getString("账户组合"));
        info.setZDesc(rs.getString("账户描述"));
        info.setTransactionId(rs.getString("交易对象"));
        info.setTransactionName(rs.getString("交易对象名称"));
        info.setOnlySign(info.getZ()+info.getTransactionId());
        // 公司名称
        info.setCompanyName(rs.getString("公司段描述"));
        // 用于追溯老系统
        info.setJournalExplanation(rs.getString("日记账说明"));
        return info;
    }

}
