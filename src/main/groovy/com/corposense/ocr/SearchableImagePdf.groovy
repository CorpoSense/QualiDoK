package com.corposense.ocr

import com.google.inject.Inject
import com.itextpdf.text.DocumentException
import groovy.transform.CompileStatic
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import org.im4java.core.IM4JavaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
class SearchableImagePdf {

    final Logger log = LoggerFactory.getLogger(SearchableImagePdf)

    final static String CONFIG_FILE_VALUE = "0"

    String inputFile, outputFile, configFileValue
//    Path dirPath = Paths.get("generatedFiles/createdFiles")
    Path dirPath = Paths.get("public/generatedFiles/createdFiles")
    File dir = new File(dirPath.toAbsolutePath().toString())

    @Inject
    SearchableImagePdf(String inputFile, String outputFile, String configFileValue){
        this.inputFile = inputFile
        this.outputFile = outputFile
        this.configFileValue = configFileValue
    }

    /**
     * Generate a searchable PDF from given image
     * @param imagePath
     * @param number
     */
    void textOnlyPdf(String imagePath , int number){
        List<ITesseract.RenderedFormat> formats = new ArrayList<ITesseract.RenderedFormat>(Arrays.asList(ITesseract.RenderedFormat.PDF))
        try {
            Tesseract instance = new Tesseract()
            //mode 6: Assume a single uniform block of text.
            instance.setPageSegMode(6)
            instance.setTessVariable("user_defined_dpi", "300")
            instance.setDatapath(System.getenv("TESSDATA_PREFIX"))
            // set the English+Arabic+French languages (need to be modified in order to support other language
            instance.setLanguage("ara+eng+fra")
            // TODO: replace hardcoded strings with variable name
            instance.setTessVariable("textonly_pdf_", configFileValue)
            String img = new File(dir, imagePath).toString()
            String outputFile = new File(dir, outputFile).toString()
            instance.createDocuments([img] as String[], [outputFile+number] as String[], formats)
            log.info("Output file path: ${outputFile}")
        } catch (TesseractException te){
            ("Error TE (${te.getClass().simpleName}): ${te.message}")
        }

    }

    static void createSearchablePdf(int pageNum) throws IOException, InterruptedException,
            IM4JavaException, DocumentException {

        for (int i = 1; i <= pageNum; i++) {
            File extractedImgName = new File("ExtractedImage_" + i + ".png")
        //    ImageProcessing image = new ImageProcessing(extractedImgName.name)
            ImageProcessing image = new ImageProcessing()
            String imageDeskew = image.deskewImage(extractedImgName, i)
            String imageNBorder = image.removeBorder(imageDeskew,i)
            String binaryInv = image.binaryInverse(imageNBorder, i)
            String finalImage = image.imageTransparent(imageNBorder,binaryInv, i)

            // configFileValue = 0->make the image visible, =1->make the image invisible
            SearchableImagePdf createPdf = new SearchableImagePdf(finalImage, "./textonly_pdf_", CONFIG_FILE_VALUE)
            createPdf.textOnlyPdf(finalImage, i)

            ImageLocationsAndSize.createPdfWithOriginalImage("textonly_pdf_" + i + ".pdf",
                    "newFile_pdf_" + i + ".pdf", imageNBorder)
        }
    }

    /**
     * TODO: This may comes handy when trying to detect if PDF is searchable
     * @param inputFile
     * @throws IOException
     *
    public static void extractFonts(String inputFile) throws IOException {
        PDDocument doc = PDDocument.load(new File(inputFile));
        for (int i = 0; i < doc.getNumberOfPages(); ++i)
        {
            PDPage page = doc.getPage(i);
            PDResources res = page.getResources();
            for (COSName fontName : res.getFontNames())
            {
                PDFont font = res.getFont(fontName);
                boolean isEmbedded = font.isEmbedded();
                System.out.println("the file has fonts:" +isEmbedded);
            }
        }
    }*/


}
