package org.example.test_demo;

import org.example.对比Excel.FindDiffExcel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Demo2 {
    public static void main(String[] args) {
        List<String>list=new ArrayList<>();
        list.add("你的");
        list.add("《手段》");
        list.add("~");
        list.sort(FindDiffExcel::sortPinYin);
        System.out.println(list);
    }
}
