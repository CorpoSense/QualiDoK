package com.corposense.ocr

import com.google.inject.Inject
import groovy.transform.CompileStatic
import org.im4java.core.IM4JavaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

@CompileStatic
class ImageConverter {

    final Logger log = LoggerFactory.getLogger(ImageConverter)

    @Inject
    ImageConverter(){
    }

    /**
     * Create a searchable PDF containing only text
     * @param inputFile
     * @return Output file name
     * @throws IOException
     * @throws InterruptedException
     * @throws IM4JavaException
     */
    String produceTextOnlyPdf(String inputFile, int visibleImageLayer = 0) throws IOException, InterruptedException, IM4JavaException {
        int num = 1
        // ImageProcessing image = new ImageProcessing(inputFile)
        ImageProcessing image = new ImageProcessing()
        File uploadedImg = new File(inputFile)
        log.info('Image resizing...')
        File imageResize = image.resizeImage(uploadedImg,num )
        log.info('Image deskew...')
        String imageDeskew = image.deskewImage(imageResize, num)
        log.info('Removing borders...')
        String imageNBorder = image.removeBorder(imageDeskew,num)
        log.info('Binary inversion...')
        String binaryInv = image.binaryInverse(imageNBorder, num)
        log.info('Generating output image...')
        String finalImage = image.imageTransparent(imageNBorder, binaryInv, num)
        String textOnlyFileName = 'textonly_pdf_'
        // configFileValue = 0->make the image visible, =1->make the image invisible
        SearchableImagePdf createPdf = new SearchableImagePdf(finalImage,
                textOnlyFileName, "${visibleImageLayer}")
        createPdf.textOnlyPdf(finalImage, num)
        String outputFileName = "generatedFiles/createdFiles/${textOnlyFileName}${num}.pdf"
        if (new File(outputFileName).exists()){
            log.info("getting the size and the location of the image from ${outputFileName}")
        } else {
            log.info("Wrong path of: ${outputFileName}")
        }
        return outputFileName
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
        //ImageProcessing image = new ImageProcessing(inputFile)
        ImageProcessing image = new ImageProcessing()
        File uploadedImg = new File(inputFile)
        log.info('Image resizing...')
        File imageResize = image.resizeImage(uploadedImg,1 )
        log.info('Image deskew...')
        String imageDeskew = image.deskewImage(imageResize, 1)
        log.info('Removing borders...')
        String imageNBorder = image.removeBorder(imageDeskew,1)
        log.info('Binary inversion...')
        String binaryInv = image.binaryInverse(imageNBorder, 1)
        log.info('Generating final image...')
        String finalImage = image.imageTransparent(imageNBorder,binaryInv, 1)
        //Extract text from the image.
        ImageText ocr = new ImageText(finalImage)
        log.info("OCR processing...")
        String fullText = ocr.generateText()
        return fullText
    }
}
