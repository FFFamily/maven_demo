package org.example.对比Excel;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.func_two.OtherInfo2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FindDiffExcel {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("再手段");
        list.add("发");
        list.add("腌");
        List<String> re = list.stream()
                .sorted((a, b) -> {
                    String[] al = PinyinUtil.getFirstLetter(a, ",").split(",");
                    String[] bl = PinyinUtil.getFirstLetter(b, ",").split(",");
                    int size = Math.min(al.length,bl.length);
                    ;
                    for (int i = 0; i < size; i++) {
                        int aIndex = al[i].charAt(0) - 'a';
                        int bIndex = bl[i].charAt(0) - 'a';
                        if (aIndex == bIndex) {
                            continue;
                        }
                        return aIndex - bIndex;
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        System.out.println(re);
//        List<Map<Integer,Object>> list1 = new ArrayList<>();
//        List<Map<Integer,Object>> list2 = new ArrayList<>();
//        String fileName1 = "src/main/java/org/example/对比excel/excel/excel1.xlsx";
//        EasyExcel.read(fileName1, new PageReadListener<Map<Integer,Object>>(list1::addAll)).sheet().doRead();
//        String fileName2 = "src/main/java/org/example/对比excel/excel/excel2.xlsx";
//        EasyExcel.read(fileName2, new PageReadListener<Map<Integer,Object>>(list1::addAll)).sheet().doRead();
//
//        int size1 = list1.size();
//        int size2 = list2.size();
//        int size = size1 <= size2 ? size1 : size2;
//        List<Map<Integer,Object>> result = new ArrayList<>();
//        for (int i = 0; i < size; i++) {
//            Map<Integer, Object> item1;
//            Map<Integer, Object> item2;
//            if (i < size1){
//                item1 = list1.get(i);
//            }
//            if (i < size2){
//                item2 = list2.get(i);
//            }
//            if (!item1.get(1).equals(item2.get(1))){
//                // 匹配上了
//            }
//        }
//
//
//        try (ExcelWriter excelWriter = EasyExcel.write("findDiffExcel.xlsx").build()) {
//            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "1").build();
//            excelWriter.write(result, writeSheet1);
//        }
    }


    public void sortList(List<Map<Integer,Object>> list){

    }
}
