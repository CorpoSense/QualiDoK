package com.corposense

import com.corposense.services.ImageService
import com.corposense.services.OfficeService
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.http.internal.DefaultMultipartForm
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class OcrHandlerSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest app = new GroovyRatpackMainApplicationUnderTest()
    TestHttpClient testClient = app.httpClient
    def okHttpClient = new OkHttpClient()

    @Unroll
    def "POST /upload should process the form and return the result"() {
        given:
        def uploadedFile = new File("src/main/resources/files/image3.jpg")
        def requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart('input-doc', uploadedFile.name, RequestBody.create(MediaType.parse('image/jpeg'), uploadedFile))
                .addFormDataPart('type-ocr', 'extract-text')
                .build()
        def request = new Request.Builder()
                .url(app.address.resolve('upload').toString())
                .post(requestBody)
                .build()
        def response = okHttpClient.newCall(request).execute()

        expect:
        response.code() == 200
        response.body().string().contains('Image processed successfully')
    }

    @Unroll //OK
    def "Should render the upload template with the correct content"() {
        when:
        def response = testClient.get("upload")
        then:
        response.statusCode == 200
        response.body.text.contains('QualiDoK')
    }

    @Unroll
    //OK
    def 'upload text file and extract content'() {
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        File inputFile = File.createTempFile(name, '.txt')
        inputFile.write('dummy file')
        OfficeService officeService = new OfficeService()
        def fileData = inputFile.bytes

        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(fileData).add()
                .field(ocrTypeField, extractText)

        when:"send a POST request to the server with the multipart form data attached"
        def form = builder.build()
        def response = testClient.requestSpec { spec ->
            spec.headers { headers ->
                headers.add("Content-Type", "multipart/form-data; boundary=${boundary}")
            }
            spec.body { body ->
                body.bytes(form.body.bytes)
            }
        }.post("upload")
        File uploadedFile = new File("${uploadPath}", name)

        then:
        response.statusCode == 200
        uploadedFile.exists()

        when:"read text file"
        Files.readAllBytes(uploadedFile.toPath())
        String text = officeService.readText(uploadedFile)

        then:
        uploadedFile.name.contains("text")
        text.contains("dummy file")

        where:
        name      | contentType
        'text'    | 'text/plain'
    }

    @Unroll //OK
    def 'Should create an image'(){
        given:
        def image
        when:
        image = createAnImage('text content')
        then:
        image != null
    }

    @Unroll
    //OK
    def 'extract content from image file'() {
        given:
        File inputFile = new File("src/main/resources/files/image3.jpg")
        ImageService imageService = new ImageService()

        when:
        String fileName = imageService.getFileNameWithoutExt(inputFile)
        String fullText = imageService.produceText(inputFile)

        then:
        fileName.contains('image3')
        fullText.contains('AN OBSESSION WITH TIME')
    }

    @Unroll //OK
    def 'Should upload an image file'() {
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        def image = createAnImage(imageContent)
        def imageData = imageToBytes(image)
        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(imageData).add()
                .field(ocrTypeField, extractText)

        when:"send a POST request to the server with the multipart form data attached"
        def form = builder.build()
        def response = testClient.requestSpec { spec ->
            spec.headers { headers ->
                headers.add("Content-Type", "multipart/form-data; boundary=${boundary}")
            }
            spec.body { body ->
                body.bytes(form.body.bytes)
            }
        }.post("upload")

        then:
        File uploadedFile = new File("${uploadPath}/${name}")
        //response.statusCode == 200
        imageData.size() == 7969
        uploadedFile.exists()

        where:
        name           | contentType  | imageContent
        'image3'       | 'image/jpeg' | 'This is image'
    }

    @Unroll
    //OK
    def 'extract content from Docx file'(){
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        def imageFile = new File("src/main/resources/files/${name}")
        def imageData = imageFile.bytes
        ImageService imageService = new ImageService()
        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(imageData).add()
                .field(ocrTypeField, extractText)

        when: "send a POST request to the server with the multipart form data attached"
        def form = builder.build()
        def response = testClient.requestSpec { spec ->
            spec.headers { headers ->
                headers.add("Content-Type", "multipart/form-data; boundary=${boundary}")
            }
            spec.body { body ->
                body.bytes(form.body.bytes)
            }
        }.post("upload")
        File uploadedFile = new File("${uploadPath}","${name}")
        Files.readAllBytes(uploadedFile.toPath())
        String fileName = imageService.getFileNameWithoutExt(uploadedFile)

        then:
        response.statusCode == 200
        uploadedFile.exists()
        uploadedFile.name == name
        fileName.contains(nameWithoutExt)

        where:
        name              | contentType                                                               | nameWithoutExt
        'officeDocx.docx' | 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' | 'officeDocx'
    }

// Helper functions
    BufferedImage createAnImage(String fileContent){
        BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB)
        Graphics2D g2d = image.createGraphics()
        g2d.setColor(Color.WHITE)
        g2d.setColor(Color.YELLOW)
        g2d.fillOval(50, 50, 200, 200)
        g2d.setColor(Color.BLACK)
        g2d.setFont(new Font("Arial", Font.BOLD, 24))
        g2d.drawString(fileContent, 80, 170)
        g2d.dispose()
        return image
    }

    File createFileFromImage(String fileName, BufferedImage image, String ext = "jpg"){
        File outputImage = File.createTempFile(fileName, ext) // new File("${fileName}.jpg")
        ImageIO.write(image, ext, outputImage)
        return outputImage
    }

    byte[] imageToBytes(BufferedImage bufferedImage, String ext = "jpg"){
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(bufferedImage, ext, baos)
        return baos.toByteArray()
    }

}
