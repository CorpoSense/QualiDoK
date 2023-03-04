package com.corposense.services

import com.corposense.Constants
import com.google.inject.Inject
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import com.recognition.software.jdeskew.ImageDeskew
import groovy.transform.CompileStatic
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import net.sourceforge.tess4j.util.ImageHelper
import org.apache.commons.io.FileUtils
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.im4java.process.ProcessStarter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.imageio.ImageIO
import java.awt.Image
import java.awt.image.BufferedImage

import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.html2pdf.HtmlConverter

import org.apache.pdfbox.contentstream.PDFStreamEngine
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSName

import org.apache.pdfbox.pdmodel.graphics.PDXObject
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject


@CompileStatic
class ImageService extends PDFStreamEngine {

    static {
        /**
         * Path need to be defined in your OS e.g.:
         * Windows:
         * IMAGE_MAGICK_PATH = "D:\\ImageMagick-7.1.0-Q16-HDRI";
         * Linux:
         * IMAGE_MAGICK_PATH = "/usr/bin/"
         */
        if (!new File(IMAGE_MAGICK_PATH).exists()){
            // TODO: Consider trying Q8 and see what's the difference (https://imagemagick.org/script/download.php)
            throw new FileNotFoundException("The ImageMagick cannot be found at: ${IMAGE_MAGICK_PATH}. Please make sure to define the MAGICK_HOME variable environment.")
        }
    }

    static final String TESSERACT_DATA_PATH = System.getenv("TESSDATA_PREFIX")
    static final String IMAGE_MAGICK_PATH = System.getenv('MAGICK_HOME')

    static final double MINIMUM_DESKEW_THRESHOLD = 0.05d
    // TODO: It should be detected from the image input or better calculated for best performance
    static final int IMAGE_DENSITY = 300 // 96
    static final String DEFAULT_SUPPORTED_LANGUAGES = 'eng+ara+fra'
    static final long MIN_IMAGE_FILE_SIZE = 8192
    private int imageNumber = 1
    final Logger log = LoggerFactory.getLogger(ImageService)

    Tesseract ocrEngine

//    final String uploadDir = 'uploads'
//    final String publicDir = 'public'
//    final String downloadsDir = 'downloads'
//    final Path baseDir = BaseDir.find("${publicDir}/${uploadDir}")
//    final Path downloadsPath = baseDir.resolve(downloadsDir)

    @Inject
    ImageService() {
        this.ocrEngine = new Tesseract()
        this.ocrEngine.setTessVariable("user_defined_dpi", "${IMAGE_DENSITY}");
        this.ocrEngine.setDatapath(TESSERACT_DATA_PATH)
        this.ocrEngine.setLanguage(DEFAULT_SUPPORTED_LANGUAGES)
    }

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
                String extractedFile = "ExtractedImage_${imageNumber}.png"
                File file = new File("${Constants.uploadPath}", extractedFile)
                ImageIO.write(bImage, "PNG", file)
                log.info("Image saved.")
                imageNumber++

            } else if (xObject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xObject
                showForm(form)
            }
        } else {
            super.processOperator(operator, operands)
        }
    }
    /**
     * Extract images from parsed pdf file
     * @param inputFile
     */
    void ExtractImgFromPdf (File inputFile){
        PDDocument document = null
        try {
            document = PDDocument.load(inputFile)
            ImageService printer = new ImageService()
            int pageNum = 0
            document.pages.each { PDPage page ->
                pageNum++
                log.info( "Processing page: ${pageNum}" )
                printer.processPage(page)
            }
        } catch (Exception e){
            log.error("Cannot extract image from PDF (${e.getClass().simpleName}): ${e.message}")
        } finally {
            if ( document != null ) {
                document.close()
            }
        }
    }
    /**
     * Count number of images from parsed pdf file
     * @param inputFile
     * @return number of images
     */
    int countImage(File inputFile){
        PDDocument doc = null
        int countPages
        try {
            doc = PDDocument.load(inputFile)
            countPages = doc.getNumberOfPages()
        } catch (Exception e){
            log.error("Cannot count pages from PDF (${e.getClass().simpleName}): ${e.message}")
        } finally {
            if (doc){
                doc.close()
            }
        }
        return countPages
    }
    /**
     * Produce text from parsed pdf file that contain multiple images
     * @param inputFile
     * @return text
     */
    List<String> produceTextForMultipleImg(File inputFile){

        int countPages = this.countImage(inputFile)
        log.info("Number of pages: ${countPages}")
        this.ExtractImgFromPdf(inputFile)
        List<String> fullText = new ArrayList<String>()
        for (int i = 0 ; i < countPages ; i++){
            File extractedImg = new File("${Constants.uploadPath}","ExtractedImage_${i+1}.png")
            String text = this.produceText(extractedImg)
            fullText.add(text)
            extractedImg.delete()
        }
        return fullText
    }
    /**
     * Generate searchable pdf document form parsed pdf file that contain multiple images
     * @param inputFile
     * @return pdf document
     */
    File producePdfForMultipleImg(File inputFile){

        File outputFile = null
        try {
            int countPages = this.countImage(inputFile)
            log.info("Number of pages: ${countPages}")
            this.ExtractImgFromPdf(inputFile)

            PDFMergerUtility PDFmerger = new PDFMergerUtility()
            //Setting the destination file path
            outputFile = new File("${Constants.downloadPath}","${inputFile.name}")
            PDFmerger.setDestinationFileName("${outputFile}")

            for (int i = 1 ; i <= countPages ; i++){
                File extractedImg = new File("${Constants.uploadPath}","ExtractedImage_${i}.png")
                File outputPdf = this.producePdf(extractedImg,0)
                extractedImg.delete()
                //Loading an existing PDF document
                File pdfFile = new File("${Constants.downloadPath}","${outputPdf.name}")
                PDDocument document = PDDocument.load(pdfFile)
                //adding the source files
                PDFmerger.addSource(pdfFile)
                //Merging the documents
                PDFmerger.mergeDocuments(null)
                document.close()
            }
            log.info("PDF Documents merged to a single file successfully")
            deleteDocument(countPages)

        } catch (Exception e) {
            log.error ("${e.getClass().simpleName}: ${e.message}")
        }
        return outputFile
    }
    /**
     * Generate pdf document from parsed html content
     * @param htmlContent
     * @param fileName
     * @return
     */
    File htmlToPdf(String htmlContent, String fileName){
        Document document = null
        File doc = null
        try {
            document = new Document(PageSize.LETTER)
            doc = new File("${Constants.downloadPath}", "${fileName}.pdf")
            FileOutputStream fos = new FileOutputStream(doc.toString())
            // use HTMLConverter
            HtmlConverter.convertToPdf(htmlContent, fos)
            log.info("pdf document will be created at: ${Constants.downloadPath}/${doc.name}")
        } catch (Exception e) {
            log.error ("${e.getClass().simpleName}: ${e.message}")
        } finally {
            if (document){
                document.close()
            }
        }
        return doc
    }
    /**
     * Generate pdf document form parsed Html file
     * @param htmlFile
     * @param fileName
     * @return pdf file
     */
    File htmlToPdf(File htmlFile , String fileName) {
      Document document = null
        File doc = null
        //File imageFolder = null
        try {
            document = new Document(PageSize.LETTER)
            doc = new File("${Constants.downloadPath}", "${fileName}.pdf")

            // use HTMLConverter
            HtmlConverter.convertToPdf(htmlFile,doc)
            log.info("pdf document will be created at: ${Constants.downloadPath}/${doc.name}")
            //imageFolder = new File("${Constants.downloadPath}" + File.separator + "image")
        } catch (Exception e) {
            log.error ("${e.getClass().simpleName}: ${e.message}")
        } finally {
            if (document){
                document.close()
            }
            if (htmlFile != null){
                htmlFile.delete()
            }
//            if (imageFolder.exists()){
//                FileUtils.cleanDirectory(imageFolder)
//                FileUtils.deleteDirectory(imageFolder)
//            }
        }
        return doc
    }
    /**
     * Create pdf file from the given file and text
     * @param inputFile
     * @param text
     * @return pdf file
     */
    File createPdf(File inputFile , String text){
        Document document = null
        File pdfDoc = null
        try {
            String fileName = getFileNameWithoutExt(inputFile,'.pdf')
            pdfDoc = new File("${Constants.downloadPath}", fileName)
            OutputStream fos = new FileOutputStream(pdfDoc.toString())
            //create pdf file
            document = new Document(PageSize.LETTER)
            PdfWriter.getInstance(document, fos)
            document.open()
            document.add(new Paragraph(text))
            log.info("pdf document will be created at: ${Constants.downloadPath}/${pdfDoc.name}")
        } catch (Exception e) {
            log.error ("${e.getClass().simpleName}: ${e.message}")
        } finally {
            if (document){
                document.close()
            }
        }
        return pdfDoc
    }
    /**
     * Produce text from from parsed input image.
     * @param inputImage
     * @return Text
     */
    String produceText(File inputImage){
        String fullText = null

        long fileSize = inputImage.size()
        if (fileSize < MIN_IMAGE_FILE_SIZE){
            log.warn("File size: ${fileSize} byte(s) is too small for processing.")
        } else {
            try {
                File outputImage = this.imageProcessing(inputImage)
                log.info('OCR processing...')
                fullText = this.ocrEngine.doOCR(outputImage)
                outputImage.delete()
            } catch (TesseractException e) {
                log.error("TesseractException: ${e.message}")
            }
        }
        return fullText
    }

    /**
     * Generate a PDF file from parsed input image.
     * Page segmentation modes:
     *   0    Orientation and script detection (OSD) only.
     *   1    Automatic page segmentation with OSD.
     *   2    Automatic page segmentation, but no OSD, or OCR. (not implemented)
     *   3    Fully automatic page segmentation, but no OSD. (Default)
     *   4    Assume a single column of text of variable sizes.
     *   5    Assume a single uniform block of vertically aligned text.
     *   6    Assume a single uniform block of text.
     *   7    Treat the image as a single text line.
     *   8    Treat the image as a single word.
     *   9    Treat the image as a single word in a circle.
     *  10    Treat the image as a single character.
     *  11    Sparse text. Find as much text as possible in no particular order.
     *  12    Sparse text with OSD.
     *  13    Raw line. Treat the image as a single text line,
     *        bypassing hacks that are Tesseract-specific.
     * @param inputImage Input image file
     * @param visibleImageLayer 0: keep the original image background, 1: generate text only pdf
     * @return Output pdf file
     */
    File producePdf(File inputImage, int visibleImageLayer = 0) {
        File outputFile = null
        File outputImage = null
                List<ITesseract.RenderedFormat> formats = new ArrayList<ITesseract.RenderedFormat>(Arrays.asList(ITesseract.RenderedFormat.PDF))
        try {
            // Mode 6: Assume a single uniform block of text.
            this.ocrEngine.setPageSegMode(6)
            this.ocrEngine.setTessVariable("textonly_pdf", "${visibleImageLayer}")

            outputImage = this.imageProcessing(inputImage)
            outputFile = new File("${Constants.downloadPath}", getFileNameWithoutExt(inputImage))

            this.ocrEngine.createDocuments("${outputImage}", "${outputFile}", formats)
            // Tesseract ask for file name without extension, and outputs file name with '.pdf'
            outputFile = new File(outputFile.parent, "${outputFile.name}.pdf")

            log.info("Output file: ${outputFile}")
        } catch (TesseractException te){
            log.error("Error TE (${te.getClass().simpleName}): ${te.message}")
        } finally {
            if (outputImage != null){
                outputImage.delete()
            }
        }
        return outputFile
    }

    private File imageProcessing(File inputImage){
        log.info("Resizing (if required): ${inputImage}...")
        File resizedImage = imageResizing(inputImage)
        log.info('Image deskew...')
        File deskewedImage = deskewImage(resizedImage)
        resizedImage.delete()
        log.info('Removing borders...')
        File borderlessImage = removeBorder(deskewedImage)
        deskewedImage.delete()
        log.info('Binary inversion...')
        File binaryInverseImage = binaryInverse(borderlessImage)
//        borderlessImage.delete()
        log.info('Removing background...')
        File finalOutput = imageTransparent(borderlessImage, binaryInverseImage)
        borderlessImage.delete()
        binaryInverseImage.delete()
        return finalOutput
    }

    /**
     * Resize an image if smaller than minWidth X minHeight
     * @param inputImage Input image file
     * @param scaleX scale factor
     * @param minWidth minimum image width
     * @param minHeight minimum image height
     * @return Output image file
     */
    private File imageResizing(File inputImage, int scaleX = 2,
                                 int minWidth = 250, int minHeight = 350){
        File outputImage = null
        BufferedImage img = ImageIO.read(inputImage)
        // A4 format = w210xh297
        if(img.width < minWidth && img.height< minHeight){
            int targetWidth = img.width*scaleX
            int targetHeight = img.height*scaleX
            log.info("Image size: ${img.width} x height: ${img.height}, resizing to: ${targetWidth}X${targetHeight}...")
            Image imageScaleOutput = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)
            BufferedImage bufferedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
            bufferedImage.getGraphics().drawImage(imageScaleOutput, 0, 0, null)
            outputImage = writeImage(bufferedImage, inputImage, "resized_${inputImage.name}")
        } else {
            outputImage = writeImage(img, inputImage, "original_${inputImage.name}")
        }
        return outputImage
    }

    /**
     * Deskew image (Straightening a rotated image)
     * @param inputImage Input image file
     * @return Output image file
     */
    File deskewImage(File inputImage) {
        BufferedImage bufferedImage = ImageIO.read(inputImage)
        double skewAngle = new ImageDeskew(bufferedImage).skewAngle // determine skew angle
        if ((skewAngle > MINIMUM_DESKEW_THRESHOLD || skewAngle < -(MINIMUM_DESKEW_THRESHOLD))) {
            bufferedImage = ImageHelper.rotateImage(bufferedImage, -skewAngle)
        }
        File outputImage = writeImage(bufferedImage, inputImage, "deskewImage_${inputImage.name}")
        return outputImage
    }

    /**
     * Remove the black border around the image.
     * @param inputImage
     * @return Output image file
     */
    File removeBorder(File inputImage) {
        ProcessStarter.setGlobalSearchPath(IMAGE_MAGICK_PATH)
        IMOperation op = new IMOperation()
        op.addImage()
        op.density(IMAGE_DENSITY)
        op.bordercolor("black")
                .border(1)
                .fuzz(0.95d)
                .fill("white")
                .draw("color 0,0 floodfill")
        op.addImage()
        BufferedImage bufferedImage =  ImageIO.read( inputImage )
        File outputImage = new File(inputImage.parent, "borderlessImage_${inputImage.name}")
        ConvertCmd convertCmd = new ConvertCmd()
        convertCmd.run(op, bufferedImage, outputImage.toString())
        return outputImage
    }

    /**
     * Binary image inversion
     *  Make the text white and the background black.
     *       monochrome: converts a multicolored image (RGB), to a black and white image.
     *       negate: Replace each pixel with its complementary color (White becomes black).
     *       Use .fill white .fuzz 11% p_opaque "#000000" to fill the text with white (so we can see most
     *       of the original image)
     *       Apply a light .blur (1d,1d) to the image.
     * @param inputImage file after applying deskew
     * @return Output image file
     */
    File binaryInverse(File inputImage) {
        ProcessStarter.setGlobalSearchPath(IMAGE_MAGICK_PATH)
        String imgExt = getImageExt(inputImage)
        // Create the operation, add images and operators/options
        IMOperation op = new IMOperation()
        op.addImage()
        op.density(IMAGE_DENSITY)
        op.format(imgExt)
                .monochrome()
                .negate()
                .fill("white")
                .fuzz(0.11d)
                .p_opaque("#000000")
                .blur(1d,1d)
        op.addImage()

        // Execute the operation
        BufferedImage bufferedImage =  ImageIO.read( inputImage )
        File outputImage = new File(inputImage.parent, "binaryInverseImg_${inputImage.name}")
        ConvertCmd convertCmd = new ConvertCmd()
        convertCmd.run(op, bufferedImage, outputImage.toString())
        return outputImage
    }

    /**
     * Convert black to transparent.
     * Combine the original image with binaryInverseImg (the black and white version).
     * @param inputImage Original input image file
     * @param borderlessImage borderless image file
     * @return Output image file
     */
    File imageTransparent(File inputImage, File borderlessImage) {
        ProcessStarter.setGlobalSearchPath(IMAGE_MAGICK_PATH)
        IMOperation op = new IMOperation()
        op.addImage()
        op.density(IMAGE_DENSITY)
        op.addImage()
        op.density(IMAGE_DENSITY)
        op.alpha("off").compose("copy_opacity").composite()
        op.addImage()
        ConvertCmd convertCmd = new ConvertCmd()
        BufferedImage bufferedInput =  ImageIO.read(inputImage)
        BufferedImage bufferedBorderless =  ImageIO.read(borderlessImage)
        File outputImage = new File(inputImage.parent, "transparentImg_${inputImage.name}")
        convertCmd.run(op, bufferedInput, bufferedBorderless, outputImage.toString())
        return outputImage
    }

    private File writeImage(BufferedImage bufferedInput, File inputImage, String outputImageName){
        String imageExt = getImageExt(inputImage)
        File outputImage = new File(inputImage.parent, outputImageName)
        ImageIO.write(bufferedInput, imageExt, outputImage)
        return outputImage
    }

    private String getImageExt(File inputImage){
        return inputImage.name.with {it.substring(it.lastIndexOf('.')+1)}
    }

    /**
     * Return the file name without extension (e.g. image.png => image)
     * You can define your own extension
     * @param inputFile input file
     * @param newExt New extension (must include '.' sign)
     * @return
     */
    String getFileNameWithoutExt(File inputFile, String newExt = ''){
        return inputFile.name.with {it.take(it.lastIndexOf('.'))} +newExt
    }
    /**
     * Delete Extracted images
     * @param countPages
     */
    private void deleteDocument(int countPages){
        for(int i = 1 ; i <= countPages ; i++){
            File outputPdf = new File("${Constants.downloadPath}","ExtractedImage_${i}.pdf")
            outputPdf.delete()
        }
    }
    /**
     * Rename file with the given file path and the new file name
     * @param filePath
     * @param newFileName
     * @return new file with the new name
     */
    File renameFile(String filePath, String newFileName){
        File outputFile = new File(filePath)
        File newFile = new File("${outputFile.getParent()}","${newFileName}${"."}${getImageExt(outputFile)}")
        outputFile.renameTo(newFile)
        return newFile
    }

}
