package com.corposense.services

import com.google.inject.Inject
import groovy.transform.CompileStatic
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.http.client.RequestSpec
// import ratpack.form.UploadedFile
// import ratpack.test.http.internal.DefaultMultipartForm
import groovyx.net.http.OkHttpBuilder
import groovyx.net.http.OkHttpEncoders
import groovyx.net.http.MultipartContent


@CompileStatic
class UploadService {

    /*/ Trying to use HttpClient to send a file remotely
    def upload(UploadedFile f, int folderId, String dest, String language) {
      def url = 'http://192.168.1.13:8080/logicaldoc/services/rest/document/upload'.toURI()
      def builder = DefaultMultipartForm.builder()
      builder.field('folderId', "${folderId}")
      builder.field('filename', f.fileName)
      builder.field('language', language)
      builder.file()
        .field()
        .contentType('multipart/form-data')
        .encoding('multipart/form-data')
        .data(f.bytes)
    }*/

    Integer upload(String fileName) {

          /*
          OkHttpClient client = new OkHttpClient();

          MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
          RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"folderId\"\r\n\r\n4\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"filename\"\r\n\r\nPersonnel_v1.pdf\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\nfr\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"filedata\"\r\n\r\n\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");
          Request request = new Request.Builder()
            .url("http://192.168.1.43:8080/logicaldoc/services/rest/document/upload")
            .post(body)
            .addHeader("content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
            .addHeader("accept", "application/json")
            .addHeader("authorization", "Basic YWRtaW46YWRtaW4=")
            .addHeader("cache-control", "no-cache")
            .build();

          Response response = client.newCall(request).execute();
           */

          /*/ POST Request
           def url = 'http://192.168.1.4:8080/logicaldoc/services/rest/document/upload'.toURI()
           client.request(url, { RequestSpec reqSpec ->
              reqSpec
                      .method('POST')
                      .body { RequestSpec.Body body ->
                          body.type('multipart/form-data')
                      }
                      .headers { MutableHeaders headers ->
                  headers.set('Content-type','multipart/form-data')
              }
          }).then { ReceivedResponse res ->
              res.forwardTo(response)
          }
*/
           return fileName.size()
    }

    Promise<Boolean> uploadFile(File docFile, String server){
      Blocking.get {
        OkHttpBuilder.configure {
           request.uri = "${server}/logicaldoc/services/rest/document/upload".toURI() //this.server.uri
           request.auth.basic 'admin', 'admin' //this.server.username, this.server.password
           request.contentType = 'application/json'
       }.post {
          request.uri.path = '/logicaldoc/services/rest/document/upload'
          request.contentType = 'multipart/form-data'
          request.body = MultipartContent.multipart {
            field 'folderId', '4'
            field 'filename', docFile.name
            field 'language', 'fr'
            part 'filedata', 'filename', 'application/octet-stream', docFile
          }
          request.encoder 'multipart/form-data', OkHttpEncoders.&multipart
        }
        return true
      }
    }

}
