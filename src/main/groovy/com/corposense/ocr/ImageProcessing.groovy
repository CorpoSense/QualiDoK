package com.corposense.ocr

import com.google.inject.Inject
import com.recognition.software.jdeskew.ImageDeskew
import groovy.transform.CompileStatic
import net.sourceforge.tess4j.util.ImageHelper
import org.im4java.core.ConvertCmd
import org.im4java.core.IM4JavaException
import org.im4java.core.IMOperation
import org.im4java.process.ProcessStarter

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Paths

@CompileStatic
class ImageProcessing {

    static final String IMAGE_MAGICK_PATH = System.getenv('MAGICK_HOME')
    static final double MINIMUM_DESKEW_THRESHOLD = 0.05d
    private String imagePath

    String dirPath = Paths.get("public/generatedFiles/createdFiles").toAbsolutePath().toString();
    File dir = new File(dirPath)

    static {
        if (!new File(IMAGE_MAGICK_PATH).exists()){
            throw new FileNotFoundException("The ImageMagick cannot be found at: ${IMAGE_MAGICK_PATH}. Please make sure to define the MAGICK_HOME variable environment.")
        }
        /*if (Utils.isWindows()){
            // need to be moved, use environment variable instead
            // ImageMagick-7.1.0-portable-Q16-HDRI-x64.zip (146Mb)
            // TODO: Consider trying Q8 and see what's the difference (https://imagemagick.org/script/download.php)
            IMAGE_MAGICK_PATH = "D:\\ImageMagick-7.1.0-Q16-HDRI";
        } else {
            IMAGE_MAGICK_PATH = "/usr/bin/";
        }*/
    }

    @Inject
    ImageProcessing(String imagePath){
        this.imagePath = imagePath;
    }

    /*
     * Straightening a rotated image.
     */
    String deskewImage(String inputImgPath , int num) throws IOException {
        BufferedImage bi = ImageIO.read(new File(dirPath, inputImgPath))
        ImageDeskew id = new ImageDeskew(bi)
        double imageSkewAngle = id.skewAngle // determine skew angle
        if ((imageSkewAngle > MINIMUM_DESKEW_THRESHOLD || imageSkewAngle < -(MINIMUM_DESKEW_THRESHOLD))) {
            // deskew image
            bi = ImageHelper.rotateImage(bi, -imageSkewAngle)
        }
        String straightenImgPath = "deskewImage_" + num + ".png";
        ImageIO.write(bi, "png", new File(dirPath,straightenImgPath))
        return straightenImgPath
    }

    // TODO: Seems to be duplicated!
    String rotateImage(String inputImgPath , int num) throws IOException {
        BufferedImage bi = ImageIO.read( new File(inputImgPath))
        ImageDeskew id = new ImageDeskew(bi)
        double imageSkewAngle = id.skewAngle // determine skew angle
        if ((imageSkewAngle > MINIMUM_DESKEW_THRESHOLD || imageSkewAngle < -(MINIMUM_DESKEW_THRESHOLD))) {
            // deskew image
            bi = ImageHelper.rotateImage(bi, -imageSkewAngle)
        }
        String straightenImgPath = "deskewImage_" + num + ".png";
        ImageIO.write(bi, "png", new File(dirPath, straightenImgPath))
        return straightenImgPath
    }

    /**
     * Get rid of a black border around image.
     * @param inputImage
     * @param num
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws IM4JavaException
     */
    String removeBorder(String inputImage , int num) throws IOException, InterruptedException, IM4JavaException {
        ProcessStarter.setGlobalSearchPath(IMAGE_MAGICK_PATH)
        IMOperation op = new IMOperation()
        op.addImage()
        op.density(300);
        op.bordercolor("black").border(1).fuzz(0.95d).fill("white").draw("color 0,0 floodfill");
        op.addImage();
        ConvertCmd cmd = new ConvertCmd()
        BufferedImage image =  ImageIO.read(new File(dirPath,inputImage))
        String outFile = "./borderRemoved_" + num + ".png"
        String file = new File(dirPath,outFile).toString()
        cmd.run(op,image,file)
        return outFile
    }

    /*
      In this step we make the text white and background black.
      monochrome: converts a multicolored image (RGB), to a black and white image.
      negate: Replace each pixel with its complementary color (White becomes black).
      Use .fill white .fuzz 11% p_opaque "#000000" to fill the text with white (so we can see most
      of the original image)
      Apply a light .blur (1d,1d) to the image.
     */
    String binaryInverse(String deskew , int num) throws IOException,
            InterruptedException,
            IM4JavaException {

        ProcessStarter.setGlobalSearchPath(IMAGE_MAGICK_PATH)
        // create the operation, add images and operators/options
        IMOperation op = new IMOperation()
        op.addImage()
        op.density(300)
        op.format("png")
                .monochrome()
                .negate()
                .fill("white")
                .fuzz(0.11d)
                .p_opaque("#000000")
                .blur(1d,1d)
        op.addImage()

        // execute the operation
        ConvertCmd cmd = new ConvertCmd();
        BufferedImage img =  ImageIO.read(new File(dirPath,deskew));
        String outfile = "./binaryInverseImg_" + num + ".png";
        String file = new File(dirPath,outfile).toString()
        cmd.run(op, img, file);
        return outfile
    }

    /**
     * Make every thing in black becoming transparent.
     * we simply combine the original image with binaryInverseImg (the black and white version).
     * @param originalImgPath
     * @param nbackgroundImgPath
     * @param num
     * @return The output file name
     * @throws IOException
     * @throws InterruptedException
     * @throws IM4JavaException
     */
    String imageTransparent(String originalImgPath, String nbackgroundImgPath, int num)
            throws IOException, InterruptedException, IM4JavaException {
        ProcessStarter.setGlobalSearchPath(IMAGE_MAGICK_PATH)
        IMOperation op = new IMOperation()
        op.addImage()
        op.density(300)
        op.addImage()
        op.density(300)
        op.alpha("off").compose("copy_opacity").composite()
        op.addImage()
        ConvertCmd cmd = new ConvertCmd()
        BufferedImage img1 =  ImageIO.read(new File(dirPath, originalImgPath))
        BufferedImage img2 =  ImageIO.read(new File(dirPath, nbackgroundImgPath))
        String outputFile = "./transparentImg_" + num + ".png"
        String file = new File(dirPath, outputFile).toString()
        cmd.run(op, img1, img2, file)
        return outputFile
    }
}
