package org.example.word;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class DocxReader2 {
    public static String getFirstPageContent(String filePath) {
        StringBuilder content = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // 获取所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            // 遍历段落并拼接内容
            for (XWPFParagraph paragraph : paragraphs) {
                // 拼接段落内容
                content.append(paragraph.getText()).append("\n");

                // 根据需要调整读取条件，这里假设读取一定数量的段落代表第一页
                if (content.toString().contains("第 - 1 - 页 共")) { // 判断是否到了第一页末尾
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading file.";
        }

        return content.toString().trim();
    }

    public static void main(String[] args) {
        ZipSecureFile.setMinInflateRatio(-1.0d);
        String filePath = "/Users/tutu/Downloads/project/idea/maven_demo/src/main/java/org/example/word/test.docx"; // 替换为您的文件路径
        String firstPage = getFirstPageContent(filePath);
        System.out.println("第一页内容：");
        System.out.println(firstPage);
    }
}
