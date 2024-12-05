package org.example.寻找等级;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.Data;
import org.example.enitty.Assistant;
import org.example.utils.*;
import org.example.寻找等级.old_excel.MappingCustomerExcel;
import org.example.寻找等级.old_excel.MappingNccToFmsExcel;
import org.example.寻找等级.old_excel.MappingProjectExcel;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TODO 只对B和C的进行对比，如果是系统的就不往上追
 * TODO 并且要展示最初的项目名称
 */
@Service
public class FindLevel {
    @Resource
    private FindNccLangJiLevel findNccLangJiLevel;
    @Resource
    private FindNccYuZhouLevel findNccYuZhouLevel;

    public  List<OtherInfo3> doMain(boolean isOpenFindUp,
                                    boolean isFindAll,
                                    boolean findBySql,
                                    List<OtherInfo3> oldCachedDataList,
                                    List<OtherInfo3> cachedDataList,
                                    List<OtherInfo3> startCollect,
                                    String z,
                                    String originCode) {

        List<OtherInfo3> finalResult;
        if (isFindAll){
            finalResult = startCollect;
        }else {
            finalResult = LevelUtil.FindFirstLevel(startCollect,z);
        }
        Deque<OtherInfo3> deque = new LinkedList<>();
        List<OtherInfo3> result = new ArrayList<>();
        for (int i = 0; i < finalResult.size(); i++) {
            OtherInfo3 otherInfo3 = finalResult.get(i);
//            int level = 1;
//            otherInfo3.setLevel(otherInfo3.getLevel() == null ? i : otherInfo3.getLevel());
//            otherInfo3.setNo(otherInfo3.getNo()==null ? String.valueOf(otherInfo3.getLevel()) : otherInfo3.getNo()+"-"+(i+1));
            otherInfo3.setLevel(1);
            otherInfo3.setNo(String.valueOf(i+1));
            // 计算余额
            BigDecimal lastBalance;
            if (i > 0){
                lastBalance = finalResult.get(i-1).getBalanceSum();
            }else {
                lastBalance = BigDecimal.ZERO;
            }
            otherInfo3.setBalanceSum(lastBalance.add(CommonUtil.getBigDecimalValue(otherInfo3.getV()).subtract(CommonUtil.getBigDecimalValue(otherInfo3.getW()))));
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
                            Set<OtherInfo3> childList = find(oldCachedDataList,cachedDataList, parentItem, originCode, level, isOpenFindUp, findBySql);
                            pushChild(childList,parentItem,deque,level);
                        }
                    } else {
                        judgeJoin(result,parentItem,no,level);
                        Set<OtherInfo3> childList = find(oldCachedDataList,cachedDataList, parentItem, originCode, level, isOpenFindUp, findBySql);
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

    public  Set<OtherInfo3> find(List<OtherInfo3> oldCachedDataList,List<OtherInfo3> cachedDataList, OtherInfo3 parentItem, String originCode, int level, boolean isOpenFindUp,Boolean findBySql) {
        List<OtherInfo3> list = "老系统".equals(parentItem.getSystemForm())  ? oldCachedDataList : cachedDataList;
        int thisLevel = level+1;
        Set<OtherInfo3> childList = doUpFilter(list, parentItem, originCode, thisLevel, isOpenFindUp,findBySql);
        if (childList.size() == 1) {
            // 如果只是返回了一条，证明两种：1 他就是和父类能够借贷相抵 || 2他的子集也是一条
            Iterator<OtherInfo3> iterator = childList.iterator();
            OtherInfo3 child = iterator.next();
            if (child.getR().equals(parentItem.getR()) && (child.getV() != null ? child.getV().equals(parentItem.getW()) : child.getW().equals(parentItem.getV()))) {
                // 如果凭证一样 && 借贷相抵
                return new HashSet<>();
            }
        }else if (childList.isEmpty() && isOpenFindUp ){
            // 如果没办法找到子类，那么就去老系统找
            // 通过公司名称判断是哪个系统
            String companyName = parentItem.getCompanyName();
            String companyType = CompanyTypeConstant.mapping.get(companyName);
            if (companyType.equals(CompanyTypeConstant.LANG_JI)){
                // 朗基逻辑
                // 如果是老系统的数据就不需要判断是不是期初导入
                // 不是老系统就得判断
                if (Objects.equals(parentItem.getSystemForm(),"老系统") || (parentItem.getJournalExplanation() != null && (
                        parentItem.getJournalExplanation().contains("期初数据导入")
                                || parentItem.getJournalExplanation().contains("发生额数据导入")
                ))){
                    // 老系统1级
                    return findNccLangJi(parentItem);
                }
            }else if (companyType.equals(CompanyTypeConstant.YU_ZHOU)){
                // 禹州逻辑
                return findNccYuZhouLevel.findNccYuZhouList(parentItem);
            }else if (companyType.equals(CompanyTypeConstant.ZHONG_NAN)){
                // 中南

            }else {
                throw new RuntimeException("不存在的公司类型");
            }
        }
        return childList;
    }

    public Set<OtherInfo3> findNccLangJi(OtherInfo3 parentItem){
        if (parentItem.getSystemForm().equals("老系统")){
            // 如果是老系统的，直接放行
            // 因为当老系统向上查找的过程中会存在找不到上级的情况，childList 为空，就会走到这个逻辑，但是这个逻辑是 新系统找老系统的方法
            return new HashSet<>();
        }
        // 找一级的余额组成
        Set<OtherInfo3> otherInfo3s = findNccLangJiLevel.findNccLangJiList(parentItem);
//        otherInfo3s.forEach(item -> item.setSystemForm("老系统"));
        // 余额相等证明找到了
        // 校验余额是否一致
        return otherInfo3s;
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
                List<OtherInfo3> findOne = LevelUtil.disSameX(collect1)
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
                        otherInfo3Sup = LevelUtil.FindFirstLevel(
                                collect1.subList(0, indexOf),
                                otherInfo3.getV() != null ? String.valueOf(otherInfo3.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo3.getW()).toString()
                        );
                        if (otherInfo3Sup.isEmpty() && indexOf != (collect1.size() - 1)) {
                            otherInfo3Slow = LevelUtil.FindFirstLevel(
                                    collect1.subList(indexOf + 1, collect1.size()),
                                    otherInfo3.getV() != null ? String.valueOf(otherInfo3.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo3.getW()).toString()
                            );
                        }
                    } else {
                        otherInfo3Sup.add(otherInfo3);
                    }
                } else {
                    otherInfo3Sup = LevelUtil.FindFirstLevel(
                            findOne.stream().skip((long) findOne.size() - 1).collect(Collectors.toList()),
                            otherInfo3.getV() != null ? String.valueOf(otherInfo3.getV().doubleValue()) : BigDecimal.ZERO.subtract(otherInfo3.getW()).toString()
                    );
                }
                otherInfo3s.addAll(otherInfo3Sup.isEmpty() ? otherInfo3Slow : otherInfo3Sup);
                result.addAll(otherInfo3s);
            }
        }
        return result;
    }








}