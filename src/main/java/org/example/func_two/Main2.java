package org.example.func_two;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.Assistant;
import org.example.Info;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO 只对B和C的进行对比，如果是系统的就不往上追
 * TODO 并且要展示最初的项目名称
 */
public class Main2 implements ReadListener<Info> {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 80000;


    public static void main(String[] args) {
        List<OtherInfo2> cachedDataList = new ArrayList<>(BATCH_COUNT);
        List<Assistant> assistantList = new ArrayList<>();
        String fileName1 = "src/main/java/org/example/excel/往来科目明细.xlsx";
        String fileName2 = "src/main/java/org/example/excel/副本厦门往来清理跟进表-全匹配版 （禹洲泉州）-标识.xlsx";
        EasyExcel.read(fileName1, OtherInfo2.class, new PageReadListener<OtherInfo2>(dataList -> {
            organizeData(dataList);
            cachedDataList.addAll(dataList);
        })).sheet().doRead();
        EasyExcel.read(
                        fileName2,
                        Assistant.class,
                        new PageReadListener<Assistant>(assistantList::addAll)
                ).sheet("往来清理明细表")
                .doRead();
        List<Assistant> realAssistantList = assistantList.stream()
                .filter(item -> "禹洲物业服务有限公司泉州分公司其他应收款-其他其他---泉州海德堡SS:117483:JODV0:SYZ000012".equals(item.getR()))
//                .skip(1)
                .collect(Collectors.toList());
        List<OtherInfo2> result1 = new ArrayList<>();
        List<OtherInfo2> result2 = new ArrayList<>();
        for (int i = 0; i < realAssistantList.size(); i++) {
            Assistant assistant = realAssistantList.get(i);
            System.out.println("当前行：" + (i + 2));
            String z = assistant.getZ();
            if (z == null) {
                System.out.println("z 为null 当前月无需处理");
                continue;
            }
            String projectName = assistant.getR();
            List<OtherInfo2> startCollect = cachedDataList.stream()
                    .filter(item -> item.getZ().equals(projectName))
                    .collect(Collectors.toList());
            List<OtherInfo2> result = doMain(true,
                    cachedDataList,
                    startCollect,
                    assistant.getZ(),
                    projectName,
                    0);
            if (result.size() == startCollect.size() && startCollect.size() != 1) {
                result1.addAll(result);
            } else {
                result2.addAll(result);
            }
        }
        String resultFileName = "模版" + ".xlsx";
        try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo2.class).build();
            List<OtherInfo2> data1 = result2;
            excelWriter.write(data1, writeSheet1);
            WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "未能匹配").head(OtherInfo2.class).build();
            List<OtherInfo2> data2 = result1;
            excelWriter.write(data2, writeSheet2);
        }
    }

    public static List<OtherInfo2> doMain(boolean isOpenFindUp,
                                          List<OtherInfo2> cachedDataList,
                                          List<OtherInfo2> startCollect,
                                          String z,
                                          String originProjectName,
                                          Integer level) {
        // 先处理一下余额等于某个借款的时候
        // 拿到最终余额
        BigDecimal balance;
        try {
            balance = new BigDecimal(z.replace(",", "").replace("(", "").replace(")", ""));
        } catch (Exception e) {
            balance = BigDecimal.ZERO;
        }
        // 排序
        List<OtherInfo2> sortedStartCollect = disSameX(startCollect, originProjectName);
        List<OtherInfo2> OtherInfo2s = new ArrayList<>();
        OtherInfo2 temporaryResult = null;
        if (z.contains("(") || z.contains(")")) {
            // 余额为负去贷找
            List<OtherInfo2> first = new ArrayList<>();
            boolean flag = true;
            for (OtherInfo2 OtherInfo2 : sortedStartCollect) {
                if (flag && OtherInfo2.getW() != null && balance.compareTo(OtherInfo2.getW()) == 0) {
                    temporaryResult = OtherInfo2;
                    flag = false;
                } else {
                    first.add(OtherInfo2);
                }
            }
            if (first.size() != sortedStartCollect.size()) {
                // 证明已经被过滤
                OtherInfo2s = doFilter(first);
            }
        } else {
            // 余额为正去借找
            List<OtherInfo2> first = new ArrayList<>();
            boolean flag = true;
            for (OtherInfo2 OtherInfo2 : sortedStartCollect) {
                if (flag && OtherInfo2.getV() != null && balance.compareTo(OtherInfo2.getV()) == 0) {
                    temporaryResult = OtherInfo2;
                    flag = false;
                } else {
                    first.add(OtherInfo2);
                }
            }
            if (first.size() != sortedStartCollect.size()) {
                // 证明已经被过滤
                OtherInfo2s = doFilter(first);
            }
        }
        List<OtherInfo2> finalResult;
        if (OtherInfo2s.isEmpty() && temporaryResult != null) {
            // 证明就是正确结果
            finalResult = new ArrayList<>();
            finalResult.add(temporaryResult);
        } else {
            finalResult = doFilter(sortedStartCollect);
        }
        // 打印已经配置的信息
        // 拿到结果，对结果进行向上查找
        List<OtherInfo2> result = new ArrayList<>();
        if (level == 0) {
            for (int i = 0; i < finalResult.size(); i++) {
                OtherInfo2 parentItem = finalResult.get(i);
                parentItem.setLevel(level + 1);
                parentItem.setNo(String.valueOf(i + 1));
                String form = parentItem.getS();
                result.add(parentItem);
                // 只有一级的时候进行判断
                if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
                    find(result, cachedDataList, parentItem, originProjectName, level + 1, isOpenFindUp);
                }
            }
        } else {
            for (int i = 0; i < finalResult.size(); i++) {
                OtherInfo2 parentItem = finalResult.get(i);
                parentItem.setLevel(level + 1);
                parentItem.setNo(String.valueOf(i + 1));
                result.add(parentItem);
                find(result, cachedDataList, parentItem, originProjectName, level + 1, isOpenFindUp);
            }
        }
        return result;
    }

    public static void find(List<OtherInfo2> result, List<OtherInfo2> cachedDataList, OtherInfo2 parentItem, String originProjectName, int level, boolean isOpenFindUp) {
        List<OtherInfo2> childList = doUpFilter(cachedDataList, parentItem, originProjectName, level + 1, isOpenFindUp);
        if (childList.size() == 1) {
            // 如果只是返回了一条，证明两种：1他没有子集 || 2他的子集也是一条
            OtherInfo2 child = childList.get(0);
            if (child.getR().equals(parentItem.getR()) && (child.getV() != null ? child.getV().equals(parentItem.getW()) : child.getW().equals(parentItem.getV()))) {
                // 如果凭证一样 && 借贷相抵
                return;
            }
        }
        for (int i1 = 0; i1 < childList.size(); i1++) {
            OtherInfo2 child = childList.get(i1);
            child.setLevel(child.getLevel() == null ? parentItem.getLevel() + 1 : child.getLevel());
            child.setNo(parentItem.getNo() + "-" + (i1 + 1));
        }
        result.addAll(childList);
    }

    public static List<OtherInfo2> disSameX(List<OtherInfo2> list, String originProjectName) {
        return list.stream()
                .sorted((a, b) -> DateUtil.date(b.getN()).toInstant().compareTo(DateUtil.toInstant(a.getN())))
                .collect(Collectors.groupingBy(OtherInfo2::getR))
                .entrySet()
                .stream()
                .filter(item -> mergeSameX(item.getValue()))
                .flatMap(item -> item.getValue().stream())
                .peek(item -> item.setOriginZ(originProjectName))
                .collect(Collectors.toList());
    }

    private static boolean mergeSameX(List<OtherInfo2> list) {
        // 拿到相同方向的
        Map<String, List<OtherInfo2>> XMap = list.stream().collect(Collectors.groupingBy(OtherInfo2::getX));
        Set<String> keySet = XMap.keySet();
        if (keySet.size() != 1) {
            // 证明有多种方向,直接放行
            return true;
        }
        return XMap.get(keySet.toArray()[0]).stream().reduce(BigDecimal.ZERO, (prev, curr) -> {
            prev = prev.add(curr.getV() == null ? BigDecimal.ZERO : curr.getV().equals(BigDecimal.ZERO.stripTrailingZeros()) ? BigDecimal.ZERO : curr.getV());
            prev = prev.subtract(curr.getW() == null ? BigDecimal.ZERO : curr.getW().equals(BigDecimal.ZERO) ? BigDecimal.ZERO : curr.getW());
            return prev;
        }, (l, r) -> l).compareTo(BigDecimal.ZERO) != 0;
    }


    private static List<OtherInfo2> doUpFilter(List<OtherInfo2> cachedDataList,
                                               OtherInfo2 item,
                                               String originProjectName,
                                               Integer level,
                                               boolean isOpenFindUp) {
        if (!isOpenFindUp) {
            return new ArrayList<>();
        }
        if (level > 10) {
            // 级别超过10次
            item.setErrorMsg("循环超过10次");
            return new ArrayList<>();
        }
        BigDecimal v = item.getV();
        BigDecimal w = item.getW();
        List<OtherInfo2> collect = cachedDataList.stream()
                // 凭证号相等 && 编号不能相等 && 合并字段不相同
                .filter(temp -> temp.getR().equals(item.getR())
                        && ((v != null && temp.getW() != null && v.compareTo(temp.getW()) == 0) || w != null && temp.getV() != null && w.compareTo(temp.getV()) == 0)
                        && !temp.getA().equals(item.getA())
//                        && temp.getX().equals(item.getX())
                        && !temp.getZ().equals(item.getZ()))
                .collect(Collectors.toList());
        List<OtherInfo2> result = new ArrayList<>();
        if (collect.isEmpty()) {
            // 证明只能找到自己
        } else {
            if (collect.size() > 1) {
                item.setErrorMsg("存在多个匹配情况");
                return result;
            }
            // 同一凭证下，借贷需要抵消的数据
            List<OtherInfo2> otherInfo2s = new ArrayList<>();
            for (OtherInfo2 otherInfo2 : collect) {
                List<OtherInfo2> collect1 = cachedDataList.stream()
                        .filter(i -> i.getZ().equals(otherInfo2.getZ()))
                        .sorted((a, b) -> {
                            int i = DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN()));
                            if (i == 0) {
                                return a.getQ() - b.getQ();
                            }
                            return i;
                        })
                        .collect(Collectors.toList());
                // 先找当前数据借贷抵消的数据
                List<OtherInfo2> findOne = disSameX(collect1, originProjectName)
                        .stream()
                        .filter(i ->
                                (otherInfo2.getV() != null && otherInfo2.getV().equals(i.getW())) || (otherInfo2.getW() != null && otherInfo2.getW().equals(i.getV()))
                                        && !otherInfo2.getZ().equals(item.getZ())
                        )
                        .collect(Collectors.toList());
                List<OtherInfo2> otherInfo2sup = new ArrayList<>();
                List<OtherInfo2> otherInfo2slow = new ArrayList<>();
                if (findOne.isEmpty()) {
                    // 没有找到就开始找
                    int indexOf = collect1.indexOf(otherInfo2);
                    List<OtherInfo2> findCollect1 = collect1.subList(0, indexOf + 1);
                    // 计算上半部和是否为0
                    BigDecimal collect1Sum = findCollect1.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(curr.getV() != null ? curr.getV() : BigDecimal.ZERO).subtract(curr.getW() != null ? curr.getW() : BigDecimal.ZERO), (l, r) -> l);
                    if (collect1Sum.compareTo(BigDecimal.ZERO) == 0) {
                        otherInfo2sup = doMain(
                                true,
                                cachedDataList,
                                collect1.subList(0, indexOf),
                                otherInfo2.getV() != null ? String.valueOf(otherInfo2.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo2.getW()).toString(),
                                originProjectName,
                                level
                        );
                        if (otherInfo2sup.isEmpty() && indexOf != (collect1.size() - 1)) {
                            otherInfo2slow = doMain(
                                    true,
                                    cachedDataList,
                                    collect1.subList(indexOf + 1, collect1.size()),
                                    otherInfo2.getV() != null ? String.valueOf(otherInfo2.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo2.getW()).toString(),
                                    originProjectName,
                                    level
                            );
                        }
                    } else {
                        otherInfo2sup.add(otherInfo2);
                    }
                } else {
                    otherInfo2sup = doMain(
                            true,
                            cachedDataList,
                            findOne.stream().skip((long) findOne.size() - 1).collect(Collectors.toList()),
                            otherInfo2.getV() != null ? String.valueOf(otherInfo2.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo2.getW()).toString(),
                            originProjectName,
                            level
                    );
                }
                otherInfo2s.addAll(otherInfo2sup.isEmpty() ? otherInfo2slow : otherInfo2sup);
                result.addAll(otherInfo2s);
            }
        }
        return result;
    }

    private static List<OtherInfo2> doFilter(List<OtherInfo2> startCollect) {
        List<OtherInfo2> collect = startCollect
                .stream()
                .sorted((a, b) -> DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN())))
                .collect(Collectors.toList());

        int size = collect.size();
        if (size == 0) {
            return new ArrayList<>();
        }
        System.out.println("一共检索到" + size + "明细数据");
        HashMap<String, List<OtherInfo2>> vMap = new HashMap<>();
        HashMap<String, List<OtherInfo2>> WMap = new HashMap<>();
        // 整理数据，并拿到起始索引
//        organizeData(collect);
        int start = findStart(collect);
        for (int i1 = start; i1 < collect.size(); i1++) {
            OtherInfo2 OtherInfo2 = collect.get(i1);
            BigDecimal V = OtherInfo2.getV();
            BigDecimal W = OtherInfo2.getW();
            if (V != null) {
                String realV = String.valueOf(V);
                List<OtherInfo2> list = vMap.getOrDefault(realV, new ArrayList<>());
                list.add(OtherInfo2);
                vMap.put(realV, list);
            } else {
                OtherInfo2.setV(null);
            }
            if (W != null) {
                String realW = String.valueOf(W);
                List<OtherInfo2> list = WMap.getOrDefault(realW, new ArrayList<>());
                list.add(OtherInfo2);
                WMap.put(realW, list);
            } else {
                OtherInfo2.setW(null);
            }
        }
        List<OtherInfo2> itemResult = new ArrayList<>();
        for (Map.Entry<String, List<OtherInfo2>> entry : vMap.entrySet()) {
            String VKey = entry.getKey();
//            String VTargetKey = String.valueOf(0d - Double.parseDouble(VKey));
            if (WMap.get(VKey) == null) {
                itemResult.addAll(entry.getValue());
            } else {
                List<OtherInfo2> VList = entry.getValue();
                List<OtherInfo2> WList = WMap.get(entry.getKey());
                if (VList.size() != WList.size()) {
                    // 存在无法完全抵消的情况
                    // 1，取时间尾部的数据
                    // 2, 若时间相同，则以凭证号为排序，取尾部数据
                    itemResult.addAll(
                            VList.stream().skip(WList.size())
                                    .sorted(Comparator.comparingInt(OtherInfo2::getQ))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }

        for (Map.Entry<String, List<OtherInfo2>> entry : WMap.entrySet()) {
            if (vMap.get(entry.getKey()) == null) {
                itemResult.addAll(entry.getValue());
            } else {
                List<OtherInfo2> WList = entry.getValue();
                List<OtherInfo2> VList = vMap.get(entry.getKey());
                if (VList.size() != WList.size()) {
                    // 存在无法完全抵消的情况
                    // 1，取时间尾部的数据
                    // 2, 若时间相同，则以凭证号为排序，取尾部数据
                    itemResult.addAll(
                            WList.stream().skip(VList.size())
                                    .sorted(Comparator.comparingInt(OtherInfo2::getQ))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }
        return itemResult;
    }

    public static void organizeData(List<OtherInfo2> collect) {
        for (org.example.func_two.OtherInfo2 OtherInfo2 : collect) {
            try {
                // 借方金额
                BigDecimal V = OtherInfo2.getV() == null ? null : OtherInfo2.getV().equals(BigDecimal.ZERO) ? null : OtherInfo2.getV();
                // 贷方金额
                BigDecimal W = OtherInfo2.getW() == null ? null : OtherInfo2.getW().equals(BigDecimal.ZERO) ? null : OtherInfo2.getW();
                // 方向
                String x = OtherInfo2.getX();
                if (W != null) {
                    if (x.equals("贷")) {
                        if (V == null && W.compareTo(BigDecimal.ZERO) < 0) {
                            // 说明是以负数的形式表示借
                            V = BigDecimal.ZERO.subtract(W);
                            W = null;
                        } else {
                            V = null;
                        }
                    }
                }
                if (V != null) {
                    // 借
                    if (W == null && V.compareTo(BigDecimal.ZERO) < 0) {
                        // 说明是以负数的形式表示借
                        W = BigDecimal.ZERO.subtract(V);
                        V = null;
                    } else {
                        W = null;
                    }
                }
                OtherInfo2.setV(V);
                OtherInfo2.setW(W);
            } catch (Exception e) {
                System.out.println("解析出现异常,当前解析对象为：" + OtherInfo2);
                throw e;
            }
        }
    }

    private static Integer findStart(List<OtherInfo2> collect) {
        int start = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (int n = 0; n < collect.size(); n++) {
            OtherInfo2 OtherInfo2 = collect.get(n);
            // 借方金额
            BigDecimal V = OtherInfo2.getV() == null ? null : OtherInfo2.getV().equals(BigDecimal.ZERO) ? null : OtherInfo2.getV();
            // 贷方金额
            BigDecimal W = OtherInfo2.getW() == null ? null : OtherInfo2.getW().equals(BigDecimal.ZERO) ? null : OtherInfo2.getW();
            sum = sum.add(V == null ? BigDecimal.ZERO : V);
            sum = sum.subtract(W == null ? BigDecimal.ZERO : W);
            if (sum.compareTo(BigDecimal.ZERO) == 0) {
                start = n + 1;
            }
        }
        return start;
    }

    @Override
    public void invoke(Info info, AnalysisContext analysisContext) {
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}