package com.corposense

import com.corposense.handlers.OcrHandler
import com.corposense.services.AccountService
import com.corposense.services.DirectoriesService
import com.corposense.services.ImageService
import com.corposense.services.OfficeService
import com.corposense.services.UploadService
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.test.http.internal.DefaultMultipartForm
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import ratpack.http.client.HttpClient



class HomeSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest app = new GroovyRatpackMainApplicationUnderTest()
    TestHttpClient testClient = app.httpClient


    @Unroll
    def 'Response should return ok'() {
        when:
        def response = aut.httpClient.get()

        then:
        response.statusCode == 200

        where:
            aut | type
            app | 'ratpack.groovy'
    }

    @Unroll
    def "Should render the upload template with the correct content"() {
        when:
        def response = testClient.get("upload")

        then:
        response.statusCode == 200
        response.body.text.contains('This is the expected content')
    }

    def 'upload single file'() {
        given:
        builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(data).add()
                //.field(ocrTypeField,extractText)

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
        response.statusCode == 200

        where:
        builder = DefaultMultipartForm.builder()
        boundary = builder.boundary
        field = 'input-doc'
        name = 'filename.txt'
        contentType = 'text/plain'
        data = '<content>'
        //ocrTypeField = 'type-ocr'
        //extractText = 'extract-text'
        //value2 = 'produce-pdf'
    }
/*
    @Unroll
    def "should retrieve directory list from logicalDoc"() {
        given:
        String url = (System.getenv('GITPOD_HOST')?'http://127.0.0.1:8080':'http://0.0.0.0:8080/logicaldoc')
        String username = 'admin'
        String password = 'admin'
        Serializable folderId = 4
        URI uri = "${url}/services/rest/folder/listChildren?folderId=${folderId}".toURI()
        HttpClient client = Mock(HttpClient)
        DirectoriesService directoriesService = new DirectoriesService(client)

        def response = [
                statusCode: 200,
                body: [
                        [name: "Finance", id: 100]
                ] as ArrayList
        ] as ReceivedResponse

        client.get(uri) >> { RequestSpec reqSpec, Closure closure ->
            closure(response)
        }

        when:
        def result = directoriesService.listDirectories(url, username, password, folderId)

        then:
        result.then { directories ->
            assert directories == response
        }
    }
*/

}
