package org.example.新老系统;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.Data;
import org.example.enitty.OracleData;
import org.example.enitty.zhong_nan.Step6OldDetailExcel;
import org.example.enitty.zhong_nan.Step6Result1;
import org.example.enitty.zhong_nan.ZNProjectMapping;
import org.example.utils.CommonUtil;
import org.example.utils.CompanyConstant;
import org.example.utils.CoverNewDate;
import org.example.寻找等级.FindNccZhongNanLevel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class Step6 {
    @Resource
    private Step5 step5;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private CoverNewDate coverNewDate;
    @Resource
    private FindNccZhongNanLevel findNccZhongNanLevel;
    @Data
    public static class Step6TestResult{
        List<Step6Result1> result1s;
        List<OracleData> result2s;
        List<Step6OldDetailExcel> result3s;
        List<OracleData> oracleDataList;

        public Step6TestResult(List<Step6Result1> result1s, List<OracleData> result2s, List<Step6OldDetailExcel> result3s,List<OracleData> oracleDataList) {
            this.result1s = result1s;
            this.result2s = result2s;
            this.result3s = result3s;
            this.oracleDataList = oracleDataList;
        }
    }
    public Step6TestResult step6Test(String companyName, Map<String, List<Step6OldDetailExcel>> companyMap){
        List<Step6Result1> result1s = new ArrayList<>();
        List<OracleData> result2s = new ArrayList<>();
        List<Step6OldDetailExcel> result3s = new ArrayList<>();
        String[] split = companyName.split("-");
        String newCompanyName = CompanyConstant.getNewCompanyByOldCompany(split[0]);
//        System.out.println("当前公司为： "+newCompanyName);
//        if (!newCompanyName.equals("唐山中南国际旅游度假物业服务有限责任公司")){
//            return null;
//        }
        List<Step6OldDetailExcel> list = companyMap.get(companyName);
        // 新系统全部数据
        List<OracleData> step5Result = step5.step5Test(newCompanyName)
                .stream()
                .filter(item -> item.get额外字段() == null)
                .filter(item -> {
                    try {
                        String time = item.get期间();
                        String[] split1 = time.split("-");
                        String year = split1[0];
                        int i = Integer.parseInt(year);
                        String month = split1[1];
                        int i1 = Integer.parseInt(month);
                        return i == 2023 && (i1 >= 7 && i1 <= 12);
                    }catch (Exception e){
//                        System.out.println("解析时间出错："+e.getMessage());
                        return true;
                    }
                })
                .peek(item -> {
                    String newProject = getNewProject(item);
                    item.setActualProject(newProject);
                    if (newProject.contains("合同负债") || newProject.contains("预收账款")){
                        item.setMatchProject("合同负债/预收账款");
                    }else {
                        item.setMatchProject(newProject);
                    }
                })
                .filter(item -> isBackProject(item.getActualProject()))
                .collect(Collectors.toList());
        // 将新系统过滤出NCC导入的数据
        List<OracleData> nccstep5Result = step5Result
                .stream()
                .filter(item -> item.get日记账说明().contains("NCC"))
                .collect(Collectors.toList());
        List<OracleData> oracleData = new ArrayList<>();
        // 新系统不含 NCC 导入的数量
        List<OracleData> notWithNcc = step5Result.stream()
                .filter(item -> !item.get日记账说明().contains("NCC"))
                .peek(item -> item.setForm("23年7-12月新系统序时账"))
                .collect(Collectors.toList());
        for (OracleData data : nccstep5Result) {
            if (filterCondition(newCompanyName,data)){
                data.setForm("23年7-12月新系统序时账单独过滤");
                notWithNcc.add(data);
            }else {
                oracleData.add(data);
            }
        }

        oracleData.addAll(addCondition(newCompanyName,step5Result));
        // 按月进行分组
        Map<String, List<Step6OldDetailExcel>> timeOldCollect = list.stream().collect(Collectors.groupingBy(item -> {
            DateTime date = DateUtil.parseDate(item.getTime());
            int year = date.year();
            int month = date.month() + 1;
            return year + "-" + (month > 9 ? month : "0" + month);
        }));
        Map<String, List<OracleData>> timeNewCollect = oracleData.stream().collect(Collectors.groupingBy(OracleData::get期间));
        List<String> timeOldKeyCollect = new ArrayList<>(timeOldCollect.keySet());
        List<String> timeNewKeyCollect = new ArrayList<>(timeNewCollect.keySet());
        // 所有的时间
        List<String> allTimeKey = Stream.of(timeOldKeyCollect, timeNewKeyCollect).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        for (String timeKey : allTimeKey) {
            List<Step6OldDetailExcel>  timeGroupOld = timeOldCollect.getOrDefault(timeKey,new ArrayList<>());
            List<OracleData> timeGroupNew = timeNewCollect.getOrDefault(timeKey,new ArrayList<>());
            Map<String, List<Step6OldDetailExcel>> projectOldMap = timeGroupOld.stream().collect(Collectors.groupingBy(Step6OldDetailExcel::getMatchProject));
            Map<String, List<OracleData>> projectNewMap = timeGroupNew.stream().collect(Collectors.groupingBy(OracleData::getMatchProject));
            List<String> allProjectKey = Stream.of(projectOldMap.keySet(), projectNewMap.keySet()).flatMap(Collection::stream).distinct().collect(Collectors.toList());
            for (String projectKey : allProjectKey) {
                List<Step6OldDetailExcel>  projectOld = projectOldMap.getOrDefault(projectKey,new ArrayList<>());
                List<OracleData> projectNew = projectNewMap.getOrDefault(projectKey,new ArrayList<>());
                BigDecimal oldSum = projectOld.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.getV()).subtract(CommonUtil.getBigDecimalValue(curr.getW()))), (l, r) -> l);
                BigDecimal newSum = projectNew.stream().reduce(BigDecimal.ZERO, (prev, curr) -> prev.add(CommonUtil.getBigDecimalValue(curr.get输入借方()).subtract(CommonUtil.getBigDecimalValue(curr.get输入贷方()))), (l, r) -> l);
                if (oldSum.compareTo(newSum) != 0) {
                    // 两个余额不相等
                    findOld(projectOld,projectNew,result2s,result3s);
//                    findNew(projectOld,projectNew,result2s);
                    Step6Result1 step6Result1 = create(
                            newCompanyName,
                            timeKey,
                            projectOld.stream().map(Step6OldDetailExcel::getActualProject).distinct().collect(Collectors.joining("、")),
                            projectNew.stream().map(OracleData::getActualProject).distinct().collect(Collectors.joining("、")),
                            oldSum,
                            newSum,
                            projectKey);
                    step6Result1.setRemark("余额不相等");
                    result1s.add(step6Result1);
                }else {
                    Step6Result1 step6Result1 = create(
                            newCompanyName,
                            timeKey,
                            projectOld.stream().map(Step6OldDetailExcel::getActualProject).distinct().collect(Collectors.joining("、")),
                            projectNew.stream().map(OracleData::getActualProject).distinct().collect(Collectors.joining("、")),
                            oldSum,
                            newSum,
                            projectKey);
                    result1s.add(step6Result1);
                }
                result2s.addAll(projectNew);
                result3s.addAll(projectOld);
            }
        }
//        result2s.stream().filter(item -> "和旧系统余额不相等".equals(item.get备注())).forEach(item ->{
//            item.setForm("新系统和旧系统余额不相等保留数据");
//            notWithNcc.add(item);
//        });
//        result2s.stream().filter(item -> "多余数据".equals(item.get备注())).forEach(item ->{
//            item.setForm("新系统多余数据");
//            notWithNcc.add(item);
//        });
        result2s.stream().filter(item -> item.getRemark() ==null || !item.getRemark().equals("匹配成功")).forEach(notWithNcc::add);
        return new Step6TestResult(
                result1s,
                result2s,
                result3s,
                notWithNcc
        );
    }

    private Collection<? extends OracleData> addCondition(String newCompanyName, List<OracleData> step5Result) {
        if (newCompanyName.equals("江苏中南物业服务有限公司梅州分公司")){
            return step5Result.stream()
                    .filter(item ->item.get日记账说明().contains("ZZTY2023110110151本地业财发起待确认事项：梅州雅居乐10.30-10.31周报、应收冲抵单")
                    || item.get日记账说明().contains("ZZTY2023103010321本地业财发起待确认事项：梅州雅居乐10.1-10.29周报、应收冲抵单adi"))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private boolean filterCondition(String companyName,OracleData oracleData){
        if (companyName.equals("江苏中南物业服务有限公司天津分公司")){
            return  oracleData.get日记账说明().equals("FYGD2023122610021_前期NCC凭证-冲销22年底计提审计费")
                    || oracleData.get日记账说明().equals("FMS跑的计提与NCC重复，冲回-ZZTY2023092810121");
        }else if (companyName.equals("唐山中南国际旅游度假物业服务有限责任公司")){
            return oracleData.get日记账说明().contains("YGCB2023120510075总账通用计提：NCC11月导入未配置交易对象，补录交易对象");
        }else if (companyName.equals("江苏中南物业服务有限公司")){
            return oracleData.get日记账说明().contains("FYGD2023122610021_前期NCC凭证-冲销22年底计提审计费")
                    || oracleData.get日记账说明().contains("FMS跑的计提与NCC重复，冲回-ZZTY2023092810121");
        }else if (companyName.equals("江苏中南物业服务有限公司嘉兴分公司")){
            return oracleData.get日记账说明().contains("FYGD2024010110094_前期NCC凭证-冲销4-6月手工计提");
        }else if (companyName.equals("江苏中南物业服务有限公司台州分公司")){
            return oracleData.get日记账说明().contains("YGCB2024010210505 总账通用计提：台州分公司202312应收科目调整（冲销前期NCC调整）")
                    || oracleData.get日记账说明().contains("YGCB2023110110585 总账通用计提：台州分公司前期中南单据已于NCC归档待支付（进项已抵扣），由于现从报账系统重新发起支付，导致成本进项重复");
        }else if (companyName.equals("江苏中南物业服务有限公司昆明分公司")){
            return oracleData.get日记账说明().contains("ZZTY2023120210748本地业财发起待确认事项：11月垃圾清运NCC推单调整")
                    || oracleData.get日记账说明().contains("YGCB2024010410337总账通用计提：NCC推送错误：应收客商及应收科目调整");
        }else if (companyName.equals("江苏中南物业服务有限公司泰兴分公司")){
            return oracleData.get日记账说明().contains("ZZTY2023092910010关于对【NCC中计提过的已报销成本费用】的账务冲销处理")
                    || oracleData.get日记账说明().contains("YGCB2023120210370_ncc凭证号2022-12-594#；2022-12-595#冲销往年计提")
                    || oracleData.get日记账说明().contains("FYGD2023112910101_NCC凭证11-23#、12-498#按总部要求调整成本科目");
        }else if (companyName.equals("江苏中南物业服务有限公司东台分公司")){
            return oracleData.get日记账说明().contains("FYGD2023120610006_NCC2022-6-150#、2022-7-76#冲销2022年6.7月份计提成本")
                    || oracleData.get日记账说明().contains("FYGD2023082910023调整：冲销NCC成本计提");
        }else if (companyName.equals("青岛中南物业管理有限公司烟台分公司")){
            return oracleData.get日记账说明().contains("FYGD2023100110051费用账务处理工单：冲销原NCC计提明细表中调整审批单页签");
        }else if (companyName.equals("江苏中南物业服务有限公司淮安分公司")){
            return oracleData.get日记账说明().contains("FYGD2023090110025费用账务处理工单_冲销1-6NCC计提成本")
                    || oracleData.get日记账说明().contains("FYGD2023113010155_冲销原NCC账面暂估计提的3月成本")
                    || oracleData.get日记账说明().contains("JCFS0|YGCB2024010111115总账通用计提:调整NCC映射科目与FMS科目不一致，统一将地产应收归口至应收物业服务款-被收购公司原关联方款项");
        }else if (companyName.equals("江苏中南物业服务有限公司成都分公司")){
            return oracleData.get日记账说明().contains("KQLU0|YGCB2023120310516：总账通用计提：中南成都分公司案场收入NCC与应收管理平台冲平")
                    || oracleData.get日记账说明().contains("YGCB2023110210684:中南一二类问题整改（中南物业垫付，找中南地产结算）NCC导入到其他应付款-应付在建工程及设备款科目，现改到其他应收款")
                    || oracleData.get日记账说明().contains("NCC导入数据调整");
        }else if (companyName.equals("江苏中南物业服务有限公司太仓分公司")){
            return oracleData.get日记账说明().contains("YGCB2024010210752总账通用计提:NCC能耗销项税税率6%调整至13%");
        }else if (companyName.equals("江苏中南物业服务有限公司梅州分公司")){
            return oracleData.get日记账说明().equals("GXZZ2023082910086:2023年8月7日收到梅州市强风艳装饰有限公司退回2021年第4季度垃圾清运重复付款（ncc凭证号：2022.04#53）")
                    || oracleData.get日记账说明().contains("调整科目：梅州23.6.09代扣5月电费（ncc凭证号23.6#15），于23.7月补走流程GCSJ2023071910060")
                    || oracleData.get日记账说明().contains("调整科目：ncc科目映射有误");
        }else if (companyName.equals("江苏中南物业服务有限公司西安分公司")){
            return oracleData.get日记账说明().equals("ZZTY2023080310151冲销NCC原计提成本费用，已入账FMS");
        }else if (companyName.equals("江苏中南物业服务有限公司惠州分公司")){
            return oracleData.get日记账说明().equals("8月ncc提税有误");
        }
        return false;
    }

    private void findOld(List<Step6OldDetailExcel>  projectOld, List<OracleData> projectNew, List<OracleData> result2s, List<Step6OldDetailExcel> result3s){
        // 找到造成差额的明细账
        int oldSize = projectOld.size();
        int newSize = projectNew.size();
        // 先从旧系统出发
//        if (oldSize > newSize) {
        matchOld(projectOld,projectNew,result2s,result3s,oldSize);
//            for (int i = newSize; i < oldSize; i++) {
//                Step6OldDetailExcel data = projectOld.get(i);
//                data.setRemark("多余数据");
//                result3s.add(data);
//            }
//        }else {
//            matchOld(projectOld,projectNew,result3s,oldSize);
//        }
    }

    private void matchOld(List<Step6OldDetailExcel>  projectOld, List<OracleData> projectNew, List<OracleData> result2s, List<Step6OldDetailExcel> result3s, int size){
        Map<String, List<OracleData>> collect = projectNew.stream().collect(Collectors.groupingBy(OracleData::get行说明));
        for (int i = 0; i < size; i++) {
            Step6OldDetailExcel oldData = projectOld.get(i);
            BigDecimal oldBalance = getOldBalance(oldData);
            List<OracleData> newDataList = collect.getOrDefault(oldData.getMatch(),null);
            if (newDataList == null){
                oldData.setRemark("未能匹配数据");
            }else if (newDataList.size() == 1){
                OracleData newData = newDataList.get(0);
                BigDecimal newBalance = getNewBalance(newData);
                if (!newData.getUsed()){
                    if (oldBalance.compareTo(newBalance) != 0) {
                        // 余额不相等
                        oldData.setRemark("和新系统余额不相等");
                    }else {
                        newData.setUsed(true);
                        oldData.setRemark("匹配成功");
                        newData.setRemark("匹配成功");
                    }
                }else {
                    oldData.setRemark("唯一新系统对应的值已被使用");
                }
            }else {
                boolean flag = true;
                for (OracleData newData : newDataList) {
                    BigDecimal newBalance = getNewBalance(newData);
                    if (!newData.getUsed() && newBalance.compareTo(oldBalance) == 0){
                        flag = false;
                        newData.setUsed(true);
                        oldData.setRemark("匹配成功");
                        newData.setRemark("匹配成功");
                        break;
                    }
                }
                if (flag){
                    oldData.setRemark("未能匹配多个数据");
//                    result3s.add(oldData);
                }
            }
//            result3s.add(oldData);
        }
    }


    private void findNew(List<Step6OldDetailExcel>  projectOld,List<OracleData> projectNew,List<OracleData> result2s){
        // 找到造成差额的明细账
        int oldSize = projectOld.size();
        int newSize = projectNew.size();
        // 先从旧系统出发
//        if (oldSize >= newSize) {
        matchNew(projectOld,projectNew,result2s,newSize);
//        }else {
//            matchNew(projectOld,projectNew,result2s,oldSize);
//            for (int i = oldSize; i < newSize; i++) {
//                OracleData data = projectNew.get(i);
//                data.set备注("多余数据");
//                result2s.add(data);
//            }
//        }
    }

    private void matchNew(List<Step6OldDetailExcel>  projectOld,List<OracleData> projectNew,List<OracleData> result2s,int size){
        Map<String, List<Step6OldDetailExcel>> collect = projectOld.stream().collect(Collectors.groupingBy(item -> item.getMatch()));
        for (int i = 0; i < size; i++) {
            OracleData newData = projectNew.get(i);
            if (newData.getRemark().equals("匹配成功")){
                continue;
            }
            BigDecimal newBalance = getNewBalance(newData);
            List<Step6OldDetailExcel> oldDataList = collect.getOrDefault(newData.get行说明(), null);
            if (oldDataList == null){
                newData.set备注("未能匹配数据");
            }else if (oldDataList.size() == 1){
                Step6OldDetailExcel oldData = oldDataList.get(0);
                BigDecimal oldBalance = getOldBalance(oldData);
                if (oldBalance.compareTo(newBalance) != 0) {
                    // 余额不相等
//                    result2s.add(newData);
                    newData.set备注("和旧系统余额不相等");
                }else {
                    oldData.setRemark("匹配成功");
                    newData.setRemark("匹配成功");
                }
            }else {
                boolean flag = true;
                for (Step6OldDetailExcel oldData : oldDataList) {
                    BigDecimal oldBalance = getOldBalance(oldData);
                    if (!oldData.getUsed() && newBalance.compareTo(oldBalance) == 0){
                        oldData.setUsed(true);
                        oldData.setRemark("匹配成功");
                        newData.setRemark("匹配成功");
                        flag = false;
                        break;
                    }
                }
                if (flag){
                    newData.set备注("未能匹配多个数据");
//                    result2s.add(newData);
                }
            }
            result2s.add(newData);
        }
    }

    private BigDecimal getOldBalance(Step6OldDetailExcel oldData){
        return CommonUtil.getBigDecimalValue(oldData.getV()).subtract(CommonUtil.getBigDecimalValue(oldData.getW()));
    }

    private BigDecimal getNewBalance(OracleData newData){
        return CommonUtil.getBigDecimalValue(newData.get输入借方()).subtract(CommonUtil.getBigDecimalValue(newData.get输入贷方()));
    }

    private Step6Result1 create(String companyName,String timeKey,String oldProjectKey,String newProjectKey,BigDecimal oldSum,BigDecimal newSum,String matchKey){
        Step6Result1 step6Result1 = new Step6Result1();
        step6Result1.setCompanyName(companyName);
        step6Result1.setOldProject(oldProjectKey);
        step6Result1.setNewProject(newProjectKey);
        step6Result1.setMatchProject(matchKey);
        step6Result1.setOldMoney(oldSum);
        step6Result1.setNewMoney(newSum);
        step6Result1.setTime(timeKey);
        step6Result1.setSubMoney(oldSum.subtract(newSum));
        return step6Result1;
    }

    private String getOldProject(Step6OldDetailExcel excel){
        return excel.getProjectName().split("－")[0];
    }

    private String getNewProject(OracleData oracleData){
        return oracleData.get科目段描述().split("-")[0];
    }

    private Boolean isBackProject(String projectName){
        return projectName.startsWith("应付账款")
                || projectName.startsWith("预付账款")
                || projectName.startsWith("合同负债")
                || projectName.startsWith("预收账款")
                || projectName.startsWith("应收账款")
                || projectName.startsWith("其他应付款")
                || projectName.startsWith("其他应收款");
    }

    /**
     * 读取物业excel
     * @return
     */
    public List<Step6OldDetailExcel> readPropertyExcel(String fileName){
        List<Step6OldDetailExcel> excels = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/detail/"+fileName, Step6OldDetailExcel.class,
                        new PageReadListener<Step6OldDetailExcel>(dataList -> {
                            for (Step6OldDetailExcel data : dataList) {
                                try {
                                    if (data.getV() == null && data.getW() == null){
                                        throw new RuntimeException("无法计算金额");
                                    }
                                    String time = data.getTime();
                                    DateTime date = DateUtil.parseDate(time);
                                    if (date.isBefore(DateUtil.parse("2023-07-01")) || date.isAfter(DateUtil.parse("2023-12-31"))) {
                                        // 只需要 07-12 月的
                                        continue;
                                    }
                                    // 科目
                                    String projectName = data.getProjectName();
                                    if (!(isBackProject(projectName) || projectName.startsWith("其他货币资金"))){
                                        // 只需要7大往来
                                        continue;
                                    }
                                    // 其他货币基金只取 9-12月
                                    if (projectName.startsWith("其他货币资金") && (date.isBefore(DateUtil.parse("2023-09-01")) || date.isAfter(DateUtil.parse("2023-12-31")))){
                                        continue;
                                    }
                                    // 摘要
                                    String match = data.getMatch();
                                    if (match.contains("资金归集")){
                                        continue;
                                    }

//                                    String oldProject = getOldProject(data);
                                    data.setOldProject(getOldProject(data));
                                    String oldProject = coverNewDate.getProjectName(data).split("-")[0];
                                    data.setActualProject(oldProject);
                                    if (oldProject.startsWith("其他应收款") || oldProject.startsWith("其他货币资金")){
                                        data.setMatchProject("其他应收款");
                                    }else if (oldProject.startsWith("合同负债") || oldProject.startsWith("预收账款")){
                                        data.setMatchProject("合同负债/预收账款");
                                    } else {
                                        data.setMatchProject(oldProject);
                                    }
                                    ZNProjectMapping znProjectMapping = findNccZhongNanLevel.znProjectMapping.get(data.getProjectCode());
                                    data.setProjectName(znProjectMapping.getFmsProjectName());
                                    excels.add(data);
                                }catch (Exception e){
                                    System.out.println("解析中南老系统明细数据出错: "+e.getMessage());
                                    System.out.println(data);
                                }

                            }
                        }))
                .sheet("综合查询表").doRead();
        return excels;
    }
}
