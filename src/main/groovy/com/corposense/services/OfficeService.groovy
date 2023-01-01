package com.corposense.services

import com.corposense.Constants
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument


class OfficeService extends ImageService{

    File convertDocToPdf(File inputFile){
        Document document = null
        HWPFDocument doc = null
        File pdfDoc = null
        WordExtractor wordExtractor = null
        try{
            FileInputStream docFile = new FileInputStream(inputFile)
            String fileName = getFileNameWithoutExt(inputFile,'.pdf')
            doc = new HWPFDocument(docFile)
            wordExtractor = new WordExtractor(doc)
            String text = wordExtractor.getText()
            log.info("Doc text: ${text}")
            pdfDoc = new File("${Constants.downloadPath}", fileName)
            OutputStream fos = new FileOutputStream(pdfDoc.toString())
            wordExtractor.close()
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
            if(doc){
                doc.close()
            }
            if(wordExtractor){
                wordExtractor.close()
            }
        }
        return pdfDoc
    }
    File ConvertDocxToPdf(File inputFile){
        Document document = null
        XWPFDocument docx = null
        File pdfDoc = null
        XWPFWordExtractor wordExtractor = null
        try{
            FileInputStream docFile = new FileInputStream(inputFile)
            String fileName = getFileNameWithoutExt(inputFile,'.pdf')
            docx = new XWPFDocument(docFile)

            wordExtractor = new XWPFWordExtractor(docx)
            String text = wordExtractor.getText()
            log.info("Docx text: ${text}")
            
            pdfDoc = new File("${Constants.downloadPath}", fileName)
            OutputStream fos = new FileOutputStream(pdfDoc.toString())
            wordExtractor.close()
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
            if(docx){
                docx.close()
            }
            if(wordExtractor){
                wordExtractor.close()
            }
        }
        return pdfDoc

    }
}
