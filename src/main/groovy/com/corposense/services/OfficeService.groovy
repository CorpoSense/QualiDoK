package com.corposense.services

import com.corposense.Constants
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.poi.EncryptedDocumentException
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.ooxml.extractor.ExtractorFactory
import org.apache.poi.poifs.crypt.Decryptor
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.GeneralSecurityException


class OfficeService {
    //Create tika instance with the default configuration
    //Tika tika = new Tika()
    final Logger log = LoggerFactory.getLogger("OfficeService.class")
    ImageService imageService = new ImageService()

    //extract the content of a file and return the result as a String – using the Parser API:
    /*String extractTextOfDocument(File file) throws Exception {
        InputStream fileStream = new FileInputStream(file);
        Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);

        TesseractOCRConfig config = new TesseractOCRConfig();
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);

        // To parse images in files those lines are needed
        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, config);
        parseContext.set(PDFParserConfig.class, pdfConfig);
        parseContext.set(Parser.class, parser); // need to add this to make sure
        // recursive parsing happens!
        try {
            parser.parse(fileStream, handler, metadata, parseContext);
            String text = handler.toString();
            if (text.trim().isEmpty()) {
                log.warn("Could not extract text of: ${file.name}");
            } else {
                log.debug("Successfully extracted the text of: ${file.name}");
            }
            return text;
        } catch (IOException | SAXException | TikaException e) {
            throw new Exception("TIKA was not able to exctract text of file: ${file.name}", e);
        } finally {
            try {
                fileStream.close();
            } catch (IOException e) {
                throw new Exception(e);
            }
        }
    }

     */

    //File convertMsWordToPdf(File inputFile) throws IOException, TikaException{
        //String text = textExtractor(inputFile)
        //String text = extractTextOfDocument(inputFile)
        //File pdf = createPdf(inputFile,text)
//        String type = tika.detect(inputFile)
//        log.info("type of is: ${type}")
       // return pdf
   // }
    File convertDocToPdf(File inputFile){
        String text = extractTextDoc(inputFile)
        File pdfDoc = createPdf(inputFile,text)
        return pdfDoc
    }
    File convertDocxToPdf(File inputFile){
        String text = extractTextDocx(inputFile)
        File pdfDoc = createPdf(inputFile,text)
        return pdfDoc
    }

/*
    //extract content using facade
    private String textExtractor(File inputFile) {
        String text = tika.parseToString(inputFile)
        //log.info("using tika for extracting text: ${text}")
        return text
    }

 */

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

    static boolean isPwdProtected(File inputFile) {
        try {
            ExtractorFactory.createExtractor(new FileInputStream(inputFile));
        } catch (EncryptedDocumentException e) {
            return true
        } catch (Exception e) {
            Throwable[] throwables = ExceptionUtils.getThrowables(e)
            for (Throwable throwable : throwables) {
                if (throwable instanceof EncryptedDocumentException) {
                    return true
                }
            }
        }
        return false
    }

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

}
