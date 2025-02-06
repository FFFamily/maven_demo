package org.example.word;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class demo {
    public static void main(String[] args) {
        ZipSecureFile.setMinInflateRatio(-1.0d);
        System.out.println(getContentDocx("/Users/tutu/Downloads/project/idea/maven_demo/src/main/java/org/example/word/test.docx"));
    }

    /**
     * 获取正文文件内容，docx方法
     *
     * @param path
     * @return
     */
    public static Map<String, String> getContentDocx(String path) {
        Map<String, String> map = new HashMap<>();
        StringBuilder content = new StringBuilder();
        String result = "0";  // 0表示获取正常，1表示获取异常
        InputStream is = null;
        try {
            is = Files.newInputStream(new File(path).toPath());
            XWPFDocument document = new XWPFDocument(is);
//            System.out.println(new XWPFWordExtractor(xwpf).getText());
            List<XWPFParagraph> paragraphs = document.getParagraphs();

//            for (XWPFParagraph paragraph : paragraphs) {
//                if (xwpf.getPosOfParagraph(paragraph) >= xwpf.getPosOfParagraph(paragraphs.get(0)) + xwpf.getParagraphs().size()) {
//                    break; // 超过第一页，停止读取
//                }
//                System.out.println(paragraph.getParagraphText());
//            }


            String regex = "编号：\\s*(\\w+)";
            Pattern pattern = Pattern.compile(regex);


            XWPFWordExtractor xwpfWordExtractor = new XWPFWordExtractor(document);
            String text = xwpfWordExtractor.getText();
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
//            System.out.println(text);
//            POIXMLDocument document = xwpfWordExtractor.getDocument();
//            StringBuilder text = new StringBuilder(64);
//            XWPFHeaderFooterPolicy hfPolicy = document.getHeaderFooterPolicy();

            // Start out with all headers
//            extractHeaders(text, hfPolicy);

            // Process all body elements
//            for (IBodyElement e : document.getBodyElements()) {
//                System.out.println(e);
//                e.getBody().getParagraphs().forEach(item -> System.out.println(item.getBody()));
//                text.append('\n');
//            }

            // Finish up with all the footers
//            extractFooters(text, hfPolicy);

//            return text.toString();
//            POIXMLDocument document = xwpfWordExtractor.getDocument();

            System.out.println();
//            document.getAllEmbeddedParts().forEach(item -> item.get);
        } catch (Exception e) {
            e.printStackTrace();
            result = "1"; // 出现异常
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {

                }
            }
        }
        return map;
    }
}
