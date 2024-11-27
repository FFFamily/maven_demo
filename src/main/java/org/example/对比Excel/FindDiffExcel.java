package org.example.对比Excel;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.对比Excel.entity.DiffExcelData1;
import org.example.对比Excel.entity.DiffExcelData2;

import java.math.BigDecimal;
import java.util.*;

public class FindDiffExcel {
    public static void main(String[] args) {
        List<DiffExcelData1> list1 = new ArrayList<>();
        List<DiffExcelData2> list2 = new ArrayList<>();
        String fileName1 = "src/main/java/org/example/对比excel/excel/2022年1-4月NC科目辅助余额表-0506.xlsx";
        EasyExcel.read(fileName1,DiffExcelData1.class, new PageReadListener<DiffExcelData1>(list1::addAll))
                .sheet("2022年1-4月NC账面数-六大往来")
                .doRead();
        String fileName2 = "src/main/java/org/example/对比excel/excel/CRC_B00_GL_辅助核算余额 _211124-禹洲切换日22.5-12(六大往来）.xlsx";
        EasyExcel.read(fileName2, DiffExcelData2.class,new PageReadListener<DiffExcelData2>(list2::addAll))
                .sheet("Sheet1")
//                .headRowNumber(6)
                .doRead();

        // 对旧版进行排序
        // step1 辅助核算字段
//        list1.sort((a, b) ->  sortPinYin(a.getK(), b.getK()));
//        // 金额
//        list1.sort((a,b) -> {
//            if (a.getL() != null && b.getL() != null){
//                return a.getL().compareTo(b.getL());
//            }
//            return -1;
//        });
//        // step2  科目
//        list1.sort((a,b) -> sortPinYin(a.getM(), b.getM()));
//        // 公司
//        list1.sort((a,b) -> sortPinYin(a.getN(), b.getN()));
//        // 辅助核算字段
//        list2.sort((a, b) -> sortPinYin(a.getL(), b.getL()));
//        // 金额
//        list2.sort((a,b) -> {
//            if (a.getM() != null && b.getM() != null){
//                return a.getM().compareTo(b.getM());
//            }
//            return -1;
//        });
//        // 科目
//        list2.sort((a, b) -> sortPinYin(a.getN(), b.getN()));
//        // 分公司
//        list2.sort((a, b) -> sortPinYin(a.getO(), b.getO()));


//        list1.sort(Comparator.comparing(DiffExcelData1::getK, Comparator.nullsLast(FindDiffExcel::sortPinYin))
//                .thenComparing(DiffExcelData1::getL, Comparator.nullsLast(BigDecimal::compareTo))
//                .thenComparing(DiffExcelData1::getM, Comparator.nullsLast(FindDiffExcel::sortPinYin))
//                .thenComparing(DiffExcelData1::getN, Comparator.nullsLast(FindDiffExcel::sortPinYin)));
//
//        list2.sort(Comparator
//                .comparing(DiffExcelData2::getL, Comparator.nullsLast(FindDiffExcel::sortPinYin))
//                .thenComparing(DiffExcelData2::getM, Comparator.nullsLast(BigDecimal::compareTo))
//                .thenComparing(DiffExcelData2::getN, Comparator.nullsLast(FindDiffExcel::sortPinYin))
//                .thenComparing(DiffExcelData2::getO, Comparator.nullsLast(FindDiffExcel::sortPinYin)));
        Deque<DiffExcelData2> deque2 = new LinkedList<>(list2);
        Deque<DiffExcelData1> deque1 = new LinkedList<>(list1);

        // TODO 对新版进行排序
        // 已匹配
        List<List<Object>> matchResult = new ArrayList<>();
        // 无法匹配
        List<List<Object>> notMatchResultLeft = new ArrayList<>();
        List<List<Object>> notMatchResultRight = new ArrayList<>();
        int size1 = list1.size();
        int size2 = list2.size();
        int minSize = Math.min(size1, size2);
        while (!deque1.isEmpty()){
            DiffExcelData1 item1 = deque1.pop();
            DiffExcelData2 item2 = deque2.isEmpty() ? null : deque2.pop();
            compareExcel(item1,item2,deque1,deque2,matchResult,notMatchResultLeft,notMatchResultRight);
        }

        while (!deque2.isEmpty()){
            DiffExcelData1 item1 = deque1.isEmpty() ? null : deque1.pop();
            DiffExcelData2 item2 = deque2.pop();
            compareExcel(item1,item2,deque1,deque2,matchResult,notMatchResultLeft,notMatchResultRight);
        }


        try (ExcelWriter excelWriter = EasyExcel.write("findDiffExcel"+System.currentTimeMillis()+".xlsx").build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "命中").head(head()).build();
            excelWriter.write(matchResult, writeSheet1);
            WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "左表").head(head()).build();
            excelWriter.write(notMatchResultLeft, writeSheet2);
            WriteSheet writeSheet3 = EasyExcel.writerSheet(2, "右表").head(head()).build();
            excelWriter.write(notMatchResultRight, writeSheet3);

        }
    }

    public static void compareExcel(DiffExcelData1 item1, DiffExcelData2 item2,
                                    Deque<DiffExcelData1> deque1,
                                    Deque<DiffExcelData2> deque2,
                                    List<List<Object>> matchResult,
                                    List<List<Object>> notMatchResultLeft,
                                    List<List<Object>> notMatchResultRight){
        String type;
        if (item1 != null && item2 != null){
            if (item1.getL() != null && item2.getM() != null) {
                int compareValue = item1.getL().compareTo(item2.getM());
                if(compareValue == 0){
                    // 匹配
                    type = "TRUE";
                    List<Object> data = pushData(item1, item2,type);
                    matchResult.add(data);
                }else if (compareValue > 0){
                    List<Object> data = pushData(item1, null,"FALSE");
                    notMatchResultLeft.add(data);
                    deque2.addFirst(item2);
                }else {
                    List<Object> data = pushData(null, item2,"FALSE");
                    notMatchResultRight.add(data);
                    deque1.addFirst(item1);
                }
            }else {
                type = "比较字段存在空值";
                List<Object> data = pushData(item1, item2,type);
                matchResult.add(data);
            }
        }else {
            type = "无法判断";
            List<Object> data = pushData(item1, item2,type);
            matchResult.add(data);
        }
    }

    public static int sortPinYin(String a, String b){
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        String[] al = PinyinUtil.getFirstLetter(a, ",").split(",");
        String[] bl = PinyinUtil.getFirstLetter(b, ",").split(",");
        int pinyinSize = Math.min(al.length,bl.length);
        for (int i = 0; i < pinyinSize; i++) {
            char c1 = al[i].charAt(0);
            char c2 = bl[i].charAt(0);
            if (c1 != c2) {
                return Character.compare(c1, c2); // 使用安全的字符比较
            }
        }
//            return 0;
        // 长度比较，避免溢出
        return Integer.compare(al.length, bl.length);

    }

    private static List<List<String>> head() {
        List<List<String>> list = ListUtils.newArrayList();
//        List<String> head0 = ListUtils.newArrayList();
//        head0.add("科目编码");
        List<String> head1 = ListUtils.newArrayList();
        head1.add("科目编码");
        List<String> head2 = ListUtils.newArrayList();
        head2.add("科目名称");
        List<String> head3 = ListUtils.newArrayList();
        head3.add("辅助核算");
        List<String> head4 = ListUtils.newArrayList();
        head4.add("核算账簿名称");
        List<String> head5 = ListUtils.newArrayList();
        head5.add("分公司");
        List<String> head6 = ListUtils.newArrayList();
        head6.add("方向");
        List<String> head7 = ListUtils.newArrayList();
        head7.add("期末余额");
        List<String> head8 = ListUtils.newArrayList();
        head8.add("科目-分列");
        List<String> head9 = ListUtils.newArrayList();
        head9.add("借正贷负");
        List<String> head10 = ListUtils.newArrayList();
        head10.add("是否内部抵消");
        List<String> head11 = ListUtils.newArrayList();
        head11.add("排序1：辅助核算");
        List<String> head12 = ListUtils.newArrayList();
        head12.add("排序2：金额（借正贷负）");
        List<String> head13 = ListUtils.newArrayList();
        head13.add("排序3：科目");
        List<String> head14 = ListUtils.newArrayList();
        head14.add("排序4：分公司");
        List<String> headMerge = ListUtils.newArrayList();
        headMerge.add("区分列");
        List<String> head15 = ListUtils.newArrayList();
        head15.add("科目代码");
        List<String> head16 = ListUtils.newArrayList();
        head16.add("科目名称");
        List<String> head17 = ListUtils.newArrayList();
        head17.add("辅助核算段");
        List<String> head18 = ListUtils.newArrayList();
        head18.add("期初借方");
        List<String> head19 = ListUtils.newArrayList();
        head19.add("期初贷方");
        List<String> head20 = ListUtils.newArrayList();
        head20.add("本期借方");
        List<String> head21 = ListUtils.newArrayList();
        head21.add("本期贷方");
        List<String> head22 = ListUtils.newArrayList();
        head22.add("期末借方");
        List<String> head23 = ListUtils.newArrayList();
        head23.add("期末贷方");
        List<String> head24 = ListUtils.newArrayList();
        head24.add("本年借方");
        List<String> head25 = ListUtils.newArrayList();
        head25.add("本年贷方");
        List<String> head26 = ListUtils.newArrayList();
        head26.add("排序1：辅助核算");
        List<String> head27 = ListUtils.newArrayList();
        head27.add("排序2：金额（借正贷负）");
        List<String> head28 = ListUtils.newArrayList();
        head28.add("排序3：科目");
        List<String> head29 = ListUtils.newArrayList();
        head29.add("排序4：分公司");
//        list.add(head0);
        list.add(head1);
        list.add(head2);
        list.add(head3);
        list.add(head4);
        list.add(head5);
        list.add(head6);
        list.add(head7);
        list.add(head8);
        list.add(head9);
        list.add(head10);
        list.add(head11);
        list.add(head12);
        list.add(head13);
        list.add(head14);
        list.add(headMerge);
        list.add(head15);
        list.add(head16);
        list.add(head17);
        list.add(head18);
        list.add(head19);
        list.add(head20);
        list.add(head21);
        list.add(head22);
        list.add(head23);
        list.add(head24);
        list.add(head25);
        list.add(head26);
        list.add(head27);
        list.add(head28);
        list.add(head29);
        return list;
    }

    public static List<Object> pushData(DiffExcelData1 item1, DiffExcelData2 item2, String type){
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

        data.add(type);

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
