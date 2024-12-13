package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.Assistant;
import org.example.enitty.OracleData;
import org.example.enitty.yu_zhou.YuZhouOldBalanceExcel;
import org.example.enitty.yu_zhou.YuZhouOldDetailExcel;
import org.example.enitty.zhong_nan.OldZNAuxiliaryBalanceSheet;
import org.example.utils.CommonUtil;
import org.example.utils.LevelUtil;
import org.example.寻找等级.FindLevel;
import org.example.寻找等级.OtherInfo3;
import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.example.utils.CommonUtil.getZ;

@SpringBootTest
public class YuZhouTest {
    @Resource
    private FindLevel findLevel;
    @Test
    void test1() {
        // 余额
        Map<String, List<Assistant>> collect = readBalanceExcel().stream().collect(Collectors.groupingBy(item -> item.getE()));
        for (String company : collect.keySet()) {
            List<Assistant> assistants = collect.get(company);
            List<OtherInfo3> result = new ArrayList<>();
            System.out.println("当前公司："+company);
            // 便利余额
            for (Assistant assistant : assistants) {
                String companyName = assistant.getE();

                // 这个公司的所有明细
                List<OtherInfo3> otherInfo3s = readDetailExcel(companyName);
                List<OtherInfo3> startCollect = otherInfo3s.stream().filter(item -> item.getOnlySign().equals(assistant.getOnlySign())).collect(Collectors.toList());
                List<OtherInfo3> res = findLevel.doMain(
                        true,
                        false,
                        false,
                        otherInfo3s,
                        null,
                        startCollect,
                        assistant.getZ(),
                        assistant
                );
                result.addAll(res);
            }
            String fileName = "禹州老系统分级-"+company+ ".xlsx";
            EasyExcel.write(fileName, OtherInfo3.class).sheet("模板").doWrite(result);
        }
    }
    // 读取余额表
    public List<Assistant> readBalanceExcel(){
        List<Assistant> balanceExcels = new ArrayList<>();
        // 读取旧系统的余额信息 2022年
        EasyExcel.read("src/main/java/org/example/excel/yu_zhou/01禹州南京-D类客商映射表.xlsx",
                        YuZhouOldBalanceExcel.class,
                        new PageReadListener<YuZhouOldBalanceExcel>(dataList -> {
                            for (YuZhouOldBalanceExcel data : dataList) {
                                try {
                                    Assistant assistant = new Assistant();
                                    BigDecimal money = data.getV();
                                    // 左前缀匹配
                                    assistant.setZ(getZ(money));
                                    // 唯一标识：科目编码+辅助段
                                    String onlySign = data.getN();
                                    assistant.setOnlySign(onlySign);
                                    String regex = "(?<=：)[^【】]+";
                                    Pattern pattern = Pattern.compile(regex);
                                    // 唯一标识
                                    Matcher matcher = pattern.matcher(data.getP());
                                    while (matcher.find()) {
                                        assistant.setOnlySign(assistant.getOnlySign() + matcher.group().trim());
                                    }
                                    assistant.setE(data.getQ().split("-")[0]);
                                    balanceExcels.add(assistant);
                                }catch (Exception e){
                                    System.out.println("解析禹州数据出错");
                                    System.out.println(data);
//                                    e.printStackTrace();
                                }

                            }
                        }))
                .sheet("3六大往来明细表-禹州南京分公司").headRowNumber(2).doRead();
        return balanceExcels;
    }

    /**
     * 明细账
     * @return
     */
    public List<OtherInfo3> readDetailExcel(String companyName){
        List<OtherInfo3> result = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/yu_zhou/序时账-20-22年4月-调整数字格式.xls",
                        YuZhouOldDetailExcel.class,
                        new PageReadListener<YuZhouOldDetailExcel>(dataList -> {
                            for (YuZhouOldDetailExcel data : dataList) {

                                try {
                                    if (data.getA() == null || data.getB() == null || data.getC() == null) {
                                        System.out.println("错误的格式："+data);
                                        continue;
                                    }
                                    OtherInfo3 otherInfo3 = new OtherInfo3();
                                    String dateStr = data.getA()+"-"+data.getB()+"-"+data.getC();
                                    // 公司
                                    otherInfo3.setCompanyName(companyName);
                                    // 总账日期
                                    DateTime dateTime = DateUtil.parse(dateStr);
                                    otherInfo3.setN(dateTime);
                                    // 凭证号
                                    String pz = data.getD().split("-")[1];
                                    otherInfo3.setQ(Integer.valueOf(pz));
                                    // 拼接凭证号
                                    otherInfo3.setR(dateTime.year() + "-" + (dateTime.month()+1) + otherInfo3.getQ());
                                    // 来源随便写一个，以便于分级查找的时候不被拦截
                                    otherInfo3.setS("人工");
                                    // 借
//                                    otherInfo3.setV(data.getL() == null ? null : new BigDecimal(data.getL()));
                                    otherInfo3.setV(data.getL());
                                    // 贷
//                                    otherInfo3.setW(data.getN() == null ? null : new BigDecimal(data.getN()));
                                    otherInfo3.setW(data.getN());
                                    otherInfo3.setX(CommonUtil.getX(otherInfo3.getV(), otherInfo3.getW()));
                                    // 唯一标识
                                    // 科目编码-辅助段
//                                    String regex = "(?<=：)[^【】]+";
                                    String regex = "(?<=：)([^/【】]+)(?:/([^【】]+))?";
                                    Pattern pattern = Pattern.compile(regex);
                                    // 唯一标识
                                    String onlySign = data.getG();
                                    otherInfo3.setOnlySign(onlySign);
                                    if (data.getI() != null){
                                        Matcher matcher = pattern.matcher( data.getI());
                                        while (matcher.find()) {
                                            String trim = matcher.group().trim();
                                            String[] split = trim.split("/");
                                            if (split.length == 1){
                                                otherInfo3.setOnlySign(otherInfo3.getOnlySign() + split[0]);
                                            }else {
                                                otherInfo3.setOnlySign(otherInfo3.getOnlySign() + split[1]);
                                            }

                                        }
                                    }
                                    otherInfo3.setSystemForm("老系统");
                                    result.add(otherInfo3);
                                }catch (Exception e){
                                    System.out.println("处理序时账户出错： "+e.getMessage());
                                    System.out.println(data);
                                    e.printStackTrace();
                                }

                            }
                        }))
                .sheet(companyName).headRowNumber(2).doRead();
        return result;
    }
}
