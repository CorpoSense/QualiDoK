package com.corposense.ocr

import com.itextpdf.text.DocumentException
import groovy.transform.CompileStatic
import org.im4java.core.IM4JavaException

@CompileStatic
class PdfConverter {

//    @Inject
//    public PdfConverter(){
//    }

    void produceSearchablePdf(String inputFile) throws IOException, DocumentException,
            InterruptedException, IM4JavaException {
        SearchableImagePdf.createSearchablePdf(countAndExtract(inputFile))
    }

    void produceTextOverlay(String inputFile) throws IOException, DocumentException,
            InterruptedException, IM4JavaException {
        TextPdf.createTextOverlay( countAndExtract(inputFile) )
    }

    private int countAndExtract(String inputFile){
        int imageNum = ExtractImage.countImage(inputFile)
        ExtractImage.takeImageFromPdf(inputFile)
        return imageNum
    }
}
