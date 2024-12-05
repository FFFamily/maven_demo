package org.example.utils;

import cn.hutool.core.date.DateUtil;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.OtherInfo3;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class LevelUtil {


    public static List<OtherInfo3> FindFirstLevel(List<OtherInfo3> startCollect, String z){
        // 解析金额
        BigDecimal balance = covertZToBalance(z);
        // 消除同一凭证能够借贷相抵的数据
        List<OtherInfo3> sortedStartCollect = disSameX(startCollect);
        // 先找一下能够直接借贷相抵的数据
        FindLevel.FindFirstListResult firstListResult = findFirstList(z, balance, sortedStartCollect);
        List<OtherInfo3> otherInfo3s =firstListResult.getOtherInfo3s();
        OtherInfo3 temporaryResult = firstListResult.getTemporaryResult();
        List<OtherInfo3> result;
        // 能找到直接相抵就停止找
        if (otherInfo3s.isEmpty() && temporaryResult != null) {
            result = new ArrayList<>();
            result.add(temporaryResult);
        } else {
            // 找不到就得开始过滤查找
            result = doFilter(sortedStartCollect);
        }
        return result;
    }
    public static FindLevel.FindFirstListResult findFirstList(String z, BigDecimal balance, List<OtherInfo3> sortedStartCollect){
        FindLevel.FindFirstListResult result = new FindLevel.FindFirstListResult();
        if (z.contains("(") || z.contains(")")) {
            // 余额为负去贷找
            List<OtherInfo3> first = new ArrayList<>();
            boolean flag = true;
            for (OtherInfo3 OtherInfo3 : sortedStartCollect) {
                if (flag && OtherInfo3.getW() != null && balance.compareTo(OtherInfo3.getW()) == 0) {
                    result.setTemporaryResult(OtherInfo3);
                    flag = false;
                } else {
                    first.add(OtherInfo3);
                }
            }
            if (first.size() != sortedStartCollect.size()) {
                // 证明已经被过滤
                result.setOtherInfo3s(doFilter(first));
            }
        } else {
            // 余额为正去借找
            List<OtherInfo3> first = new ArrayList<>();
            boolean flag = true;
            for (OtherInfo3 OtherInfo3 : sortedStartCollect) {
                if (flag && OtherInfo3.getV() != null && balance.compareTo(OtherInfo3.getV()) == 0) {
                    result.setTemporaryResult(OtherInfo3);
                    flag = false;
                } else {
                    first.add(OtherInfo3);
                }
            }
            if (first.size() != sortedStartCollect.size()) {
                // 证明已经被过滤
                result.setOtherInfo3s(doFilter(first));
            }
        }
        return result;
    }

    public static List<OtherInfo3> disSameX(List<OtherInfo3> list) {
        return list.stream()
                .collect(Collectors.groupingBy(OtherInfo3::getR))
                .entrySet()
                .stream()
                .filter(item -> mergeSameX(item.getValue()))
                .flatMap(item -> item.getValue().stream())
                .sorted((a, b) -> DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN())))
//                .peek(item -> item.setOriginZ(originCode))
                .collect(Collectors.toList());
    }

    private static boolean mergeSameX(List<OtherInfo3> list) {
        // 拿到相同方向的
//        Map<String, List<OtherInfo3>> XMap = list.stream().collect(Collectors.groupingBy(OtherInfo3::getX));
//        Set<String> keySet = XMap.keySet();
//        if (keySet.size() != 1) {
//            // 证明有多种方向,直接放行
//            return true;
//        }
//        return XMap.get(keySet.toArray()[0]).stream().reduce(BigDecimal.ZERO, (prev, curr) -> {
//            prev = prev.add(curr.getV() == null ? BigDecimal.ZERO : curr.getV().equals(BigDecimal.ZERO.stripTrailingZeros()) ? BigDecimal.ZERO : curr.getV());
//            prev = prev.subtract(curr.getW() == null ? BigDecimal.ZERO : curr.getW().equals(BigDecimal.ZERO) ? BigDecimal.ZERO : curr.getW());
//            return prev;
//        }, (l, r) -> l).compareTo(BigDecimal.ZERO) != 0;
        return list.stream().reduce(BigDecimal.ZERO, (prev, curr) -> {
            prev = prev.add(curr.getV() == null ? BigDecimal.ZERO : curr.getV().equals(BigDecimal.ZERO.stripTrailingZeros()) ? BigDecimal.ZERO : curr.getV());
            prev = prev.subtract(curr.getW() == null ? BigDecimal.ZERO : curr.getW().equals(BigDecimal.ZERO) ? BigDecimal.ZERO : curr.getW());
            return prev;
        }, (l, r) -> l).compareTo(BigDecimal.ZERO) != 0;
    }

    public static BigDecimal covertZToBalance(String z){
        BigDecimal balance;
        try {
            balance = new BigDecimal(z.replace(",", "").replace("(", "").replace(")", ""));
        } catch (Exception e) {
            balance = BigDecimal.ZERO;
        }
        return balance;
    }

    private static List<OtherInfo3> doFilter(List<OtherInfo3> startCollect) {
        List<OtherInfo3> collect = startCollect
                .stream()
                .sorted((a, b) -> DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN())))
                .collect(Collectors.toList());

        int size = collect.size();
        if (size == 0) {
            return new ArrayList<>();
        }
//        System.out.println("一共检索到" + size + "明细数据");
        HashMap<String, List<OtherInfo3>> vMap = new HashMap<>();
        HashMap<String, List<OtherInfo3>> WMap = new HashMap<>();
        // 整理数据，并拿到起始索引
//        organizeData(collect);
        int start = findStart(collect);
        for (int i1 = start; i1 < collect.size(); i1++) {
            OtherInfo3 OtherInfo3 = collect.get(i1);
            BigDecimal V = OtherInfo3.getV();
            BigDecimal W = OtherInfo3.getW();
            if (V != null) {
                String realV = String.valueOf(V);
                List<OtherInfo3> list = vMap.getOrDefault(realV, new ArrayList<>());
                list.add(OtherInfo3);
                vMap.put(realV, list);
            } else {
                OtherInfo3.setV(null);
            }
            if (W != null) {
                String realW = String.valueOf(W);
                List<OtherInfo3> list = WMap.getOrDefault(realW, new ArrayList<>());
                list.add(OtherInfo3);
                WMap.put(realW, list);
            } else {
                OtherInfo3.setW(null);
            }
        }
        List<OtherInfo3> itemResult = new ArrayList<>();
        for (Map.Entry<String, List<OtherInfo3>> entry : vMap.entrySet()) {
            String VKey = entry.getKey();
//            String VTargetKey = String.valueOf(0d - Double.parseDouble(VKey));
            if (WMap.get(VKey) == null) {
                itemResult.addAll(entry.getValue());
            } else {
                List<OtherInfo3> VList = entry.getValue();
                List<OtherInfo3> WList = WMap.get(entry.getKey());
                if (VList.size() != WList.size()) {
                    // 存在无法完全抵消的情况
                    // 1，取时间尾部的数据
                    // 2, 若时间相同，则以凭证号为排序，取尾部数据
                    itemResult.addAll(
                            VList.stream().skip(WList.size())
                                    .sorted(Comparator.comparingInt(OtherInfo3::getQ))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }

        for (Map.Entry<String, List<OtherInfo3>> entry : WMap.entrySet()) {
            if (vMap.get(entry.getKey()) == null) {
                itemResult.addAll(entry.getValue());
            } else {
                List<OtherInfo3> WList = entry.getValue();
                List<OtherInfo3> VList = vMap.get(entry.getKey());
                if (VList.size() != WList.size()) {
                    // 存在无法完全抵消的情况
                    // 1，取时间尾部的数据
                    // 2, 若时间相同，则以凭证号为排序，取尾部数据
                    itemResult.addAll(
                            WList.stream().skip(VList.size())
                                    .sorted(Comparator.comparingInt(OtherInfo3::getQ))
                                    .collect(Collectors.toList())
                    );
                }
            }
        }
        return itemResult;
    }

    private static Integer findStart(List<OtherInfo3> collect) {
        int start = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (int n = 0; n < collect.size(); n++) {
            OtherInfo3 OtherInfo3 = collect.get(n);
            // 借方金额
            BigDecimal V = OtherInfo3.getV() == null ? null : OtherInfo3.getV().equals(BigDecimal.ZERO) ? null : OtherInfo3.getV();
            // 贷方金额
            BigDecimal W = OtherInfo3.getW() == null ? null : OtherInfo3.getW().equals(BigDecimal.ZERO) ? null : OtherInfo3.getW();
            sum = sum.add(V == null ? BigDecimal.ZERO : V);
            sum = sum.subtract(W == null ? BigDecimal.ZERO : W);
            if (sum.compareTo(BigDecimal.ZERO) == 0) {
                start = n + 1;
            }
        }
        return start;
    }


    public static void organizeDataItem(OtherInfo3 otherInfo3){
        try {
            // 借方金额
            BigDecimal V = otherInfo3.getV() == null ? null : otherInfo3.getV().equals(BigDecimal.ZERO) ? null : otherInfo3.getV();
            // 贷方金额
            BigDecimal W = otherInfo3.getW() == null ? null : otherInfo3.getW().equals(BigDecimal.ZERO) ? null : otherInfo3.getW();
            // 方向
            String x = otherInfo3.getX();
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
            otherInfo3.setV(V);
            otherInfo3.setW(W);

        } catch (Exception e) {
            System.out.println("解析出现异常,当前解析对象为：" + otherInfo3);
            throw e;
        }
    }

}
