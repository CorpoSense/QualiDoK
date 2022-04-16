package com.corposense.ocr

import com.google.inject.Inject
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import groovy.transform.CompileStatic
import org.apache.pdfbox.contentstream.PDFStreamEngine
import org.apache.pdfbox.contentstream.operator.DrawObject
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.contentstream.operator.state.Concatenate
import org.apache.pdfbox.contentstream.operator.state.Restore
import org.apache.pdfbox.contentstream.operator.state.Save
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters
import org.apache.pdfbox.contentstream.operator.state.SetMatrix
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.PDXObject
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.util.Matrix

import java.nio.file.Paths

@CompileStatic
class ImageLocationsAndSize extends PDFStreamEngine {

    private float imageYPosition, imageXPosition, imageXScale, imageYScale;

    /**
     * @throws IOException If there is an error loading text stripper properties.
     */
    @Inject
    ImageLocationsAndSize() throws IOException
    {
        // preparing PDFStreamEngine
        addOperator(new Concatenate())
        addOperator(new DrawObject())
        addOperator(new SetGraphicsStateParameters())
        addOperator(new Save())
        addOperator(new Restore());
        addOperator(new SetMatrix())
    }

    @Override
    void processOperator(Operator operator, List<COSBase> operands) throws IOException
    {
        String operation = operator.getName();
        if( operation == 'Do')
        {
            COSName objectName = (COSName) operands.get( 0 )
            // get the PDF object
            PDXObject xobject = getResources().getXObject( objectName );
            // check if the object is an image object
            if(xobject instanceof PDImageXObject) {
                findImageScaleAndPosition();
            }
            else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject)xobject
                showForm(form)
            }
        } else {
            super.processOperator( operator, operands)
        }
    }

    private void findImageScaleAndPosition() {
        Matrix ctmNew = graphicsState.currentTransformationMatrix
        // displayed size in user space units
        imageXScale = ctmNew.getScalingFactorX()
        imageYScale = ctmNew.getScalingFactorY()
        // position of image in the pdf in terms of user space units
        imageXPosition = ctmNew.getTranslateX()
        imageYPosition = ctmNew.getTranslateY()

    }

    /**
     * Place an image on existing pdf.
     * @param inputFilePath
     * @param outputFilePath
     * @param imgPath
     * @return Output file path
     * @throws DocumentException
     * @throws IOException
     */
    String placeImageOnExistingPdf(String inputFilePath, String outputFilePath, String imgPath )
            throws DocumentException, IOException {
        String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath().toString()
        File dir = new File(dirPath)

        OutputStream file = new FileOutputStream(new File(dir,outputFilePath))
        String inputFile = new File(dir,inputFilePath).toString()

        PdfReader pdfReader = new PdfReader(inputFile)
        PdfStamper pdfStamper = new PdfStamper(pdfReader, file)
        String filePath = ''
        try {
            //Image to be added in existing pdf file.
            String img = new File(dir,imgPath).toString()
            Image image = Image.getInstance(img)
            //Scale image's width and height
            image.scaleAbsolute(imageXScale, imageYScale)
            //Set position for image in PDF
            image.setAbsolutePosition(imageXPosition,imageYPosition)
            // loop through all the PDF pages
            for (int i = 1; i <= pdfReader.numberOfPages; i++) {
                //getOverContent() allows you to write pdfContentByte on TOP of existing pdf pdfContentByte.
                PdfContentByte pdfContentByte = pdfStamper.getOverContent(i)
                pdfContentByte.addImage(image);
            }
            filePath =  new File(dir, outputFilePath).getPath()
            println("Document will be created at: "+ filePath )
        } catch (Exception e) {
            println("Error (${e.getClass().simpleName}): ${e.message}")
        } finally {
            if (pdfStamper){
                pdfStamper.close()
            }
        }
        return filePath
    }

    /**
     * Generate PDF from an an input image
     * @param existingPdfFilePath
     * @param outputFilePath
     * @param imageNBorder
     * @return Output file path
     * @throws IOException
     * @throws DocumentException
     */
    static String createPdfWithOriginalImage(String existingPdfFilePath, String outputFilePath, String imageNBorder)
            throws IOException, DocumentException {
        PDDocument document = null;
        String filePath = ''
        try {
            String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath().toString()
            File dir = new File(dirPath)
            document = PDDocument.load(new File(dir, existingPdfFilePath))
            ImageLocationsAndSize printer = new ImageLocationsAndSize()
            document.pages.each { PDPage page ->
                printer.processPage(page)
            }
            document.close()
            println("Document created.")
            //Place the original image on top of transparent image in existing PDF.
            filePath = printer.placeImageOnExistingPdf(existingPdfFilePath, outputFilePath, imageNBorder)
        } catch (Exception e){
          println("Error (${e.getClass().simpleName}): ${e.message}")
        } finally {
            if (document){
                document.close()
            }
        }
        return filePath
    }

}
