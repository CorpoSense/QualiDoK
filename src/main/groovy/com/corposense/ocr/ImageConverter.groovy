package com.corposense.ocr

import com.google.inject.Inject
import groovy.transform.CompileStatic
import org.im4java.core.IM4JavaException

@CompileStatic
class ImageConverter {

    @Inject
    ImageConverter(){
    }

    /**
     * Create a searchable PDF containing only text
     * @param inputFile
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws IM4JavaException
     */
    String createTextOnlyPdf(String inputFile) throws IOException, InterruptedException, IM4JavaException {
        ImageProcessing image = new ImageProcessing(inputFile);
        String imageDeskew = image.rotateImage(inputFile, 1);
        String imageNBorder = image.removeBorder(imageDeskew,1);
        String binaryInv = image.binaryInverse(imageNBorder, 1);
        String finalImage = image.imageTransparent(imageNBorder,binaryInv, 1);
        String textOnlyFileName = 'textonly_pdf_'
        // configfileValue = 0->make the image visible, =1->make the image invisible
        SearchableImagePdf createPdf = new SearchableImagePdf(finalImage,
                textOnlyFileName, "0")
        createPdf.textOnlyPdf(finalImage, 1)
        println("getting the size and the location of the image from ${textOnlyFileName}")
        return imageNBorder;
    }

    /**
     * Produce text form input file
     * @param inputFile
     * @return full text of the generated text
     * @throws IOException
     * @throws InterruptedException
     * @throws IM4JavaException
     * TODO: Need to apply a resize
     */
    String produceText(String inputFile) throws IOException, InterruptedException, IM4JavaException {
        ImageProcessing image = new ImageProcessing(inputFile)
        String imageDeskew = image.rotateImage(inputFile, 1)
        String imageNBorder = image.removeBorder(imageDeskew,1)
        String binaryInv = image.binaryInverse(imageNBorder, 1)
        String finalImage = image.imageTransparent(imageNBorder,binaryInv, 1)

        //Extract text from the image.
        ImageText ocr = new ImageText(finalImage)
        String fullText = ocr.generateText()

        println("Creating pdf document...")

        return fullText
    }
}
