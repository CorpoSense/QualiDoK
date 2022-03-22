package com.corposense.ocr

import com.google.inject.Inject
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException

import java.nio.file.Paths

class ImageText extends Tesseract {


    static final String TESSERACT_DATA_PATH = System.getenv("TESSDATA_PREFIX")
    private String imagePath
    String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath().toString()
    File dir = new File(dirPath)

    @Inject
    ImageText(String imagePath) {
        this.imagePath = imagePath;
    }

    String generateText() {
        this.setTessVariable("user_defined_dpi", "300");
        this.setDatapath(TESSERACT_DATA_PATH)
        //set the English+Arabic+French language (need to be dynamic)
        this.setLanguage("ara+eng+fra")
        String fullText = null
        try {
            fullText = this.doOCR(new File(dir,imagePath))
        } catch (TesseractException e) {
            println("TesseractException: ${e.message}")
        }
        return fullText
    }

}
