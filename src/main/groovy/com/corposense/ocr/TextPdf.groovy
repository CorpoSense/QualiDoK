package com.corposense.ocr

import com.google.inject.Inject
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import org.im4java.core.IM4JavaException

import java.nio.file.Paths

class TextPdf {

    final String fullText
    final String docPath

    String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath().toString()
    File dir = new File(dirPath)

    @Inject
    TextPdf(String fullText, String docPath) {
        this.fullText = fullText;
        this.docPath = docPath;
    }

    String generateDocument(String fullText , int number) throws FileNotFoundException, DocumentException {
        Document document = null
        //2) Get a PdfWriter instance
        String doc = ''
        try {
            document = new Document(PageSize.LETTER)
            doc = new File(dir, docPath).toString()
            FileOutputStream fos = new FileOutputStream(doc)
            println("File will be created at: " + new File(dir,this.docPath).path)
            PdfWriter.getInstance(document, fos)
            //3) Open the Document
            document.open()
            //4) Add content
            Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK)
            /*Paragraph paragraph1 = new Paragraph("****** Result for Image/Page "+number+" ******")
                Paragraph paragraph2 = new Paragraph(fullText, font)
                document.add(paragraph1)
                document.add(paragraph2)*/
        } catch (Exception e) {
            println("Error (${e.getClass().simpleName}): ${e.message}")
        } finally {
            //5) Close the document
            if (document){
                document.close()
            }
        }
        return doc
    }

    static void createTextOverlay(int pageNum) throws DocumentException,
            IOException, InterruptedException, IM4JavaException {
        for( int i = 1 ; i <= pageNum; i++){
            String extractedImgName = "ExtractedImage_" + i + ".png"
            ImageProcessing image = new ImageProcessing(extractedImgName)
            String imageDeskew = image.deskewImage(extractedImgName, i)
            String imageNBorder = image.removeBorder(imageDeskew,i)
            String binaryInv = image.binaryInverse(imageNBorder, i)
            String finalImage = image.imageTransparent(imageNBorder, binaryInv, i)

            //Extract text from the image.
            ImageText ocr = new ImageText(finalImage)
            String fulltext = ocr.generateText()

            println("Creating pdf document...")
            TextPdf textPdf = new TextPdf(fulltext, "./ocrDemo_pdf_" + i + ".pdf")
            println("Document " + i + " created.")
            textPdf.generateDocument(fulltext, i)
        }
    }

}
