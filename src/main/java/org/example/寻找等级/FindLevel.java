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
public class FindLevel {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 80000;
    @Resource
    private SqlUtil sqlUtil;


    public static void main(String[] args) {
        List<OtherInfo3> cachedDataList = new ArrayList<>(BATCH_COUNT);
        List<Assistant> assistantList = new ArrayList<>();
        String fileName1 = "src/main/java/org/example/excel/往来科目明细.xlsx";
        String fileName2 = "src/main/java/org/example/excel/副本厦门往来清理跟进表-全匹配版 （禹洲泉州）-标识.xlsx";
        EasyExcel.read(fileName1, OtherInfo3.class, new PageReadListener<OtherInfo3>(dataList -> {
            for (OtherInfo3 item : dataList) {
                new FindLevel().organizeDataItem(item);
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

    public  List<OtherInfo3> doMain(boolean isOpenFindUp,
                                    boolean isFindAll,
                                    boolean findBySql,
                                    List<OtherInfo3> cachedDataList,
                                    List<OtherInfo3> startCollect,
                                    String z,
                                    String originCode) {

        List<OtherInfo3> finalResult;
        if (isFindAll){
            finalResult = startCollect;
        }else {
            finalResult = FindFirstLevel(startCollect,z,originCode);
        }
        Deque<OtherInfo3> deque = new LinkedList<>();
        List<OtherInfo3> result = new ArrayList<>();
        for (int i = 0; i < finalResult.size(); i++) {
            OtherInfo3 otherInfo3 = finalResult.get(i);
//            int level = 1;
            otherInfo3.setLevel(1);
            otherInfo3.setNo(String.valueOf(i));
            // 遍历一级
            deque.push(otherInfo3);
            // 准备进行迭代遍历
            while (!deque.isEmpty()){
                // 对当前层进行遍历
                int dequeSize = deque.size();
                for (int dequeIndex = 0; dequeIndex < dequeSize; dequeIndex++) {
                    OtherInfo3 parentItem = deque.poll();
                    assert parentItem != null;
                    int level = parentItem.getLevel();
                    String no = parentItem.getNo() == null ? String.valueOf(i+1) : parentItem.getNo();
                    if (level == 1) {
                        judgeJoin(result,parentItem,no,level);
                        String form = parentItem.getS();
                        // 只有一级的时候进行判断
                        if (form.equals("电子表格") || form.equals("人工") || form.equals("自动复制")) {
                            Set<OtherInfo3> childList = find(cachedDataList, parentItem, originCode, level, isOpenFindUp, findBySql);
                            pushChild(childList,parentItem,deque,level);
                        }
                    } else {
                        judgeJoin(result,parentItem,no,level);
                        Set<OtherInfo3> childList = find(cachedDataList, parentItem, originCode, level, isOpenFindUp, findBySql);
                        pushChild(childList,parentItem,deque,level);
                    }
                }
            }
        }
        return result;
    }

    public static void judgeJoin(List<OtherInfo3> result,OtherInfo3 parentItem,String no,Integer level){
        if (result.isEmpty() || !result.contains(parentItem)){
            parentItem.setLevel(level);
            parentItem.setNo(no);
            result.add(parentItem);
        }
    }

    public static List<OtherInfo3> FindFirstLevel(List<OtherInfo3> startCollect, String z, String originCode){
        // 解析金额
        BigDecimal balance = covertZToBalance(z);
        // 消除同一凭证能够借贷相抵的数据
        List<OtherInfo3> sortedStartCollect = disSameX(startCollect, originCode);
        // 先找一下能够直接借贷相抵的数据
        FindFirstListResult firstListResult = findFirstList(z, balance, sortedStartCollect);
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

    public  Set<OtherInfo3> find(List<OtherInfo3> cachedDataList, OtherInfo3 parentItem, String originCode, int level, boolean isOpenFindUp,Boolean findBySql) {
        Set<OtherInfo3> childList = doUpFilter(cachedDataList, parentItem, originCode, level+1, isOpenFindUp,findBySql);
        if (childList.size() == 1) {
            // 如果只是返回了一条，证明两种：1 他就是和父类能够借贷相抵 || 2他的子集也是一条
            Iterator<OtherInfo3> iterator = childList.iterator();
            OtherInfo3 child = iterator.next();
            if (child.getR().equals(parentItem.getR()) && (child.getV() != null ? child.getV().equals(parentItem.getW()) : child.getW().equals(parentItem.getV()))) {
                // 如果凭证一样 && 借贷相抵
                return new HashSet<>();
            }
        }else if (childList.isEmpty()){
            // 如果没办法找到子类，那么就去老系统找
            // 朗基逻辑
            // 拿到账户组合进行拆分
            String[] z = parentItem.getZ().split("\\.");



        }
        return childList;
    }

    public void pushChild(Set<OtherInfo3> childSet,OtherInfo3 parentItem,Deque<OtherInfo3> deque,Integer parentLevel){
        List<OtherInfo3> childList = new ArrayList<>(childSet);
        if (!deque.isEmpty()){
            // 如果有值，证明可能是上一级
            for (int i1 = childList.size()-1; i1 >= 0; i1--) {
                OtherInfo3 child = childList.get(i1);
                child.setNo(parentItem.getNo() + "-" + (i1 + 1));
                child.setLevel(parentLevel+1);
                deque.push(child);
            }
        }else {
            for (int i1 = 0; i1 < childList.size(); i1++) {
                OtherInfo3 child = childList.get(i1);
                child.setNo(parentItem.getNo() + "-" + (i1 + 1));
                child.setLevel(parentLevel+1);
                deque.add(child);
            }
        }
    }

    @Data
    public static class FindFirstListResult {
        private OtherInfo3 temporaryResult;
        List<OtherInfo3> otherInfo3s;
        public FindFirstListResult(){
            this.temporaryResult = null;
            this.otherInfo3s = new ArrayList<>();
        }
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

    public static FindFirstListResult findFirstList(String z, BigDecimal balance, List<OtherInfo3> sortedStartCollect){
        FindFirstListResult result = new FindFirstListResult();
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

    public static List<OtherInfo3> disSameX(List<OtherInfo3> list, String originCode) {
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


    private  Set<OtherInfo3> doUpFilter(List<OtherInfo3> cachedDataList,
                                        OtherInfo3 item,
                                        String originCode,
                                        Integer level,
                                        boolean isOpenFindUp,
                                        boolean findBySql) {
        if (!isOpenFindUp) {
//            return new ArrayList<>();
            return new HashSet<>();
        }
        if (level > 10) {
            // 级别超过10次
            item.setErrorMsg("循环超过10次");
//            return new ArrayList<>();
            return new HashSet<>();
        }
        BigDecimal v = item.getV();
        BigDecimal w = item.getW();
        List<OtherInfo3> collect;
//        if(findBySql){
//            String findSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE z.\"有效日期\" = TO_DATE('"+item.getN()+"','yyyy-mm-dd hh24:mi:ss') AND z.\"单据编号\" = "+item.getQ()+" AND z.\"账户组合\" <> '"+item.getZ()+"' AND z.\"交易对象\" <> '"+item.getTransactionId()+"'";
//            String appendSql = v != null ? " AND z.\"输入贷方\" = " + v : " AND z.\"输入借方\" = " + w;
//            collect = sqlUtil.find(findSql+appendSql).stream().peek(this::organizeDataItem).collect(Collectors.toList());
//        }else {
        collect = cachedDataList.stream()
                // 凭证号相等 && 编号不能相等 && 合并字段不相同
                .filter(temp -> temp.getR().equals(item.getR())
                                && ((v != null && temp.getW() != null && v.compareTo(temp.getW()) == 0) || w != null && temp.getV() != null && w.compareTo(temp.getV()) == 0)
                                && !temp.equals(item)
//                            && !temp.getA().equals(item.getA())
//                        && temp.getX().equals(item.getX())
//                            && !temp.getZ().equals(item.getZ())
                                && !temp.getOnlySign().equals(item.getOnlySign())
                        // TODO 交易对象是否也需要不同
//                            && ((temp.getTransactionId() == null && item.getTransactionId() == null ) || !Objects.equals(temp.getTransactionId(),item.getTransactionId()))
                )
                .collect(Collectors.toList());
//        }
        Set<OtherInfo3> result = new HashSet<>();
        if (collect.isEmpty()) {
        } else {
            if (collect.size() > 1) {
                item.setErrorMsg("存在多个匹配情况");
                return result;
            }
            // 同一凭证下，借贷需要抵消的数据
            List<OtherInfo3> otherInfo3s = new ArrayList<>();
            // 往下找下一个之前先添加自己
            for (OtherInfo3 otherInfo3 : collect) {
                List<OtherInfo3> collect1;
//                if (findBySql){
//                    String findSql = "SELECT * FROM ZDPROD_EXPDP_20241120 z WHERE  z.\"账户组合\" = '"+item.getZ()+"'";
//                    if (item.getTransactionId() != null){
//                        String appendSql = "AND z.\"交易对象\" = '"+item.getTransactionId()+"'";
//                        collect1 = sqlUtil.find(findSql+appendSql).stream().peek(this::organizeDataItem).collect(Collectors.toList());
//                    }else {
//                        collect1 = sqlUtil.find(findSql);
//                    }
//                }else {
                // 展开同一凭证号能借贷相抵的项目名称
                collect1 = cachedDataList.stream()
//                            .filter(i -> i.getZ().equals(otherInfo3.getZ()))
                        .filter(i -> i.getOnlySign().equals(otherInfo3.getOnlySign()))
                        .sorted((a, b) -> {
                            int i = DateUtil.date(a.getN()).toInstant().compareTo(DateUtil.toInstant(b.getN()));
                            if (i == 0) {
                                return a.getQ() - b.getQ();
                            }
                            return i;
                        })
                        .collect(Collectors.toList());
//                }

                // 先找当前数据借贷抵消的数据
                List<OtherInfo3> findOne = disSameX(collect1, originCode)
                        .stream()
                        .filter(i ->
                                        (otherInfo3.getV() != null && otherInfo3.getV().equals(i.getW())) || (otherInfo3.getW() != null && otherInfo3.getW().equals(i.getV()))
//                                        && !otherInfo3.getZ().equals(item.getZ())
                                                && !otherInfo3.getOnlySign().equals(item.getOnlySign())
                        )
                        .collect(Collectors.toList());
                List<OtherInfo3> otherInfo3Sup = new ArrayList<>();
                List<OtherInfo3> otherInfo3Slow = new ArrayList<>();
                if (findOne.isEmpty()) {
                    // 没有找到就开始找
                    int indexOf = collect1.indexOf(otherInfo3);
                    List<OtherInfo3> findCollect1 = collect1.subList(0, indexOf + 1);
                    // 计算上半部和是否为0
                    BigDecimal collect1Sum = findCollect1.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(curr.getV() != null ? curr.getV() : BigDecimal.ZERO).subtract(curr.getW() != null ? curr.getW() : BigDecimal.ZERO), (l, r) -> l);
                    if (collect1Sum.compareTo(BigDecimal.ZERO) == 0) {
                        otherInfo3Sup = FindFirstLevel(
                                collect1.subList(0, indexOf),
                                otherInfo3.getV() != null ? String.valueOf(otherInfo3.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo3.getW()).toString(),
                                originCode
                        );
                        if (otherInfo3Sup.isEmpty() && indexOf != (collect1.size() - 1)) {
                            otherInfo3Slow = FindFirstLevel(
                                    collect1.subList(indexOf + 1, collect1.size()),
                                    otherInfo3.getV() != null ? String.valueOf(otherInfo3.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo3.getW()).toString(),
                                    originCode
                            );
                        }
                    } else {
                        otherInfo3Sup.add(otherInfo3);
                    }
                } else {
                    otherInfo3Sup = FindFirstLevel(
                            findOne.stream().skip((long) findOne.size() - 1).collect(Collectors.toList()),
                            otherInfo3.getV() != null ? String.valueOf(otherInfo3.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo3.getW()).toString(),
                            originCode
                    );
                }
                otherInfo3s.addAll(otherInfo3Sup.isEmpty() ? otherInfo3Slow : otherInfo3Sup);
                result.addAll(otherInfo3s);
            }
        }
        return result;
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



    public void organizeDataItem(OtherInfo3 otherInfo3){
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

}