package org.example.pdf;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFParserExample {
    public static void main(String[] args) {
        // PDF 文件路径
        String pdfFilePath = "/Users/tutu/Downloads/project/idea/maven_demo/src/main/java/org/example/pdf/test.pdf";
//        PDDocument pdDocument = new PDDocument();
        try (PDDocument document = Loader.loadPDF(new File(pdfFilePath))) {
            if (!document.isEncrypted()) {
                // 创建 PDFTextStripper 对象
                PDFTextStripper textStripper = new PDFTextStripper();

                // 设置只读取第一页
                textStripper.setStartPage(1);
                textStripper.setEndPage(1);
                // 提取第一页的文本内容
                String text = textStripper.getText(document);
//                System.out.println(text);
                String regex = "编号：\\s*(\\w+)";
                Pattern pattern = Pattern.compile(regex);


                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    System.out.println(matcher.group(1));
                }

                String regex2 = "项目名称：\\s*(.+)";
                Pattern pattern2 = Pattern.compile(regex2);
                Matcher matcher2 = pattern2.matcher(text);
                while (matcher2.find()) {
                    System.out.println(matcher2.group(1));
                }
            } else {
                System.out.println("PDF 文件已加密，无法读取内容。");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

