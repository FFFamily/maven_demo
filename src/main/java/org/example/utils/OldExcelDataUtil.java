package org.example.utils;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import org.example.寻找等级.OtherInfo3;
import org.example.寻找等级.old_excel.OldExcelTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OldExcelDataUtil {
    public static List<OtherInfo3> getOldExcel(String path, String sheetName) {
        List<OtherInfo3> data = new ArrayList<>();
        EasyExcel.read(path, OldExcelTemplate.class, new PageReadListener<OldExcelTemplate>(dataList -> {
            for (OldExcelTemplate oldExcelTemplate : dataList) {
                OtherInfo3 otherInfo3 = new OtherInfo3();
                String year = oldExcelTemplate.getA().split("年")[0];
                String month = oldExcelTemplate.getB();
                String day = oldExcelTemplate.getC();
                String dateStr = year + "-" + month + "-" + day;
                // 公司
                otherInfo3.setCompanyName(oldExcelTemplate.getCompanyName());
                // 总账日期
                otherInfo3.setN(DateUtil.parse(dateStr));
                // 凭证号
                otherInfo3.setQ(Integer.valueOf(oldExcelTemplate.getD().split("-")[1]));
                // 拼接凭证号
                otherInfo3.setR(year + "-" + month + otherInfo3.getQ());
                // 来源随便写一个，以便于分级查找的时候不被拦截
                otherInfo3.setS("人工");
                // 借
                otherInfo3.setV(oldExcelTemplate.getL());
                // 贷
                otherInfo3.setW(oldExcelTemplate.getN());
                otherInfo3.setX(CommonUtil.getX(otherInfo3.getV(), otherInfo3.getW()));
                // TODO 余额
                String regex = "(?<=：)[^【】]+";
                Pattern pattern = Pattern.compile(regex);
                // 唯一标识
                otherInfo3.setOnlySign(oldExcelTemplate.getG());
                if (oldExcelTemplate.getI() != null) {
                    Matcher matcher = pattern.matcher(oldExcelTemplate.getI());
                    while (matcher.find()) {
                        otherInfo3.setOnlySign(otherInfo3.getOnlySign() + "-" + matcher.group().trim());
                    }
                }
                // ncc 科目
                otherInfo3.setNccProjectCode(oldExcelTemplate.getG());
                // ncc 辅助核算
                otherInfo3.setNccAssistantCode(oldExcelTemplate.getI());
                otherInfo3.setSystemForm("老系统");
                data.add(otherInfo3);
            }
        })).sheet(sheetName).doRead();
        return data;
    }
}
