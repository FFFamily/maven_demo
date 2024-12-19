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
    private FindUtil findUtil;
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
                .filter(item -> findUtil.isBackProject(item.getActualProject()))
                .collect(Collectors.toList());
        // 将新系统过滤出NCC导入的数据
        List<OracleData> nccstep5Result = step5Result
                .stream()
                .filter(item -> item.get日记账说明().contains("NCC"))
                .collect(Collectors.toList());
        // 新系统不含 NCC 导入的数量
        List<OracleData> notWithNccOracleList = step5Result.stream()
                .filter(item -> !item.get日记账说明().contains("NCC"))
                .peek(item -> item.setForm("23年7-12月新系统序时账"))
                .collect(Collectors.toList());
        List<OracleData> oracleData = new ArrayList<>();
        for (OracleData data : nccstep5Result) {
            if (filterCondition(newCompanyName,data)){
                data.setForm("23年7-12月新系统序时账单独过滤");
                notWithNccOracleList.add(data);
            }else {
                oracleData.add(data);
            }
        }
        List<OracleData> notWithNcc = new ArrayList<>();
        // 在不包含ncc的数据中找到本来就是ncc导入的
        for (OracleData data : notWithNccOracleList) {
            if (filterAddCondition(newCompanyName,data)){
                data.setForm("23年7-12月新系统序时账单独添加");
                oracleData.add(data);
            }else {
                notWithNcc.add(data);
            }
        }

//        oracleData.addAll(addCondition(newCompanyName,step5Result));


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
        result2s.stream().filter(item -> item.getRemark() ==null || !item.getRemark().equals("匹配成功")).forEach(item -> {
            item.setRemark("");
            notWithNcc.add(item);
        });
        return new Step6TestResult(
                result1s,
                result2s,
                result3s,
                notWithNcc
        );
    }

    private boolean filterAddCondition(String newCompanyName, OracleData item) {
        if (newCompanyName.equals("江苏中南物业服务有限公司梅州分公司")){
            return item.get日记账说明().contains("ZZTY2023110110151本地业财发起待确认事项：梅州雅居乐10.30-10.31周报、应收冲抵单")
                    || item.get日记账说明().contains("ZZTY2023103010321本地业财发起待确认事项：梅州雅居乐10.1-10.29周报、应收冲抵单adi");
        }else if (newCompanyName.equals("余姚中锦物业服务有限公司")){
            return item.get日记账说明().contains("BKDT0|YGCB2024010210474 总账通用计提:余姚中锦-2023年12月预估收缴率收入");
        }else if (newCompanyName.equals("江苏中南物业服务有限公司仁寿分公司")){
            return item.get日记账说明().contains("HNCX0|YGCB2024010111235：总账通用计提：中南仁寿分公司收缴率还原");
        }else if (newCompanyName.equals("江苏中南物业服务有限公司抚顺分公司")){
            return item.get日记账说明().contains("ERXX0|YGCB2023123010142总账通用计提:收抚顺中南熙悦2022年12月住宅物业费(基础包干制)")
                    || item.get日记账说明().contains("ERXX0|YGCB2024010110009总账通用计提:收入（抚顺中南熙悦12.31）外系统生成凭证");
        }else if (newCompanyName.equals("江苏中南物业服务有限公司长丰分公司")){
            return item.get日记账说明().contains("LXHT0|GXZZ2023122110061 基础物业服务收入工单：中南长丰宸悦收入ADI导入12.1-12.17");
        }
        return false;
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
        }else if (companyName.equals("余姚中锦物业服务有限公司")){
            return oracleData.get日记账说明().equals("ZZTY2023092810346本地业财发起待确认事项：NCC科目调整");
        }else if (companyName.equals("江苏中南物业服务有限公司余姚分公司")){
            return oracleData.get日记账说明().equals("FYGD2023082910075冲销原NCC630计提")
                    || oracleData.get日记账说明().equals("MJNZ0|YGCB2023121110136:总账通用计提_冲销NCC成本计提")
                    || oracleData.get日记账说明().equals("MJNZ0|YGCB2023121310203:总账通用计提_调整在途科目调整以及NCC以前年度计提冲销")
                    || oracleData.get日记账说明().equals("YGCB2023120310421:总账通用计提_调整NCC银行")
                    || oracleData.get日记账说明().equals("ZZTY2023092610246本地业财发起待确认事项:冲销NCC能耗计提+项目段调整+客商调整+新旧科目调整")
                    || oracleData.get日记账说明().equals("ZZTY2023100111033本地业财发起待确认事项:NCC成本已入账，冲销成本及应付科目_:64/DGFY2023091810531");
        }else if (companyName.equals("江苏中南物业服务有限公司宁波分公司")){
            return oracleData.get日记账说明().equals("IFZH0|YGCB2023112710128:总账通用计提_冲销630NCC计提已付款成本")
                    || oracleData.get日记账说明().equals("冲销原NCC计提凭证ZZTY2023082910278");
        }else if (companyName.equals("江苏中南物业服务有限公司诸暨分公司")){
            return oracleData.get日记账说明().equals("YGCB2023120810083:总账通用计提_冲销NCC成本计提");
        }else if (companyName.equals("江苏中南物业服务有限公司宁波杭州湾新区分公司")){
            return oracleData.get日记账说明().equals("NCC已入成本未付款本月FMS重复成本冲销FYGD2023073110263")
                    || oracleData.get日记账说明().equals("ZZTY2023083110296冲销原NCC计提成本")
                    || oracleData.get日记账说明().equals("冲销原NCC计提凭证ZZTY2023082910129");
        }else if (companyName.equals("江苏中南物业服务有限公司桐庐分公司")){
            return oracleData.get日记账说明().equals("ZZTY2023080410118调整GYSF2023072410012费用类型|托收款补录NCC5月6月已入账");
        }else if (companyName.equals("江苏中南物业服务有限公司温州分公司")){
            return oracleData.get日记账说明().equals("ZWTZ2023121410013悦+财务一体化账务调整:申请冲销前期NCC导入暂估计提收入");
        }else if (companyName.equals("江苏中南物业服务有限公司湖州分公司")) {
            return oracleData.get日记账说明().equals("FYGD2023120110192_NCC22年9-11月凭证-冲销往年计提")
                    || oracleData.get日记账说明().equals("YGCB2023120310575总账通用计提：湖州分调整NCC入账")
                    || oracleData.get日记账说明().equals("YGCB2023120211393总账通用计提:NCC入账错误调整（退装修保证金）")
                    || oracleData.get日记账说明().equals("YGCB2023120310350总账通用计提：湖州分调整NCC入账");
        }else if (companyName.equals("江苏中南物业服务有限公司金华分公司")){
            return oracleData.get日记账说明().equals("GXZZ2024010110622 共享会计自主做账：冲销金华共享服务费计提：YKBK0-202306账期-NCC系统导入数据补入");
        }else if (companyName.equals("江苏中南物业服务有限公司宁波奉化分公司")) {
            return oracleData.get日记账说明().equals("FYGD2023080110192费用账务处理工单_账务调整：冲原NCC2022年12月12#凭证税金借方费用贷方|DSFY2023071910192侯宝利21416733办公行政费")
                    || oracleData.get日记账说明().equals("ZZTY2023090110303冲销原NCC归档待支付凭证")
                    || oracleData.get日记账说明().equals("冲销原NCC计提凭证ZZTY2023082910150")
                    || oracleData.get日记账说明().equals("冲销：24/DGFY2023071810166NCC6-47#成本重复FYGD2023073110218");
        }else if (companyName.equals("江苏中南物业服务有限公司许昌分公司")){
            return oracleData.get日记账说明().equals("ZZTY2023092910145 本地业财发起待确认事项 许昌分9月调整及NCC计提冲销");
        }else if (companyName.equals("江苏中南物业服务有限公司海门分公司")){
            return oracleData.get日记账说明().contains("NCC账务调整")
                    || oracleData.get日记账说明().contains("YGCB2023112810032总账通用计提：NCC6月凭证科目有误：海门春江电信付款科目调整");
        }else if (companyName.equals("江苏中南物业服务有限公司昆山分公司")){
            return oracleData.get日记账说明().equals("NCC账务处理调整");
        }else if (companyName.equals("江苏中南物业服务有限公司苏州分公司")){
            return oracleData.get日记账说明().equals("YGCB2023120210727总账通用计提：规范ncc导入凭证银行科目");
        }else if (companyName.equals("江苏中南物业服务有限公司常熟分公司")){
            return oracleData.get日记账说明().contains("YGCB2024010210750总账通用计提:NCC能耗销项税税率6%调整至13%")
                    || oracleData.get日记账说明().contains("2.NNGV0|ZZTY2023093010578：调整成本（熟林樾成本NCC与ERP重复成本）");
        }else if (companyName.equals("江苏中南物业服务有限公司南通分公司")){
            return oracleData.get日记账说明().contains("FYGD2023083010095费用账务处理工单：冲销NCC系统2023.1-6月的合同计提金额（冲销的是报账系统2023.8月流程通过的费用）")
                    || oracleData.get日记账说明().contains("ZZTY2023090110779本地业财发起待确认事项：因ERP合同期初录入，需冲销NCC合同计提")
                    || oracleData.get日记账说明().contains("FYGD2023103010062_冲减前期NCC计提凭证");
        }else if (companyName.equals("江苏中南物业服务有限公司无锡分公司")){
            return oracleData.get日记账说明().equals("CGIT0|ZWTZ2023120110270悦+财务一体化账务调整中南无锡NCC导入调整");
        }else if (companyName.equals("江苏中南物业服务有限公司乍浦分公司")){
            return oracleData.get日记账说明().contains("FYGD2023113010124_NCC凭证冲销前期手工计提合同成本")
                    || oracleData.get日记账说明().contains("FYGD2023122210029_前期NCC凭证-冲销前期手工计提合同成本")
                    || oracleData.get日记账说明().contains("FYGD2023122610036_前期NCC凭证-冲销前期手工计提合同成本");
        }else if (companyName.equals("青岛中南物业管理有限公司")) {
            return oracleData.get日记账说明().equals("FYGD2023110110096冲销上线共享前原NCC计提")
                    || oracleData.get日记账说明().equals("GXZZ2023100210342 基础物业服务收入工单调整金石NCC9月重复生成应收")
                    || oracleData.get日记账说明().equals("FYGD2023123010054冲销原NCC计提费用及进项转出调整")
                    || oracleData.get日记账说明().equals("FYGD2023120110136调整原NCC计提冲销");
        }else if (companyName.equals("青岛中南物业管理有限公司东营分公司")) {
            return oracleData.get日记账说明().equals("SRWR0|YGCB2024010211229 总账通用计提：NCC科目调整")
                    || oracleData.get日记账说明().equals("FYGD2023113010049冲销前期NCC计提凭证")
                    || oracleData.get日记账说明().equals("YGCB2024010310084 总账通用计提：青岛东营银行科目梳理调整-调整天问NCC导入错误")
                    || oracleData.get日记账说明().equals("YGCB2024010410696 总账通用计提：天问导入NCC有误更正");
        }else if (companyName.equals("江苏中南物业服务有限公司平度分公司")){
            return oracleData.get日记账说明().equals("VBCV0|FYGD2023100110081：冲销NCC计提凭证");
        }else if (companyName.equals("江苏中南物业服务有限公司潍坊分公司")){
            return oracleData.get日记账说明().contains("GXZZ2024010310865 潍坊熙悦NCC推错收入调减")
                    || oracleData.get日记账说明().contains("YGCB2023120210016 总账通用计提：NCC导入调整");
        }else if (companyName.equals("江苏中南物业服务有限公司济宁分公司")){
            return oracleData.get日记账说明().contains("FYGD2023080110161：与NCC账面重复，发票已认证，故冲销此笔费用，原NCC凭证2023-01-50#")
                    || oracleData.get日记账说明().contains("FYGD2024010110120冲销NCC成本计提")
                    || oracleData.get日记账说明().contains("FYGD2023122610032 费用账务处理工单_冲销手工计提账务：冲销ncc账面计提");
        }else if (companyName.equals("江苏中南物业服务有限公司淄博分公司")){
            return oracleData.get日记账说明().equals("GXZZ2024010211057基础物业服务收入工单：12月余额负数-建筑垃圾（已通过NCC推送）");
        }else if (companyName.equals("江苏中南物业服务有限公司龙口分公司")){
            return oracleData.get日记账说明().contains("FYGD2023122210025冲销原NCC计提成本调整")
                    || oracleData.get日记账说明().contains("FYGD2023100110042 费用账务处理工单_冲销手工计提账务：调整冲销NCC原计提凭证—9月ERP已推送计提到FMS");
        }else if (companyName.equals("江苏中南物业服务有限公司邹城分公司")){
            return oracleData.get日记账说明().contains("FYGD2024010110116冲销NCC成本计提")
                    || oracleData.get日记账说明().contains("RDJU0|YGCB2023120211475 总账通用计提:辅助调整及NCC拆税")
                    || oracleData.get日记账说明().contains("RDJU0|FYGD2023122610038 费用账务处理工单_冲销手工计提账务:NCC能源费计提冲销");
        }else if (companyName.equals("江苏中南物业服务有限公司威海分公司")) {
            return oracleData.get日记账说明().equals("ZZTY2023100210358 本地业财发起待确认事项：调整银行在途以及冲销ncc管销费科目")
                    || oracleData.get日记账说明().equals("ZZTY2023092010360 本地业财发起待确认事项：拆分中南ncc客商")
                    || oracleData.get日记账说明().equals("UBIF0|YGCB2024010311201 总账通用计提：ncc导入的资金归集错误数据，做负数冲平")
                    || oracleData.get日记账说明().equals("YGCB2024010211412 总账通用计提：冲减NCC中sbu和项目段不匹配业务");
        }else if (companyName.equals("江苏中南物业服务有限公司日照分公司")) {
            return oracleData.get日记账说明().equals("FYGD2023122210013冲销原NCC计提成本调整")
                    || oracleData.get日记账说明().equals("FYGD2023100110038 费用账务处理工单:冲销往期NCC计提成本")
                    || oracleData.get日记账说明().equals("FYGD2023122910074冲销原NCC计提成本")
                    || oracleData.get日记账说明().equals("CRXL0|CRXL0-202401账期-NCC系统导入数据");
        }else if (companyName.equals("江苏中南物业服务有限公司即墨分公司")){
            return oracleData.get日记账说明().equals("YGCB2023110210162 总账通用计提：江苏即墨ncc导入银行科目调整（退押金部分）");
        }else if (companyName.equals("江苏中南物业服务有限公司丹阳分公司")){
            return oracleData.get日记账说明().contains("ZZTY2023110310373 本地业财发起待确认事项:NCC导入凭证sbu补挂")
                    || oracleData.get日记账说明().contains("ZZTY2023110210742 本地业财发起待确认事项:NCC导入凭证sbu补挂");
        }else if (companyName.equals("江苏中南物业服务有限公司宿迁分公司")){
            return oracleData.get日记账说明().equals("FYGD2023090110125费用账务处理工单_冲销NCC往期暂估计提凭证");
        }else if (companyName.equals("江苏中南物业服务有限公司泉州分公司")){
            return oracleData.get日记账说明().equals("ZZTY2023101610093本地业财发起待确认事项：冲销NCC计提单据：6月报销2023年3月份物资采购费用（退汇、8月重新发起付款）");
        }else if (companyName.equals("江苏中南物业服务有限公司江津分公司")){
            return oracleData.get日记账说明().equals("YGCB2024010310368总账通用计提：调整银行-天问推送NCC押金转预存凭证导致借方虚增错误");
        }else if (companyName.equals("青岛中南物业管理有限公司沈阳分公司")){
            return oracleData.get日记账说明().equals("ZZTY2023110310055本地业财发起待确认事项：调整沈阳ncc推送银行科目");
        }else if (companyName.equals("江苏中南物业服务有限公司邯郸分公司")){
            return oracleData.get日记账说明().equals("FYGD2023112310014_冲减2023年6月NCC计提，无需计提（赵路伟435.2元、修玘昆194.43元）");
        }else if (companyName.equals("江苏中南物业服务有限公司慈溪分公司")) {
            return oracleData.get日记账说明().equals("PCJE0|YGCB2023121310077:总账通用计提_冲销原NCC计提凭证")
                    || oracleData.get日记账说明().equals("FYGD2023090110166冲销NCC的计提凭证")
                    || oracleData.get日记账说明().equals("PCJE0|YGCB2023112810473:总账通用计提_冲销NCC成本计提")
                    || oracleData.get日记账说明().equals("ZZTY2023092710317本地业财发起待确认事项：FMS跑的计提与NCC重复，需冲回")
                    || oracleData.get日记账说明().equals("YGCB2023110210040:总账通用计提_调整NCC中账做在了预收账款，导出ADI后变成了其他应付款，因此需调整科目")
                    || oracleData.get日记账说明().equals("冲销NCC计提凭证FYGD2023082910046");
        }else if (companyName.equals("江苏中南物业服务有限公司张家港分公司")){
            return oracleData.get日记账说明().contains("FYGD2023083010074调整：冲销NCC计提成本")
                    || oracleData.get日记账说明().contains("YGCB2024010410030总账通用计提：调整NCC导入凭证入错科目")
                    || oracleData.get日记账说明().contains("YGCB2024010210758总账通用计提:NCC能耗销项税税率6%调整至13%");
        }else if (companyName.equals("江苏中南物业服务有限公司德清分公司")){
            return oracleData.get日记账说明().contains("YGCB2023110310405总账通用计提：NCC入账错误调整")
                    || oracleData.get日记账说明().contains("YGCB2023120211401总账通用计提:NCC入账错误调整（退装修保证金）");
        }else if (companyName.equals("江苏中南物业服务有限公司马鞍山分公司")){
            return oracleData.get日记账说明().contains("NIUF0|NIUF0-202310账期-NCC系统导入数据 电子表格 A 48651827 71652849")
                    || oracleData.get日记账说明().contains("GXZZ2023110310383：共享会计自主做账-调账：中南NCC导入凭证，未带摘要，现冲销原凭证");
        }else if (companyName.equals("江苏中南物业服务有限公司佛山高明分公司")){
            return oracleData.get日记账说明().equals("从 \"2023-11\" 反冲 \"ESNY0|ESNY0-202311账期-NCC系统导入数据 电子表格 A 48646649 71627024\" 批的 \"ESNY0|ESNY0-202311账期-NCC系统导入数据 CRC表内凭证 CNY Use\" 日记账。");
        }else if (companyName.equals("江苏中南物业服务有限公司揭阳分公司")){
            return oracleData.get日记账说明().equals("1.调整科目：ncc系统在途款映射成银行存款");
        }else if (companyName.equals("江苏中南物业服务有限公司万宁分公司")){
            return oracleData.get日记账说明().equals("YGCB2024010410023总账通用计提：NCC4-63#已退款，5-38#录入天问在途退款导致账面重复挂账负数");
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



    private String getNewProject(OracleData oracleData){
        return oracleData.get科目段描述().split("-")[0];
    }




}
