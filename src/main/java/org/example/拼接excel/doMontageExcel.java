package org.example.拼接excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.example.Assistant;
import org.example.func_two.OtherInfo2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class doMontageExcel {
    public static final List<Map<Integer, Object>> list =  new ArrayList<>();

    public static void main(String[] args) throws IOException {
        String path = Objects.requireNonNull(montageExcel.class.getClassLoader().getResource("montage/excel")).getPath();
        List<String> fileNames = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
//                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
        String sheetName = "1";
        for (String fileName : fileNames) {
            // 这里 只要，然后读取第一个sheet 同步读取会自动finish
            EasyExcel.read(fileName, new PageReadListener<Map<Integer,Object>>(list::addAll)).sheet(sheetName).doRead();
        }

        try (ExcelWriter excelWriter = EasyExcel.write(sheetName+".xlsx").build()) {
            WriteSheet writeSheet1 = EasyExcel.writerSheet(0, sheetName).build();
            excelWriter.write(list, writeSheet1);
        }

    }
}
