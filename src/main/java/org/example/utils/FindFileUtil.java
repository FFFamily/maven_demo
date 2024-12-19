package org.example.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.Assistant;
import org.example.enitty.LevelFileExcel;
import org.example.寻找等级.OtherInfo3;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FindFileUtil {
    public static List<OtherInfo3> readDetailExcel(String company){
        List<OtherInfo3> cachedDataList = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/ewai/总帐凭证行查 _"+company+".xlsx", LevelFileExcel.class,
                new PageReadListener<LevelFileExcel>(dataList -> {
                    for (LevelFileExcel levelFileExcel : dataList) {
//                                    String s = levelFileExcel.getS();
                        String project = levelFileExcel.getProject();
                        if (!(project.startsWith("应付账款")
                                || project.startsWith("预付账款")
                                || project.startsWith("合同负债")
                                || project.startsWith("预收账款")
                                || project.startsWith("应收账款")
                                || project.startsWith("其他应付款")
                                || project.startsWith("其他应收款"))){
                            continue;
                        }
                        OtherInfo3 info = new OtherInfo3();
                        //
                        // 有效日期
                        DateTime date = DateUtil.date(levelFileExcel.getN());
                        int year = date.year();
                        int month = date.month()+1;
                        int code = levelFileExcel.getQ();
                        info.setQ(code);
                        info.setR(year+"-"+month+"-"+code);
                        info.setV(levelFileExcel.getV());
                        info.setW(levelFileExcel.getW());
                        // 有效日期
                        info.setN(date);
                        info.setS(levelFileExcel.getS());
                        // 有借就是 借方向
                        info.setX(info.getV() != null ? "借" : "贷");
                        info.setZ(levelFileExcel.getZ());
                        info.setZCopy(levelFileExcel.getZ().replace(".","-"));
                        info.setZDesc(levelFileExcel.getZDesc());
                        info.setTransactionId(getStr(levelFileExcel.getTransactionId()));
                        info.setTransactionName(getStr( levelFileExcel.getTransactionName()));
                        info.setTransactionCodeCopy(getStr(levelFileExcel.getTransactionCodeCopy()));
                        info.setOnlySign(info.getZCopy()+info.getTransactionCodeCopy());
//                                    info.setOriginZCopy(info.getZCopy()+info.getTransactionCodeCopy());
                        // 公司名称
                        info.setCompanyName(company);
                        // 用于追溯老系统
                        info.setJournalExplanation(levelFileExcel.getJournalExplanation());
                        cachedDataList.add(info);
                    }
                })
        ).sheet(0).doRead();
        return cachedDataList;
    }

    public static List<Assistant> redaBalance(String company){
        List<Assistant> assistants = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/ewai/"+company+"-辅助核算余额.xlsx",
                new AnalysisEventListener<Map<Integer,String>>() {
                    @Override
                    public void invoke(Map<Integer,String> o, AnalysisContext analysisContext) {
                        Assistant assistant3 = new Assistant();

                        // 左前缀匹配
                        BigDecimal v = new BigDecimal(o.get(7).replaceAll(",",""));
                        BigDecimal w = new BigDecimal(o.get(8).replaceAll(",",""));
                        assistant3.setZ(CommonUtil.getZ(CommonUtil.getBigDecimalValue(v).subtract(CommonUtil.getBigDecimalValue(w))));
                        String code = o.get(0);
                        assistant3.setR(code);
                        assistant3.setRDesc(o.get(1));
                        // 机构
                        assistant3.setE(company);

                        // 辅助核算段
                        String s = o.get(2);
                        String[] split = s.split("\\.");
                        assistant3.setA(s);
                        assistant3.setTransactionObjectId(split[1].equals("-") ? "" : split[1]);
                        assistant3.setTransactionObjectCode("");
                        assistant3.setTransactionObjectName("");
                        assistant3.setTransactionObjectCodeCopy(split[1].equals("-") ? "" : split[1]);
                        // 科目段描述
                        String codeName = o.get(1);
                        assistant3.setRDesc(codeName);
//                        assistant3.setCompanyCode(o.get(0));
//                        assistant3.setForm(o.get(0));
                        // 唯一标识：账户组合+交易Id
                        assistant3.setOnlySign(assistant3.getR()+assistant3.getTransactionObjectCodeCopy());
                        assistants.add(assistant3);
                    }
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

                    }
                }).sheet(0).headRowNumber(2).doRead();
        return assistants;
    }


    private static String getStr(String str){
        return str == null ?"":str;
    }
}
