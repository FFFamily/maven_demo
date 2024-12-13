package org.example;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.enitty.Assistant;
import org.example.enitty.yu_zhou.YuZhouOldBalanceExcel;
import org.example.enitty.yu_zhou.YuZhouOldDetailExcel;
import org.example.enitty.zhong_nan.OldZNAuxiliaryBalanceSheet;
import org.example.utils.CommonUtil;
import org.example.寻找等级.OtherInfo3;
import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.example.utils.CommonUtil.getZ;

@SpringBootTest
public class YuZhouTest {
    @Test
    void test1() {
        // 余额
        List<Assistant> assistants = readBalanceExcel();
        // 便利余额
        for (Assistant assistant : assistants) {

        }
    }
    // 读取余额表
    public List<Assistant> readBalanceExcel(){
        List<Assistant> balanceExcels = new ArrayList<>();
        // 读取旧系统的余额信息 2022年
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/中南22年新旧系统辅助科目余额表（处理后）.xlsx",
                        YuZhouOldBalanceExcel.class,
                        new PageReadListener<YuZhouOldBalanceExcel>(dataList -> {
                            for (YuZhouOldBalanceExcel data : dataList) {
                                Assistant assistant = new Assistant();
                                BigDecimal money = data.getV();
                                // 左前缀匹配
                                assistant.setZ(getZ(money));
                                // 唯一标识：科目编码+科目名称+辅助段+账套
                                String onlySign = data.getN()+data.getO()+data.getP()+data.getQ();
                                assistant.setOnlySign(onlySign);
                                balanceExcels.add(assistant);
                            }
                        }))
                .sheet("3六大往来明细表-禹州南京分公司").doRead();
        return balanceExcels;
    }

    /**
     * 明细账
     * @return
     */
    public List<OtherInfo3> readDetailExcel(String companyName){
        List<OtherInfo3> result = new ArrayList<>();
        EasyExcel.read("src/main/java/org/example/excel/zhong_nan/中南22年新旧系统辅助科目余额表（处理后）.xlsx",
                        YuZhouOldDetailExcel.class,
                        new PageReadListener<YuZhouOldDetailExcel>(dataList -> {
                            for (YuZhouOldDetailExcel data : dataList) {
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
                                otherInfo3.setV(data.getL());
                                // 贷
                                otherInfo3.setW(data.getN());
                                otherInfo3.setX(CommonUtil.getX(otherInfo3.getV(), otherInfo3.getW()));
                                // TODO 余额
                                String regex = "(?<=：)[^【】]+";
                                Pattern pattern = Pattern.compile(regex);
                                // 唯一标识
                                // 科目编码-科目名称-辅助段-账套
//                                科目编码-科目名称-辅助段-账套String  onlySign = data.getC() + data.getU()+ data.getO();
//                                if (data.getM() == null || data.getM().isEmpty()) {
//                                    onlySign += (data.getK() == null ? "" : data.getK());
//                                }else {
//                                    onlySign+=data.getM();
//                                }
//                                otherInfo3.setOnlySign(onlySign);
//                                otherInfo3.setSystemForm("老系统");
//                                res.add(otherInfo3);
                            }
                        }))
                .sheet(companyName).doRead();
        return null;
    }
}
