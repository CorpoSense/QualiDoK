package com.corposense.ocr

import com.itextpdf.text.DocumentException
import org.im4java.core.IM4JavaException

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

    int countAndExtract(String inputFile){
        int imageNum = ExtractImage.countImage(inputFile)
        ExtractImage.takeImageFromPdf(inputFile)
        return imageNum
    }
}
