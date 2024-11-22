package org.example;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;

import java.util.*;
import java.util.stream.Collectors;

public class Main implements ReadListener<Info> {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 80000;
    /**
     * 缓存的数据
     */
    private static final List<OtherInfo> cachedDataList = new ArrayList<>(BATCH_COUNT);

    public static void main(String[] args) {
        System.out.println("正在读取明细");
        String fileName1 = "src/main/java/org/example/excel/往来科目明细.xlsx";
        EasyExcel.read(fileName1, OtherInfo.class, new PageReadListener<OtherInfo>(cachedDataList::addAll)).sheet().doRead();
        System.out.println("正在读取辅助对象");
        List<Assistant> assistantList = new ArrayList<>();
        String fileName2 = "src/main/java/org/example/excel/副本厦门往来清理跟进表-全匹配版 （禹洲泉州）-标识.xlsx";
        EasyExcel.read(
                        fileName2,
                        Assistant.class,
                        new PageReadListener<Assistant>(assistantList::addAll)
                ).sheet("往来清理明细表")
                .doRead();
        List<Assistant> realAssistantList = assistantList.stream()
                .filter(item -> "禹洲物业服务有限公司泉州分公司应付账款-暂估款-物业-外拓项目-住宅--泉州温莎公馆SS:246435:JODV0:SYZ000136".equals(item.getR()))
//                .skip(1)
                .collect(Collectors.toList());
        List<OtherInfo> result = new ArrayList<>();
        List<OtherInfo> sepResult = new ArrayList<>();
        for (int i = 0; i < realAssistantList.size(); i++) {
            Assistant assistant = realAssistantList.get(i);
            String projectName = assistant.getR();
            System.out.println("当前行：" + (i + 2));
            System.out.println("正在结合往来明细进行查询");
            List<OtherInfo> startCollect = cachedDataList.stream().filter(item -> item.getZ().equals(projectName)).collect(Collectors.toList());
            // 先处理一下余额等于某个借款的时候
            // 拿到最终余额
            Double balance;
            String z = assistant.getZ();
            if (z == null){
                System.out.println("z 为null 当前月无需处理");
                continue;
            }
            try {
                balance =  Double.parseDouble(assistant.getZ().replace(",","").replace("(","").replace(")",""));
            }catch (Exception e){
                balance = Double.parseDouble("0");
            }
            // 格式化
            organizeData(startCollect);
            // 排序
            List<OtherInfo> sortedStartCollect = startCollect.stream().sorted((a, b) -> DateUtil.date(b.getN()).toInstant().compareTo(DateUtil.toInstant(a.getN()))).collect(Collectors.toList());
            List<OtherInfo> otherInfos = new ArrayList<>();
            OtherInfo temporaryResult = null;
//            if (balance > 0) {
            if (z.contains("(") || z.contains(")")){
                // 余额为负去贷找
                List<OtherInfo> first = new ArrayList<>();
                boolean flag = true;
                for (OtherInfo otherInfo : sortedStartCollect) {
                    if (flag && otherInfo.getW() != null && balance.compareTo(otherInfo.getW())  == 0) {
                        temporaryResult = otherInfo;
                        flag = false;
                    } else {
                        first.add(otherInfo);
                    }
                }
                if (first.size() != sortedStartCollect.size()) {
                    // 证明已经被过滤
                    otherInfos = doFilter(first);
                }
            } else {
                // 余额为正去借找
                List<OtherInfo> first = new ArrayList<>();
                boolean flag = true;
                for (OtherInfo otherInfo : sortedStartCollect) {
                    if (flag && otherInfo.getV() != null && balance.compareTo(otherInfo.getV()) == 0) {
                        temporaryResult = otherInfo;
                        flag = false;
                    } else {
                        first.add(otherInfo);
                    }
                }
                if (first.size() != sortedStartCollect.size()) {
                    // 证明已经被过滤
                    otherInfos = doFilter(first);
                }
            }
            if (otherInfos.isEmpty() && temporaryResult != null) {
                // 证明就是正确结果
                result.add(temporaryResult);
            }else {
                List<OtherInfo> finalResult = doFilter(startCollect);
                if (finalResult.size() == startCollect.size()) {
                    // 可能存在无法查找到的数据
                    sepResult.addAll(finalResult.stream().sorted((a, b) -> DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN()))).collect(Collectors.toList()));
                } else {
                    result.addAll(finalResult.stream().sorted((a, b) -> DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN()))).collect(Collectors.toList()));
                }
            }
        }
        String resultFileName = "模版" + ".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo.class).build();
            List<OtherInfo> data1 = result;
            excelWriter.write(data1, writeSheet1);
            WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "未能匹配").head(OtherInfo.class).build();
            List<OtherInfo> data2 = sepResult;
            excelWriter.write(data2, writeSheet2);
        }
    }

    private static List<OtherInfo> doFilter(List<OtherInfo> startCollect) {
        List<OtherInfo> collect = startCollect
                .stream()
                .collect(Collectors.groupingBy(OtherInfo::getR))
                .entrySet()
                .stream()
                .filter(item -> {
                    // 拿到相同方向的
                    Map<String, List<OtherInfo>> XMap = item.getValue().stream().collect(Collectors.groupingBy(OtherInfo::getX));
                    Set<String> keySet = XMap.keySet();
                    if (keySet.size() != 1) {
                        // 证明有多种方向,直接放行
                        return true;
                    }
                    return XMap.get(keySet.toArray()[0]).stream().reduce(0d, (prev, curr) -> {
                        prev += curr.getV() == null ? 0 : curr.getV() == 0d ? 0 : curr.getV();
                        prev -= curr.getW() == null ? 0 : curr.getW() == 0d ? 0 : curr.getW();
                        return prev;
                    }, (l, r) -> l) .compareTo(0.0000000000001d) == 0;
                })
                .flatMap(item -> item.getValue().stream())
                .sorted((a, b) -> DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN())))
                .collect(Collectors.toList());

        int size = collect.size();
        if (size == 0) {
            System.out.println("未能匹配到相关明细");
            return new ArrayList<>();
        }
        System.out.println("一共检索到" + size + "明细数据");
        System.out.println("正在处理贷方金额为负值的情况");
        HashMap<String, List<OtherInfo>> vMap = new HashMap<>();
        HashMap<String, List<OtherInfo>> WMap = new HashMap<>();
        // 整理数据，并拿到起始索引
        organizeData(collect);
        int start = findStart(collect);
        for (int i1 = start; i1 < collect.size(); i1++) {
            OtherInfo otherInfo = collect.get(i1);
            Double V = otherInfo.getV();
            Double W = otherInfo.getW();
            if (V != null) {
                String realV = String.valueOf(V);
                List<OtherInfo> list = vMap.getOrDefault(realV, new ArrayList<>());
                list.add(otherInfo);
                vMap.put(realV, list);
            } else {
                otherInfo.setV(null);
            }
            if (W != null) {
                String realW = String.valueOf(W);
                List<OtherInfo> list = WMap.getOrDefault(realW, new ArrayList<>());
                list.add(otherInfo);
                WMap.put(realW, list);
            } else {
                otherInfo.setW(null);
            }
        }
        List<OtherInfo> itemResult = new ArrayList<>();
        for (Map.Entry<String, List<OtherInfo>> entry : vMap.entrySet()) {
            String VKey = entry.getKey();
            String VTargetKey = String.valueOf(0d - Double.parseDouble(VKey));
            if (WMap.get(VKey) == null) {
                itemResult.addAll(entry.getValue());
            } else {
                List<OtherInfo> VList = entry.getValue();
                List<OtherInfo> WList = WMap.get(entry.getKey());
                if (VList.size() == WList.size()) {
                    System.out.println("全部借方金额抵消");
                } else {
                    // 存在无法完全抵消的情况
                    // 1，取时间尾部的数据
                    // 2, 若时间相同，则以凭证号为排序，取尾部数据
                    itemResult.addAll(
                            VList.stream().skip(WList.size())
                                    .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.getQ())))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }

        for (Map.Entry<String, List<OtherInfo>> entry : WMap.entrySet()) {
            if (vMap.get(entry.getKey()) == null) {
                itemResult.addAll(entry.getValue());
            } else {
                List<OtherInfo> WList = entry.getValue();
                List<OtherInfo> VList = vMap.get(entry.getKey());
                if (VList.size() == WList.size()) {
                    System.out.println("全部贷方金额抵消");
                } else {
                    // 存在无法完全抵消的情况
                    // 1，取时间尾部的数据
                    // 2, 若时间相同，则以凭证号为排序，取尾部数据
                    itemResult.addAll(
                            WList.stream().skip(VList.size())
                                    .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.getQ())))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }
        return itemResult;
    }

    private static void organizeData(List<OtherInfo> collect) {
        int start = 0;
        for (int n = 0; n < collect.size(); n++) {
            OtherInfo otherInfo = collect.get(n);
            try {
                // 借方金额
                Double V = otherInfo.getV() == null ? null : otherInfo.getV() == 0d ? null : otherInfo.getV();
                // 贷方金额
                Double W = otherInfo.getW() == null ? null : otherInfo.getW() == 0d ? null : otherInfo.getW();
                // 方向
                String x = otherInfo.getX();
                if (W != null) {
                    if (x.equals("贷")) {
                        if (V == null && W.compareTo(0d) < 0) {
                            // 说明是以负数的形式表示借
                            V = 0d - W;
                            W = null;
                        } else {
                            V = null;
                        }
                    }
                }
                if (V != null) {
                    // 借
                    if (W == null && V.compareTo(0d) < 0) {
                        // 说明是以负数的形式表示借
                        W = 0d - V;
                        V = null;
                    } else {
                        W = null;
                    }
                }
                otherInfo.setV(V);
                otherInfo.setW(W);
            } catch (Exception e) {
                System.out.println("解析出现异常");
                System.out.println("当前解析对象为：");
                System.out.println(otherInfo);
                throw e;
            }
        }
    }

    private static Integer findStart(List<OtherInfo> collect) {
        int start = 0;
        Double sum = 0d;
        for (int n = 0; n < collect.size(); n++) {
            OtherInfo otherInfo = collect.get(n);
            // 借方金额
            Double V = otherInfo.getV() == null ? null : otherInfo.getV() == 0d ? null : otherInfo.getV();
            // 贷方金额
            Double W = otherInfo.getW() == null ? null : otherInfo.getW() == 0d ? null : otherInfo.getW();
            sum += V == null ? 0 : V;
            sum -= W == null ? 0 : W;
            if (sum.compareTo(0d) == 0) {
                start = n + 1;
            }
        }
        return start;
    }

    @Override
    public void invoke(Info info, AnalysisContext analysisContext) {
//        cachedDataList.add(info);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
//        if (cachedDataList.size() >= BATCH_COUNT) {
//            // 存储完成清理 list
//            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
//        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}