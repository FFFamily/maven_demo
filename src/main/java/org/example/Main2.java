//package org.example;
//
//import cn.hutool.core.util.StrUtil;
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.context.AnalysisContext;
//import com.alibaba.excel.read.listener.PageReadListener;
//import com.alibaba.excel.read.listener.ReadListener;
//import com.alibaba.excel.util.ListUtils;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class Main2 implements ReadListener<Info> {
//    /**
//     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
//     */
//    private static final int BATCH_COUNT = 80000;
//    /**
//     * 缓存的数据
//     */
//    private static List<OtherInfo> cachedDataList = new ArrayList<>(BATCH_COUNT);
//
//    public static void main(String[] args) {
//        System.out.println("正在读取明细");
//        String fileName1 = "src/main/java/org/example/excel/往来科目明细.xlsx";
//        EasyExcel.read(fileName1, OtherInfo.class, new PageReadListener<OtherInfo>(dataList -> {
//            cachedDataList.addAll(dataList);
//        })).sheet().doRead();
//        System.out.println("正在读取辅助对象");
//        List<Assistant> assistantList = new ArrayList<>();
//        String fileName2 = "src/main/java/org/example/excel/副本厦门往来清理跟进表-全匹配版 （禹洲泉州）-标识.xlsx";
//        EasyExcel.read(fileName2, Assistant.class, new PageReadListener<Assistant>(assistantList::addAll)).sheet("往来清理明细表").doRead();
//        List<OtherInfo> result = new ArrayList<>();
//        for (int i = 0; i < assistantList.size(); i++) {
//            Assistant assistant = assistantList.get(i);
//            if (i > 0){
////                String projectName = assistant.getR();
//                String projectName = "禹洲物业服务有限公司泉州分公司应付账款-暂估款----泉州温莎公馆SS:153942:JODV0:总部";
//                System.out.println("当前处理的辅助对象的科目名称为："+ projectName);
//                System.out.println("当前行："+(i + 2));
//                System.out.println("正在结合往来明细进行查询");
//                List<OtherInfo> collect = cachedDataList.stream()
//                        .filter(item -> item.getZ().equals(projectName))
//                        .collect(Collectors.toList());
//                int size = collect.size();
//                if (size == 0){
//                    System.out.println("未能匹配到相关明细");
//                    continue;
//                }
//                System.out.println("一共检索到"+size+"明细数据");
//                System.out.println("正在处理贷方金额为负值的情况");
//                HashMap<String,List<OtherInfo>> vMap = new HashMap<>();
//                HashMap<String,List<OtherInfo>> WMap = new HashMap<>();
//                for (int n = 0; n < collect.size(); n++) {
//                    OtherInfo otherInfo = collect.get(n);
//                    try {
//                        String V = otherInfo.getV() ;
//                        String W = otherInfo.getW();
//                        if (StrUtil.isNotBlank(W)){
//                            if (otherInfo.getX().equals("贷")){
//                                Double.parseDouble(otherInfo.getW());
//                            }
//                            List<OtherInfo> list = WMap.getOrDefault(W, new ArrayList<>());
//                            list.add(otherInfo);
//                            WMap.put(W,list);
//                        }
//                        if (StrUtil.isNotBlank(V) && !V.equals("0")){
//                            List<OtherInfo> list = vMap.getOrDefault(V, new ArrayList<>());
//                            list.add(otherInfo);
//                            vMap.put(V, list);
//                        }
//
//                    }catch (Exception e){
//                        System.out.println("解析出现异常");
//                        System.out.println("当前解析对象为：");
//                        System.out.println(otherInfo);
//                        e.printStackTrace();
//                    }
//                }
//                // 借
//                for (Map.Entry<String, List<OtherInfo>> entry : vMap.entrySet()) {
//                    if (WMap.get(entry.getKey()) == null){
//                        result.addAll(entry.getValue());
//                    }else {
//                        List<OtherInfo> VList = entry.getValue();
//                        List<OtherInfo> WList = WMap.get(entry.getKey());
//                        if (VList.size() == WList.size()){
//                            System.out.println("全部借方金额抵消");
//                        }else {
//                            // 存在无法完全抵消的情况
//                            // 1，取时间尾部的数据
//                            // 2, 若时间相同，则以凭证号为排序，取尾部数据
//                            result.addAll(
//                                    VList.stream().skip(WList.size())
//                                            .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.getQ())))
//                                            .collect(Collectors.toList())
//                            );
//                        }
//                    }
//                }
//
//                for (Map.Entry<String, List<OtherInfo>> entry : WMap.entrySet()) {
//                    if (vMap.get(entry.getKey()) == null){
//                        result.addAll(entry.getValue());
//                    }else {
//                        List<OtherInfo> WList = entry.getValue();
//                        List<OtherInfo> VList = vMap.get(entry.getKey());
//                        if (VList.size() == WList.size()){
//                            System.out.println("全部贷方金额抵消");
//                        }else {
//                            // 存在无法完全抵消的情况
//                            // 1，取时间尾部的数据
//                            // 2, 若时间相同，则以凭证号为排序，取尾部数据
//                            result.addAll(
//                                    WList.stream().skip(VList.size())
//                                            .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.getQ())))
//                                            .collect(Collectors.toList())
//                            );
//                        }
//                    }
//                }
////                List<OtherInfo> result = Stream.of(
////                        vMap.entrySet().stream().filter(item -> item.getValue() != null),
////                        WMap.entrySet().stream().filter(item -> item.getValue() != null)
////                ).flatMap(item -> item).map(item -> item.getValue()).collect(Collectors.toList());
//                System.out.println("处理完成");
//            }
//        }
//
//        String  resultFileName = "simpleWrite" + System.currentTimeMillis() + ".xlsx";
//        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
//        // 如果这里想使用03 则 传入excelType参数即可
//        EasyExcel.write(resultFileName, OtherInfo.class).sheet("模板").doWrite(result);
//    }
//
//    private static List<Info> data() {
//        List<Info> list = ListUtils.newArrayList();
//        for (int i = 0; i < 10; i++) {
////            Info data = new Info();
////            data.setName("字符串" + i);
////            data.setAge((double) i);
////            list.add(data);
//        }
//        return list;
//    }
//
//    @Override
//    public void invoke(Info info, AnalysisContext analysisContext) {
////        cachedDataList.add(info);
//        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
////        if (cachedDataList.size() >= BATCH_COUNT) {
////            // 存储完成清理 list
////            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
////        }
//    }
//
//    @Override
//    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
//
//    }
//}