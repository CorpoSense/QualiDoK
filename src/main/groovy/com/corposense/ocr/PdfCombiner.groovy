package com.corposense.ocr

import com.google.inject.Inject
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import groovy.transform.CompileStatic

import java.nio.file.Paths

@CompileStatic
class PdfCombiner {

    static String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath().toString()
    static File dir = new File(dirPath)

    @Inject
    PdfCombiner(){
    }

    static void mergePdfDocuments(String uploadedFile, String ocrFile , String outputFile) throws IOException {

        //Loading an existing PDF document
        //Create PDFMergerUtility class object
        PDFMergerUtility PDFmerger = new PDFMergerUtility()

        //Setting the destination file path
        PDFmerger.setDestinationFileName(outputFile)

        for (int i=1 ; i<= ExtractImage.countImage(uploadedFile); i++) {

            File file = new File(dir,ocrFile + i + ".pdf")
            PDDocument document = PDDocument.load(file)

            //adding the source files
            PDFmerger.addSource(file)

            //Merging the documents
            PDFmerger.mergeDocuments(null)

            println("PDF Documents merged to a single file successfully")

            //Close documents
            document.close()

        }
    }
}
