package com.corposense

import com.corposense.services.ImageService
import com.corposense.services.OfficeService
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.http.internal.DefaultMultipartForm
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path

class OcrHandlerSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest app = new GroovyRatpackMainApplicationUnderTest()
    TestHttpClient testClient = app.httpClient

    @Unroll
    def "Should render the upload template with the correct content"() {
        when:
        def response = testClient.get("upload")

        then:
        response.statusCode == 200
        response.body.text.contains('This is the expected content')
    }
/*
    @Unroll
    def'upload different file format (PNG,JPG,PDF,DOC,DOCX,text) via form'(){
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        def file = new File("src/test/groovy/com/corposense/files/${name}")
        def fileData = file.bytes

        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(fileData).add()
                .field(ocrTypeField,extractText)

        when:"send a POST request to the server with the multipart form data attached"
        def form = builder.build()
        def response = testClient.requestSpec { spec ->
            spec.headers { headers ->
                headers.add("Content-Type", "multipart/form-data; boundary=${boundary}")
            }
            spec.body { body ->
                def formData = "--${boundary}\r\nContent-Disposition: form-data; name=\"input-doc\"; filename=\"${name}\"\r\nContent-Type: ${contentType}\r\n\r\n${fileData}\r\n--${boundary}--"
                body.text(formData)
            }
        }.post("upload")
        File uploadedFile = new File("${uploadPath}", name)

        then:
        response.statusCode == 200
        uploadedFile.exists()

        where:
        name             | contentType
        'text.txt'       | 'text/plain'
        'officeDocx.docx'       | 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        'image1.jpg'     |  'image/jpeg'
        'wordDoc.doc'    | 'application/msword'
        'demojournal.pdf'| 'application/pdf'
    }
 */

    @Unroll
    def 'upload text file & extract content'() {
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        File inputFile = new File("src/test/groovy/com/corposense/files/${name}")

        ImageService imageService = new ImageService()
        OfficeService officeService = new OfficeService()
        def fileData = inputFile.bytes

        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(fileData).add()
                .field(ocrTypeField,extractText)

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
        //response.headers.each {println(it.dump())}
        uploadedFile.exists()

        when:"read text file"
        Files.readAllBytes(uploadedFile.toPath())
        String fileName = imageService.getFileNameWithoutExt(uploadedFile)
        String text = officeService.readText(uploadedFile)

        then:
        uploadedFile.name.contains("text.txt")
        fileName.contains("text")
        text.contains("Apprendre la programmation informatique")

        where:
        name           | contentType
        'text.txt'    | 'text/plain'
    }

    @Unroll
    def 'extract content from image file'() {
        given:
        File inputFile = new File("src/test/groovy/com/corposense/files/image1.jpg")
        ImageService imageService = new ImageService()

        when:
        String fileName = imageService.getFileNameWithoutExt(inputFile)
        String fullText = imageService.produceText(inputFile)

        then:
        fileName.contains('image1')
        fullText.contains('AN OBSESSION WITH TIME')

    }

    @Unroll
    def 'upload image & perform OCR file'() {
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        def imageFile = new File("src/test/groovy/com/corposense/files/${name}")
        def imageData = imageFile.bytes
        ImageService imageService = new ImageService()

        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(imageData).add()
                .field(ocrTypeField,extractText)

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
        File uploadedFile = new File("${uploadPath}/${name}")

        then:
        response.statusCode == 200
        uploadedFile.exists()

        when:"perform ocr"
        //Files.readAllBytes(uploadedFile.toPath())
        String fileName = imageService.getFileNameWithoutExt(uploadedFile)
        String fullText = imageService.produceText(uploadedFile)

        then:
        uploadedFile.name == name
        fileName.contains(nameWithoutExt)
        fullText.contains(extractedText)

        where:
        name           | contentType   || nameWithoutExt || extractedText
        'image1.jpg'   | 'image/jpeg'  || 'image1'       || 'an obsession with time'
        'image2.png'   | 'image/png'   || 'image2'       || 'an obsession with time'
    }
}
