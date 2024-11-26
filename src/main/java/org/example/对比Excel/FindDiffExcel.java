package org.example.对比Excel;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
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
        EasyExcel.read(fileName1, new PageReadListener<DiffExcelData1>(list1::addAll)).sheet().doRead();
        String fileName2 = "src/main/java/org/example/对比excel/excel/excel2.xlsx";
        EasyExcel.read(fileName2, new PageReadListener<DiffExcelData2>(list2::addAll)).sheet().doRead();
        int size1 = list1.size();
        int size2 = list2.size();
        int size = Math.min(size1, size2);
        // 对旧版进行排序
        list1.stream()
                .sorted((a, b) -> {
                    // step1 辅助核算字段
                    int sortRes1 = sortPinYin(a.getK(), b.getK());
                    if (sortRes1 != 0){
                        return sortRes1;
                    }
                    // step2 对科目名称排序
                    int sortRes2 = sortPinYin(a.getL(), b.getL());
                    if (sortRes2 != 0){
                        return sortRes2;
                    }
                    // step3 排序
//                    return sortPinYin(a.getSort3(), b.getSort3());
                    return 0;
                });
        // TODO 对新版进行排序
        // 已匹配
        List<DiffExcelResult> matchResult = new ArrayList<>();
        // 无法匹配
        List<DiffExcelResult> notMatchResult = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            DiffExcelData1 item1 = list1.get(i);
            DiffExcelData2 item2 = list2.get(i);

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
}
