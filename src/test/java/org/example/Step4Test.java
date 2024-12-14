package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.enitty.Assistant;
import org.example.enitty.zhong_nan.OldZNAuxiliaryBalanceSheet;
import org.example.enitty.zhong_nan.OldZNChronologicalAccount2022;
import org.example.utils.CommonUtil;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.OtherInfo3;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.example.utils.CommonUtil.getZ;

@SpringBootTest
public class Step4Test {
    @Resource
    private FindLevel findLevel;
    @Test
    void test1() {
        List<OldZNAuxiliaryBalanceSheet> balanceSheets2022 = new ArrayList<>();
        // 读取旧系统的余额信息 2022年
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/中南22年新旧系统辅助科目余额表（处理后）.xlsx", OldZNAuxiliaryBalanceSheet.class, new PageReadListener<OldZNAuxiliaryBalanceSheet>(balanceSheets2022::addAll))
                .sheet("旧系统").doRead();
        // 根据公司分组
        Map<String, List<OldZNAuxiliaryBalanceSheet>> companyGroup = balanceSheets2022
                .stream()
                .collect(Collectors.groupingBy(OldZNAuxiliaryBalanceSheet::getP));
        // 2022 序时账
        List<OtherInfo3> cachedDataList = readOldZNChronologicalAccount2022();
        for (String companyName : companyGroup.keySet()) {

            if (!companyName.equals("江苏中南物业服务有限公司温州分公司")){
                continue;
            }
            System.out.println("当前公司："+companyName);
            List<OldZNAuxiliaryBalanceSheet> balanceSheets = companyGroup.get(companyName);
            List<OtherInfo3> res = new ArrayList<>();
            for (int i = 0; i < balanceSheets.size(); i++) {
                // 取一条余额信息
                OldZNAuxiliaryBalanceSheet balanceSheet = balanceSheets.get(i);
                Assistant assistant = convertAssistant(balanceSheet);
                List<OtherInfo3> startCollect = cachedDataList.stream().filter(item -> item.getOnlySign().equals(assistant.getOnlySign())).collect(Collectors.toList());
                List<OtherInfo3> result = findLevel.doMain(
                        true,
                        false,
                        false,
                        cachedDataList,
                        null,
                        startCollect,
                        assistant.getZ(),
                        assistant
                );
                int finalI = i;
                result.forEach(item -> {
                    item.setA(String.valueOf(finalI));
                });
                res.addAll(result);
            }
            String resultFileName = "第四步-中南老系统分级-" + companyName + "-" + System.currentTimeMillis() + ".xlsx";
            try (ExcelWriter excelWriter = EasyExcel.write(resultFileName).build()) {
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "已匹配").head(OtherInfo3.class).build();
                excelWriter.write(res, writeSheet1);
                System.out.println(resultFileName+"导出完成");
            }
        }


    }

    private Assistant convertAssistant(OldZNAuxiliaryBalanceSheet balanceSheet) {
        Assistant assistant3 = new Assistant();
        BigDecimal money = CommonUtil.getBigDecimalValue(balanceSheet.getH()) .subtract(CommonUtil.getBigDecimalValue(balanceSheet.getI()));
        // 左前缀匹配
        assistant3.setZ(getZ(money));
        assistant3.setE(balanceSheet.getB());
        // 唯一标识：账户组合+交易Id
        // 科目代码-主体
        String onlySign = balanceSheet.getR()+balanceSheet.getP()+balanceSheet.getT()+balanceSheet.getU();
        assistant3.setOnlySign(onlySign);
        return assistant3;
    }

    /**
     * 读取2022年的序时账
     * @return
     */
    private List<OtherInfo3> readOldZNChronologicalAccount2022(){
        List<OtherInfo3> res = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/江苏中南物业服务有限公司温州分公司2022.xlsx", OldZNChronologicalAccount2022.class, new PageReadListener<OldZNChronologicalAccount2022>(dataList -> {
                    for (OldZNChronologicalAccount2022 data : dataList) {
                        OtherInfo3 otherInfo3 = new OtherInfo3();
                        try {
                            String dateStr = data.getA();
                            // 公司
                            otherInfo3.setCompanyName(data.getU());
                            // 总账日期
                            DateTime dateTime = DateUtil.parse(dateStr);
                            otherInfo3.setN(dateTime);
                            // 凭证号
                            otherInfo3.setQ(data.getB());
                            // 拼接凭证号
                            otherInfo3.setR(dateTime.year() + "-" + (dateTime.month()+1) + otherInfo3.getQ());
                            // 来源随便写一个，以便于分级查找的时候不被拦截
                            otherInfo3.setS("人工");
                            // 借
                            otherInfo3.setV(data.getH());
                            // 贷
                            otherInfo3.setW(data.getI());
                            otherInfo3.setX(CommonUtil.getX(otherInfo3.getV(), otherInfo3.getW()));
                            // TODO 余额
                            String regex = "(?<=：)[^【】]+";
                            Pattern pattern = Pattern.compile(regex);
                            // 唯一标识
                            // 科目编码-业务单元-项目-客商-人员档案
                            String  onlySign = data.getC() + data.getU()+ data.getO();
                            if (data.getM() == null || data.getM().isEmpty()) {
                                onlySign += (data.getK() == null ? "" : data.getK());
                            }else {
                                onlySign+=data.getM();
                            }
                            otherInfo3.setOnlySign(onlySign);
                            otherInfo3.setSystemForm("老系统");
                            res.add(otherInfo3);
                        }catch (Exception e){
                            System.out.println("解析出错啦：");
                            System.out.println(data);
                            e.printStackTrace();
                        }
                    }
                }))
                .sheet("综合查询表").headRowNumber(3).doRead();
        return res;
    }
}
