package com.corposense

import com.corposense.services.ImageService
import com.corposense.services.OfficeService
import io.netty.buffer.ByteBuf
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.http.internal.DefaultMultipartForm
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer
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

    @Unroll
    def'upload different file format (PNG,JPG,DOC,DOCX,text) via form'(){
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        def file = new File("src/main/resources/files/${name}")
        //def fileData = file.bytes
        // Read the file data into a buffer
        def buffer = ByteBuffer.allocate(file.length().intValue())
        file.withInputStream { inputStream ->
            buffer.put(inputStream.readAllBytes())
        }
        buffer.flip()
        // Convert the buffer to a byte array
        byte[] fileData = new byte[buffer.remaining()]
        buffer.get(fileData)

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
        uploadedFile.exists()

        where:
        name             | contentType
        'text.txt'       | 'text/plain'
       // 'officeDocx.docx'| 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        'image3.jpg'     | 'image/jpeg'
        //'wordDoc.doc'    | 'application/msword'
    }

    @Unroll
    def 'upload text file & extract content'() {
        given:
        def builder = DefaultMultipartForm.builder()
        def boundary = builder.boundary
        def ocrTypeField = 'type-ocr'
        def extractText = 'extract-text'
        def field = 'input-doc'
        Path uploadPath = Constants.uploadPath
        File inputFile = new File("src/main/resources/files/${name}")

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
        File inputFile = new File("src/main/resources/files/image3.jpg")
        ImageService imageService = new ImageService()

        when:
        String fileName = imageService.getFileNameWithoutExt(inputFile)
        String fullText = imageService.produceText(inputFile)

        then:
        fileName.contains('image3')
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
        def imageFile = new File("src/main/resources/files/${name}")
        def imageData = imageFile.bytes
        ImageService imageService = new ImageService()

        builder.file()
                .field(field)
                .contentType(contentType)
                //.encoding('binary')
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
        'image3.jpg'   | 'image/jpeg'  || 'image3'       || 'an obsession with time'
    }
    @Unroll
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
        OfficeService officeService = new OfficeService()

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

        then:
        response.statusCode == 200
        uploadedFile.exists()

        when: "get html content"

        String fileName = imageService.getFileNameWithoutExt(uploadedFile)
        String fullText = officeService.docxToHtml(uploadedFile)

        then:
        uploadedFile.name == name
        fileName.contains(nameWithoutExt)
        fullText.contains(extractedText)

        where:
        name              | contentType                                                               || nameWithoutExt || extractedText
        'officeDocx.docx' | 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' || 'officeDocx'   || 'Apprendre la programmation informatique '

    }
}
