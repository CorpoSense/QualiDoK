package com.corposense.services

import groovy.transform.CompileStatic
import ratpack.exec.Blocking
import ratpack.exec.Promise
import groovyx.net.http.OkHttpBuilder
import groovyx.net.http.OkHttpEncoders
import groovyx.net.http.MultipartContent


@CompileStatic
class UploadService {

    Promise<Boolean> uploadFile(File docFile, String server, String folderId, String language){
      Blocking.get {
        OkHttpBuilder.configure {
           request.uri = "${server}/services/rest/document/upload".toURI() //this.server.uri
           request.auth.basic 'admin', 'admin' //this.server.username, this.server.password
//           request.contentType = 'application/json'
       }.post {
          request.uri.path = '/services/rest/document/upload'
          request.contentType = 'multipart/form-data'
          request.body = MultipartContent.multipart {
            field 'folderId', "${folderId}"
            field 'filename', docFile.name
            field 'language', "${language}"
            part 'filedata', 'filename', 'application/octet-stream', docFile
          }
          request.encoder 'multipart/form-data', OkHttpEncoders.&multipart
        }
        return true
      }
    }

}
