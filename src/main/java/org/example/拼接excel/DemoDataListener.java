//package org.example.拼接excel;
//
//import com.alibaba.excel.context.AnalysisContext;
//import com.alibaba.excel.read.listener.ReadListener;
//import com.alibaba.excel.util.ListUtils;
//
//import java.util.List;
//import java.util.Map;
//
//
//public class DemoDataListener implements ReadListener<Map<String,Object>> {
//
//    /**
//     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
//     */
//    private static final int BATCH_COUNT = 100;
//    /**
//     * 缓存的数据
//     */
//    private List<Map<String,Object>> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
//
//    @Override
//    public void invoke(Map<String,Object> data, AnalysisContext context) {
//        cachedDataList.add(data);
//        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
//        if (cachedDataList.size() >= BATCH_COUNT) {
//            saveData();
//            // 存储完成清理 list
//            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
//        }
//    }
//
//    /**
//     * 所有数据解析完成了 都会来调用
//     *
//     * @param context
//     */
//    @Override
//    public void doAfterAllAnalysed(AnalysisContext context) {
//        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
//        saveData();
//        log.info("所有数据解析完成！");
//    }
//
//    /**
//     * 加上存储数据库
//     */
//    private void saveData() {
//
//    }
//}
