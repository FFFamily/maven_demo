package org.example.寻找等级.测试;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.utils.LevelUtil;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.OtherInfo3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FindLevelTest {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 80000;

    public static void main(String[] args) {
        List<OtherInfo3> cachedDataList = new ArrayList<>(BATCH_COUNT);
        List<Assistant> assistantList = new ArrayList<>();
        String fileName1 = "src/main/java/org/example/excel/往来科目明细.xlsx";
        String fileName2 = "src/main/java/org/example/excel/副本厦门往来清理跟进表-全匹配版 （禹洲泉州）-标识.xlsx";
        EasyExcel.read(fileName1, OtherInfo3.class, new PageReadListener<OtherInfo3>(dataList -> {
            for (OtherInfo3 item : dataList) {
                LevelUtil.organizeDataItem(item);
                cachedDataList.add(item);
            }
        })).sheet("应收账款").doRead();
        EasyExcel.read(fileName2, Assistant.class, new PageReadListener<Assistant>(assistantList::addAll))
                .sheet("往来清理明细表")
                .doRead();
        List<Assistant> realAssistantList = assistantList.stream()
                .filter(item -> "禹洲物业服务有限公司泉州分公司其他应收款-其他其他---泉州温莎美地CS:CYZ000110:JODV0:CYZ000110".equals(item.getR()))
//                .skip(1)
                .collect(Collectors.toList());
        List<OtherInfo3> result1 = new ArrayList<>();
        List<OtherInfo3> result2 = new ArrayList<>();
        for (Assistant assistant : realAssistantList) {
            String z = assistant.getZ();
            if (z == null) {
                continue;
            }
            String projectName = assistant.getR();
            List<OtherInfo3> startCollect = cachedDataList.stream()
                    .filter(item -> item.getZ().equals(projectName))
                    .collect(Collectors.toList());
            List<OtherInfo3> result = new FindLevel().doMain(
                    true,
                    false,
                    false,
                    new ArrayList<>(),
                    cachedDataList,
                    startCollect,
                    assistant.getZ(),
                    projectName);
            if (result.size() == startCollect.size() && startCollect.size() != 1) {
                result1.addAll(result);
            } else {
                result2.addAll(result);
            }
        }
        String resultFileName = "模版" + System.currentTimeMillis()+".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
            excelWriter.write(result2, writeSheet1);
            WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "未能匹配").head(OtherInfo3.class).build();
            excelWriter.write(result1, writeSheet2);
        }
    }
}
