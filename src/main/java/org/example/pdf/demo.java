package org.example.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class demo {
    public static void main(String[] args) {
        try {
            File file = new File("/Users/tutu/Downloads/project/idea/maven_demo/src/main/java/org/example/pdf/test.pdf"); // 替换为你的PDF文件路径
            PDDocument document = Loader.loadPDF(file);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            String[] split = text.split("\n");
            for (String s : split) {
                System.out.println("s = " + s);
            }
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
