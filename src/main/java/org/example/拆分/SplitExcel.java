package org.example.拆分;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.util.ListUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SplitExcel extends AnalysisEventListener<Map<Integer, String>> {
    public static void main(String[] args) {
        String fileName = "src/main/java/org/example/拆分/CRC_B00_GL_辅助核算明细__211124-禹洲.xls";
        // 这里 只要，然后读取第一个sheet 同步读取会自动finish
        EasyExcel.read(fileName, new SplitExcel())
                .excelType(ExcelTypeEnum.XLS)
                .sheet().doRead();
    }

    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 100000;
    private List<Map<Integer, String>> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        cachedDataList.add(data);
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        EasyExcel.write("CRC_B00_GL_辅助核算明细__211124-禹洲.xls").sheet("模板").doWrite(cachedDataList);
        System.out.println("处理结束");
    }
}
