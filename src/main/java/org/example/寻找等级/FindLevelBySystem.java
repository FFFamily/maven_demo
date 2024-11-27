package org.example.寻找等级;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.Data;
import org.example.enitty.Assistant;
import org.example.utils.SqlUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO 只对B和C的进行对比，如果是系统的就不往上追
 * TODO 并且要展示最初的项目名称
 */
@Service
public class FindLevelBySystem {
    @Resource
    private SqlUtil sqlUtil;
    @Resource
    private FindLevel findLevel;
    public  List<OtherInfo3> doMain(String z, String originProjectName, String code) {
        String findStartCollectSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"账户组合\" = '"+originProjectName;
        if (code != null){
            findStartCollectSql += "' and z.\"交易对象\" = '"+code+"'";
        }else {
            findStartCollectSql += "' and z.\"交易对象\" IS NULL";
        }
        List<OtherInfo3> startCollect = sqlUtil.find(findStartCollectSql).stream().peek(item -> findLevel.organizeDataItem(item)).collect(Collectors.toList());

        List<OtherInfo3> finalResult;
        finalResult = FindLevel.FindFirstLevel(startCollect,z,originProjectName);
        Deque<OtherInfo3> deque = new LinkedList<>();
        List<OtherInfo3> result = new ArrayList<>();
        for (int i = 0; i < finalResult.size(); i++) {
            OtherInfo3 otherInfo3 = finalResult.get(i);
            int level = 1;
            deque.push(otherInfo3);
            // 准备进行迭代遍历
            while (!deque.isEmpty()){
                // 对当前层进行遍历
                int dequeSize = deque.size();
                for (int dequeIndex = 0; dequeIndex < dequeSize; dequeIndex++) {
                    OtherInfo3 parentItem = deque.poll();
                    assert parentItem != null;
                    String no = parentItem.getNo() == null ? String.valueOf(i+1) : parentItem.getNo();
                    parentItem.setLevel(level);
                    if (level == 1) {
                        FindLevel.judgeJoin(result,parentItem,no,level);
                        String form = parentItem.getS();
                        // 只有一级的时候进行判断
                        if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
                            level = findLevel.find(deque,null,parentItem,originProjectName,level,true,true);
                        }
                    } else {
                        FindLevel.judgeJoin(result,parentItem,no,level);
                        level = findLevel.find(deque,null,parentItem,originProjectName,level,true,true);
                    }
                }
            }
        }
        return result;
    }


}