package com.corposense.services

import com.corposense.Constants
import fr.opensagres.poi.xwpf.converter.core.ImageManager
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions
import org.apache.commons.io.FileUtils
import org.apache.poi.EncryptedDocumentException
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.converter.PicturesManager
import org.apache.poi.hwpf.converter.WordToHtmlConverter
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.hwpf.usermodel.PictureType
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult



class OfficeService {

    final Logger log = LoggerFactory.getLogger("OfficeService.class")

    /**
     * Return html content from the given input file (.docx document)
     * @param inputFile
     * @return html content
     */
    String docxToHtml(File inputFile){
        String htmlData = null
        try {
            InputStream input = new FileInputStream(inputFile)
            XWPFDocument docxDocument = new XWPFDocument(input)
            File imageFolder = new File("${Constants.downloadPath}")
            //Parse XHTML configuration
            XHTMLOptions options = XHTMLOptions.create()
            //Set the storage path of the image to (downloads/image) (The default storage path is : word/media file )
            options.setImageManager(new ImageManager((imageFolder),"image"))
            options.setIgnoreStylesIfUnused(false)
            //Parse the image address (downloads/image) into the generated html tag
            options.getURIResolver()
            ByteArrayOutputStream htmlStream = new ByteArrayOutputStream()
            XHTMLConverter.getInstance().convert(docxDocument, htmlStream, options)
            htmlData = htmlStream.toString()
            docxDocument.close()
            htmlStream.close()
        } catch (IOException e) {
            log.error ("${e.getClass().simpleName}: ${e.message}")
        }
        return htmlData
    }
    /**
     * Return Html content from the given input file (.doc document)
     * @param inputFile (.doc document)
     * @return Html content
     */
    String wordToHtml(File inputFile) {
        InputStream input = new FileInputStream(inputFile)
        HWPFDocument wordDocument = new HWPFDocument(input)
        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .newDocument())
        /*
        Extract Image
        In order to extract out the image, we have to set the call back functions when converter
        handling the image in doc. And the converter lib provide a class to do this:
         */
        wordToHtmlConverter.setPicturesManager(new PicturesManager() {
            @Override
            String savePicture(byte[] content, PictureType pictureType,
                               String suggestedName, float widthInches, float heightInches) {
                File file = new File("${Constants.downloadPath}"+ File.separator + suggestedName)
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
        return content
    }

    /**
     * Return Html file from parsed input file (.docx) and file name
     * @param inputFile
     * @param fileName
     * @return html file
     */
    File convertDocxToHtml(File inputFile, String fileName) {
        String html = docxToHtml(inputFile)
        File htmlFile = writeHtmlFile(html,fileName)
        return htmlFile
    }

    /**
     * Return Html file from parsed input file (.doc) and the given file name
     * @param inputFile
     * @param fileName
     * @return html file
     */
    File convertDocToHtml(File inputFile, String fileName){
        String html = wordToHtml(inputFile)
        File htmlFile = writeHtmlFile(html, fileName)
        return htmlFile
    }
    /**
     * Writes a String to a file
     * @param htmlContent
     * @param fileName
     * @return html file
     */
    static File writeHtmlFile(String htmlContent, String fileName) {
        File htmlFile = new File("${Constants.downloadPath}", fileName +".html")
        FileUtils.writeStringToFile(htmlFile, htmlContent, "utf-8")
        return htmlFile
    }

    /**
     * Tests whether the input document is password protected
     * @param inputFile can be .Doc or .Docx
     * @return false
     */
    static boolean isPwdProtected(File inputFile){
        if(inputFile.toString().endsWith('.doc')){
            try {
                HWPFDocument doc = new HWPFDocument(new FileInputStream(inputFile))
                new WordExtractor(doc)
            } catch (EncryptedDocumentException e) {
                return true
            }
            return false
        }else if(inputFile.toString().endsWith('.docx')) {
            try {
                XWPFDocument docx = new XWPFDocument(new FileInputStream(inputFile))
                new XWPFWordExtractor(docx)
            } catch (Exception e) {
                return true
            }
            return false
        }
    }

/*
   String officeDocxToHtml(File inputFile) {
        DocumentConverter converter = new DocumentConverter()
        Result<String> result = converter.convertToHtml(inputFile)
        String html = result.getValue() // The generated HTML
        Set<String> warnings = result.getWarnings() // Any warnings during conversion
        return html
    }

 */
/*
    private File createPdf(File inputFile , String text){
        Document document = null
        File pdfDoc = null
        try {
            String fileName = imageService.getFileNameWithoutExt(inputFile,'.pdf')
            pdfDoc = new File("${Constants.downloadPath}", fileName)
            OutputStream fos = new FileOutputStream(pdfDoc.toString())
            //create pdf file
            document = new Document(PageSize.LETTER)
            PdfWriter.getInstance(document, fos)
            document.open()
            document.add(new Paragraph(text))
            log.info("pdf document will be created at: ${Constants.downloadPath}/${pdfDoc.name}")
        } catch (Exception e) {
            log.error ("${e.getClass().simpleName}: ${e.message}")
        } finally {
            if (document){
                document.close()
            }
        }
        return pdfDoc
    }

 */
/*
    String extractTextDoc(File inputFile){
        HWPFDocument doc = null
        WordExtractor wordExtractor = null
        String text = null
        try{
            FileInputStream docFile = new FileInputStream(inputFile)
            doc = new HWPFDocument(docFile)
            wordExtractor = new WordExtractor(doc)
            text = wordExtractor.getText()
            wordExtractor.close()
        } catch (Exception e) {
        log.error ("${e.getClass().simpleName}: ${e.message}")
        } finally {
            if(doc){
                doc.close()
            }
            if(wordExtractor){
                wordExtractor.close()
            }
        }
        return text
    }
    String extractTextDocx(File inputFile) {
        XWPFDocument docx = null
        XWPFWordExtractor wordExtractor = null
        String text = null
        try {
            FileInputStream docFile = new FileInputStream(inputFile)
            docx = new XWPFDocument(docFile)
            wordExtractor = new XWPFWordExtractor(docx)
            text = wordExtractor.getText()
            wordExtractor.close()
        } catch (Exception e) {
            log.error("${e.getClass().simpleName}: ${e.message}")
        } finally {
            if (docx) {
                docx.close()
            }
            if (wordExtractor) {
                wordExtractor.close()
            }
        }
        return text

    }

 */

}
