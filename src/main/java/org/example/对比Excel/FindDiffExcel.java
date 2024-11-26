package org.example.对比Excel;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.对比Excel.entity.DiffExcelData1;
import org.example.对比Excel.entity.DiffExcelData2;
import org.example.对比Excel.entity.DiffExcelResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FindDiffExcel {
    public static void main(String[] args) {
        List<DiffExcelData1> list1 = new ArrayList<>();
        List<DiffExcelData2> list2 = new ArrayList<>();
        String fileName1 = "src/main/java/org/example/对比excel/excel/excel1.xlsx";
        EasyExcel.read(fileName1, new PageReadListener<DiffExcelData1>(list1::addAll)).sheet("2022年1-4月NC账面数-六大往来").doRead();
        String fileName2 = "src/main/java/org/example/对比excel/excel/excel2.xlsx";
        EasyExcel.read(fileName2, new PageReadListener<DiffExcelData2>(list2::addAll)).sheet("Sheet1").doRead();
        int size1 = list1.size();
        int size2 = list2.size();
        int minSize = Math.min(size1, size2);
        // 对旧版进行排序
        list1.stream()
                .sorted((a, b) -> {
                    // step1 辅助核算字段
                    int sortRes1 = sortPinYin(a.getK(), b.getK());
                    if (sortRes1 != 0){
                        return sortRes1;
                    }
                    // step3 金额
                    int sortRes3 =  a.getM().compareTo(b.getM());
                    if (sortRes3 != 0){
                        return sortRes3;
                    }
                    // step2  科目
                    int sortRes2 = sortPinYin(a.getL(), b.getL());
                    if (sortRes2 != 0){
                        return sortRes2;
                    }
                    // step 分公司
                    return sortPinYin(a.getN(), b.getN());
                });

        list2.stream()
                .sorted((a, b) -> {
                    // step1 辅助核算字段
                    int sortRes1 = sortPinYin(a.getL(), b.getL());
                    if (sortRes1 != 0){
                        return sortRes1;
                    }
                    // step2 金额
                    int sortRes2 = a.getM().compareTo(b.getM());
                    if (sortRes2 != 0){
                        return sortRes2;
                    }
                    // step3 科目
                    int sortRes3 =  sortPinYin(a.getN(), b.getN());
                    if (sortRes3 != 0){
                        return sortRes3;
                    }
                    // step 分公司
                    return sortPinYin(a.getO(), b.getO());
                });
        // TODO 对新版进行排序
        // 已匹配
        List<List<Object>> matchResult = new ArrayList<>();
        // 无法匹配
        List<List<Object>> notMatchResult = new ArrayList<>();
        for (int i = 0; i < minSize; i++) {
            DiffExcelData1 item1 = list1.get(i);
            DiffExcelData2 item2 = list2.get(i);
            List<Object> data = pushData(item1, item2);
            if(item1.getM().compareTo(item2.getM()) == 0){
                // 匹配
                matchResult.add(data);
            }else {
                notMatchResult.add(data);
            }
        }

        if (size1 < size2){
            for (int i = size1; i < size2; i++) {
                notMatchResult.add(pushData(null,list2.get(i)));
            }
        }else {
            for (int i = size2; i < size1; i++) {
                notMatchResult.add(pushData(list1.get(i),null));
            }
        }
        try (ExcelWriter excelWriter = EasyExcel.write("findDiffExcel.xlsx").build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "1").build();
                excelWriter.write(matchResult, writeSheet1);
        }
    }

    public static int sortPinYin(String a, String b){
        String[] al = PinyinUtil.getFirstLetter(a, ",").split(",");
        String[] bl = PinyinUtil.getFirstLetter(b, ",").split(",");
        int pinyinSize = Math.min(al.length,bl.length);
        for (int i = 0; i < pinyinSize; i++) {
            int aIndex = al[i].charAt(0) - 'a';
            int bIndex = bl[i].charAt(0) - 'a';
            if (aIndex == bIndex) {
                continue;
            }
            return aIndex - bIndex;
        }
        return 0;
    }

    public static List<Object> pushData(DiffExcelData1 item1, DiffExcelData2 item2){
        List<Object> data = ListUtils.newArrayList();
        data.add(item1 == null ? "": item1.getA());
        data.add(item1 == null ? "": item1.getB());
        data.add(item1 == null ? "": item1.getC());
        data.add(item1 == null ? "": item1.getD());
        data.add(item1 == null ? "": item1.getE());
        data.add(item1 == null ? "": item1.getF());
        data.add(item1 == null ? "": item1.getG());
        data.add(item1 == null ? "": item1.getH());
        data.add(item1 == null ? "": item1.getI());
        data.add(item1 == null ? "": item1.getJ());
        data.add(item1 == null ? "": item1.getK());
        data.add(item1 == null ? "": item1.getL());
        data.add(item1 == null ? "": item1.getM());
        data.add(item1 == null ? "": item1.getN());
        data.add("");
        data.add(item2 == null ? "": item2.getA());
        data.add(item2 == null ? "": item2.getB());
        data.add(item2 == null ? "": item2.getC());
        data.add(item2 == null ? "": item2.getD());
        data.add(item2 == null ? "": item2.getE());
        data.add(item2 == null ? "": item2.getF());
        data.add(item2 == null ? "": item2.getG());
        data.add(item2 == null ? "": item2.getH());
        data.add(item2 == null ? "": item2.getI());
        data.add(item2 == null ? "": item2.getJ());
        data.add(item2 == null ? "": item2.getK());
        data.add(item2 == null ? "": item2.getL());
        data.add(item2 == null ? "": item2.getM());
        data.add(item2 == null ? "": item2.getN());
        data.add(item2 == null ? "": item2.getO());
        return data;
    }
}
