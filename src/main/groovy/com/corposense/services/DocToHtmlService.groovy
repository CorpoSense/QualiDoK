package com.corposense.services

import com.corposense.Constants
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.text.PageSize
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.converter.PicturesManager
import org.apache.poi.hwpf.converter.WordToHtmlConverter
import org.apache.poi.hwpf.usermodel.PictureType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.apache.commons.io.FileUtils


class DocToHtmlService {

    static File convertWord(File inputFile, String fileName) {
        InputStream input = new FileInputStream(inputFile)
        HWPFDocument wordDocument = new HWPFDocument(input)
        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .newDocument())
        wordToHtmlConverter.setPicturesManager(new PicturesManager() {
            @Override
            String savePicture(byte[] content, PictureType pictureType,
                                      String suggestedName, float widthInches, float heightInches) {
                File file = new File("${Constants.downloadPath}" + File.separator + suggestedName)
                FileOutputStream fos
                try {
                    fos = new FileOutputStream(file)
                    fos.write(content)
                    fos.close()
                } catch (Exception e) {
                    e.printStackTrace()
                }
                return suggestedName
            }
        })
        wordToHtmlConverter.processDocument(wordDocument)
        //Extract image from word document
//        List pics = wordDocument.getPicturesTable().getAllPictures()
//        if (pics != null) {
//            for (int i = 0; i < pics.size(); i++) {
//                Picture pic = (Picture) pics.get(i)
//                try {
//                    File image = new File("${Constants.downloadPath}", pic.suggestFullFileName())
//                    println(pic.suggestFullFileName())
//                    pic.writeImageContent(new FileOutputStream(image))
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace()
//                }
//            }
//        }
        Document htmlDocument = wordToHtmlConverter.getDocument()
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        DOMSource domSource = new DOMSource(htmlDocument)
        StreamResult streamResult = new StreamResult(outStream)
        TransformerFactory tf = TransformerFactory.newInstance()
        Transformer serializer = tf.newTransformer()
        serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8")
        serializer.setOutputProperty(OutputKeys.INDENT, "yes")
        serializer.setOutputProperty(OutputKeys.METHOD, "html")
        serializer.transform(domSource, streamResult)
        outStream.close()
        String content = new String(outStream.toByteArray())
        File htmlFile = new File("${Constants.downloadPath}", fileName +".html")
        FileUtils.writeStringToFile(htmlFile, content, "utf-8")

        return htmlFile
    }

}
