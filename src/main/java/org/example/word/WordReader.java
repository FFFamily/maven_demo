package org.example.word;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class WordReader {
    public static void main(String[] args) throws IOException {
        ZipSecureFile.setMinInflateRatio(-1.0d);
        FileInputStream fis = new FileInputStream("/Users/tutu/Downloads/project/idea/maven_demo/src/main/java/org/example/word/test.docx");
        XWPFDocument document = new XWPFDocument(fis);

        // 获取文档中的所有段落
        List<XWPFParagraph> paragraphs = document.getParagraphs();

        // 控制读取的页数，假设第一页的内容就能完全在这里读取
        int count = 0; // 计数器，限制读取的段落数量
        for (XWPFParagraph paragraph : paragraphs) {
            if (count == 20) { // 假设第一页最多20个段落
                break;
            }
            for (XWPFRun run : paragraph.getRuns()) {
                System.out.print(run.text()); // 打印段落的文本内容
            }
            System.out.println();
            count++;
        }

        fis.close();
    }
}
