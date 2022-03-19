package com.corposense.ocr

import com.google.inject.Inject
import org.apache.pdfbox.contentstream.PDFStreamEngine
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.PDXObject
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Paths

class ExtractImage extends PDFStreamEngine{

    String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath()
    File dir = new File(dirPath)

//    @Inject
//    ExtractImage(){
//    }

    private int imageNumber = 1;

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.name
        if (operation == 'Do') {
            COSName objectName = (COSName) operands.get(0)
            PDXObject xObject = getResources().getXObject(objectName)
            if (xObject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xObject

                // save image to local
                BufferedImage bImage = image.getImage()
                println("dirPath: ${dir}");
                String extractedFile = "ExtractedImage_" + imageNumber + ".png"
                File file = new File(dir, extractedFile)
                ImageIO.write(bImage, "PNG", file)
                println("Image saved.")
                imageNumber++

            } else if (xObject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xObject
                showForm(form)
            }
        } else {
            super.processOperator(operator, operands)
        }
    }


    static void takeImageFromPdf (String fileName) throws IOException {
        PDDocument document = null;
        try {
            document = PDDocument.load( new File(fileName) );
            ExtractImage printer = new ExtractImage();
            int pageNum = 0;
            document.pages.each { PDPage page ->
                pageNum++;
                println( "Processing page: " + pageNum )
                printer.processPage(page);
            }
        } catch (Exception e){
          println("Cannot extract image from PDF (${e.getClass().simpleName}): ${e.message}")
        } finally {
            if ( document != null ) {
                document.close()
            }
        }
    }

    static int countImage(String fileName) throws IOException {
        PDDocument doc = null
        int countPages
        try {
            PDDocument.load(new File(fileName));
            countPages = doc.numberOfPages
        } catch (Exception e){

        } finally {
            if (doc){
                doc.close()
            }
        }
        return countPages
    }

}
