package org.example.word;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class demo2 {
    public static void main(String[] args) {
        demo2 demo2 = new demo2();
        try {
            ZipSecureFile.setMinInflateRatio(-1.0d);
            demo2.handlerByDocxFile(Files.newInputStream(Paths.get("/Users/tutu/Downloads/project/idea/maven_demo/src/main/java/org/example/word/test.docx")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handlerByDocxFile(InputStream is) throws IOException, InvalidFormatException {
        XWPFDocument xwpfDocument = new XWPFDocument(is);
        Iterator<IBodyElement> bodyElementsIterator = xwpfDocument.getBodyElementsIterator();
        List<Object> datas=new ArrayList<>();
        while (bodyElementsIterator.hasNext()) {
            IBodyElement bodyElement = bodyElementsIterator.next();
            String content = handlerByBodyType(bodyElement,bodyElement.getPartType());
            datas.add(content);
        }
        xwpfDocument.close();
        is.close();
        printAllDatas(datas);
    }

    public void printAllDatas(Collection<?> datas) {
        System.out.println(datas);
    }


    //开始处理当前的身体元素
    public String handlerByIBodyElement(IBodyElement bodyElement) {
        String content=null;
        //用于处理XWPFParagraph
        if(bodyElement instanceof XWPFParagraph) {
            System.out.println("当前获取的元素类型为：XWPFParagraph");
            content=handlerXWPFParagraphType(bodyElement);
        }
        return content;
    }

    //用于处理当前的XWPFParagraph类型的数据
    public String handlerXWPFParagraphType(IBodyElement bodyElement) {
        XWPFParagraph xwpfParagraph = (XWPFParagraph) bodyElement;
        BodyElementType elementType = xwpfParagraph.getElementType();
        String content = getStringByBodyElementType(xwpfParagraph,elementType);
        System.out.println("当前文本的内容为："+content);
        return content;
    }

    //通过当前的类型和元素进行相对应的处理
    public String getStringByBodyElementType(XWPFParagraph xwpfParagraph,BodyElementType bodyElementType) {
        System.out.println(bodyElementType);//当前测试结果为：PARAGRAPH
        String content="";
        switch (bodyElementType) {
            case CONTENTCONTROL:
                //如果使用的是文本控件
                break;
            case PARAGRAPH:
                //如果是段落的处理结果
                content=xwpfParagraph.getParagraphText();
                break;
            case TABLE:
                //如果当前的的元素部分为表格
                break;

            default:
                break;
        }
        return content;
    }

    //通过身体类型来处理
    public String handlerByBodyType(IBodyElement bodyElement , BodyType partType) {
        System.out.println("当前的BodyType为："+partType);
        String content=null;
        switch (partType) {
            case CONTENTCONTROL:
                break;
            case DOCUMENT:
                content=handlerByIBodyElement(bodyElement);
                break;
            case HEADER:

                break;
            case FOOTER:

                break;
            case FOOTNOTE:

                break;
            case TABLECELL:

                break;
            default:
                throw new IllegalArgumentException("there is no this document type !please check this type!");
        }
        return content;
    }

}
